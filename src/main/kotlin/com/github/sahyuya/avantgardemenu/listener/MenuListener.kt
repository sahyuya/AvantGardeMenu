package com.github.sahyuya.avantgardemenu.listener

import com.github.sahyuya.avantgardemenu.AvantGardeMenu
import com.github.sahyuya.avantgardemenu.menu.JavaMenuBuilder
import io.papermc.paper.connection.PlayerGameConnection
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

    /**
     * ダイアログのカスタムクリックイベントを処理
     */
    @EventHandler
    fun onCustomClick(event: PlayerCustomClickEvent) {
        // イベントのKey（識別子）を取得
        val identifier = event.identifier

        // avantgardemenu名前空間のイベントのみ処理
        if (identifier.namespace() != "avantgardemenu") {
            return
        }

        // "menu_item_" で始まるKeyのみ処理
        val key = identifier.value()
        if (!key.startsWith("menu_item_")) {
            return
        }

        // アイテムIDを抽出
        val itemId = key.removePrefix("menu_item_")

        // プレイヤーを取得
        val player = getPlayerFromEvent(event) ?: return

        // メニュービルダーでクリック処理
        val menuBuilder = JavaMenuBuilder(plugin)
        menuBuilder.handleMenuClick(player, itemId)

        // ダイアログを閉じる
        player.closeDialog()
    }

    /**
     * PlayerCustomClickEventからPlayerオブジェクトを取得
     */
    private fun getPlayerFromEvent(event: PlayerCustomClickEvent): Player? {
        val connection = event.commonConnection

        // PlayerGameConnectionの場合のみプレイヤーを取得可能
        return if (connection is PlayerGameConnection) {
            connection.player
        } else {
            plugin.logger.warning("Cannot get player from connection: ${connection::class.java.simpleName}")
            null
        }
    }
}