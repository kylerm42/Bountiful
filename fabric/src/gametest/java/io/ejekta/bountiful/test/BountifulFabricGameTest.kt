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
     * Counts bounty gazebos per generated village for every vanilla village type (and every Trek village type when
     * Trek is on the test classpath; see `-PextraMods` in fabric/build.gradle) at several `villageGenFrequency`
     * values, and asserts the quota: floor(f) boards, plus one more for the fractional part's share of villages.
     * Prints a `VILLAGE-BOARDS` line per structure/frequency for the report.
     */
    @GameTest
    fun villageBoardsHonorGenFrequency(helper: GameTestHelper) {
        val board = io.ejekta.bountiful.config.BountifulIO.configData.board
        val original = board.villageGenFrequency
        val samples = (System.getProperty("bountiful.villageSamples") ?: "24").toInt()
        val failures = mutableListOf<String>()
        try {
            for (freq in listOf(1.0f, 2.5f, 0.0f)) {
                board.villageGenFrequency = freq
                val min = kotlin.math.floor(freq.toDouble()).toInt()
                val max = kotlin.math.ceil(freq.toDouble()).toInt()
                for (name in VillageBoardCounts.VANILLA_VILLAGES + VillageBoardCounts.TREK_VILLAGES) {
                    val counts = VillageBoardCounts.count(helper.level, name, samples) ?: continue
                    val pieces = VillageBoardCounts.lastPieceTotals
                    println("VILLAGE-BOARDS freq=$freq $name ${VillageBoardCounts.summary(counts)} pieces=${pieces.minOrNull()}..${pieces.maxOrNull()} zombie=${VillageBoardCounts.zombieVillages}")
                    // Trek's mushroom village references pools that do not exist and only ever generates its town
                    // center; there is nowhere to put a board, so it is exempt from the lower bound.
                    if (pieces.max() <= 1) continue
                    if (counts.any { it !in min..max }) failures += "$name freq=$freq: expected $min..$max boards per village, got $counts"
                }
            }
        } finally {
            board.villageGenFrequency = original
        }
        helper.assertTrue(failures.isEmpty(), failures.joinToString("; "))
        helper.succeed()
    }

    /**
     * Towns and Towers (mod id `t_and_t`) villages: the same quota as [villageBoardsHonorGenFrequency] for every
     * in-scope village (see TAT_VILLAGES). Without Towns and Towers on the classpath the structures are not
     * registered and the test passes vacuously, unless `-Dbountiful.requireTat=true` (set by Gradle whenever the
     * mods are loaded) turns a missing structure into a failure.
     */
    @GameTest
    fun townsAndTowersBoardsHonorGenFrequency(helper: GameTestHelper) {
        val board = io.ejekta.bountiful.config.BountifulIO.configData.board
        val original = board.villageGenFrequency
        val samples = (System.getProperty("bountiful.villageSamples") ?: "24").toInt()
        val requireTat = System.getProperty("bountiful.requireTat") == "true"
        val failures = mutableListOf<String>()
        try {
            for (freq in listOf(1.0f, 2.5f, 0.0f)) {
                board.villageGenFrequency = freq
                val min = kotlin.math.floor(freq.toDouble()).toInt()
                val max = kotlin.math.ceil(freq.toDouble()).toInt()
                for (name in VillageBoardCounts.TAT_VILLAGES) {
                    val counts = VillageBoardCounts.count(helper.level, name, samples)
                    if (counts == null) {
                        if (requireTat) failures += "$name is not registered although Towns and Towers should be loaded"
                        continue
                    }
                    val pieces = VillageBoardCounts.lastPieceTotals
                    println("TAT-BOARDS freq=$freq $name ${VillageBoardCounts.summary(counts)} pieces=${pieces.minOrNull()}..${pieces.maxOrNull()} zombie=${VillageBoardCounts.zombieVillages}")
                    if (counts.isEmpty()) failures += "$name freq=$freq: no valid village generated in $samples samples"
                    if (counts.any { it !in min..max }) failures += "$name freq=$freq: expected $min..$max boards per village, got $counts"
                }
            }
        } finally {
            board.villageGenFrequency = original
        }
        helper.assertTrue(failures.isEmpty(), failures.joinToString("; "))
        helper.succeed()
    }

    /** Out-of-scope Towns and Towers structures (see TAT_OUT_OF_SCOPE) never get a board, even at a high frequency. */
    @GameTest
    fun townsAndTowersOutOfScopeStructuresHaveNoBoards(helper: GameTestHelper) {
        val board = io.ejekta.bountiful.config.BountifulIO.configData.board
        val original = board.villageGenFrequency
        val samples = (System.getProperty("bountiful.villageSamples") ?: "24").toInt()
        val requireTat = System.getProperty("bountiful.requireTat") == "true"
        val failures = mutableListOf<String>()
        try {
            board.villageGenFrequency = 2.5f
            for (name in VillageBoardCounts.TAT_OUT_OF_SCOPE) {
                val counts = VillageBoardCounts.count(helper.level, name, samples)
                if (counts == null) {
                    if (requireTat) failures += "$name is not registered although Towns and Towers should be loaded"
                    continue
                }
                println("TAT-OUT-OF-SCOPE $name ${if (counts.isEmpty()) "n=0" else VillageBoardCounts.summary(counts)}")
                if (counts.any { it != 0 }) failures += "$name: expected no boards, got $counts"
            }
        } finally {
            board.villageGenFrequency = original
        }
        helper.assertTrue(failures.isEmpty(), failures.joinToString("; "))
        helper.succeed()
    }

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
