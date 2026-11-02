package io.ejekta.bountiful.content.villager

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import io.ejekta.bountiful.decree.DecreeSpawnCondition
import io.ejekta.bountiful.decree.DecreeSpawnRank
import io.ejekta.bountiful.content.item.DecreeItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.storage.loot.LootContext
import net.minecraft.world.level.storage.loot.functions.LootItemFunction

/**
 * Loot function (`bountiful:random_decree`) that replaces the given stack with a decree containing
 * [count] randomly picked decrees that may be bought from a wandering trader.
 *
 * Used by the data-driven wandering trader trades in `data/bountiful/villager_trade/wandering_trader`.
 */
class RandomDecreeFunction(val count: Int) : LootItemFunction {

    override fun codec(): MapCodec<out LootItemFunction> = CODEC

    override fun apply(stack: ItemStack, context: LootContext): ItemStack {
        return DecreeItem.create(DecreeSpawnCondition.WANDERING_TRADER, count, DecreeSpawnRank.CONSTANT)
    }

    companion object {
        val CODEC: MapCodec<RandomDecreeFunction> = RecordCodecBuilder.mapCodec { inst ->
            inst.group(
                Codec.intRange(1, 16).fieldOf("count").forGetter { it.count }
            ).apply(inst, ::RandomDecreeFunction)
        }
    }
}
