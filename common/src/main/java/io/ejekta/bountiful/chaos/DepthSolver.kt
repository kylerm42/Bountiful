package io.ejekta.bountiful.chaos

import io.ejekta.bountiful.Bountiful
import io.ejekta.bountiful.bounty.BountyRarity
import io.ejekta.bountiful.content.BountifulContent
import io.ejekta.bountiful.data.Decree
import io.ejekta.bountiful.data.Pool
import io.ejekta.bountiful.data.PoolEntry
import io.ejekta.bountiful.util.getNullable
import io.ejekta.kambrik.ext.id
import kotlinx.serialization.Serializable
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Rarity
import net.minecraft.world.item.crafting.RecipeHolder
import net.minecraft.world.item.crafting.RecipeManager
import net.minecraft.world.item.crafting.display.SlotDisplayContext
import java.util.*
import kotlin.jvm.optionals.getOrNull

class DepthSolver(val server: MinecraftServer, val data: BountifulChaosData, val info: BountifulChaosInfo) {

    private val recipeManager: RecipeManager = server.recipeManager
    private val regManager = server.registryAccess()

    private val terminators = mutableSetOf<Identifier>()
    private val deps = mutableMapOf<Identifier, MutableSet<Identifier>>()

    // Final cost map
    private val costMap = mutableMapOf<Item, Double>()
    private val pathLenMap = mutableMapOf<Item, Int>()
    private val matchCosts = mutableMapOf<Item, Double>()
    private val ignoreSet = mutableSetOf<Item>()
    // Items confirmed unsolvable; repeat visits return null immediately
    private val unresolvableCache = mutableSetOf<Item>()

    fun costOf(item: Item): Double? {
        return costMap[item] ?: matchCosts[item]
    }

    // Populate cost map from config
    init {
        Bountiful.LOGGER.debug("Init solve system..")


        for (item in BuiltInRegistries.ITEM) {
            if (data.matching.isIgnored(item)) {
                ignoreSet.add(item)
                continue
            }
            val matchCost = data.matching.matchCost(item, server)
            if (matchCost != null) {
                Bountiful.LOGGER.debug("Adding match cost: ${item.id}")
                matchCosts[item] = matchCost
            }
        }

        // Priced entries in `optional` act as fixed prices too; `required` wins when both are priced
        for (prices in listOf(data.required, data.optional)) {
            for ((itemId, price) in prices) {
                if (price == null) continue
                val item = BuiltInRegistries.ITEM.getOptional(itemId).getOrNull() ?: continue
                if (item !in matchCosts && item !in costMap) {
                    costMap[item] = price
                }
            }
        }
    }

    private val ItemStack.recipes: List<RecipeHolder<*>>
        get() = stackLookup[this.item] ?: emptyList()

    private val RecipeHolder<*>.inItemSets: List<Set<ItemStack>>
        get() = this.value.placementInfo().ingredients().map { ingredient ->
            ingredient.items().toList().sortedBy { holder -> holder.value().id.toString() }.map { holder -> ItemStack(holder.value()) }.toSet()
        }

    private val displayContext = SlotDisplayContext.fromLevel(server.overworld())

    private fun RecipeHolder<*>.resultStack(): ItemStack? {
        val display = value.display().firstOrNull() ?: return null
        val stack = display.result().resolveForFirstStack(displayContext)
        return stack.takeUnless { it.isEmpty }
    }

    // Recipes are sorted by id so cycle-dependent solve results (see unresolvableCache) do not vary between runs
    private val stackLookup = recipeManager.recipes.toList().sortedBy { it.id().identifier().toString() }.mapNotNull { holder ->
        val resultStack = holder.resultStack() ?: return@mapNotNull null
        resultStack.item to holder
    }.groupBy({ it.first }, { it.second })

    /**
     * Result of pricing one recipe's ingredient slots.
     * @param cost summed cost of the slots that were solved
     * @param unsolved number of slots for which no option could be priced; a recipe is only usable when this is 0
     */
    internal data class IngredientPricing(val cost: Double, val unsolved: Int)

