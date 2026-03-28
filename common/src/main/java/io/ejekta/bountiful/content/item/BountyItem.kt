package io.ejekta.bountiful.content.item

import io.ejekta.bountiful.bounty.BountyRarity
import io.ejekta.bountiful.components.BountyStack
import io.ejekta.bountiful.config.BountifulIO
import io.ejekta.kambrik.bridge.Kambridge
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.component.TooltipDisplay
import java.util.*
import java.util.function.Consumer

class BountyItem(props: Properties) : Item(
    props.stacksTo(1).fireResistant()
) {

    override fun getName(stack: ItemStack): Component {
        if (Kambridge.isOnServer()) {
            return Component.translatable("bountiful.bounty")
        }
        val info = BountyStack(stack).info
        var text = Component.translatable(info.rarity.name.lowercase()
            // Capitalizing
            .replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            } + " Bounty ").withStyle(info.rarity.color)
        if (info.rarity == BountyRarity.LEGENDARY) {
            text = text.withStyle(ChatFormatting.BOLD)
        }
        if (BountifulIO.configData.bounty.shouldHaveTimersAndExpire) {
            text = text.append(
                Component.literal("(")
                    .append(info.formattedTimeLeft(Minecraft.getInstance().level!!))
                    .append(Component.literal(")"))
                    .withStyle(ChatFormatting.WHITE)
            )
        }
        return text
    }

    fun tryCashIn(player: Player, stack: ItemStack): Boolean {
        return BountyStack(stack).tryCashIn(player)
    }

    override fun appendHoverText(
        pStack: ItemStack,
        pContext: TooltipContext,
        pTooltipDisplay: TooltipDisplay,
        pTooltipComponents: Consumer<Component>,
        pTooltipFlag: TooltipFlag
    ) {
        if (Kambridge.isOnServer()) {
            return
        }
        val tips = BountyStack(pStack).genTooltip(Kambridge.isOnServer(), pTooltipFlag)
        tips.forEach(pTooltipComponents)
        super.appendHoverText(pStack, pContext, pTooltipDisplay, pTooltipComponents, pTooltipFlag)
    }

}