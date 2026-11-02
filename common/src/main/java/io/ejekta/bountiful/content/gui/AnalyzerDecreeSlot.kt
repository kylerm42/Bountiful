package io.ejekta.bountiful.content.gui

import io.ejekta.bountiful.content.BountifulContent
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

// Changes to this slot are picked up by the AnalyzerScreen itself, which compares the slot contents every frame.
class AnalyzerDecreeSlot(inv: SimpleContainer, val usingPlayer: Player, index: Int, x: Int, y: Int) : Slot(inv, index, x, y) {
    override fun mayPlace(pStack: ItemStack) = pStack.item == BountifulContent.DECREE_ITEM

    override fun mayPickup(pPlayer: Player) = true
}