    @JvmRecord
    data class SolveResult(val amt: Double, val path: List<ItemStack>)

    // Attempts to solve for stack cost.
    fun solveFor(stack: ItemStack, path: List<ItemStack>): SolveResult? {
        if (stack.item in unresolvableCache) return null

        val recipes = stack.recipes

        // If no recipe exists, it is a terminator.
        if (recipes.isEmpty()) {
            terminators.add(stack.id!!)
            // Add dependency on said item
            for (pathItem in path) {
                deps.getOrPut(stack.id!!) { mutableSetOf() }.add(pathItem.id!!)
            }
            return null
        }

        val recipeCosts = mutableListOf<Double>()

        for (recipe in recipes) {
            //val inputCounts = recipe.inItemSets.flatten().groupBy { it.item }.map { it.key to it.value.sumOf { stack -> stack.count } }.toMap()

            val inputSets = recipe.inItemSets

            // Currently grabs the first solved option per slot; this assumes tag ingredients all have the same cost.
            val pricing = priceIngredients(inputSets) { option ->
                if (option.item in path.map { it.item } || path.size > 24) { // Cyclic dependency or too much recursion!
                    null
                } else {
                    // Check for already calculated cost (O(1)), otherwise try to iteratively find one
                    val perItem = costOf(option.item) ?: solveFor(option, path + stack)?.amt
                    perItem?.let { it * option.count }
                }
            }

            var ingredientRunningCost = pricing.cost
            val numUnsolvedIngredients = pricing.unsolved

            // If a recipe makes 3 of something, the actual cost is only 1/3 as much
            val resultCount = recipe.resultStack()?.count ?: 1
            ingredientRunningCost /= resultCount

            if (numUnsolvedIngredients > 0) {
                //println("Could not solve for ${stack.identifier}".padStart(padding))
            } else {
                //println("Resolved all ingredients for: ${stack.identifier}".padStart(padding))
                recipeCosts.add(ingredientRunningCost)
                //println("Cost was: $ingredientRunningCost".padStart(padding))
            }

        }

        val finalCost = recipeCosts.minOrNull()

        // Of all calculated recipe costs, find the minimum
        if (finalCost != null) {
            costMap[stack.item] = finalCost
            pathLenMap[stack.item] = path.size // may not accurately reflect path len?
        } else {
            unresolvableCache.add(stack.item)
        }

        return finalCost?.let { SolveResult(it, path) }
    }

    fun solveRequiredRecipes() {
        var unsolved = 0
        val unsolvedList = mutableListOf<Item>()
        val allItemSet = mutableSetOf<Item>()
        for (item in BuiltInRegistries.ITEM) {
            allItemSet.add(item)
            // If there exists a recipe for it, solve it
            if (item in stackLookup.keys) {
                val didSolve = solveFor(ItemStack(item), emptyList())
                if (didSolve == null && data.required[item.id!!] == null) {
                    unsolved += 1
                    unsolvedList.add(item)
                }
            } else {
                unsolved += 1
                unsolvedList.add(item)
            }
        }

        info.redundant = matchCosts.keys.map { it.id!! }.sorted().toMutableList()
        info.unsolvedList = (allItemSet.filter { !data.matching.isIgnored(it) }.toSet() - matchCosts.keys - costMap.keys).map { it.id!! }.toList().sorted()

        //info.unsolvedList = unsolvedList.map { it.id!! }.sorted() - info.redundant
    }

