package com.github.sahyuya.avantgardemenu.config

import com.github.sahyuya.avantgardemenu.AvantGardeMenu
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

data class MenuItem(
    val id: String,
    val title: String,
    val icon: Material,
    val description: List<String>,
    val command: String?,
    val submenu: String?,
    val permission: String?,
    val order: Int
)

data class SubMenuConfig(
    val id: String,
    val title: String,
    val bedrockTitle: String,
    val bedrockContent: String,
    val items: List<MenuItem>
)

class MenuConfig(private val plugin: AvantGardeMenu) {

    private val miniMessage = MiniMessage.miniMessage()

    var menuTitle: String = "§b§lメインメニュー"
    var bedrockMenuTitle: String = "メインメニュー"
    var bedrockMenuContent: String = "メニューから項目を選択してください"
    var items: List<MenuItem> = emptyList()

    // サブメニューのマップ
    private val submenus = mutableMapOf<String, SubMenuConfig>()

    fun load() {
        val config = plugin.config

        // メニュー基本設定
        menuTitle = config.getString("menu.title") ?: "§b§lメインメニュー"
        bedrockMenuTitle = config.getString("menu.bedrock.title") ?: "メインメニュー"
        bedrockMenuContent = config.getString("menu.bedrock.content") ?: "メニューから項目を選択してください"

        // メインメニューアイテムの読み込み
        items = loadMenuItems(config.getConfigurationSection("items"))

        // サブメニューの読み込み
        loadSubmenus()

        plugin.logger.info("Loaded ${items.size} main menu items")
        plugin.logger.info("Loaded ${submenus.size} submenus")
    }

    private fun loadSubmenus() {
        submenus.clear()

        val menusFolder = File(plugin.dataFolder, "menus")
        if (!menusFolder.exists()) {
            menusFolder.mkdirs()
            // デフォルトの設定ファイルを作成
            saveDefaultSubmenuFiles()
        }

        val submenuFiles = menusFolder.listFiles { file ->
            file.extension == "yml" || file.extension == "yaml"
        } ?: emptyArray()

        for (file in submenuFiles) {
            try {
                val submenuConfig = YamlConfiguration.loadConfiguration(file)
                val submenuSection = submenuConfig.getConfigurationSection("submenu") ?: continue

                val id = submenuSection.getString("id") ?: file.nameWithoutExtension
                val title = submenuSection.getString("title") ?: id
                val bedrockTitle = submenuSection.getString("bedrock.title") ?: title
                val bedrockContent = submenuSection.getString("bedrock.content") ?: ""

                val items = loadMenuItems(submenuConfig.getConfigurationSection("items"))

                submenus[id] = SubMenuConfig(id, title, bedrockTitle, bedrockContent, items)
                plugin.logger.info("Loaded submenu: $id with ${items.size} items")
            } catch (e: Exception) {
                plugin.logger.warning("Failed to load submenu from ${file.name}: ${e.message}")
            }
        }
    }

    private fun loadMenuItems(section: org.bukkit.configuration.ConfigurationSection?): List<MenuItem> {
        if (section == null) return emptyList()

        val itemsList = mutableListOf<MenuItem>()

        section.getKeys(false).forEach { key ->
            try {
                val itemPath = key
                val title = section.getString("$itemPath.title") ?: key
                val iconString = section.getString("$itemPath.icon") ?: "STONE"
                val icon = try {
                    Material.valueOf(iconString.uppercase())
                } catch (e: IllegalArgumentException) {
                    plugin.logger.warning("Invalid material: $iconString, using STONE")
                    Material.STONE
                }
                val description = section.getStringList("$itemPath.description")
                val command = section.getString("$itemPath.command")
                val submenu = section.getString("$itemPath.submenu")
                val permission = section.getString("$itemPath.permission")
                val order = section.getInt("$itemPath.order", itemsList.size)

                itemsList.add(MenuItem(key, title, icon, description, command, submenu, permission, order))
            } catch (e: Exception) {
                plugin.logger.warning("Failed to load menu item $key: ${e.message}")
            }
        }

        return itemsList.sortedBy { it.order }
    }

    private fun saveDefaultSubmenuFiles() {
        val menusFolder = File(plugin.dataFolder, "menus")

        // shop.yml
        plugin.saveResource("menus/shop.yml", false)
        // teleport.yml
        plugin.saveResource("menus/teleport.yml", false)
        // utilities.yml
        plugin.saveResource("menus/utilities.yml", false)
        // links.yml
        plugin.saveResource("menus/links.yml", false)
    }

    fun getSubmenu(id: String): SubMenuConfig? {
        return submenus[id]
    }

    fun parseComponent(text: String): Component {
        return miniMessage.deserialize(text)
            .decoration(TextDecoration.ITALIC, false)
    }
}