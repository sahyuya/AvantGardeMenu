package com.github.sahyuya.avantgardemenu.menu

import com.github.sahyuya.avantgardemenu.AvantGardeMenu
import com.github.sahyuya.avantgardemenu.config.MenuItem
import com.github.sahyuya.avantgardemenu.config.SubMenuConfig
import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.input.DialogInput
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*

class JavaMenuBuilder(private val plugin: AvantGardeMenu) {

    // プレイヤーの現在のメニュー位置を追跡
    private val menuStack = mutableMapOf<UUID, MutableList<String>>()

    fun openMenu(player: Player) {
        // メニュースタックをリセット
        menuStack[player.uniqueId] = mutableListOf("main")
        openMainMenu(player)
    }

    fun openMainMenu(player: Player) {
        val config = plugin.menuConfig

        val availableItems = config.items.filter { item ->
            item.permission == null || player.hasPermission(item.permission)
        }

        if (availableItems.isEmpty()) {
            player.sendMessage(Component.text("メニューに表示できるアイテムがありません", NamedTextColor.RED))
            return
        }

        val dialog = createMenuDialog(availableItems, config.menuTitle, "main")
        player.showDialog(dialog)
    }

    fun openSubmenu(player: Player, submenuId: String) {
        plugin.logger.info("Attempting to open submenu: $submenuId for player ${player.name}")

        val submenu = plugin.menuConfig.getSubmenu(submenuId)
        if (submenu == null) {
            plugin.logger.warning("Submenu not found: $submenuId")
            player.sendMessage(Component.text("サブメニューが見つかりません: $submenuId", NamedTextColor.RED))
            return
        }

        plugin.logger.info("Submenu found: ${submenu.id}, items count: ${submenu.items.size}")

        // メニュースタックに追加
        val stack = menuStack.getOrPut(player.uniqueId) { mutableListOf("main") }
        stack.add(submenuId)
        plugin.logger.info("Menu stack for ${player.name}: $stack")

        val availableItems = submenu.items.filter { item ->
            item.permission == null || player.hasPermission(item.permission)
        }

        plugin.logger.info("Available items in submenu: ${availableItems.size}")

        if (availableItems.isEmpty()) {
            player.sendMessage(Component.text("このサブメニューに表示できるアイテムがありません", NamedTextColor.RED))
            return
        }

        val dialog = createMenuDialog(availableItems, submenu.title, submenuId)
        player.showDialog(dialog)
        plugin.logger.info("Submenu dialog shown to ${player.name}")
    }

    fun goBack(player: Player) {
        val stack = menuStack[player.uniqueId] ?: mutableListOf("main")

        // 現在のメニューを削除
        if (stack.size > 1) {
            stack.removeAt(stack.size - 1)
        }

        // 一つ前のメニューを開く
        val previousMenu = stack.lastOrNull() ?: "main"

        if (previousMenu == "main") {
            openMainMenu(player)
        } else {
            // スタックから削除してから開く（openSubmenuで再度追加されるため）
            stack.removeAt(stack.size - 1)
            openSubmenu(player, previousMenu)
        }
    }

    private fun createMenuDialog(items: List<MenuItem>, title: String, menuId: String): Dialog {
        return Dialog.create { factory ->
            factory.empty()
                .base(createDialogBase(items, title))
                .type(createDialogType(items, menuId))
        }
    }

    private fun createDialogBase(items: List<MenuItem>, title: String): DialogBase {
        return DialogBase.builder(parseTitle(title))
            .canCloseWithEscape(true)
            .body(createBodyComponents())
            .build()
    }

