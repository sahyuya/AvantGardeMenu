package com.github.sahyuya.avantgardemenu.menu

import com.github.sahyuya.avantgardemenu.AvantGardeMenu
import com.github.sahyuya.avantgardemenu.config.MenuItem
import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player

class JavaMenuBuilder(private val plugin: AvantGardeMenu) {

    fun openMenu(player: Player) {
        val config = plugin.menuConfig

        // メニューアイテムのフィルタリング(権限チェック)
        val availableItems = config.items.filter { item ->
            item.permission == null || player.hasPermission(item.permission)
        }.sortedBy { it.slot }

        if (availableItems.isEmpty()) {
            player.sendMessage(
                Component.text("メニューに表示できるアイテムがありません", NamedTextColor.RED)
            )
            return
        }

        // ダイアログを作成
        val dialog = createMenuDialog(availableItems, config.menuTitle)

        // ダイアログを表示
        player.showDialog(dialog)
    }

    private fun createMenuDialog(items: List<MenuItem>, title: String): Dialog {
        return Dialog.create { factory ->
            factory.empty()
                .base(createDialogBase(items, title))
                .type(createDialogType(items))
        }
    }

    private fun createDialogBase(items: List<MenuItem>, title: String): DialogBase {
        return DialogBase.builder(parseTitle(title))
            .canCloseWithEscape(true)
            .body(createBodyComponents())
            .build()
    }

    private fun createBodyComponents(): List<DialogBody> {
        val bodies = mutableListOf<DialogBody>()

        // タイトルメッセージを追加
        bodies.add(
            DialogBody.plainMessage(
                Component.text("以下から項目を選択してください")
                    .color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false)
            )
        )

        return bodies
    }

    private fun createDialogType(items: List<MenuItem>): DialogType {
        // 各アイテムのActionButtonを作成
        val actionButtons = items.map { item ->
            createActionButton(item)
        }

        // multiActionタイプのダイアログを返す（.build()を呼び出す）
        return DialogType.multiAction(actionButtons).build()
    }

    private fun createActionButton(item: MenuItem): ActionButton {
        // ボタンのテキストを作成（絵文字 + タイトル）
        val buttonText = Component.text()
            .append(Component.text("${getItemIcon(item.icon)} "))
            .append(parseTitle(item.title))
            .decoration(TextDecoration.ITALIC, false)
            .build()

        // ツールチップを作成（説明文）
        val tooltip = if (item.description.isNotEmpty()) {
            val builder = Component.text()
            item.description.forEachIndexed { index, line ->
                if (index > 0) {
                    builder.append(Component.newline())
                }
                builder.append(
                    Component.text(line)
                        .color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
                )
            }
            builder.build()
        } else {
            Component.text("クリックして実行", NamedTextColor.GRAY)
        }

        // カスタムクリックアクションを作成
        val action = DialogAction.customClick(
            Key.key("avantgardemenu", "menu_item_${item.id}"),
            null
        )

        // ActionButtonを構築
        return ActionButton.builder(buttonText)
            .tooltip(tooltip)
            .action(action)
            .width(200)
            .build()
    }

    private fun parseTitle(text: String): Component {
        // MiniMessage形式かレガシーカラーコードかを判定
        return if (text.contains("<") && text.contains(">")) {
            // MiniMessage形式
            plugin.menuConfig.parseComponent(text)
        } else {
            // レガシーカラーコード（§）をそのまま使用
            Component.text(text)
                .decoration(TextDecoration.ITALIC, false)
        }
    }

    private fun getItemIcon(material: Material): String {
        // マテリアルに応じた視認性の高い絵文字アイコンを返す
        return when (material) {
            Material.COMPASS -> "🧭"
            Material.DIAMOND_SWORD -> "⚔️"
            Material.BOOK -> "📖"
            Material.ENDER_PEARL -> "🔮"
            Material.CHEST -> "📦"
            Material.EMERALD -> "💎"
            Material.GOLDEN_APPLE -> "🍎"
            Material.SHIELD -> "🛡️"
            Material.BOW -> "🏹"
            Material.CRAFTING_TABLE -> "🔨"
            Material.ANVIL -> "⚒️"
            Material.ENCHANTING_TABLE -> "✨"
            Material.BEACON -> "💫"
            Material.REDSTONE -> "⚡"
            Material.TNT -> "💣"
            Material.IRON_PICKAXE, Material.DIAMOND_PICKAXE -> "⛏️"
            Material.MAP -> "🗺️"
            Material.BELL -> "🔔"
            Material.NETHER_STAR -> "⭐"
            Material.HOPPER -> "⬇️"
            Material.OBSERVER -> "👁️"
            Material.COMMAND_BLOCK -> "💻"
            Material.ELYTRA -> "🪽"
            Material.TRIDENT -> "🔱"
            Material.TOTEM_OF_UNDYING -> "🗿"
            Material.DRAGON_HEAD -> "🐉"
            Material.NETHERITE_SWORD -> "🗡️"
            else -> "▪️"
        }
    }

    /**
     * メニューアイテムのクリックを処理する
     * PlayerCustomClickEventから呼び出される
     */
    fun handleMenuClick(player: Player, itemId: String) {
        val config = plugin.menuConfig
        val item = config.items.find { it.id == itemId } ?: return

        // 権限チェック
        if (item.permission != null && !player.hasPermission(item.permission)) {
            player.sendMessage(
                Component.text("このアイテムを使用する権限がありません", NamedTextColor.RED)
            )
            return
        }

        // コマンドを実行
        executeCommand(player, item)
    }

    private fun executeCommand(player: Player, item: MenuItem) {
        val command = item.command ?: return

        // メインスレッドでコマンドを実行
        plugin.server.scheduler.runTask(plugin, Runnable {
            when {
                command.startsWith("[console]") -> {
                    // コンソールコマンドとして実行
                    val cmd = command.substring(9).replace("%player%", player.name)
                    plugin.server.dispatchCommand(plugin.server.consoleSender, cmd)
                }
                command.startsWith("[player]") -> {
                    // プレイヤーコマンドとして実行
                    val cmd = command.substring(8).replace("%player%", player.name)
                    player.performCommand(cmd)
                }
                else -> {
                    // デフォルトはプレイヤーコマンド
                    player.performCommand(command.replace("%player%", player.name))
                }
            }
        })
    }
}