package io.ejekta.bountiful.test

import io.ejekta.bountiful.bounty.BountyRarity
import io.ejekta.bountiful.bounty.types.BountyTypeRegistry
import io.ejekta.bountiful.client.AnalyzerScreen
import io.ejekta.bountiful.client.BoardScreen
import io.ejekta.bountiful.client.EditorScreen
import io.ejekta.bountiful.components.BountyDataEntry
import io.ejekta.bountiful.components.BountyInfo
import io.ejekta.bountiful.components.BountyStack
import io.ejekta.bountiful.content.BountifulContent
import io.ejekta.bountiful.content.board.BoardInventory
import io.ejekta.bountiful.content.gui.AnalyzerScreenHandler
import io.ejekta.bountiful.content.gui.BoardScreenHandler
import io.ejekta.bountiful.content.gui.EditorScreenHandler
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.SimpleContainerData
import net.minecraft.world.item.ItemStack

/**
 * Client game test (Fabric API client gametest framework). It opens the real [BoardScreen] on a client-side
 * [BoardInventory] holding entity, item and item-tag objectives, renders it for several frames (including a hover
 * frame) and takes screenshots. A render crash (e.g. an entity without an id) fails the run.
 *
 * Run with `./gradlew :fabric:runClientGameTest` (needs a display; screenshots land in
 * `fabric/build/run/clientGameTest/screenshots`).
 */
class BountifulClientGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.worldBuilder().create().use {
            context.waitTicks(20)
            context.runOnClient<RuntimeException> { mc -> openBoard(mc) }
            context.waitForScreen(BoardScreen::class.java)
            // Several frames: entity previews are created on the first frame and re-rendered afterwards
            context.waitTicks(10)
            context.takeScreenshot("board_screen")

            // Hover the first bounty so the objective tooltip path renders too
            // (cursor positions are in window pixels; the screen lays out in GUI-scaled pixels)
            val cursor = context.computeOnClient<DoubleArray, RuntimeException> { mc ->
                val screen = mc.gui.screen()!!
                val scale = mc.window.guiScale.toDouble()
                doubleArrayOf(((screen.width - 348) / 2 + 50) * scale, ((screen.height - 165) / 2 + 28) * scale)
            }
            context.input.setCursorPos(cursor[0], cursor[1])
            context.waitTicks(5)
            context.takeScreenshot("board_screen_hover")

            // The other client screens were never rendered before the 26.x port; render each for a few frames
            context.runOnClient<RuntimeException> { mc ->
                val inv = mc.player!!.inventory
                mc.gui.setScreen(AnalyzerScreen(AnalyzerScreenHandler(1, inv), inv, Component.literal("Analyzer")))
            }
            context.waitForScreen(AnalyzerScreen::class.java)
            context.waitTicks(5)
            context.takeScreenshot("analyzer_screen")

            context.runOnClient<RuntimeException> { mc ->
                val inv = mc.player!!.inventory
                mc.gui.setScreen(EditorScreen(EditorScreenHandler(2, inv), inv, Component.literal("Editor")))
            }
            context.waitForScreen(EditorScreen::class.java)
            context.waitTicks(5)
            context.takeScreenshot("editor_screen")
        }
    }

    private fun openBoard(mc: Minecraft) {
        val player = mc.player!!
        val now = mc.level!!.gameTime
        val inventory = BoardInventory(BlockPos.ZERO)
        val bounties = listOf(
            bounty(now, objective("entity", "minecraft:spider", BountyTypeRegistry.ENTITY.id.toString(), 3)),
            bounty(now, objective("entity", "minecraft:zombie", BountyTypeRegistry.ENTITY.id.toString(), 2)),
            bounty(now, objective("item", "minecraft:wheat", BountyTypeRegistry.ITEM.id.toString(), 16)),
            bounty(now, objective("tag", "minecraft:logs", BountyTypeRegistry.ITEM_TAG.id.toString(), 8))
        )
        bounties.forEachIndexed { index, stack -> inventory.setItem(index, stack) }
        val handler = BoardScreenHandler(0, player.inventory, inventory, SimpleContainerData(1))
        mc.gui.setScreen(BoardScreen(handler, player.inventory, Component.literal("Bounty Board")))
    }

    private fun objective(name: String, content: String, logic: String, amount: Int) = BountyDataEntry(
        id = "client_test.$name",
        content = content,
        rarity = BountyRarity.COMMON,
        logicName = logic,
        amount = amount
    )

    private fun bounty(now: Long, objective: BountyDataEntry): ItemStack {
        val stack = ItemStack(BountifulContent.BOUNTY_ITEM)
        BountyStack(stack).apply {
            objs = listOf(objective)
            rews = listOf(
                BountyDataEntry(
                    id = "client_test.reward",
                    content = "minecraft:emerald",
                    rarity = BountyRarity.RARE,
                    logicName = BountyTypeRegistry.ITEM.id.toString(),
                    amount = 2
                )
            )
            info = BountyInfo(
                rarity = BountyRarity.COMMON,
                timeStarted = now - 20L,
                timeToComplete = 6000L,
                timePickedUp = now - 20L
            )
        }
        return stack
    }
}
