package io.ejekta.bountiful.client

import io.ejekta.bountiful.Bountiful
import io.ejekta.bountiful.client.widgets.AnalyzerPoolWidget
import io.ejekta.bountiful.content.BountifulContent
import io.ejekta.bountiful.content.gui.AnalyzerScreenHandler
import io.ejekta.kambrik.gui.draw.KGui
import io.ejekta.kambrik.gui.draw.reactor.MouseReactor
import io.ejekta.kambrik.gui.draw.widgets.KScrollbarVertical
import io.ejekta.kambrik.gui.screen.KambrikContainerScreen
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.ItemStack
import kotlin.math.roundToInt


class AnalyzerScreen(handler: AbstractContainerMenu, inventory: Inventory, title: Component) : KambrikContainerScreen<AbstractContainerMenu>(
    handler, inventory, title
) {

    private var scanResolution = 1

    private val guiWidth = 177
    private val guiHeight = 167

    private val bgGui = kambrikGui {
        img(TEXTURE, guiWidth, guiHeight)
    }

    private var poolWidgets = listOf<AnalyzerPoolWidget>()

    private var statusMessage: Component? = Component.translatable("bountiful.analyzer.place_decree")

    // The decree slot can change through several menu paths (clicks, quick moves, server syncs), so the screen
    // compares the slot contents each frame instead of relying on one specific Slot method being called
    private var lastSeenStack: ItemStack = ItemStack.EMPTY

    /** Number of pool charts currently shown. */
    fun chartCount() = poolWidgets.size

    fun toggleMode() {
        showMode = Mode.entries[(showMode.ordinal + 1) % Mode.entries.size]
        refreshWidgets()
    }

    enum class Mode(val symbol: String) {
        OBJ("O"),
        REW("R")
    }

    var showMode = Mode.OBJ

    val modeClicker = MouseReactor().apply {
        onClickDown = { relX: Int, relY: Int, button: Int ->
            toggleMode()
        }
    }

    fun refreshWidgets() {
        Bountiful.LOGGER.debug("Refreshing analyzer widgets")

        val analyzerMenu = (menu as? AnalyzerScreenHandler) ?: return

        val slotStack = analyzerMenu.container.getItem(0)
        lastSeenStack = slotStack.copy()

        val decreeIds = slotStack[BountifulContent.DECREE_DATA]?.ids
        if (decreeIds.isNullOrEmpty()) {
            // Nothing (or a decree that has not been set yet) in the slot: no charts
            poolWidgets = emptyList()
            statusMessage = Component.translatable(
                if (slotStack.isEmpty) "bountiful.analyzer.place_decree" else "bountiful.decree.notset"
            )
            return
        }

        val decrees = decreeIds.mapNotNull { id -> BountifulContent.Decrees.find { d -> d.id == id } }

        val allPools = decrees.map {
            if (showMode == Mode.OBJ) it.objectivePools else it.rewardPools
        }.flatten().toSet().toList()
        statusMessage = if (allPools.isEmpty()) Component.translatable("bountiful.analyzer.no_pools") else null

        // Scale every chart to the most valuable entry of the pools being displayed
        val maxWorth = allPools.maxOfOrNull { pool ->
            pool.items.maxOfOrNull { it.amount.max * it.unitWorth } ?: 0.0
        }?.takeIf { it > 0.0 } ?: 1.0

        poolWidgets = allPools.map {
            AnalyzerPoolWidget(it, maxWorth, scanResolution, 64 / allPools.size)
        }.sortedBy {
            it.pool.id
        }
    }

    // 18x43
    private val scroller = KScrollbarVertical(43, 18, 8, SCROLLER, 0x0)

    private fun drawGui(): KGui {
        return kambrikGui {

            area(guiWidth, guiHeight) {
                text(7, 6) {
                    add(Component.translatable("bountiful.analyzer.title"))
                }

                area(152, 6, 18, 9) {
                    rect(0x0)
                    reactWith(modeClicker)
                    offset(9, 1) {
                        textCentered {
                            addLiteral(showMode.symbol)
                        }
                    }
                    onHover {
                        tooltip {
                            add(Component.translatable("bountiful.analyzer.mode.tooltip", showMode.symbol))
                        }
                    }
                }

                statusMessage?.let { message ->
                    text(12, 22) {
                        add(message)
                    }
                }

                offset(9, 17) {
                    for (i in poolWidgets.indices) {
                        val currWid = poolWidgets[i]
                        offset(0, i * currWid.height) {
                            widget(currWid)
                        }
                    }
                }

                offset(152, 38) {
                    area(18, 43) {
                        //reactWith(mouseReact)
                        widget(scroller)
                        if (!scroller.reactor.isDragging) {
                            onHover {
                                tooltip { add(Component.translatable("bountiful.analyzer.sample_resolution")) }
                            }
                        } else {
                            val resolutions = listOf(1, 2, 3, 4, 5, 6, 8, 10, 12, 15)
                            val resIndex = (scroller.percent * (resolutions.size - 1)).roundToInt() + 1
                            if (scanResolution != resIndex) {
                                scanResolution = resIndex
                                refreshWidgets()
                            }
                        }
                    }
                }
            }
        }
    }

    private val fgGui = drawGui()

    override fun onDrawBackground(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        bgGui.draw(context, mouseX, mouseY, delta)
    }

    override fun onDrawForeground(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val currentStack = (menu as? AnalyzerScreenHandler)?.container?.getItem(0) ?: ItemStack.EMPTY
        if (!ItemStack.matches(currentStack, lastSeenStack)) {
            refreshWidgets()
        }
        fgGui.draw(context, mouseX, mouseY, delta)
    }

    override fun init() {
        super.init()
    }

    companion object {
        private val TEXTURE = Bountiful.id("analyzer_bg")
        private val SCROLLER = Bountiful.id("analyzer_scroller")
//        private val SELECTOR = Bountiful.id("selector")
//        private val SCROLLER = ResourceLocation.parse("container/villager/scroller")
//        private val XP_FG = ResourceLocation.parse("container/villager/experience_bar_current")
//        private val XP_BG = ResourceLocation.parse("container/villager/experience_bar_background")
    }
}

