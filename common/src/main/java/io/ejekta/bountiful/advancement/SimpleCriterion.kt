package io.ejekta.bountiful.advancement

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import net.minecraft.advancements.triggers.SimpleCriterionTrigger
import net.minecraft.core.Holder
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition
import net.minecraft.server.level.ServerPlayer
import java.util.*

class SimpleCriterion : SimpleCriterionTrigger<SimpleCriterionTrigger.SimpleInstance>() {

    override fun codec(): Codec<SimpleCriterionTrigger.SimpleInstance> =
        MapCodec.unitCodec(SimpleCriterionTrigger.SimpleInstance { Optional.empty<Holder<LootItemCondition>>() })

    fun trigger(player: ServerPlayer) {
        trigger(player) { true }
    }

}
