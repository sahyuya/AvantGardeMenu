package com.github.sahyuya.avantgardemenu.command

import com.github.sahyuya.avantgardemenu.AvantGardeMenu
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import java.io.File

class MenuAdminCommand(private val plugin: AvantGardeMenu) {

    fun onReloadCommand(sender: CommandSender) {
        if (!sender.hasPermission("visualmenu.admin")) {
            sender.sendMessage(Component.text("権限がありません", NamedTextColor.RED))
            return
        }

        try {
            plugin.reload()
            sender.sendMessage(Component.text("設定ファイルを再読み込みしました", NamedTextColor.GREEN))
        } catch (e: Exception) {
            sender.sendMessage(Component.text("再読み込み中にエラーが発生しました: ${e.message}", NamedTextColor.RED))
            plugin.logger.severe("Error reloading config: ${e.message}")
            e.printStackTrace()
        }
    }

    fun onDebugCommand(sender: CommandSender) {
        if (!sender.hasPermission("visualmenu.admin")) {
            sender.sendMessage(Component.text("権限がありません", NamedTextColor.RED))
            return
        }

        sender.sendMessage(Component.text("=== AvantGardeMenu Debug Info ===", NamedTextColor.GOLD))

        // メインメニュー情報
        val mainItems = plugin.menuConfig.items
        sender.sendMessage(Component.text("Main menu items: ${mainItems.size}", NamedTextColor.AQUA))
        mainItems.forEach { item ->
            sender.sendMessage(
                Component.text("  - ${item.id}: submenu=${item.submenu}, command=${item.command}", NamedTextColor.GRAY)
            )
        }

        // サブメニュー情報
        sender.sendMessage(Component.text("Submenus:", NamedTextColor.AQUA))
        val menusFolder = File(plugin.dataFolder, "menus")
        sender.sendMessage(Component.text("  Folder: ${menusFolder.absolutePath}", NamedTextColor.GRAY))
        sender.sendMessage(Component.text("  Exists: ${menusFolder.exists()}", NamedTextColor.GRAY))

        if (menusFolder.exists()) {
            val files = menusFolder.listFiles()
            sender.sendMessage(Component.text("  Files: ${files?.size ?: 0}", NamedTextColor.GRAY))
            files?.forEach { file ->
                sender.sendMessage(Component.text("    - ${file.name}", NamedTextColor.DARK_GRAY))
            }
        }

        // 読み込まれたサブメニュー
        val loadedSubmenus = getLoadedSubmenusCount()
        sender.sendMessage(Component.text("Loaded submenus: $loadedSubmenus", NamedTextColor.AQUA))
        getLoadedSubmenuIds().forEach { id ->
            val submenu = plugin.menuConfig.getSubmenu(id)
            sender.sendMessage(
                Component.text("  - $id: ${submenu?.items?.size ?: 0} items", NamedTextColor.GRAY)
            )
        }

        sender.sendMessage(Component.text("================================", NamedTextColor.GOLD))
    }

    fun onListMenusCommand(sender: CommandSender) {
        if (!sender.hasPermission("visualmenu.admin")) {
            sender.sendMessage(Component.text("権限がありません", NamedTextColor.RED))
            return
        }

        sender.sendMessage(Component.text("=== Available Menus ===", NamedTextColor.GOLD))

        // メインメニュー
        sender.sendMessage(Component.text("Main Menu:", NamedTextColor.AQUA))
        plugin.menuConfig.items.forEach { item ->
            sender.sendMessage(Component.text("  - ${item.id} (${item.title})", NamedTextColor.GRAY))
        }

        // サブメニュー
        sender.sendMessage(Component.text("Submenus:", NamedTextColor.AQUA))
        getLoadedSubmenuIds().forEach { id ->
            val submenu = plugin.menuConfig.getSubmenu(id)
            sender.sendMessage(Component.text("  [$id] ${submenu?.title ?: "Unknown"}", NamedTextColor.YELLOW))
            submenu?.items?.forEach { item ->
                sender.sendMessage(Component.text("    - ${item.id} (${item.title})", NamedTextColor.DARK_GRAY))
            }
        }

        sender.sendMessage(Component.text("=======================", NamedTextColor.GOLD))
    }

    private fun getLoadedSubmenusCount(): Int {
        var count = 0
        listOf("shop", "teleport", "utilities", "links").forEach { id ->
            if (plugin.menuConfig.getSubmenu(id) != null) {
                count++
            }
        }
        return count
    }

    private fun getLoadedSubmenuIds(): List<String> {
        return listOf("shop", "teleport", "utilities", "links").filter { id ->
            plugin.menuConfig.getSubmenu(id) != null
        }
    }
}