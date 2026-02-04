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
    val titlePlain: String,  // 装飾なしのタイトル（表示用）
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
        plugin.logger.info("Loading submenus from: ${menusFolder.absolutePath}")

        if (!menusFolder.exists()) {
            plugin.logger.info("Menus folder does not exist, creating...")
            menusFolder.mkdirs()
            // デフォルトの設定ファイルを作成
            saveDefaultSubmenuFiles()
        }

        val submenuFiles = menusFolder.listFiles { file ->
            file.extension == "yml" || file.extension == "yaml"
        } ?: emptyArray()

        plugin.logger.info("Found ${submenuFiles.size} submenu files")

        for (file in submenuFiles) {
            try {
                plugin.logger.info("Loading submenu file: ${file.name}")
                val submenuConfig = YamlConfiguration.loadConfiguration(file)
                val submenuSection = submenuConfig.getConfigurationSection("submenu")

                if (submenuSection == null) {
                    plugin.logger.warning("File ${file.name} does not contain 'submenu' section")
                    continue
                }

                val id = submenuSection.getString("id") ?: file.nameWithoutExtension
                val title = submenuSection.getString("title") ?: id
                val bedrockTitle = submenuSection.getString("bedrock.title") ?: title
                val bedrockContent = submenuSection.getString("bedrock.content") ?: ""

                plugin.logger.info("Submenu ID: $id, Title: $title")

                val items = loadMenuItems(submenuConfig.getConfigurationSection("items"))

                submenus[id] = SubMenuConfig(id, title, bedrockTitle, bedrockContent, items)
                plugin.logger.info("Loaded submenu: $id with ${items.size} items")
            } catch (e: Exception) {
                plugin.logger.warning("Failed to load submenu from ${file.name}: ${e.message}")
                e.printStackTrace()
            }
        }

        plugin.logger.info("Total submenus loaded: ${submenus.size}")
        plugin.logger.info("Submenu IDs: ${submenus.keys}")
    }

    private fun loadMenuItems(section: org.bukkit.configuration.ConfigurationSection?): List<MenuItem> {
        if (section == null) return emptyList()

        val itemsList = mutableListOf<MenuItem>()

        section.getKeys(false).forEach { key ->
            try {
                val itemPath = key
                val title = section.getString("$itemPath.title") ?: key
                val titlePlain = section.getString("$itemPath.title_plain") ?: stripFormatting(title)
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

                itemsList.add(MenuItem(key, title, titlePlain, icon, description, command, submenu, permission, order))
            } catch (e: Exception) {
                plugin.logger.warning("Failed to load menu item $key: ${e.message}")
            }
        }

        return itemsList.sortedBy { it.order }
    }

    /**
     * MiniMessageやレガシーカラーコードを除去してプレーンテキストを返す
     */
    private fun stripFormatting(text: String): String {
        // MiniMessageタグを除去
        var result = text.replace(Regex("<[^>]+>"), "")
        // レガシーカラーコード(§)を除去
        result = result.replace(Regex("§[0-9a-fk-or]"), "")
        return result
    }

    private fun saveDefaultSubmenuFiles() {
        val menusFolder = File(plugin.dataFolder, "menus")

        val files = listOf("shop.yml", "teleport.yml", "utilities.yml", "links.yml", "admin.yml")

        for (fileName in files) {
            try {
                // 毎回上書きして最新の設定ファイルを生成
                plugin.saveResource("menus/$fileName", true)
                plugin.logger.info("Regenerated submenu file: $fileName")
            } catch (e: Exception) {
                plugin.logger.warning("Could not save submenu file $fileName: ${e.message}")
                plugin.logger.warning("Please manually create the file in plugins/AvantGardeMenu/menus/")
            }
        }
    }

    fun getSubmenu(id: String): SubMenuConfig? {
        return submenus[id]
    }

    fun parseComponent(text: String): Component {
        return miniMessage.deserialize(text)
            .decoration(TextDecoration.ITALIC, false)
    }
}