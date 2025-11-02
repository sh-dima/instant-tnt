package io.gitlab.shdima.tnt

import de.exlll.configlib.NameFormatters
import de.exlll.configlib.YamlConfigurationProperties
import org.bstats.bukkit.Metrics
import de.exlll.configlib.YamlConfigurations
import io.gitlab.shdima.tnt.listeners.InstantTntBreakListener
import io.gitlab.shdima.tnt.listeners.InstantTntCollideListener
import io.gitlab.shdima.tnt.listeners.InstantTntDetonateListener
import io.gitlab.shdima.tnt.listeners.InstantTntMoveListener
import io.gitlab.shdima.tnt.listeners.InstantTntPlaceListener
import io.gitlab.shdima.tnt.managers.InstantTntManager
import io.gitlab.shdima.tnt.managers.PlayerVelocityManager
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.plugin.java.JavaPlugin
import java.nio.file.Path

@Suppress("unused")
class InstantTnt : JavaPlugin() {

    lateinit var instantTntManager: InstantTntManager
    lateinit var playerVelocityManager: PlayerVelocityManager

    val instantTntKey = NamespacedKey(this, "instant_tnt")

    lateinit var config: Config

    override fun onEnable() {
        val properties = YamlConfigurationProperties.newBuilder().setNameFormatter(NameFormatters.LOWER_KEBAB_CASE).build()

        val configFile = Path.of(dataFolder.path, "config.yml")
        config = try {
            YamlConfigurations.load(configFile, Config::class.java, properties)
        } catch (e: Exception) {
            Config()
        }

        YamlConfigurations.save(configFile, Config::class.java, config, properties)

        saveResource("data/instant-tnt-blocks.json", false)

        if (!config.enabled) {
            return
        }

        val pluginManager = server.pluginManager

        playerVelocityManager = PlayerVelocityManager(this)
        playerVelocityManager.runTaskTimer(this, 0L, 1L)

        instantTntManager = InstantTntManager(this)

        val instantTnt = instantTntManager.instantTntItem

        val plankTypes = Material.entries.toList()
            .filter { material -> material.name.endsWith("PLANKS") }

        for (plankType in plankTypes) {
            val recipeKey = NamespacedKey(this, "instant_tnt_recipe_" + plankType.name.lowercase())

            val instantTntRecipe = ShapedRecipe(recipeKey, instantTnt)

            instantTntRecipe.shape("GPG", "PGP", "GPG")

            instantTntRecipe.setIngredient('G', Material.GUNPOWDER)
            instantTntRecipe.setIngredient('P', plankType)

            server.addRecipe(instantTntRecipe)
        }

        pluginManager.registerEvents(InstantTntPlaceListener(this), this)
        pluginManager.registerEvents(InstantTntBreakListener(this), this)
        pluginManager.registerEvents(InstantTntMoveListener(this), this)
        pluginManager.registerEvents(InstantTntCollideListener(this), this)
        pluginManager.registerEvents(InstantTntDetonateListener(this), this)

        try {
            Metrics(this, 27829)
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }

    override fun onDisable() {
        instantTntManager.saveInstantTntData()
    }
}
