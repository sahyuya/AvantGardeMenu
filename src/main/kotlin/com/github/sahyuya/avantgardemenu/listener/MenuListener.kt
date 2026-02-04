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

        // 管理者機能の処理
        if (key == "admin_promote_confirm") {
            handlePromoteConfirm(event, player)
            return
        }
        if (key == "admin_coreprotect_confirm") {
            handleCoreProtectConfirm(event, player)
            return
        }
        if (key == "admin_ban_confirm") {
            handleBanConfirm(event, player)
            return
        }
        if (key == "admin_worldsize_confirm") {
            handleWorldSizeConfirm(event, player)
            return
        }
        if (key == "admin_createworld_confirm") {
            handleCreateWorldConfirm(event, player)
            return
        }

        // メニューアイテムのクリック処理
        if (key.startsWith("menu_")) {
            val menuBuilder = JavaMenuBuilder(plugin)
            val shouldClose = handleMenuClick(key, player, menuBuilder)

            // サブメニューを開く場合は閉じない、コマンド実行の場合は閉じる
            if (shouldClose) {
                player.closeDialog()
            }
        }
    }

    private fun handleMenuClick(key: String, player: Player, menuBuilder: JavaMenuBuilder): Boolean {
        // "menu_<menuId>_<itemId>" の形式をパース
        val parts = key.removePrefix("menu_").split("_", limit = 2)

        if (parts.size != 2) {
            plugin.logger.warning("Invalid menu key format: $key")
            return true // エラー時は閉じる
        }

        val menuId = parts[0]
        val itemId = parts[1]

        // handleMenuClickがBooleanを返すようになったので、その値を返す
        return menuBuilder.handleMenuClick(player, menuId, itemId)
    }

    private fun handleToMapConfirm(event: PlayerCustomClickEvent, player: Player) {
        val view = event.dialogResponseView ?: return

        try {
            val url = view.getText("url") ?: run {
                player.sendMessage("§cURLが入力されていません")
                return
            }

            val height = view.getFloat("height") ?: 1f
            val width = view.getFloat("width") ?: 1f

            val menuBuilder = JavaMenuBuilder(plugin)
            menuBuilder.handleToMapConfirm(player, url, height, width)

            player.closeDialog()
        } catch (e: Exception) {
            plugin.logger.warning("Error handling tomap dialog: ${e.message}")
            player.sendMessage("§c入力の処理中にエラーが発生しました")
        }
    }

    // === 管理者機能のハンドラー ===

    private fun handlePromoteConfirm(event: PlayerCustomClickEvent, player: Player) {
        val view = event.dialogResponseView ?: return

        try {
            val targetPlayer = view.getText("player")?.trim() ?: run {
                player.sendMessage("§cプレイヤー名が入力されていません")
                return
            }

            val addGood = view.getBoolean("good") ?: false

            plugin.server.scheduler.runTask(plugin, Runnable {
                val command = if (addGood) {
                    "syokaku promote $targetPlayer GOOD"
                } else {
                    "syokaku promote $targetPlayer"
                }

                player.performCommand(command)
                player.sendMessage("§a昇格コマンドを実行しました: /$command")
            })

            player.closeDialog()
        } catch (e: Exception) {
            plugin.logger.warning("Error handling promote dialog: ${e.message}")
            player.sendMessage("§c入力の処理中にエラーが発生しました")
        }
    }

    private fun handleCoreProtectConfirm(event: PlayerCustomClickEvent, player: Player) {
        val view = event.dialogResponseView ?: return

        try {
            val doLookup = view.getBoolean("do_lookup") ?: false
            val doRollback = view.getBoolean("do_rollback") ?: false

            if (!doLookup && !doRollback) {
                player.sendMessage("§cLookupまたはRollbackのいずれかを選択してください")
                return
            }

            val targetPlayer = view.getText("player")?.trim() ?: run {
                player.sendMessage("§cプレイヤー名が入力されていません")
                return
            }

            val timeLookup = view.getText("time_lookup")?.trim() ?: "10h"
            val timeRollback = view.getText("time_rollback")?.trim() ?: "10h"
            val radius = view.getText("radius")?.trim() ?: ""
            val extraParams = view.getText("extra_params")?.trim() ?: ""
            val excludeOres = view.getBoolean("exclude_ores") ?: false

            plugin.server.scheduler.runTask(plugin, Runnable {
                // Lookupコマンドの実行
                if (doLookup) {
                    val lookupCmd = buildString {
                        append("co lookup u:$targetPlayer t:$timeLookup r:$radius")
                        if (excludeOres) {
                            append(" e:-coal_ore,-copper_ore,-iron_ore,-gold_ore,-redstone_ore,-lapis_ore,-diamond_ore,-emerald_ore")
                        }
                        if (extraParams.isNotEmpty()) {
                            append(" $extraParams")
                        }
                    }
                    player.performCommand(lookupCmd)
                    player.sendMessage("§aLookupを実行しました: /$lookupCmd")
                }

                // Rollbackコマンドの実行
                if (doRollback) {
                    val rollbackCmd = buildString {
                        append("co rollback u:$targetPlayer t:$timeRollback r:$radius")
                        if (excludeOres) {
                            append(" e:-coal_ore,-copper_ore,-iron_ore,-gold_ore,-redstone_ore,-lapis_ore,-diamond_ore,-emerald_ore,-nether_gold_ore,-nether_quartz_ore,-ancient_debris,-deepslate_coal_ore,-deepslate_copper_ore,-deepslate_iron_ore,-deepslate_gold_ore,-deepslate_redstone_ore,-deepslate_lapis_ore,-deepslate_diamond_ore,-deepslate_emerald_ore")
                        }
                        if (extraParams.isNotEmpty()) {
                            append(" $extraParams")
                        }
                    }
                    player.performCommand(rollbackCmd)
                    player.sendMessage("§cRollbackを実行しました: /$rollbackCmd")
                }
            })

            player.closeDialog()
        } catch (e: Exception) {
            plugin.logger.warning("Error handling CoreProtect dialog: ${e.message}")
            e.printStackTrace()
            player.sendMessage("§c入力の処理中にエラーが発生しました: ${e.message}")
        }
    }

    private fun handleBanConfirm(event: PlayerCustomClickEvent, player: Player) {
        val view = event.dialogResponseView ?: return

        try {
            val targetPlayer = view.getText("player")?.trim() ?: run {
                player.sendMessage("§cプレイヤー名が入力されていません")
                return
            }

            val time = view.getText("time")?.trim() ?: ""
            val reason = view.getText("reason")?.trim() ?: "理由なし"

            plugin.server.scheduler.runTask(plugin, Runnable {
                val command = "aban $targetPlayer $time $reason"
                player.performCommand(command)
                player.sendMessage("§cBANコマンドを実行しました: /$command")
            })

            player.closeDialog()
        } catch (e: Exception) {
            plugin.logger.warning("Error handling ban dialog: ${e.message}")
            player.sendMessage("§c入力の処理中にエラーが発生しました")
        }
    }

    private fun handleWorldSizeConfirm(event: PlayerCustomClickEvent, player: Player) {
        val view = event.dialogResponseView ?: return

        try {
            val targetPlayer = view.getText("player")?.trim() ?: run {
                player.sendMessage("§cプレイヤー名が入力されていません")
                return
            }

            val pointsInput = view.getText("points")?.trim() ?: "100"
            val points = try {
                pointsInput.toInt()
            } catch (e: NumberFormatException) {
                player.sendMessage("§cポイントは数値で入力してください")
                return
            }

            plugin.server.scheduler.runTask(plugin, Runnable {
                val command = "kakutyo $targetPlayer $points"
                player.performCommand(command)
                player.sendMessage("§aワールドサイズ拡張コマンドを実行しました: /$command")
            })

            player.closeDialog()
        } catch (e: Exception) {
            plugin.logger.warning("Error handling world size dialog: ${e.message}")
            player.sendMessage("§c入力の処理中にエラーが発生しました")
        }
    }

    private fun handleCreateWorldConfirm(event: PlayerCustomClickEvent, player: Player) {
        val view = event.dialogResponseView ?: return

        try {
            val worldName = view.getText("name")?.trim() ?: run {
                player.sendMessage("§cワールド名が入力されていません")
                return
            }

            val worldTypeIndex = view.getFloat("world_type")?.toInt() ?: 0
            val worldType = when (worldTypeIndex) {
                1 -> "nether"
                2 -> "the_end"
                else -> "normal"
            }

            val generationTypeIndex = view.getFloat("generation_type")?.toInt() ?: 0
            val generationType = if (generationTypeIndex == 1) "flat" else "normal"

            val noStructures = view.getBoolean("no_structures") ?: false
            val noNaturalMob = view.getBoolean("no_natural_mob") ?: false
            val noForcedMob = view.getBoolean("no_forced_mob") ?: false
            val noRadomTick = view.getBoolean("no_randomtick") ?: false

            plugin.server.scheduler.runTask(plugin, Runnable {
                // コマンドを順次実行
                val commands = mutableListOf<String>()

                // 1. ワールド作成
                val createCmd = buildString {
                    append("mv create $worldName $worldType")
                    append(" --world-type $generationType")
                    if (noStructures) {
                        append(" --no-structures")
                    }
                }
                commands.add(createCmd)

                // 2. ワールドにテレポート
                commands.add("mvtp $worldName ${player.name}")

                // 3. WorldBorder設定
                commands.add("wb $worldName set 50 0 0")
                commands.add("wb wshape $worldName square")

                // 4. モブスポーン設定
                if (noNaturalMob) {
                    commands.add("mv gamerule set doMobSpawning false $worldName")
                }
                if (noForcedMob) {
                    commands.add("rg flag __global__ mob-spawning deny")
                }

                // 5. ランダムティック設定
                if (noRadomTick) {
                    commands.add("mv gamerule set randomTickSpeed 0 $worldName")
                }

                // コマンドを順次実行（少し遅延を入れる）
                var delay = 0L
                commands.forEach { cmd ->
                    plugin.server.scheduler.runTaskLater(plugin, Runnable {
                        player.performCommand(cmd)
                        plugin.logger.info("Executed: /$cmd")
                    }, delay)
                    delay += 20L // 1秒ごとに実行
                }

                player.sendMessage("§aワールド作成を開始しました: $worldName")
                player.sendMessage("§7${commands.size}個のコマンドを順次実行中...")
            })

            player.closeDialog()
        } catch (e: Exception) {
            plugin.logger.warning("Error handling create world dialog: ${e.message}")
            e.printStackTrace()
            player.sendMessage("§c入力の処理中にエラーが発生しました: ${e.message}")
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