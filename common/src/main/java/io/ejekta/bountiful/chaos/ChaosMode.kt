package io.ejekta.bountiful.chaos

import io.ejekta.bountiful.Bountiful
import io.ejekta.bountiful.config.BountifulIO.rootFolder
import io.ejekta.bountiful.config.JsonFormats
import io.ejekta.kambrikx.file.KambrikConfigFile
import io.ejekta.kambrikx.file.KambrikParseFailMode
import net.minecraft.server.MinecraftServer

class ChaosMode {

    private val chaosFile = KambrikConfigFile(
        rootFolder,
        "bountiful-chaos.json",
        JsonFormats.Config.json,
        KambrikParseFailMode.LEAVE,
        BountifulChaosData.serializer()
    ) { BountifulChaosData() }

    private val chaosFileInfo = KambrikConfigFile(
        rootFolder,
        "bountiful-chaos-info.json",
        JsonFormats.Config.json,
        KambrikParseFailMode.LEAVE,
        BountifulChaosInfo.serializer()
    ) { BountifulChaosInfo() }

    private var chaosData = chaosFile.read()
    private var chaosInfo = chaosFileInfo.read()

    fun inject(server: MinecraftServer, combined: Boolean = false) {
        Bountiful.LOGGER.info("Injecting chaos into Bountiful (combined=$combined)...")
        chaosData = chaosFile.read()
        chaosInfo = chaosFileInfo.read()

        // Seeds a fresh file (read() creates it empty) and repairs files that only hold null prices
        val merged = ChaosDefaults.mergeDefaults(chaosData, ChaosDefaults.load())
        Bountiful.LOGGER.info(
            "Chaos: merged ${merged.required} required and ${merged.optional} optional prices, " +
                "${merged.tags} tag, ${merged.regex} regex and ${merged.ignoreRegex} ignore rules from the bundled defaults"
        )

        val solver = DepthSolver(server, chaosData, chaosInfo)
        solver.solveRequiredRecipes()

        Bountiful.LOGGER.info("Done with solves!")

        solver.syncConfig()

        chaosFile.write(chaosData)
        chaosFileInfo.write(chaosInfo)

        val poolSize = solver.sendToRegistries(combined)

        Bountiful.LOGGER.info("Chaos: $poolSize priced items in the pool")
        Bountiful.LOGGER.info("Chaos: ${chaosInfo.unsolvedList.size} items unpriced, see config/bountiful/bountiful-chaos.json (entries with null)")
        Bountiful.LOGGER.info("Chaos sent!")
    }
}