    fun syncConfig() {

        Bountiful.LOGGER.debug("Syncing Chaos Config...")


        val ignoredIds = ignoreSet.map { it.id!! }.toSet()

        // Remove redundant required items, and unpriced ones that are ignored (priced ones may still feed recipes)
        for ((id, amt) in data.required.toList()) {
            if (id in info.redundant || (amt == null && id in ignoredIds)) {
                data.required.remove(id)
            }
        }

        Bountiful.LOGGER.debug("Terminators:")
        // Insert terminators into required prop
        for (line in terminators.sorted() - info.redundant.toSet() - ignoredIds) {
            data.required[line] = costMap[BuiltInRegistries.ITEM.getNullable(line)]
            Bountiful.LOGGER.debug(line)
        }
        // Reset JSON file ordering (this is a bit hacky)
        data.required = data.required.toList().sortedBy { it.first.toString() }.toMap().toMutableMap()
        // Update dependency numbering
        //info.deps = deps.map { it.key to it.value.size }.sortedBy { -it.second }.toMap().toMutableMap()
        info.deps = deps.map { it.key to it.value }.toMap().toSortedMap()

        for (item in BuiltInRegistries.ITEM.sortedBy { it.id }) {
            val isIgnored = data.matching.isIgnored(item)
            if (costOf(item) == null && item.id !in info.redundant && !isIgnored) {
                data.optional[item.id!!] = null
            } else if (data.optional[item.id] == null || item.id in info.redundant || isIgnored) {
                // Priced optional entries are kept: they are the user's fixed prices for items nothing else can price
                data.optional.remove(item.id)
            }
        }
        data.optional = data.optional.toList().sortedBy { it.first.toString() }.toMap().toMutableMap()

    }

    /** Registers the chaos pool and returns how many priced items it holds. */
    fun sendToRegistries(combined: Boolean = false): Int {
        Bountiful.LOGGER.debug("Sending chaos data to Bountiful registries (combined=$combined)..")

        if (!combined) {
            BountifulContent.populatePools(emptyList())
            BountifulContent.Decrees.clear()
        }

        val poolId = "chaos"
        val pool = Pool(poolId)

        for (item in BuiltInRegistries.ITEM.sortedBy { it.id }) {
            Bountiful.LOGGER.debug("Item: ${item.id.toString().padEnd(50)} - ${costOf(item).toString().padEnd(16)} - ${pathLenMap[item]}")
            if (item in ignoreSet) continue
            val realCost = costOf(item) ?: continue
            val stack = ItemStack(item)
            val realAmtMax = stack.maxStackSize
            var realRarity = when (stack.rarity) {
                Rarity.UNCOMMON -> BountyRarity.RARE
                Rarity.RARE -> BountyRarity.EPIC
                Rarity.EPIC -> BountyRarity.LEGENDARY
                else -> BountyRarity.COMMON
            }

            if (stack.rarity == Rarity.COMMON && stack.maxStackSize == 1) {
                realRarity = BountyRarity.UNCOMMON
            }

            val poolEntry = PoolEntry.create().apply {
                content = stack.id.toString()
                amount = PoolEntry.EntryRange(1, realAmtMax)
                unitWorth = realCost
                rarity = realRarity
                id = UUID.randomUUID().toString()
            }

            pool.items.add(poolEntry)
        }

        pool.setup(poolId)

        val decree = Decree(poolId, mutableSetOf(poolId), mutableSetOf(poolId))
        BountifulContent.Decrees.add(decree)
        BountifulContent.populatePools(BountifulContent.Pools + listOf(pool))
        Bountiful.LOGGER.info("Chaos pool has ${pool.items.size} items (${ignoreSet.size} ignored items skipped); ${BountifulContent.Pools.size} pools and ${BountifulContent.Decrees.size} decrees registered (combined=$combined)")
        return pool.items.size
    }

    companion object {
        /**
         * Prices each ingredient slot by taking the first option that [priceOf] can resolve.
         * The unsolved counter starts at the number of slots, so a slot with no resolvable option
         * (including an empty option set) leaves the recipe unsolved instead of pricing it as free.
         */
        internal fun <T> priceIngredients(optionSets: List<Collection<T>>, priceOf: (T) -> Double?): IngredientPricing {
            var cost = 0.0
            var unsolved = optionSets.size
            for (optionSet in optionSets) {
                for (option in optionSet) {
                    val price = priceOf(option) ?: continue
                    cost += price
                    unsolved -= 1
                    break
                }
            }
            return IngredientPricing(cost, unsolved)
        }
    }

}
