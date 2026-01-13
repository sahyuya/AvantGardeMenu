package com.github.sahyuya.avantgardemenu.config

import com.github.sahyuya.avantgardemenu.AvantGardeMenu
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material

data class MenuItem(
    val id: String,
    val title: String,
    val icon: Material,
    val description: List<String>,
    val command: String?,
    val permission: String?,
    val slot: Int
)

class MenuConfig(private val plugin: AvantGardeMenu) {

    private val miniMessage = MiniMessage.miniMessage()

    var menuTitle: String = "§b§lメインメニュー"
    var menuSize: Int = 27
    var items: List<MenuItem> = emptyList()
    var bedrockMenuTitle: String = "メインメニュー"
    var bedrockMenuContent: String = "メニューから項目を選択してください"

    fun load() {
        val config = plugin.config

        // メニュー基本設定
        menuTitle = config.getString("menu.title") ?: "§b§lメインメニュー"
        menuSize = config.getInt("menu.size", 27)
        bedrockMenuTitle = config.getString("menu.bedrock-title") ?: "メインメニュー"
        bedrockMenuContent = config.getString("menu.bedrock-content") ?: "メニューから項目を選択してください"

        // メニューアイテムの読み込み
        val itemsList = mutableListOf<MenuItem>()
        val itemsSection = config.getConfigurationSection("menu.items")

        itemsSection?.getKeys(false)?.forEach { key ->
            val itemPath = "menu.items.$key"
            val title = config.getString("$itemPath.title") ?: key
            val iconString = config.getString("$itemPath.icon") ?: "STONE"
            val icon = try {
                Material.valueOf(iconString.uppercase())
            } catch (e: IllegalArgumentException) {
                plugin.logger.warning("Invalid material: $iconString, using STONE")
                Material.STONE
            }
            val description = config.getStringList("$itemPath.description")
            val command = config.getString("$itemPath.command")
            val permission = config.getString("$itemPath.permission")
            val slot = config.getInt("$itemPath.slot", itemsList.size)

            itemsList.add(MenuItem(key, title, icon, description, command, permission, slot))
        }

        items = itemsList
    }

    fun parseComponent(text: String): Component {
        return miniMessage.deserialize(text)
            .decoration(TextDecoration.ITALIC, false)
    }
}