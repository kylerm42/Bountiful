package io.ejekta.bountiful.bounty

import net.minecraft.ChatFormatting
import net.minecraft.world.scores.TeamColor
import kotlin.math.max
import kotlin.math.pow

enum class BountyRarity(val color: ChatFormatting, val weight: Int, val repTier: Int) {
    COMMON(ChatFormatting.WHITE, 1024, -30),
    UNCOMMON(ChatFormatting.AQUA, 512, 5),
    RARE(ChatFormatting.YELLOW, 256, 15),
    EPIC(ChatFormatting.LIGHT_PURPLE, 128, 25),
    LEGENDARY(ChatFormatting.GOLD, 6, 30);

    /** RGB value of [color]. ChatFormatting no longer carries a color value as of 26.3, so look it up via TeamColor. */
    val rgb: Int = checkNotNull(TeamColor.byName(color.name.lowercase())) { "No team color for $color" }.textColor().value

    private fun weightAdjustedFor(currRarity: BountyRarity): Double {
        return weight.toDouble() / (rarityWeightScaling.pow(max(currRarity.ordinal - ordinal, 0)))
    }

    fun weightAdjustedFor(rep: Int) = weightAdjustedFor(forReputation(rep))

    companion object {
        fun forReputation(rep: Int): BountyRarity {
            return entries.last { rep >= it.repTier  }
        }

        const val rarityWeightScaling = 2.25
    }
}