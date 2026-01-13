package com.github.sahyuya.avantgardemenu.command

import com.github.sahyuya.avantgardemenu.AvantGardeMenu
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender

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
}