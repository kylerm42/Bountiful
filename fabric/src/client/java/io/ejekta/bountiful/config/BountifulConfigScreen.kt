package io.ejekta.bountiful.config

import io.ejekta.bountiful.Bountiful
import io.ejekta.bountiful.chaos.ChaosModeOption
import io.ejekta.bountiful.data.PoolEntry
import me.shedaniel.clothconfig2.api.ConfigBuilder
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

/**
 * Cloth Config screen for [BountifulConfigData]. This is the only class that references Cloth Config, and it is only
 * ever loaded through [BountifulModMenu] after checking that Cloth Config is installed (it is an optional dependency).
 */
object BountifulConfigScreen {

    private fun tr(path: String, vararg args: Any): Component = Component.translatable("bountiful.config.$path", *args)

    /** Builds the config screen. [parent] is the screen to return to when it closes. Saved values go to bountiful.json. */
    fun buildScreen(parent: Screen?): Screen {
        val config = BountifulIO.configData

        val builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Component.translatable("bountiful.mod.name"))
            .setSavingRunnable(BountifulIO::reloadConfig)

        val creator = builder.entryBuilder()

        val generalCat = builder.getOrCreateCategory(tr("category.general"))

        generalCat.addEntry(
            creator.startStrList(
                tr("general.excluded_data_paths"),
                config.general.dataPathsToExclude
            ).setDefaultValue {
                BountifulConfigData.GeneralConfigData().dataPathsToExclude
            }.setTooltip(
                tr("general.excluded_data_paths.tooltip")
            ).setSaveConsumer {
                config.general.dataPathsToExclude = it
            }.setAddButtonTooltip(tr("general.excluded_data_paths.add_tooltip"))
                .build()
        )

        val boardCat = builder.getOrCreateCategory(tr("category.general_board"))

        boardCat.addEntry(
            creator.startBooleanToggle(
                tr("board.breakable_boards"),
                config.board.canBreak
            ).setDefaultValue(true).setTooltip(
                tr("board.breakable_boards.tooltip")
            ).setSaveConsumer {
                config.board.canBreak = it
            }.build()
        )

        boardCat.addEntry(
            creator.startBooleanToggle(
                tr("board.global_board_state"),
                config.board.globalBoardState
            ).setDefaultValue(false).setTooltip(
                tr("board.global_board_state.tooltip")
            ).setSaveConsumer {
                config.board.globalBoardState = it
            }.build()
        )

        boardCat.addEntry(
            creator.startIntField(
                tr("board.update_frequency"),
                config.board.updateFrequencySecs
            ).setDefaultValue(45).setMin(1).setTooltip(
                tr("board.update_frequency.tooltip")
            ).setSaveConsumer {
                config.board.updateFrequencySecs = it
            }.build()
        )

        boardCat.addEntry(
            creator.startFloatField(
                tr("board.village_gen_frequency"),
                config.board.villageGenFrequency
            ).setDefaultValue(1.0f)
                .setMin(0.0f)
                .setTooltip(tr("board.village_gen_frequency.tooltip"))
                .setSaveConsumer { config.board.villageGenFrequency = it }
                .requireRestart()
                .build()
        )

        val bountyCat = builder.getOrCreateCategory(tr("category.general_bounty"))

        bountyCat.addEntry(
            creator.startBooleanToggle(
                tr("bounty.expiry_timers"),
                config.bounty.shouldHaveTimersAndExpire
            ).setDefaultValue(true).setTooltip(
                tr("bounty.expiry_timers.tooltip")
            ).setSaveConsumer {
                config.bounty.shouldHaveTimersAndExpire = it
            }.build()
        )

        bountyCat.addEntry(
            creator.startIntSlider(
                tr("bounty.objective_requirement_multiplier"),
                config.bounty.fillerDifficultyModifierPercent,
                -50, 100
            ).setDefaultValue(0).setTooltip(
                tr("bounty.objective_requirement_multiplier.tooltip")
            ).setSaveConsumer {
                config.bounty.fillerDifficultyModifierPercent = it
            }.setTextGetter {
                tr("bounty.objective_requirement_multiplier.value", it)
            }.build()
        )

        bountyCat.addEntry(
            creator.startBooleanToggle(
                tr("bounty.allow_decree_mixing"),
                config.bounty.allowDecreeMixing
            ).setDefaultValue(true).setTooltip(
                tr("bounty.allow_decree_mixing.tooltip")
            ).setSaveConsumer {
                config.bounty.allowDecreeMixing = it
            }.build()
        )

        bountyCat.addEntry(
            creator.startIntSlider(
                tr("bounty.min_number_of_rewards"),
                config.bounty.initialCountPreference.min,
                1, 4
            ).setDefaultValue(1).setTooltip(
                tr("bounty.min_number_of_rewards.tooltip")
            ).setSaveConsumer {
                val current = config.bounty.initialCountPreference
                config.bounty.initialCountPreference = PoolEntry.EntryRange(it, current.max.coerceAtLeast(it))
            }.setTextGetter {
                tr("bounty.number_of_rewards.value", it)
            }.build()
        )

