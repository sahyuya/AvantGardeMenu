package com.github.sahyuya.avantgardemenu.manager

import com.github.sahyuya.avantgardemenu.AvantGardeMenu
import org.bukkit.entity.Player
import org.geysermc.cumulus.form.SimpleForm
import org.geysermc.geyser.api.GeyserApi

class GeyserManager(private val plugin: AvantGardeMenu) {

    val isGeyserEnabled: Boolean = plugin.server.pluginManager.getPlugin("Geyser-Spigot") != null

    fun isBedrockPlayer(player: Player): Boolean {
        if (!isGeyserEnabled) return false

        return try {
            val geyserApi = GeyserApi.api()
            geyserApi.isBedrockPlayer(player.uniqueId)
        } catch (e: Exception) {
            plugin.logger.warning("Error checking if player is Bedrock: ${e.message}")
            false
        }
    }

    fun openBedrockMenu(player: Player) {
        if (!isGeyserEnabled) return

        try {
            val geyserApi = GeyserApi.api()
            val connection = geyserApi.connectionByUuid(player.uniqueId) ?: return

            val config = plugin.menuConfig

            // メニューアイテムのフィルタリング
            val availableItems = config.items
                .filter { item ->
                    item.permission == null || player.hasPermission(item.permission)
                }
                .sortedBy { it.slot }

            // SimpleFormの作成
            val formBuilder = SimpleForm.builder()
                .title(config.bedrockMenuTitle)
                .content(config.bedrockMenuContent)

            // ボタンを追加
            availableItems.forEach { item ->
                val buttonText = buildString {
                    append(item.title)
                    if (item.description.isNotEmpty()) {
                        append("\n")
                        append(item.description.joinToString("\n"))
                    }
                }
                formBuilder.button(buttonText)
            }

            // レスポンスハンドラーを設定
            formBuilder.validResultHandler { response ->
                val clickedIndex = response.clickedButtonId()
                if (clickedIndex >= 0 && clickedIndex < availableItems.size) {
                    val selectedItem = availableItems[clickedIndex]
                    executeCommand(player, selectedItem.command)
                }
            }

            // フォームを送信
            connection.sendForm(formBuilder.build())

        } catch (e: Exception) {
            plugin.logger.warning("Error opening Bedrock menu: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun executeCommand(player: Player, command: String?) {
        if (command == null) return

        plugin.server.scheduler.runTask(plugin, Runnable {
            when {
                command.startsWith("[console]") -> {
                    val cmd = command.substring(9).replace("%player%", player.name)
                    plugin.server.dispatchCommand(plugin.server.consoleSender, cmd)
                }
                command.startsWith("[player]") -> {
                    val cmd = command.substring(8).replace("%player%", player.name)
                    player.performCommand(cmd)
                }
                else -> {
                    player.performCommand(command.replace("%player%", player.name))
                }
            }
        })
    }
}