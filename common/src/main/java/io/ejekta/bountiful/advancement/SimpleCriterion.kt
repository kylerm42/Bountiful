package io.ejekta.bountiful.advancement

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import net.minecraft.advancements.criterion.ContextAwarePredicate
import net.minecraft.advancements.criterion.SimpleCriterionTrigger
import net.minecraft.server.level.ServerPlayer
import java.util.*

class SimpleCriterion : SimpleCriterionTrigger<SimpleCriterion.Companion.FreeCondition>() {

    override fun codec(): Codec<FreeCondition> = MapCodec.unit(FreeCondition()).codec()

    fun trigger(player: ServerPlayer) {
        trigger(player) { true }
    }

    companion object {
        class FreeCondition : SimpleInstance {
            override fun player(): Optional<ContextAwarePredicate> {
                return Optional.empty()
            }
        }
    }

}