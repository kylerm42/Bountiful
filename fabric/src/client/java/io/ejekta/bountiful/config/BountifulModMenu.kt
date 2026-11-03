package io.ejekta.bountiful.config

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import net.fabricmc.loader.api.FabricLoader

/**
 * ModMenu entrypoint. ModMenu itself is optional (this class is only loaded by ModMenu), and so is Cloth Config:
 * without it no config screen is offered, and [BountifulConfigScreen] (which references Cloth) is never loaded.
 */
class BountifulModMenu : ModMenuApi {

    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
        if (!FabricLoader.getInstance().isModLoaded(CLOTH_CONFIG_ID)) {
            return ConfigScreenFactory { null }
        }
        return ConfigScreenFactory { parent -> BountifulConfigScreen.buildScreen(parent) }
    }

    private companion object {
        const val CLOTH_CONFIG_ID = "cloth-config"
    }
}
