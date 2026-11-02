package io.ejekta.bountiful.test

import net.fabricmc.fabric.api.gametest.v1.GameTest
import io.ejekta.bountiful.content.item.DecreeItem
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.tags.VillagerTradeTags
import net.minecraft.util.Unit
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.level.storage.loot.LootContext
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
import java.util.Optional

// Fabric API 0.165 (26.3): game tests are plain public methods taking a GameTestHelper, annotated with Fabric's
// own @GameTest (which defaults to the empty structure). The class is registered as a `fabric-gametest` entrypoint.
class BountifulFabricGameTest {

    @GameTest
    fun generatedBountyHasObjectivesAndRewards(helper: GameTestHelper) = BountifulGameTestCases.generatedBountyHasObjectivesAndRewards(helper)

    @GameTest
    fun boardBootstrapPopulatesDecreesAndBounties(helper: GameTestHelper) = BountifulGameTestCases.boardBootstrapPopulatesDecreesAndBounties(helper)

    @GameTest
    fun completionFlowSchedulesVillager(helper: GameTestHelper) = BountifulGameTestCases.completionFlowSchedulesVillager(helper)

    @GameTest
    fun entityKillProgressAdvancesMatchingBounty(helper: GameTestHelper) = BountifulGameTestCases.entityKillProgressAdvancesMatchingBounty(helper)

    @GameTest
    fun currencyGreedyModeUsesLargestRepresentableCurrency(helper: GameTestHelper) = BountifulGameTestCases.currencyGreedyModeUsesLargestRepresentableCurrency(helper)

    @GameTest
    fun serializationRoundTripPreservesBoardState(helper: GameTestHelper) = BountifulGameTestCases.serializationRoundTripPreservesBoardState(helper)

    @GameTest
    fun maskedPlayersDoNotSeeTakenBounties(helper: GameTestHelper) = BountifulGameTestCases.maskedPlayersDoNotSeeTakenBounties(helper)

    @GameTest
    fun takenBountyIsNotOfferedAgain(helper: GameTestHelper) = BountifulGameTestCases.takenBountyIsNotOfferedAgain(helper)

    @GameTest
    fun bountyAndDecreeComponentsCanBeHashedForContainerClicks(helper: GameTestHelper) = BountifulGameTestCases.bountyAndDecreeComponentsCanBeHashedForContainerClicks(helper)

    @GameTest
    fun quickMovedBountyIsNotOfferedAgain(helper: GameTestHelper) = BountifulGameTestCases.quickMovedBountyIsNotOfferedAgain(helper)

    /**
     * Wandering trader offers are generated lazily and randomly, so instead of waiting for a trader that happens
     * to roll a decree this checks the data-driven decree trades directly: all four tiers are in the trader's
     * common trade tag and each one produces a decree with the tier's count.
     */
    @GameTest
    fun wanderingTraderDecreeTradesProduceDecrees(helper: GameTestHelper) {
        val trades = helper.level.registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE)
        val common = trades.getOrThrow(VillagerTradeTags.WANDERING_TRADER_COMMON)
        val decreeTrades = common.filter { it.unwrapKey().get().identifier().path.startsWith("wandering_trader/decree_") }
        helper.assertValueEqual(decreeTrades.size, 4, "decree trades in #minecraft:wandering_trader/common")

        val trader = helper.spawn(EntityTypes.WANDERING_TRADER, BlockPos(2, 1, 2))
        val context = LootContext.Builder(
            LootParams.Builder(helper.level)
                .withParameter(LootContextParams.ORIGIN, trader.position())
                .withParameter(LootContextParams.THIS_ENTITY, trader)
                .withParameter(LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED, Unit.INSTANCE)
                .create(LootContextParamSets.VILLAGER_TRADE)
        ).create(Optional.empty())

        for (holder in decreeTrades) {
            val offer = holder.value().getOffer(context)
            helper.assertTrue(offer != null, "Decree trade ${holder.unwrapKey().get().identifier()} should produce an offer")
            helper.assertTrue(offer!!.result.item is DecreeItem, "Decree trade should sell a decree")
        }

        helper.succeed()
    }
}
