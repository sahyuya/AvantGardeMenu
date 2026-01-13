package com.github.sahyuya.avantgardemenu.listener

import com.github.sahyuya.avantgardemenu.AvantGardeMenu
import com.github.sahyuya.avantgardemenu.menu.JavaMenuBuilder
import io.papermc.paper.connection.PlayerGameConnection
import io.papermc.paper.dialog.DialogResponseView
import io.papermc.paper.event.player.PlayerCustomClickEvent
import net.kyori.adventure.key.Key
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class MenuListener(private val plugin: AvantGardeMenu) : Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        // Geyserプレイヤーかどうかをログに記録(デバッグ用)
        if (plugin.geyserManager.isGeyserEnabled) {
            val isBedrock = plugin.geyserManager.isBedrockPlayer(player)
            plugin.logger.info("${player.name} joined as ${if (isBedrock) "Bedrock" else "Java"} player")
        }
    }

    @EventHandler
    fun onCustomClick(event: PlayerCustomClickEvent) {
        val identifier = event.identifier

        // avantgardemenu名前空間のイベントのみ処理
        if (identifier.namespace() != "avantgardemenu") {
            return
        }

        val player = getPlayerFromEvent(event) ?: return
        val key = identifier.value()

        // tomap確認ダイアログの処理
        if (key == "tomap_confirm") {
            handleToMapConfirm(event, player)
            return
        }

        // メニューアイテムのクリック処理
        if (key.startsWith("menu_")) {
            handleMenuClick(key, player)
            player.closeDialog()
        }
    }

    private fun handleMenuClick(key: String, player: Player) {
        // "menu_<menuId>_<itemId>" の形式をパース
        val parts = key.removePrefix("menu_").split("_", limit = 2)

        if (parts.size != 2) {
            plugin.logger.warning("Invalid menu key format: $key")
            return
        }

        val menuId = parts[0]
        val itemId = parts[1]

        val menuBuilder = JavaMenuBuilder(plugin)
        menuBuilder.handleMenuClick(player, menuId, itemId)
    }

    private fun handleToMapConfirm(event: PlayerCustomClickEvent, player: Player) {
        val view = event.dialogResponseView ?: return

        try {
            val url = view.getText("url") ?: run {
                player.sendMessage("§cURLが入力されていません")
                return
            }

            val width = view.getFloat("width") ?: 1f
            val height = view.getFloat("height") ?: 1f

            val menuBuilder = JavaMenuBuilder(plugin)
            menuBuilder.handleToMapConfirm(player, url, width, height)

            player.closeDialog()
        } catch (e: Exception) {
            plugin.logger.warning("Error handling tomap dialog: ${e.message}")
            player.sendMessage("§c入力の処理中にエラーが発生しました")
        }
    }

    private fun getPlayerFromEvent(event: PlayerCustomClickEvent): Player? {
        val connection = event.commonConnection

        return if (connection is PlayerGameConnection) {
            connection.player
        } else {
            plugin.logger.warning("Cannot get player from connection: ${connection::class.java.simpleName}")
            null
        }
    }
}