    private fun createBodyComponents(): List<DialogBody> {
        return listOf(
            DialogBody.plainMessage(
                Component.text("以下から項目を選択してください")
                    .color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false)
            )
        )
    }

    private fun createDialogType(items: List<MenuItem>, menuId: String): DialogType {
        val actionButtons = items.map { item ->
            createActionButton(item, menuId)
        }

        return DialogType.multiAction(actionButtons).build()
    }

    private fun createActionButton(item: MenuItem, menuId: String): ActionButton {
        val buttonText = Component.text()
            .append(Component.text("${getItemIcon(item.icon)} "))
            .append(parseTitle(item.title))
            .decoration(TextDecoration.ITALIC, false)
            .build()

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

        val action = DialogAction.customClick(
            Key.key("avantgardemenu", "menu_${menuId}_${item.id}"),
            null
        )

        return ActionButton.builder(buttonText)
            .tooltip(tooltip)
            .action(action)
            .width(200)
            .build()
    }

    fun handleMenuClick(player: Player, menuId: String, itemId: String): Boolean {
        plugin.logger.info("Menu click: menuId=$menuId, itemId=$itemId, player=${player.name}")

        // メインメニューまたはサブメニューからアイテムを取得
        val item = if (menuId == "main") {
            plugin.menuConfig.items.find { it.id == itemId }
        } else {
            plugin.menuConfig.getSubmenu(menuId)?.items?.find { it.id == itemId }
        }

        if (item == null) {
            plugin.logger.warning("Menu item not found: $menuId/$itemId")
            player.sendMessage(Component.text("メニューアイテムが見つかりませんでした", NamedTextColor.RED))
            return true // ダイアログを閉じる
        }

        plugin.logger.info("Found item: ${item.id}, submenu=${item.submenu}, command=${item.command}")

        // 権限チェック
        if (item.permission != null && !player.hasPermission(item.permission)) {
            player.sendMessage(Component.text("このアイテムを使用する権限がありません", NamedTextColor.RED))
            return true // ダイアログを閉じる
        }

        // サブメニューを持つ場合
        if (item.submenu != null) {
            plugin.logger.info("Opening submenu: ${item.submenu}")
            openSubmenu(player, item.submenu)
            return false // ダイアログを閉じない（サブメニューが開くため）
        }

        // コマンドを実行
        if (item.command != null) {
            plugin.logger.info("Executing command: ${item.command}")
            return executeCommand(player, item.command)
        } else {
            plugin.logger.warning("Item has no command or submenu: ${item.id}")
        }

        return true // ダイアログを閉じる
    }

    /**
     * コマンドを実行し、ダイアログを閉じるべきかどうかを返す
     * @return true: ダイアログを閉じる, false: ダイアログを閉じない（新しいダイアログが開く）
     */
    private fun executeCommand(player: Player, command: String): Boolean {
        return when {
            command == "[special]back" -> {
                // 戻るボタンは新しいダイアログを開くので閉じない
                goBack(player)
                false
            }
            command == "[special]nightvision_toggle" -> {
                toggleNightVision(player)
                true // ダイアログを閉じる
            }
            command == "[special]tomap_dialog" -> {
                // tomap入力ダイアログを開くので閉じない
                openToMapDialog(player)
                false
            }
            command == "[special]admin_promote" -> {
                openPromoteDialog(player)
                false
            }
            command == "[special]admin_coreprotect" -> {
                openCoreProtectDialog(player)
                false
            }
            command == "[special]admin_ban" -> {
                openBanDialog(player)
                false
            }
            command == "[special]admin_worldsize" -> {
                openWorldSizeDialog(player)
                false
            }
            command == "[special]admin_createworld" -> {
                openCreateWorldDialog(player)
                false
            }
            command.startsWith("[special]open_url:") -> {
                val url = command.substring("[special]open_url:".length)
                openUrl(player, url)
                true // ダイアログを閉じる
            }
            command.startsWith("[console]") -> {
                plugin.server.scheduler.runTask(plugin, Runnable {
                    val cmd = command.substring(9).replace("%player%", player.name)
                    plugin.server.dispatchCommand(plugin.server.consoleSender, cmd)
                })
                true // ダイアログを閉じる
            }
            command.startsWith("[player]") -> {
                plugin.server.scheduler.runTask(plugin, Runnable {
                    val cmd = command.substring(8).replace("%player%", player.name)
                    player.performCommand(cmd)
                })
                true // ダイアログを閉じる
            }
            else -> {
                plugin.server.scheduler.runTask(plugin, Runnable {
                    player.performCommand(command.replace("%player%", player.name))
                })
                true // ダイアログを閉じる
            }
        }
    }

    private fun toggleNightVision(player: Player) {
        if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
            player.removePotionEffect(PotionEffectType.NIGHT_VISION)
            player.sendMessage(Component.text("暗視をオフにしました", NamedTextColor.YELLOW))
        } else {
            player.addPotionEffect(
                PotionEffect(PotionEffectType.NIGHT_VISION, -1, 0, false, false)
            )
            player.sendMessage(Component.text("暗視をオンにしました", NamedTextColor.GREEN))
        }
    }

    private fun openUrl(player: Player, url: String) {
        val message = Component.text()
            .append(Component.text("クリックしてリンクを開く: ", NamedTextColor.AQUA))
            .append(
                Component.text(url)
                    .color(NamedTextColor.YELLOW)
                    .decoration(TextDecoration.UNDERLINED, true)
                    .clickEvent(ClickEvent.openUrl(url))
            )
            .build()

        player.sendMessage(message)
    }

    private fun openToMapDialog(player: Player) {
        val dialog = Dialog.create { factory ->
            factory.empty()
                .base(
                    DialogBase.builder(Component.text("画像をマップアート化", NamedTextColor.LIGHT_PURPLE))
                        .canCloseWithEscape(true)
                        .body(listOf(
                            DialogBody.plainMessage(
                                Component.text("画像のURLとサイズを入力してください")
                                    .color(NamedTextColor.GRAY)
                            )
                        ))
                        .inputs(listOf(
                            DialogInput.text("url", Component.text("画像URL", NamedTextColor.AQUA))
                                .maxLength(300)
                                .build(),
                            DialogInput.numberRange("height", Component.text("縦幅", NamedTextColor.GREEN), 1f, 16f)
                                .initial(1f)
                                .step(1f)
                                .width(250)
                                .build(),

                            DialogInput.numberRange("width", Component.text("横幅", NamedTextColor.GREEN), 1f, 16f)
                                .initial(1f)
                                .step(1f)
                                .width(250)
                                .build()
                        ))
                        .build()
                )
                .type(
                    DialogType.confirmation(
                        ActionButton.builder(Component.text("作成", NamedTextColor.GREEN))
                            .tooltip(Component.text("マップアートを作成します"))
                            .action(DialogAction.customClick(
                                Key.key("avantgardemenu", "tomap_confirm"),
                                null
                            ))
                            .build(),
                        ActionButton.builder(Component.text("キャンセル", NamedTextColor.RED))
                            .tooltip(Component.text("キャンセルします"))
                            .action(null)
                            .build()
                    )
                )
        }

        player.showDialog(dialog)
    }

    fun handleToMapConfirm(player: Player, url: String, width: Float, height: Float) {
        val widthInt = width.toInt()
        val heightInt = height.toInt()

        plugin.server.scheduler.runTask(plugin, Runnable {
            val command = "tomap $url resize $heightInt $widthInt"
            player.performCommand(command)
            player.sendMessage(
                Component.text("マップアートを作成中... ($widthInt x $heightInt)", NamedTextColor.GREEN)
            )
        })
    }

    // === 管理者用ダイアログ ===

    private fun openPromoteDialog(player: Player) {
        val dialog = Dialog.create { factory ->
            factory.empty()
                .base(
                    DialogBase.builder(Component.text("プレイヤー昇格", NamedTextColor.GOLD))
                        .canCloseWithEscape(true)
                        .body(listOf(
                            DialogBody.plainMessage(
                                Component.text("昇格させるプレイヤー名を入力してください")
                                    .color(NamedTextColor.GRAY)
                            )
                        ))
                        .inputs(listOf(
                            DialogInput.text("player", Component.text("プレイヤー名", NamedTextColor.AQUA))
                                .initial("")
                                .build(),
                            DialogInput.bool("good", Component.text("GOODを付ける", NamedTextColor.YELLOW))
                                .initial(false)
                                .build()
                        ))
                        .build()
                )
                .type(
                    DialogType.confirmation(
                        ActionButton.builder(Component.text("実行", NamedTextColor.GREEN))
                            .tooltip(Component.text("昇格を実行します"))
                            .action(DialogAction.customClick(
                                Key.key("avantgardemenu", "admin_promote_confirm"),
                                null
                            ))
                            .build(),
                        ActionButton.builder(Component.text("キャンセル", NamedTextColor.RED))
                            .action(null)
                            .build()
                    )
                )
        }

        player.showDialog(dialog)
    }

    private fun openCoreProtectDialog(player: Player) {
        val dialog = Dialog.create { factory ->
            factory.empty()
                .base(
                    DialogBase.builder(Component.text("CoreProtect操作", NamedTextColor.BLUE))
                        .canCloseWithEscape(true)
                        .body(listOf(
                            DialogBody.plainMessage(
                                Component.text("CoreProtectのlookup/rollbackを実行します")
                                    .color(NamedTextColor.GRAY)
                            )
                        ))
                        .inputs(listOf(
                            DialogInput.bool("do_lookup", Component.text("Lookupを実行", NamedTextColor.AQUA))
                                .initial(true)
                                .build(),
                            DialogInput.bool("do_rollback", Component.text("Rollbackを実行", NamedTextColor.RED))
                                .initial(false)
                                .build(),
                            DialogInput.text("player", Component.text("プレイヤー名", NamedTextColor.YELLOW))
                                .initial("")
                                .build(),
                            DialogInput.text("time_lookup", Component.text("Lookup時間 (例: 1h, 30m)", NamedTextColor.GREEN))
                                .initial("10h")
                                .build(),
                            DialogInput.text("time_rollback", Component.text("Rollback時間 (例: 1h, 30m)", NamedTextColor.GREEN))
                                .initial("10h")
                                .build(),
                            DialogInput.text("radius", Component.text("範囲 (半径)", NamedTextColor.LIGHT_PURPLE))
                                .initial("")
                                .build(),
                            DialogInput.text("extra_params", Component.text("追加パラメータ (任意)", NamedTextColor.GRAY))
                                .initial("")
                                .build(),
                            DialogInput.bool("exclude_ores", Component.text("鉱石を除外", NamedTextColor.GOLD))
                                .initial(false)
                                .build()
                        ))
                        .build()
                )
                .type(
                    DialogType.confirmation(
                        ActionButton.builder(Component.text("実行", NamedTextColor.GREEN))
                            .action(DialogAction.customClick(
                                Key.key("avantgardemenu", "admin_coreprotect_confirm"),
                                null
                            ))
                            .build(),
                        ActionButton.builder(Component.text("キャンセル", NamedTextColor.RED))
                            .action(null)
                            .build()
                    )
                )
        }

        player.showDialog(dialog)
    }

    private fun openBanDialog(player: Player) {
        val dialog = Dialog.create { factory ->
            factory.empty()
                .base(
                    DialogBase.builder(Component.text("プレイヤーBAN", NamedTextColor.DARK_RED))
                        .canCloseWithEscape(true)
                        .body(listOf(
                            DialogBody.plainMessage(
                                Component.text("BANするプレイヤー情報を入力してください")
                                    .color(NamedTextColor.GRAY)
                            )
                        ))
                        .inputs(listOf(
                            DialogInput.text("player", Component.text("プレイヤー名", NamedTextColor.AQUA))
                                .initial("")
                                .build(),
                            DialogInput.text("time", Component.text("BAN期間 (例: 7d, 1mo)", NamedTextColor.YELLOW))
                                .initial("")
                                .build(),
                            DialogInput.text("reason", Component.text("理由", NamedTextColor.RED))
                                .initial("")
                                .build()
                        ))
                        .build()
                )
                .type(
                    DialogType.confirmation(
                        ActionButton.builder(Component.text("BAN実行", NamedTextColor.DARK_RED))
                            .action(DialogAction.customClick(
                                Key.key("avantgardemenu", "admin_ban_confirm"),
                                null
                            ))
                            .build(),
                        ActionButton.builder(Component.text("キャンセル", NamedTextColor.GREEN))
                            .action(null)
                            .build()
                    )
                )
        }

        player.showDialog(dialog)
    }

    private fun openWorldSizeDialog(player: Player) {
        val dialog = Dialog.create { factory ->
            factory.empty()
                .base(
                    DialogBase.builder(Component.text("ワールドサイズ拡張", NamedTextColor.GREEN))
                        .canCloseWithEscape(true)
                        .body(listOf(
                            DialogBody.plainMessage(
                                Component.text("ワールドサイズを拡張します")
                                    .color(NamedTextColor.GRAY)
                            )
                        ))
                        .inputs(listOf(
                            DialogInput.text("player", Component.text("プレイヤー名", NamedTextColor.AQUA))
                                .initial("")
                                .build(),
                            DialogInput.text("points", Component.text("消費ポイント (100の倍数で入力して下さい)", NamedTextColor.YELLOW))
                                .initial("1")
                                .build()
                        ))
                        .build()
                )
                .type(
                    DialogType.confirmation(
                        ActionButton.builder(Component.text("実行", NamedTextColor.GREEN))
                            .action(DialogAction.customClick(
                                Key.key("avantgardemenu", "admin_worldsize_confirm"),
                                null
                            ))
                            .build(),
                        ActionButton.builder(Component.text("キャンセル", NamedTextColor.RED))
                            .action(null)
                            .build()
                    )
                )
        }

        player.showDialog(dialog)
    }

    private fun openCreateWorldDialog(player: Player) {
        val dialog = Dialog.create { factory ->
            factory.empty()
                .base(
                    DialogBase.builder(Component.text("ワールド作成", NamedTextColor.AQUA))
                        .canCloseWithEscape(true)
                        .body(listOf(
                            DialogBody.plainMessage(
                                Component.text("新しいワールドを作成します")
                                    .color(NamedTextColor.GRAY)
                            )
                        ))
                        .inputs(listOf(
                            DialogInput.text("name", Component.text("ワールド名", NamedTextColor.YELLOW))
                                .initial("")
                                .build(),
                            DialogInput.numberRange("world_type", Component.text("ワールドタイプ", NamedTextColor.GREEN), 0f, 2f)
                                .initial(0f)
                                .step(1f)
                                .labelFormat("%1\$s:%2\$s (0=通常, 1=ネザー, 2=エンド)")
                                .width(300)
                                .build(),
                            DialogInput.numberRange("generation_type", Component.text("地形タイプ", NamedTextColor.LIGHT_PURPLE), 0f, 1f)
                                .initial(0f)
                                .step(1f)
                                .labelFormat("%1\$s:%2\$s (0=通常, 1=フラット)")
                                .width(300)
                                .build(),
                            DialogInput.bool("no_structures", Component.text("構造物なし", NamedTextColor.GRAY))
                                .initial(false)
                                .build(),
                            DialogInput.bool("no_natural_mob", Component.text("自然モブスポーン禁止", NamedTextColor.RED))
                                .initial(false)
                                .build(),
                            DialogInput.bool("no_forced_mob", Component.text("モブスポーン強制禁止", NamedTextColor.DARK_RED))
                                .initial(false)
                                .build(),
                            DialogInput.bool("no_randomtick", Component.text("自然変化なし", NamedTextColor.GOLD))
                                .initial(false)
                                .build()
                        ))
                        .build()
                )
                .type(
                    DialogType.confirmation(
                        ActionButton.builder(Component.text("作成", NamedTextColor.GREEN))
                            .action(DialogAction.customClick(
                                Key.key("avantgardemenu", "admin_createworld_confirm"),
                                null
                            ))
                            .build(),
                        ActionButton.builder(Component.text("キャンセル", NamedTextColor.RED))
                            .action(null)
                            .build()
                    )
                )
        }

        player.showDialog(dialog)
    }

    private fun parseTitle(text: String): Component {
        return if (text.contains("<") && text.contains(">")) {
            plugin.menuConfig.parseComponent(text)
        } else {
            Component.text(text).decoration(TextDecoration.ITALIC, false)
        }
    }

    private fun getItemIcon(material: Material): String {
        return when (material) {
            Material.PLAYER_HEAD -> "👤"
            Material.EMERALD -> "💎"
            Material.DIAMOND -> "💠"
            Material.ENDER_PEARL -> "🔮"
            Material.CHEST -> "📦"
            Material.MAP, Material.FILLED_MAP -> "🗺️"
            Material.COMMAND_BLOCK -> "💻"
            Material.STONE -> "🧱"
            Material.FLOWER_POT -> "🪴"
            Material.DIAMOND_ORE -> "⛏️"
            Material.IRON_PICKAXE -> "⚒️"
            Material.GOLD_INGOT -> "🪙"
            Material.BARRIER -> "❌"
            Material.BEACON -> "✨"
            Material.GRASS_BLOCK -> "🌱"
            Material.GOLD_BLOCK -> "🎰"
            Material.DIAMOND_SWORD -> "⚔️"
            Material.CRAFTING_TABLE -> "🔨"
            Material.ENDER_CHEST -> "📥"
            Material.LAVA_BUCKET -> "🗑️"
            Material.GOLDEN_CARROT -> "🔦"
            Material.ELYTRA -> "🪽"
            Material.BOOK -> "📖"
            Material.PAPER -> "📄"
            else -> "▪️"
        }
    }
}