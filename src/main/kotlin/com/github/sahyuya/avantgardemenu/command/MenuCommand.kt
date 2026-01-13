package com.github.sahyuya.avantgardemenu.command

import com.github.sahyuya.avantgardemenu.AvantGardeMenu
import com.github.sahyuya.avantgardemenu.menu.JavaMenuBuilder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class MenuCommand(private val plugin: AvantGardeMenu) {

    fun onCommand(sender: CommandSender) {
        if (sender !is Player) {
            sender.sendMessage(Component.text("このコマンドはプレイヤーのみ実行できます", NamedTextColor.RED))
            return
        }

        if (!sender.hasPermission("visualmenu.use")) {
            sender.sendMessage(Component.text("権限がありません", NamedTextColor.RED))
            return
        }

        // Bedrockプレイヤーの場合はCumulusフォームを使用
        if (plugin.geyserManager.isBedrockPlayer(sender)) {
            plugin.geyserManager.openBedrockMenu(sender)
        } else {
            // Javaプレイヤーの場合はDialog APIを使用
            val menuBuilder = JavaMenuBuilder(plugin)
            menuBuilder.openMenu(sender)
        }
    }
}