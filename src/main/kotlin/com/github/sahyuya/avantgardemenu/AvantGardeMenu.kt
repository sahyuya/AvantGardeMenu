package com.github.sahyuya.avantgardemenu

import com.github.sahyuya.avantgardemenu.command.MenuAdminCommand
import com.github.sahyuya.avantgardemenu.command.MenuCommand
import com.github.sahyuya.avantgardemenu.config.MenuConfig
import com.github.sahyuya.avantgardemenu.listener.MenuListener
import com.github.sahyuya.avantgardemenu.manager.GeyserManager
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin

class AvantGardeMenu : JavaPlugin() {

    lateinit var menuConfig: MenuConfig
        private set

    lateinit var geyserManager: GeyserManager
        private set

    override fun onEnable() {
        // プラグインフォルダの作成
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }

        // 設定の読み込み
        saveDefaultConfig()
        menuConfig = MenuConfig(this)
        menuConfig.load()

        // Geyserマネージャーの初期化
        geyserManager = GeyserManager(this)

        // コマンドの登録（Paper plugin方式）
        registerCommands()

        // リスナーの登録
        server.pluginManager.registerEvents(MenuListener(this), this)

        logger.info("AvantGardeMenu has been enabled!")
        logger.info("Geyser support: ${geyserManager.isGeyserEnabled}")
    }

    override fun onDisable() {
        logger.info("AvantGardeMenu has been disabled!")
    }

    private fun registerCommands() {
        val lifecycleManager: LifecycleEventManager<Plugin> = this.lifecycleManager
        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            val commands = event.registrar()

            // /menu コマンドの登録
            commands.register(
                Commands.literal("menu")
                    .executes { context ->
                        MenuCommand(this).onCommand(context.source.sender)
                        1
                    }
                    .build(),
                "Open the main menu",
                listOf("vmenu", "visualmenu")
            )

            // /menuadmin コマンドの登録
            commands.register(
                Commands.literal("menuadmin")
                    .then(
                        Commands.literal("reload")
                            .executes { context ->
                                MenuAdminCommand(this).onReloadCommand(context.source.sender)
                                1
                            }
                    )
                    .then(
                        Commands.literal("debug")
                            .executes { context ->
                                MenuAdminCommand(this).onDebugCommand(context.source.sender)
                                1
                            }
                    )
                    .then(
                        Commands.literal("listmenus")
                            .executes { context ->
                                MenuAdminCommand(this).onListMenusCommand(context.source.sender)
                                1
                            }
                    )
                    .build(),
                "Admin commands for AvantGardeMenu"
            )
        }
    }

    fun reload() {
        reloadConfig()
        menuConfig.load()
    }
}