        bountyCat.addEntry(
            creator.startIntSlider(
                tr("bounty.max_number_of_rewards"),
                config.bounty.initialCountPreference.max,
                1, 4
            ).setDefaultValue(2).setTooltip(
                tr("bounty.max_number_of_rewards.tooltip")
            ).setSaveConsumer {
                val current = config.bounty.initialCountPreference
                config.bounty.initialCountPreference = PoolEntry.EntryRange(current.min.coerceAtMost(it), it)
            }.setTextGetter {
                tr("bounty.number_of_rewards.value", it)
            }.build()
        )

        bountyCat.addEntry(
            creator.startIntSlider(
                tr("bounty.min_number_of_objectives"),
                config.bounty.fillerCountPreference.min,
                1, 4
            ).setDefaultValue(1).setTooltip(
                tr("bounty.min_number_of_objectives.tooltip")
            ).setSaveConsumer {
                val current = config.bounty.fillerCountPreference
                config.bounty.fillerCountPreference = PoolEntry.EntryRange(it, current.max.coerceAtLeast(it))
            }.setTextGetter {
                tr("bounty.number_of_objectives.value", it)
            }.build()
        )

        bountyCat.addEntry(
            creator.startIntSlider(
                tr("bounty.max_number_of_objectives"),
                config.bounty.fillerCountPreference.max,
                1, 4
            ).setDefaultValue(2).setTooltip(
                tr("bounty.max_number_of_objectives.tooltip")
            ).setSaveConsumer {
                val current = config.bounty.fillerCountPreference
                config.bounty.fillerCountPreference = PoolEntry.EntryRange(current.min.coerceAtMost(it), it)
            }.setTextGetter {
                tr("bounty.number_of_objectives.value", it)
            }.build()
        )

        bountyCat.addEntry(
            creator.startStrField(
                tr("bounty.filler_currency_pool"),
                config.bounty.fillerCurrencyPool ?: ""
            ).setDefaultValue("").setTooltip(
                tr("bounty.filler_currency_pool.tooltip")
            ).setSaveConsumer {
                config.bounty.fillerCurrencyPool = it.trim().ifEmpty { null }
            }.build()
        )

        bountyCat.addEntry(
            creator.startBooleanToggle(
                tr("bounty.reverse_entry_matching_algorithm"),
                config.bounty.reverseMatchingAlgorithm
            ).setDefaultValue(false).setTooltip(
                tr("bounty.reverse_entry_matching_algorithm.tooltip")
            ).setSaveConsumer {
                config.bounty.reverseMatchingAlgorithm = it
            }.build()
        )

        bountyCat.addEntry(
            creator.startIntSlider(
                tr("bounty.bonus_time"),
                config.bounty.flatBonusTimePerBountyInSecs,
                0, 3600
            ).setDefaultValue(0).setTooltip(
                tr("bounty.bonus_time.tooltip")
            ).setSaveConsumer {
                config.bounty.flatBonusTimePerBountyInSecs = it
            }.build()
        )

        val clientCat = builder.getOrCreateCategory(tr("category.client"))

        clientCat.addEntry(
            creator.startBooleanToggle(
                tr("client.completion_toast_messages"),
                config.client.showCompletionToast
            ).setDefaultValue(true).setTooltip(
                tr("client.completion_toast_messages.tooltip")
            ).setSaveConsumer {
                config.client.showCompletionToast = it
            }.build()
        )

        val chaosCat = builder.getOrCreateCategory(tr("category.chaos"))

        chaosCat.addEntry(
            creator.startEnumSelector(
                tr("chaos.mode"),
                ChaosModeOption::class.java,
                config.chaos.mode
            ).setDefaultValue(ChaosModeOption.OFF).setEnumNameProvider {
                tr("chaos.mode.${(it as ChaosModeOption).name.lowercase()}")
            }.setTooltip(
                tr("chaos.mode.tooltip_off"),
                tr("chaos.mode.tooltip_on"),
                tr("chaos.mode.tooltip_combined"),
                tr("chaos.mode.tooltip_prices"),
                tr("chaos.mode.tooltip_restart")
            ).setSaveConsumer {
                config.chaos.mode = it
            }.build()
        )

        val debugCat = builder.getOrCreateCategory(tr("category.debug"))

        debugCat.addEntry(
            creator.startBooleanToggle(
                tr("debug.pack_mode"),
                Bountiful.packMode
            ).setDefaultValue(false).setTooltip(
                tr("debug.pack_mode.tooltip")
            ).setSaveConsumer {
                Bountiful.packMode = it
            }.build()
        )

        return builder.build()
    }
}
