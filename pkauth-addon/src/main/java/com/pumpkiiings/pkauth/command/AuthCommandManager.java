package com.pumpkiiings.pkauth.command;

import com.pumpkiiings.pkauth.PkAuthAddon;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

@SuppressWarnings("UnstableApiUsage")
public class AuthCommandManager {

    public static void register(PkAuthAddon plugin) {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final io.papermc.paper.command.brigadier.Commands commands = event.registrar();
            
            commands.register(
                Commands.literal("auth")
                    .then(Commands.literal("reload")
                        .requires(source -> source.getSender().hasPermission("pkauth.admin"))
                        .executes(ctx -> {
                            plugin.reloadConfig();
                            plugin.getRpsManager().loadConfig();
                            if (plugin.getScoreboardManager() != null) {
                                plugin.getScoreboardManager().setupAllTablists();
                            }
                            ctx.getSource().getSender().sendRichMessage(plugin.getConfig().getString("messages.config_reloaded", "<#55FF55>Configuración de PkAuth recargada correctamente."));
                            return 1;
                        })
                    )
                    .then(Commands.literal("quit")
                        .executes(ctx -> {
                            if (ctx.getSource().getSender() instanceof org.bukkit.entity.Player player) {
                                com.pumpkiiings.pkauth.games.snake.game.SnakeGame game = com.pumpkiiings.pkauth.games.snake.game.SnakeGame.getGame(player);
                                if (game != null) game.stop();
                                
                                plugin.getRpsManager().quitGame(player);
                            }
                            return 1;
                        })
                    )
                    .then(Commands.literal("play")
                        .then(Commands.literal("snake")
                            .executes(ctx -> {
                                if (ctx.getSource().getSender() instanceof org.bukkit.entity.Player player) {
                                    new com.pumpkiiings.pkauth.games.snake.game.SnakeGame(player).start();
                                }
                                return 1;
                            })
                        )
                        .then(Commands.literal("rps")
                            .then(Commands.literal("bot")
                                .executes(ctx -> {
                                    if (ctx.getSource().getSender() instanceof org.bukkit.entity.Player player) {
                                        plugin.getRpsManager().openBotGame(player);
                                    }
                                    return 1;
                                })
                            )
                            .then(Commands.literal("accept")
                                .executes(ctx -> {
                                    if (ctx.getSource().getSender() instanceof org.bukkit.entity.Player player) {
                                        plugin.getRpsManager().acceptChallenge(player);
                                    }
                                    return 1;
                                })
                            )
                            .then(Commands.argument("jugador", io.papermc.paper.command.brigadier.argument.ArgumentTypes.player())
                                .executes(ctx -> {
                                    if (ctx.getSource().getSender() instanceof org.bukkit.entity.Player player) {
                                        try {
                                            org.bukkit.entity.Player target = ctx.getArgument("jugador", io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver.class).resolve(ctx.getSource()).get(0);
                                            plugin.getRpsManager().challengePlayer(player, target);
                                        } catch (Exception e) {
                                            player.sendRichMessage(plugin.getConfig().getString("messages.player_not_found", "<#FF5555>Jugador no encontrado."));
                                        }
                                    }
                                    return 1;
                                })
                            )
                        )
                    )
                    .build(),
                "Comando de administración de PkAuth"
            );
        });

        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final io.papermc.paper.command.brigadier.Commands commands = event.registrar();
            
            commands.register(
                Commands.literal("rps")
                    .then(Commands.literal("play")
                        .executes(ctx -> {
                            if (ctx.getSource().getSender() instanceof org.bukkit.entity.Player player) {
                                plugin.getRpsManager().openBotGame(player);
                            }
                            return 1;
                        })
                        .then(Commands.argument("jugador", io.papermc.paper.command.brigadier.argument.ArgumentTypes.player())
                            .executes(ctx -> {
                                if (ctx.getSource().getSender() instanceof org.bukkit.entity.Player player) {
                                    try {
                                        org.bukkit.entity.Player target = ctx.getArgument("jugador", io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver.class).resolve(ctx.getSource()).get(0);
                                        plugin.getRpsManager().challengePlayer(player, target);
                                    } catch (Exception e) {
                                        player.sendRichMessage(plugin.getConfig().getString("messages.player_not_found", "<#FF5555>Jugador no encontrado."));
                                    }
                                }
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("accept")
                        .executes(ctx -> {
                            if (ctx.getSource().getSender() instanceof org.bukkit.entity.Player player) {
                                plugin.getRpsManager().acceptChallenge(player);
                            }
                            return 1;
                        })
                    )
                    .then(Commands.literal("quit")
                        .executes(ctx -> {
                            if (ctx.getSource().getSender() instanceof org.bukkit.entity.Player player) {
                                plugin.getRpsManager().quitGame(player);
                            }
                            return 1;
                        })
                    )
                    .build(),
                "Comandos de Piedra Papel o Tijeras"
            );
            
            commands.register(
                Commands.literal("snake")
                    .then(Commands.literal("play")
                        .executes(ctx -> {
                            if (ctx.getSource().getSender() instanceof org.bukkit.entity.Player player) {
                                new com.pumpkiiings.pkauth.games.snake.game.SnakeGame(player).start();
                            }
                            return 1;
                        })
                    )
                    .then(Commands.literal("quit")
                        .executes(ctx -> {
                            if (ctx.getSource().getSender() instanceof org.bukkit.entity.Player player) {
                                com.pumpkiiings.pkauth.games.snake.game.SnakeGame game = com.pumpkiiings.pkauth.games.snake.game.SnakeGame.getGame(player);
                                if (game != null) game.stop();
                            }
                            return 1;
                        })
                    )
                    .build(),
                "Comandos de Snake"
            );
        });
    }
}
