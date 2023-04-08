package com.radangelzero.lastclickingwins;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scheduler.BukkitWorker;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

public class SubCommandsHandler implements CommandExecutor {
    private final LastClickingWins plugin;

    public SubCommandsHandler(LastClickingWins plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player p) {
            if (plugin.getConfig().get("enabled") == null) {
                plugin.saveConfig();
                p.sendMessage(ChatColor.RED + "No se ha encontrado el archivo de configuraciones, se ha vuelto a crear con las configuraciones por defecto");
                return true;
            }
            if (args.length > 0 && !args[0].equalsIgnoreCase("help")) {
                if (!plugin.getConfig().getBoolean("enabled")) {
                    p.sendMessage(ChatColor.RED + "El plugin esta desctivado");
                    if (!p.hasPermission("lcw.admin")) {
                        p.sendMessage(ChatColor.RED + "No tienes permiso para activar el plugin");
                        return true;
                    }
                    p.sendMessage("Activalo con el comando" + ChatColor.GOLD + "/lcw enable");
                    return true;
                }
                if (args[0].equalsIgnoreCase("join")) {
                    if (plugin.getConfig().getBoolean("started")) {
                        p.sendMessage(ChatColor.RED + "Hay un evento en curso");
                        return true;
                    }
                    plugin.getConfig().set("players." + p.getName(), null);
                    if (plugin.getConfig().get("totalPlayers") == null || plugin.getConfig().get("inGamePlayers") == null) {
                        plugin.getConfig().set("totalPlayers", 0);
                        plugin.getConfig().set("inGamePlayers", 0);
                        plugin.saveConfig();
                    }

                    plugin.getConfig().set("players." + p.getName() + ".isBreaking", false);
                    if (plugin.getConfig().get("players." + p.getName()) != null) {
                        plugin.getConfig().set("totalPlayers", plugin.getConfig().getInt("totalPlayers") + 1);
                        plugin.getConfig().set("inGamePlayers", plugin.getConfig().getInt("inGamePlayers") + 1);
                    }
                    p.setHealth(20f);
                    p.setFoodLevel(20);

                    plugin.saveConfig();
                    p.sendMessage(ChatColor.GREEN + "Has entrado al evento");
                    return true;
                }
                if (args[0].equalsIgnoreCase("leave")) {
                    if (plugin.getConfig().get("players." + p.getName()) == null) {
                        p.sendMessage(ChatColor.RED + "No estas registrado en el evento actual");
                        return true;
                    }
                    if (plugin.getConfig().getBoolean("started")) {
                        p.sendMessage(ChatColor.RED + "El evento ya ha comenzado, la unica forma de salir es la muerte");
                        return true;
                    }
                    if (plugin.getConfig().get("players." + p.getName() + ".isSpectator") != null) {
                        plugin.getConfig().set("totalPlayers", plugin.getConfig().getInt("totalPlayers") - 1);
                        plugin.getConfig().set("inGamePlayers", plugin.getConfig().getInt("inGamePlayers") - 1);
                    }
                    plugin.getConfig().set("players." + p.getName(), null);
                    plugin.saveConfig();
                    p.sendMessage(ChatColor.GREEN + "Has salido del evento");
                    return true;
                }

                if (!p.hasPermission("lcw.admin")) {
                    p.sendMessage(ChatColor.RED + "No tienes permisos para hacer esto");
                    return true;
                }

                if (args[0].equalsIgnoreCase("joinall")) {
                    int totalPlayersInGame = 0;
                    p.sendMessage(ChatColor.GOLD + "Agregando jugadores");
                    if (plugin.getConfig().get("totalPlayers") == null || plugin.getConfig().get("inGamePlayers") == null) {
                        plugin.getConfig().set("totalPlayers", 0);
                        plugin.getConfig().set("inGamePlayers", 0);
                        plugin.saveConfig();
                    }
                    for (Player player : plugin.getServer().getOnlinePlayers()) {
                        if (plugin.getConfig().get("players." + player.getName()) != null) {
                            continue;
                        }
                        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
                            @Override
                            public void run() {
                                if (!player.getGameMode().equals(GameMode.SURVIVAL)) {
                                    player.setGameMode(GameMode.SURVIVAL);
                                }
                            }
                        });
                        plugin.getConfig().set("players." + player.getName() + ".isBreaking", false);
                        player.setHealth(20f);
                        player.setFoodLevel(20);
                        p.sendMessage(ChatColor.GREEN + player.getName() + " agregado");
                    }
                    for (String name : plugin.getConfig().getConfigurationSection("players").getKeys(false)) {
                        if (plugin.getConfig().get("players." + name + ".isSpectator") != null) {
                            continue;
                        }
                        totalPlayersInGame += 1;
                    }
                    plugin.getConfig().set("totalPlayers", totalPlayersInGame);
                    plugin.getConfig().set("inGamePlayers", totalPlayersInGame);
                    plugin.saveConfig();
                    return true;
                }
                if (args[0].equalsIgnoreCase("leaveall")) {
                    plugin.getConfig().set("players", null);
                    plugin.getConfig().set("totalPlayers", null);
                    plugin.getConfig().set("inGamePlayers", null);
                    plugin.saveConfig();
                    return true;
                }
                if (args[0].equalsIgnoreCase("joinspectator")) {
                    if (plugin.getConfig().get("players." + p.getName()) != null) {
                        plugin.getConfig().set("totalPlayers", plugin.getConfig().getInt("totalPlayers") - 1);
                        plugin.getConfig().set("inGamePlayers", plugin.getConfig().getInt("inGamePlayers") - 1);
                        plugin.getConfig().set("players." + p.getName(), null);

                    }
                    plugin.getConfig().set("players." + p.getName() + ".isSpectator", true);
                    plugin.saveConfig();
                    p.sendMessage(ChatColor.GREEN + p.getName() + ", has entrado como espectador");
                    return true;
                }
                if (args[0].equalsIgnoreCase("pause")) {
                    plugin.getServer().getScheduler().cancelTask(plugin.getConfig().getInt("mainTaskID"));
                    plugin.getConfig().set("mainTaskID", null);
                    for (String name : plugin.getConfig().getConfigurationSection("players").getKeys(false)) {
                        Player player = plugin.getServer().getPlayer(name);
                        if (player == null) {
                            plugin.getConfig().set("players." + name, null);
                            continue;
                        }
                        player.showTitle(new Title() {
                            @Override
                            public @NotNull Component title() {
                                return Component.text(ChatColor.GOLD + "El evento ha sido pausado");
                            }

                            @Override
                            public @NotNull Component subtitle() {
                                return Component.text("Pueden tomar un descanso, se les notificara cuando empieze nuevamente");
                            }

                            @Override
                            public @Nullable Times times() {
                                return new Times() {
                                    @Override
                                    public @NotNull Duration fadeIn() {
                                        return Duration.ofMillis(100);
                                    }

                                    @Override
                                    public @NotNull Duration stay() {
                                        return Duration.ofMillis(4800);
                                    }

                                    @Override
                                    public @NotNull Duration fadeOut() {
                                        return Duration.ofMillis(100);
                                    }
                                };
                            }

                            @Override
                            public <T> @UnknownNullability T part(@NotNull TitlePart<T> part) {
                                return null;
                            }
                        });
                    }
                }
                if (args[0].equalsIgnoreCase("enable")) {
                    plugin.getConfig().set("enabled", true);
                    plugin.saveConfig();
                    p.sendMessage("Se habilito el plugin LastClickingWins");
                    return true;
                }
                if (args[0].equalsIgnoreCase("disable")) {
                    if (plugin.getConfig().getBoolean("started")) {
                        p.sendMessage(ChatColor.RED + "Hay un evento en curso");
                        return true;
                    }
                    plugin.getConfig().set("enabled", false);
                    plugin.getConfig().set("mainTaskID", null);
                    plugin.saveConfig();
                    p.sendMessage("Se deshabilito el plugin LastClickingWins");
                    return true;
                }
                if (args[0].equalsIgnoreCase("start") || args[0].equalsIgnoreCase("resume")) {
                    if (!plugin.getConfig().getBoolean("enabled")) {
                        p.sendMessage(ChatColor.RED + "LastClickingWins esta desactivado");
                        return true;
                    }
                    if (plugin.getConfig().getBoolean("started") && !args[0].equalsIgnoreCase("resume")) {
                        p.sendMessage(ChatColor.RED + "Ya hay otro evento en curso");
                        return true;
                    }
                    ConfigurationSection players = plugin.getConfig().getConfigurationSection("players");
                    if (players == null) {
                        p.sendMessage(ChatColor.RED + "No hay jugadores registrados");
                        return true;
                    }
                    plugin.getConfig().set("countdown", 10);
                    plugin.getConfig().set("phase", 1);
                    for (String name : players.getKeys(false)) {
                        Player player = plugin.getServer().getPlayer(name);
                        if (player != null) {
                            player.showTitle(new Title() {
                                @Override
                                public @NotNull Component title() {
                                    return Component.text(ChatColor.GOLD + "El evento va a " + (args[0].equalsIgnoreCase("start") ? "comenzar" : "ser reanudado"));
                                }

                                @Override
                                public @NotNull Component subtitle() {
                                    return Component.text(ChatColor.RED + "Comienza a picar antes de que acabe la cuenta regresiva");
                                }

                                @Override
                                public @Nullable Times times() {
                                    return new Times() {
                                        @Override
                                        public @NotNull Duration fadeIn() {
                                            return Duration.ofMillis(100);
                                        }

                                        @Override
                                        public @NotNull Duration stay() {
                                            return Duration.ofMillis(4800);
                                        }

                                        @Override
                                        public @NotNull Duration fadeOut() {
                                            return Duration.ofMillis(100);
                                        }
                                    };
                                }

                                @Override
                                public <T> @UnknownNullability T part(@NotNull TitlePart<T> part) {
                                    return null;
                                }
                            });
                        }
                    }
                    BukkitTask countdown = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, new Runnable() {
                        @Override
                        public void run() {
                            if (plugin.getConfig().getInt("countdown") <= 0) {
                                Date startDate = new Date();
                                plugin.getConfig().set("startTime", startDate.getTime());
                                plugin.getLogger().log(Level.INFO, String.valueOf(startDate.getTime()));
                                plugin.getConfig().set("countdown", null);
                                plugin.getServer().getScheduler().cancelTask(plugin.getConfig().getInt("countdownTaskID"));
                                for (String name : plugin.getConfig().getConfigurationSection("players").getKeys(false)) {
                                    Player player = plugin.getServer().getPlayer(name);
                                    if (player != null) {
                                        player.showTitle(new Title() {
                                            @Override
                                            public @NotNull Component title() {
                                                return Component.text(ChatColor.GOLD + "YA");
                                            }

                                            @Override
                                            public @NotNull Component subtitle() {
                                                return Component.text(ChatColor.RED + "No levantes el dedo");
                                            }

                                            @Override
                                            public @Nullable Times times() {
                                                return new Times() {
                                                    @Override
                                                    public @NotNull Duration fadeIn() {
                                                        return Duration.ofMillis(100);
                                                    }

                                                    @Override
                                                    public @NotNull Duration stay() {
                                                        return Duration.ofMillis(4800);
                                                    }

                                                    @Override
                                                    public @NotNull Duration fadeOut() {
                                                        return Duration.ofMillis(100);
                                                    }
                                                };
                                            }

                                            @Override
                                            public <T> @UnknownNullability T part(@NotNull TitlePart<T> part) {
                                                return null;
                                            }
                                        });
                                    }
                                }
                            } else {
                                for (String name : plugin.getConfig().getConfigurationSection("players").getKeys(false)) {
                                    Player player = plugin.getServer().getPlayer(name);
                                    if (player != null) {
                                        player.showTitle(new Title() {
                                            @Override
                                            public @NotNull Component title() {
                                                return Component.text(ChatColor.GOLD + Integer.toString(plugin.getConfig().getInt("countdown")));
                                            }

                                            @Override
                                            public @NotNull Component subtitle() {
                                                return Component.text(ChatColor.RED + "Comienza a picar antes de que acabe la cuenta regresiva");
                                            }

                                            @Override
                                            public @Nullable Times times() {
                                                return new Times() {
                                                    @Override
                                                    public @NotNull Duration fadeIn() {
                                                        return Duration.ofMillis(50);
                                                    }

                                                    @Override
                                                    public @NotNull Duration stay() {
                                                        return Duration.ofMillis(900);
                                                    }

                                                    @Override
                                                    public @NotNull Duration fadeOut() {
                                                        return Duration.ofMillis(50);
                                                    }
                                                };
                                            }

                                            @Override
                                            public <T> @UnknownNullability T part(@NotNull TitlePart<T> part) {
                                                return null;
                                            }
                                        });
                                    }
                                }
                                plugin.getConfig().set("countdown", plugin.getConfig().getInt("countdown") - 1);
                            }
                        }
                    }, 20 * 5, 20);
                    plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
                        @Override
                        public void run() {
                            for (String name : plugin.getConfig().getConfigurationSection("players").getKeys(false)) {
                                Player playerCheck = plugin.getServer().getPlayer(name);
                                if (playerCheck == null) {
                                    continue;
                                }
                                playerCheck.sendMessage(ChatColor.RED + "Fase 1 iniciada");
                                playerCheck.sendMessage(ChatColor.RED + plugin.getConfig().getString("phase1.description"));
                                playerCheck.showTitle(new Title() {
                                    @Override
                                    public @NotNull Component title() {
                                        return Component.text(ChatColor.RED + "Fase 1 iniciada");
                                    }

                                    @Override
                                    public @NotNull Component subtitle() {
                                        return Component.text(ChatColor.RED + plugin.getConfig().getString("phase1.description"));
                                    }

                                    @Override
                                    public @Nullable Times times() {
                                        return new Times() {
                                            @Override
                                            public @NotNull Duration fadeIn() {
                                                return Duration.ofMillis(100);
                                            }

                                            @Override
                                            public @NotNull Duration stay() {
                                                return Duration.ofMillis(4800);
                                            }

                                            @Override
                                            public @NotNull Duration fadeOut() {
                                                return Duration.ofMillis(100);
                                            }
                                        };
                                    }

                                    @Override
                                    public <T> @UnknownNullability T part(@NotNull TitlePart<T> part) {
                                        return null;
                                    }
                                });
                            }
                        }
                    }, 20 * 20);
                    plugin.getConfig().set("countdownTaskID", countdown.getTaskId());
                    BukkitTask mainTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, new Runnable() {
                        @Override
                        public void run() {
                            plugin.getLogger().log(Level.INFO, "loop");
                            Date now = new Date();
                            int leftPlayers = 0;
                            for (String name : plugin.getConfig().getConfigurationSection("players").getKeys(false)){
                                Player player = plugin.getServer().getPlayer(name);
                                if (plugin.getConfig().get("players." + name + ".isSpectator" ) == null) {
                                    continue;
                                }
                                leftPlayers += 1;
                            }
                            if (leftPlayers == 1) {
                                plugin.getLogger().log(Level.INFO, "Tenemos Ganador");
                                plugin.getServer().getScheduler().cancelTask(plugin.getConfig().getInt("mainTaskID"));
                                plugin.getConfig().set("mainTaskID", null);
                                for (String name : plugin.getConfig().getConfigurationSection("players").getKeys(false)) {
                                    Player playerCheck = plugin.getServer().getPlayer(name);
                                    if (playerCheck == null) {
                                        continue;
                                    }
                                    if (plugin.getConfig().get("players." + playerCheck.getName() + ".isSpectator") != null) {
                                        continue;
                                    }
                                    for (Player announceWinner : plugin.getServer().getOnlinePlayers()) {
                                        announceWinner.sendMessage(ChatColor.GOLD + playerCheck.getName() + " es el ganador");
                                        announceWinner.showTitle(new Title() {
                                            @Override
                                            public @NotNull Component title() {
                                                return Component.text(ChatColor.GOLD + playerCheck.getName());
                                            }

                                            @Override
                                            public @NotNull Component subtitle() {
                                                return Component.text("Es el ganador");
                                            }

                                            @Override
                                            public @Nullable Times times() {
                                                return new Times() {
                                                    @Override
                                                    public @NotNull Duration fadeIn() {
                                                        return Duration.ofMillis(200);
                                                    }

                                                    @Override
                                                    public @NotNull Duration stay() {
                                                        return Duration.ofMillis(9800);
                                                    }

                                                    @Override
                                                    public @NotNull Duration fadeOut() {
                                                        return Duration.ofMillis(200);
                                                    }
                                                };
                                            }

                                            @Override
                                            public <T> @UnknownNullability T part(@NotNull TitlePart<T> part) {
                                                return null;
                                            }
                                        });
                                    }
                                    break;
                                }
                                return;
                            }
                            if (plugin.getConfig().get("phase" + (plugin.getConfig().getInt("phase") + 1)) != null) {
                                plugin.getLogger().log(Level.INFO, "phase" + (plugin.getConfig().getInt("phase") + 1) + " isn't null");
                                if ((plugin.getConfig().getLong("phase" + (plugin.getConfig().getInt("phase") + 1) + ".timeTo") * 1000) + plugin.getConfig().getLong("startTime") <= now.getTime()) {
                                    plugin.getLogger().log(Level.INFO, "phase" + (plugin.getConfig().getInt("phase") + 1) + " deployed");
                                    plugin.getConfig().set("phase", plugin.getConfig().getInt("phase") + 1);
                                    plugin.saveConfig();
                                    for (String name : plugin.getConfig().getConfigurationSection("players").getKeys(false)) {
                                        Player playerCheck = plugin.getServer().getPlayer(name);
                                        if (playerCheck == null) {
                                            continue;
                                        }
                                        playerCheck.sendMessage(ChatColor.RED + "Fase " + plugin.getConfig().getInt("phase") + " iniciada");
                                        playerCheck.sendMessage(ChatColor.RED + plugin.getConfig().getString("phase" + plugin.getConfig().getInt("phase") + ".description"));
                                        playerCheck.showTitle(new Title() {
                                            @Override
                                            public @NotNull Component title() {
                                                return Component.text(ChatColor.RED + "Fase " + plugin.getConfig().getInt("phase") + " iniciada");
                                            }

                                            @Override
                                            public @NotNull Component subtitle() {
                                                return Component.text(ChatColor.RED + plugin.getConfig().getString("phase" + plugin.getConfig().getInt("phase") + ".description"));
                                            }

                                            @Override
                                            public @Nullable Times times() {
                                                return new Times() {
                                                    @Override
                                                    public @NotNull Duration fadeIn() {
                                                        return Duration.ofMillis(100);
                                                    }

                                                    @Override
                                                    public @NotNull Duration stay() {
                                                        return Duration.ofMillis(4800);
                                                    }

                                                    @Override
                                                    public @NotNull Duration fadeOut() {
                                                        return Duration.ofMillis(100);
                                                    }
                                                };
                                            }

                                            @Override
                                            public <T> @UnknownNullability T part(@NotNull TitlePart<T> part) {
                                                return null;
                                            }
                                        });
                                    }
                                    return;
                                }
                            }
                            for (String name : plugin.getConfig().getConfigurationSection("players").getKeys(false)) {
                                plugin.getLogger().log(Level.INFO, "Checking " + name);
                                if (plugin.getConfig().get("players." + name + ".isSpectator") != null) {
                                    continue;
                                }
                                Player player = plugin.getServer().getPlayer(name);
                                if (player == null) {
                                    plugin.getConfig().set("players." + name + ".isSpectator", true);
                                    plugin.getConfig().set("players." + name + ".disconnected", true);
                                    plugin.getConfig().set("totalPlayers", plugin.getConfig().getInt("totalPlayers") - 1);
                                    plugin.getConfig().set("inGamePlayers", plugin.getConfig().getInt("inGamePlayers") - 1);
                                    for (String notLeaved : plugin.getConfig().getConfigurationSection("players").getKeys(false)) {
                                        Player playerNotLeaved = plugin.getServer().getPlayer(notLeaved);
                                        playerNotLeaved.sendMessage(ChatColor.RED + player.getName() + " se ha retirado");
                                        playerNotLeaved.sendMessage(plugin.getConfig().getInt("inGamePlayers") + " restantes");
                                    }
                                    continue;
                                }
                                if ((!plugin.getConfig().getBoolean("players." + name + ".isBreaking")) && plugin.getConfig().get("players." + name + ".lastBreaking") == null) {
                                    plugin.getLogger().log(Level.INFO, "Establecido ultimo picado de " + name);
                                    plugin.getConfig().set("players." + name + ".lastBreaking", now.getTime());
                                    plugin.saveConfig();
                                    continue;
                                }
                                if (!plugin.getConfig().getBoolean("players." + name + ".isBreaking")) {
                                    plugin.getLogger().log(Level.INFO, name + " no esta rompiendo");
                                    plugin.getLogger().log(Level.INFO, plugin.getConfig().getLong("players." + name + ".lastBreaking") + (plugin.getConfig().getLong("phase" + plugin.getConfig().getInt("phase") + ".timeToDamage") * 1000) + "");
                                    plugin.getLogger().log(Level.INFO, now.getTime() + "");
                                    if (plugin.getConfig().getLong("players." + name + ".lastBreaking") + (plugin.getConfig().getLong("phase" + plugin.getConfig().getInt("phase") + ".timeToDamage") * 1000) <= now.getTime()) {
                                        plugin.getLogger().log(Level.INFO, name + " danado");
                                        plugin.getConfig().set("players." + name + ".lastBreaking", now.getTime());
                                        plugin.saveConfig();
                                        damagePlayer(player);
                                    }
                                    continue;
                                }
                                if (plugin.getConfig().getLong("players." + name + ".nextSkillCheck") <= now.getTime() && plugin.getConfig().get("players." + name + ".particlesHandlerID") == null) {
                                    plugin.getLogger().log(Level.INFO, name + " skillCheck iniciado");
                                    skillTest(player);
                                }
                            }
                        }
                    }, (long) 20 * 15, (long) 20);
                    plugin.getConfig().set("started", true);
                    plugin.getConfig().set("mainTaskID", mainTask.getTaskId());
                    plugin.saveConfig();
                    p.sendMessage("started");
                    return true;
                }
                if (args[0].equalsIgnoreCase("stop")) {
                    if (args[1].equalsIgnoreCase("") || !args[1].equalsIgnoreCase("confirm")) {
                        p.sendMessage(ChatColor.RED + "Para confirmar la detencion del evento escribe /lcw stop confirm");
                        return true;
                    }
                    plugin.getServer().getScheduler().cancelTasks(plugin);
                    plugin.getConfig().set("started", false);
                    plugin.getServer().getScheduler().cancelTask(plugin.getConfig().getInt("mainTaskID"));
                    plugin.getConfig().set("mainTaskID", null);
                    plugin.getConfig().set("totalPlayers", 0);
                    plugin.getConfig().set("inGamePlayers", 0);
                    plugin.getConfig().set("players", null);
                    plugin.saveConfig();
                    p.sendMessage("stopped");
                    return true;
                }
                if (args[0].equalsIgnoreCase("setloserspawn")) {
                    plugin.getConfig().set("losersSpawn", p.getLocation());
                    plugin.saveConfig();
                    p.sendMessage("Se cambio el punto de los perdedores LastClickingWins");
                    return true;
                }
                if (args[0].equalsIgnoreCase("loserspawn")) {
                    p.teleport(Objects.requireNonNull(plugin.getConfig().getLocation("losersSpawn")));
                    p.sendMessage("Moviendo al punto de los perdedores LastClickingWins");
                    return true;
                }
                if (args[0].equalsIgnoreCase("setblock")) {
                    if (plugin.getConfig().getBoolean("started")) {
                        p.sendMessage(ChatColor.RED + "Hay un evento en curso");
                        return true;
                    }
                    if (args.length != 2) {
                        p.sendMessage(ChatColor.RED + "No se ha especificado un bloque, uso /lcwsetblock <bloque>");
                        return true;
                    }
                    if (Material.getMaterial(args[1]) != null) {
                        p.sendMessage(ChatColor.RED + "No se encontro ningun objeto del juego con ese nombre");
                        return true;
                    }
                    if (Objects.requireNonNull(Material.getMaterial(args[1])).isBlock()) {
                        p.sendMessage(ChatColor.RED + "Ese no es un bloque");
                        return true;
                    }
                    plugin.getConfig().set("block", args[1]);
                    plugin.saveConfig();
                    p.sendMessage(ChatColor.GREEN + "Se ha establecido " + ChatColor.GOLD + args[1] + ChatColor.GREEN + " como el bloque del plugin");
                    return true;
                }
                if (args[0].equalsIgnoreCase("forcejoin")) {
                    if (args.length < 2) {
                        p.sendMessage(ChatColor.RED + "Tienes que especificar a alguien");
                        p.sendMessage(ChatColor.GOLD + "/lcw forcejoin <Jugador>");
                        p.sendMessage(ChatColor.RED + "Si quieres entrar usa el comnado " + ChatColor.YELLOW + ChatColor.BOLD + "/lcw join");
                        return true;
                    }
                    if (plugin.getConfig().getBoolean("started")) {
                        p.sendMessage(ChatColor.RED + "Hay un evento en curso");
                        return true;
                    }
                    if (plugin.getServer().getPlayer(args[1]) == null) {
                        p.sendMessage(ChatColor.RED + "No se encontro a " + ChatColor.YELLOW + args[1]);
                        return true;
                    }
                    if (plugin.getConfig().get("players." + args[1]) != null) {
                        p.sendMessage(ChatColor.RED + args[1] + " ya esta en el evento");
                        return true;
                    }
                    plugin.getConfig().set("players." + p.getName(), null);
                    if (plugin.getConfig().get("totalPlayers") == null || plugin.getConfig().get("inGamePlayers") == null) {
                        plugin.getConfig().set("totalPlayers", 0);
                        plugin.getConfig().set("inGamePlayers", 0);
                        plugin.saveConfig();
                    }

                    plugin.getConfig().set("players." + plugin.getServer().getPlayer(args[1]).getName() + ".isBreaking", false);
                    if (plugin.getConfig().get("players." + plugin.getServer().getPlayer(args[1]).getName()) != null) {
                        plugin.getConfig().set("totalPlayers", plugin.getConfig().getInt("totalPlayers") + 1);
                        plugin.getConfig().set("inGamePlayers", plugin.getConfig().getInt("inGamePlayers") + 1);
                    }
                    plugin.getServer().getPlayer(args[1]).setHealth(20f);
                    plugin.getServer().getPlayer(args[1]).setFoodLevel(20);
                    plugin.getConfig().set("players." + plugin.getServer().getPlayer(args[1]).getName() + ".lives", plugin.getConfig().getDouble("playerLives"));
                    plugin.saveConfig();
                    p.sendMessage(ChatColor.GREEN + "Has metido a " + ChatColor.GOLD + args[1] + ChatColor.GREEN + " al evento");
                    return true;
                }
                if (args[0].equalsIgnoreCase("forceleave")) {
                    if (args.length < 2) {
                        p.sendMessage(ChatColor.RED + "Tienes que especificar a alguien");
                        p.sendMessage(ChatColor.GOLD + "/lcw forceleave <Jugador>");
                        p.sendMessage(ChatColor.RED + "Si quieres salir usa el comnado " + ChatColor.YELLOW + ChatColor.BOLD + "/lcw leave");
                        return true;
                    }
                    if (plugin.getServer().getPlayer(args[1]) == null) {
                        p.sendMessage(ChatColor.RED + "No se encontro a " + ChatColor.YELLOW + args[1]);
                        return true;
                    }
                    if (plugin.getConfig().getBoolean("started")) {
                        p.sendMessage(ChatColor.RED + "Has eliminado a" + args[1]);
                        p.showTitle(new Title() {
                            @Override
                            public @NotNull Component title() {
                                return Component.text(ChatColor.RED + args[1]);
                            }

                            @Override
                            public @NotNull Component subtitle() {
                                return Component.text("Ha sido expulsado de la competencia");
                            }

                            @Override
                            public @Nullable Times times() {
                                return new Times() {
                                    @Override
                                    public @NotNull Duration fadeIn() {
                                        return Duration.ofMillis(100);
                                    }

                                    @Override
                                    public @NotNull Duration stay() {
                                        return Duration.ofMillis(4800);
                                    }

                                    @Override
                                    public @NotNull Duration fadeOut() {
                                        return Duration.ofMillis(100);
                                    }
                                };
                            }

                            @Override
                            public <T> @UnknownNullability T part(@NotNull TitlePart<T> part) {
                                return null;
                            }
                        });
                        if (plugin.getConfig().get("players." + args[1] + ".isSpectator") != null) {
                            Player expulsedPlayer = plugin.getServer().getPlayer(args[1]);
                            plugin.getConfig().set("totalPlayers", plugin.getConfig().getInt("totalPlayers") - 1);
                            plugin.getConfig().set("inGamePlayers", plugin.getConfig().getInt("inGamePlayers") - 1);
                            plugin.getConfig().set("players." + args[1] + ".isSpectator", true);
                            plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
                                @Override
                                public void run() {
                                    expulsedPlayer.setHealth(20f);
                                    expulsedPlayer.setGameMode(GameMode.SPECTATOR);
                                    expulsedPlayer.teleport(plugin.getConfig().getLocation("losersSpawn"));
                                }
                            });
                        }
                        plugin.saveConfig();
                        return true;
                    }
                    if (plugin.getConfig().get("players." + args[1] + ".isSpectator") != null) {
                        plugin.getConfig().set("totalPlayers", plugin.getConfig().getInt("totalPlayers") - 1);
                        plugin.getConfig().set("inGamePlayers", plugin.getConfig().getInt("inGamePlayers") - 1);
                    }
                    plugin.getConfig().set("players." + args[1], null);
                    plugin.saveConfig();
                    p.sendMessage(ChatColor.GREEN + "Has sacado a " + ChatColor.GOLD + "del evento");
                    return true;
                }
                if (args[0].equalsIgnoreCase("checkafktimeinterval")) {
                    if (plugin.getConfig().getInt("checkAFKMinTime") == plugin.getConfig().getInt("checkAFKMaxTime")) {
                        p.sendMessage("El intervalo de la prueba de afk esta establecida para ser realizada cada " + ChatColor.GOLD + plugin.getConfig().getInt("checkAFKMinTime") + ChatColor.RESET + " segundos");
                        return true;
                    }
                    p.sendMessage("El intervalo de la prueba de afk esta establecida para ser realizada entre cada " + ChatColor.GOLD + plugin.getConfig().getInt("checkAFKMinTime") + ChatColor.RESET + " y " + ChatColor.GOLD + plugin.getConfig().getInt("checkAFKMinTime") + ChatColor.RESET + " segundos");
                    return true;
                }
                if (args[0].equalsIgnoreCase("setcheckafktimeinterval")) {
                    if (args.length < 2) {
                        p.sendMessage(ChatColor.RED + "No se ha deifnido el tiempo");
                        p.sendMessage(ChatColor.GOLD + "/lcw setcheckafktimeinterval <segundos> [segundos]");
                        return true;
                    }
                    if (plugin.getConfig().getBoolean("started")) {
                        p.sendMessage(ChatColor.RED + "Hay un evento en curso");
                        return true;
                    }
                    try {
                        int firstValue = Integer.parseInt(args[1]);
                        plugin.getConfig().set("checkAFKMinTime", firstValue);
                        if (args.length == 3) {
                            try {
                                int secondValue = Integer.parseInt(args[2]);
                                plugin.getConfig().set("checkAFKMaxTime", secondValue);
                                return true;
                            } catch (NumberFormatException nfe) {
                                p.sendMessage(ChatColor.RED + args[2] + " no es un numero");
                                return true;
                            }
                        }
                        plugin.saveConfig();
                        return true;
                    } catch (NumberFormatException nfe) {
                        p.sendMessage(ChatColor.RED + args[1] + " no es un numero");
                        return true;
                    }
                }
                if (args[0].equalsIgnoreCase("reload")) {
                    if (plugin.getConfig().getBoolean("started")) {
                        p.sendMessage(ChatColor.RED + "Hay un evento en curso");
                        return true;
                    }
                    plugin.reloadConfig();
                    plugin.getLogger().log(Level.INFO, "Se ha recargado la configuracion");
                    p.sendMessage(ChatColor.YELLOW + "Configurcion de lcw recargada");
                    return true;
                }
            }
            p.sendMessage(ChatColor.YELLOW + "Ayuda de Last Clicking Wins");
            p.sendMessage(ChatColor.GRAY + "<valor> es un valor obligatorio");
            p.sendMessage(ChatColor.GRAY + "[valor] es un valor opcional");
            p.sendMessage(ChatColor.GOLD + "/lcw:" + ChatColor.RESET + " Muestra este mensaje");
            p.sendMessage(ChatColor.GOLD + "/lcw enable:" + ChatColor.RESET + " Activa LastClickingWins");
            p.sendMessage(ChatColor.GOLD + "/lcw disble:" + ChatColor.RESET + " Desactiva LastClickingWins");
            p.sendMessage(ChatColor.GOLD + "/lcw loserspawn:" + ChatColor.RESET + " Teletransportate al punto a donde se moveran los perdedores");
            p.sendMessage(ChatColor.GOLD + "/lcw setloserspawn:" + ChatColor.RESET + " Establece el punto donde seran teletransportados los perdedores");
            p.sendMessage(ChatColor.GOLD + "/lcw setblock <bloque>:" + ChatColor.RESET + " Elige el bloque que usara el plugin");
            p.sendMessage(ChatColor.GOLD + "/lcw checkafktimeinterval:" + ChatColor.RESET + " Muestra los intervalos establecidos para las pruebas de afk");
            p.sendMessage(ChatColor.GOLD + "/lcw setcheckafktimeinterval <segundos> [segundos]:" + ChatColor.RESET + " Establece el tiempo que tiene que pasar para inicar una prueba de afk, si solo se define el primer valor sera un intervalo fijo, si se definen los 2 sera un intervalo aleatorio entre esos valores");
            p.sendMessage(ChatColor.GOLD + "/lcw setplayerlives <numero_vidas>:" + ChatColor.RESET + " Elige el numero de vidas que tendran los jugadores");
            p.sendMessage(ChatColor.GOLD + "/lcw join:" + ChatColor.RESET + " Entra al evento de LCW");
            p.sendMessage(ChatColor.GOLD + "/lcw forcejoin:" + ChatColor.RESET + " Has que alguien entre al evento");
            p.sendMessage(ChatColor.GOLD + "/lcw leave:" + ChatColor.RESET + " Sal del evento de LCW");
            p.sendMessage(ChatColor.GOLD + "/lcw forceleave:" + ChatColor.RESET + " Has que alguien salga del evento");
            p.sendMessage(ChatColor.GOLD + "/lcw start:" + ChatColor.RESET + " Inicia el evento");
            p.sendMessage(ChatColor.GOLD + "/lcw stop:" + ChatColor.RESET + " Detiene el evento y regresa una lista con los jugadores restantes");
            return true;
        }
        sender.sendMessage("Es mas facil configurar esto dentro del juego");
        return true;
    }

    public void damagePlayer(Player player) {
        double playerHealth = player.getHealth();
        plugin.getLogger().log(Level.INFO, "Damage function");
        if (plugin.getConfig().getDouble("phase" + plugin.getConfig().getInt("phase") + ".damage") >= playerHealth) {
            player.setHealth(20f);
            player.setFoodLevel(20);
            plugin.getConfig().set("inGamePlayers", plugin.getConfig().getInt("inGamePlayers") - 1);
            plugin.getConfig().set("players." + player.getName() + ".isSpectator", true);
            plugin.saveConfig();
            plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    player.setGameMode(GameMode.SPECTATOR);
                    player.teleport(plugin.getConfig().getLocation("losersSpawn"));
                }
            });
            player.sendMessage(ChatColor.RED + "Has sido eliminado");
            player.sendMessage("Quedan " + plugin.getConfig().getInt("inGamePlayers") + " en el juego");
            player.showTitle(new Title() {
                @Override
                public @NotNull Component title() {
                    return Component.text(ChatColor.RED + "Has sido eliminado");
                }

                @Override
                public @NotNull Component subtitle() {
                    return null;
                }

                @Override
                public @Nullable Times times() {
                    return new Times() {
                        @Override
                        public @NotNull Duration fadeIn() {
                            return Duration.ofMillis(200);
                        }

                        @Override
                        public @NotNull Duration stay() {
                            return Duration.ofMillis(9600);
                        }

                        @Override
                        public @NotNull Duration fadeOut() {
                            return Duration.ofMillis(200);
                        }
                    };
                }

                @Override
                public <T> @UnknownNullability T part(@NotNull TitlePart<T> part) {
                    return null;
                }
            });
            for (String name : plugin.getConfig().getConfigurationSection("players").getKeys(false)) {
                Player playerPlaying = plugin.getServer().getPlayer(name);

                if (playerPlaying == null) {
                    continue;
                }
                if (name.equals(player.getName())) {
                    continue;
                }
                playerPlaying.sendMessage(ChatColor.RED + player.getName() + " ha sido eliminado");
                playerPlaying.sendMessage(plugin.getConfig().getInt("inGamePlayers") + " restantes");
            }
            return;
        }
        plugin.getLogger().log(Level.INFO, "Damage " + plugin.getConfig().getDouble("phase" + plugin.getConfig().getInt("phase") + ".damage"));
        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                player.damage(plugin.getConfig().getDouble("phase" + plugin.getConfig().getInt("phase") + ".damage"));
            }
        });
    }

    public void skillTest(Player player) {
        Date now = new Date();
        Location blockLocation = plugin.getConfig().getLocation("players." + player.getName() + ".blockLocation");
        Calendar startTime = Calendar.getInstance();
        startTime.setTime(new Date());
        startTime.add(Calendar.SECOND, 1);
        plugin.getConfig().set("players." + player.getName() + ".skillCheckStartTime", startTime.getTimeInMillis());
        Calendar endTime = Calendar.getInstance();
        endTime.setTime(new Date());
        endTime.add(Calendar.SECOND, plugin.getConfig().getInt("phase" + plugin.getConfig().getInt("phase") + ".skillCheckTimeLimit") + 2);
        plugin.getConfig().set("players." + player.getName() + ".skillCheckEndTime", endTime.getTimeInMillis());
        Location blockFaceLocation = new Location(player.getWorld(), blockLocation.getX() + 0.5, blockLocation.getY() + 0.5, blockLocation.getZ() + 0.5);
        RayTraceResult blockData = player.rayTraceBlocks(6f);
        List<BlockFace> faces = new ArrayList<BlockFace>();
        if (!Objects.equals(BlockFace.UP, blockData.getHitBlockFace())) {
            faces.add(BlockFace.UP);
        }
        if (!Objects.equals(BlockFace.NORTH, blockData.getHitBlockFace())) {
            faces.add(BlockFace.NORTH);
        }
        if (!Objects.equals(BlockFace.SOUTH, blockData.getHitBlockFace())) {
            faces.add(BlockFace.SOUTH);
        }
        if (!Objects.equals(BlockFace.EAST, blockData.getHitBlockFace())) {
            faces.add(BlockFace.EAST);
        }
        if (!Objects.equals(BlockFace.WEST, blockData.getHitBlockFace())) {
            faces.add(BlockFace.WEST);
        }

        int random = ThreadLocalRandom.current().nextInt(0, 4);
        String selectedFace = faces.get(random).toString();

        plugin.getConfig().set("players." + player.getName() + ".startFace", blockData.getHitBlockFace().toString());
        plugin.getConfig().set("players." + player.getName() + ".faceToCheck", selectedFace);

        switch (selectedFace) {
            case "UP":
                blockFaceLocation.setY(blockFaceLocation.getY() + 0.6);
                break;
            case "NORTH":
                blockFaceLocation.setZ(blockFaceLocation.getZ() - 0.6);
                break;
            case "SOUTH":
                blockFaceLocation.setZ(blockFaceLocation.getZ() + 0.6);
                break;
            case "EAST":
                blockFaceLocation.setX(blockFaceLocation.getX() + 0.6);
                break;
            case "WEST":
                blockFaceLocation.setX(blockFaceLocation.getX() - 0.6);
                break;
            default:
                break;
        }
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        player.showTitle(new Title() {
            @Override
            public @NotNull Component title() {
                return Component.text(ChatColor.GOLD + "Preparate");
            }

            @Override
            public @NotNull Component subtitle() {
                return Component.text("Iniciando prueba de habilidad");
            }

            @Override
            public @Nullable Times times() {
                return new Times() {
                    @Override
                    public @NotNull Duration fadeIn() {
                        return Duration.ofMillis(50);
                    }

                    @Override
                    public @NotNull Duration stay() {
                        return Duration.ofMillis(900);
                    }

                    @Override
                    public @NotNull Duration fadeOut() {
                        return Duration.ofMillis(50);
                    }
                };
            }

            @Override
            public <T> @UnknownNullability T part(@NotNull TitlePart<T> part) {
                return null;
            }
        });

        BukkitTask particlesHandler = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                if (plugin.getConfig().get("players." + player.getName() + ".isBreaking") == null) {
                    damagePlayer(player);
                    player.showTitle(new Title() {
                        @Override
                        public @NotNull Component title() {
                            return Component.text(ChatColor.RED + "Prueba fallida");
                        }

                        @Override
                        public @NotNull Component subtitle() {
                            return null;
                        }

                        @Override
                        public @Nullable Times times() {
                            return new Times() {
                                @Override
                                public @NotNull Duration fadeIn() {
                                    return Duration.ofMillis(100);
                                }

                                @Override
                                public @NotNull Duration stay() {
                                    return Duration.ofMillis(4800);
                                }

                                @Override
                                public @NotNull Duration fadeOut() {
                                    return Duration.ofMillis(100);
                                }
                            };
                        }

                        @Override
                        public <T> @UnknownNullability T part(@NotNull TitlePart<T> part) {
                            return null;
                        }
                    });
                    return;
                }
                if (!plugin.getConfig().getBoolean("players." + player.getName() + ".isBreaking")) {
                    damagePlayer(player);
                    player.showTitle(new Title() {
                        @Override
                        public @NotNull Component title() {
                            return Component.text(ChatColor.RED + "Prueba fallida");
                        }

                        @Override
                        public @NotNull Component subtitle() {
                            return null;
                        }

                        @Override
                        public @Nullable Times times() {
                            return new Times() {
                                @Override
                                public @NotNull Duration fadeIn() {
                                    return Duration.ofMillis(100);
                                }

                                @Override
                                public @NotNull Duration stay() {
                                    return Duration.ofMillis(4800);
                                }

                                @Override
                                public @NotNull Duration fadeOut() {
                                    return Duration.ofMillis(100);
                                }
                            };
                        }

                        @Override
                        public <T> @UnknownNullability T part(@NotNull TitlePart<T> part) {
                            return null;
                        }
                    });
                    return;
                }
                Date now = new Date();
                plugin.getLogger().log(Level.INFO, Long.toString(now.getTime()));
                RayTraceResult checkBlock = player.rayTraceBlocks(6f);

                if (Objects.equals(checkBlock.getHitBlockFace().toString(), plugin.getConfig().getString("players." + player.getName() + ".faceToCheck"))) {
                    plugin.getConfig().set("playersScore." + player.getName(), plugin.getConfig().get("playersScore." + player.getName()) == null ? 1 : plugin.getConfig().getInt("playersScore." + player.getName()) + 1);
                    plugin.getServer().getScheduler().cancelTask(plugin.getConfig().getInt("players." + player.getName() + ".particlesHandlerID"));
                    plugin.getConfig().set("players." + player.getName() + ".particlesHandlerID", null);
                    plugin.getConfig().set("players." + player.getName() + ".nextSkillCheck", ThreadLocalRandom.current().nextLong(now.getTime() + (plugin.getConfig().getLong("phase" + plugin.getConfig().getInt("phase") + ".checkAFKMinTime") * 1000), now.getTime() + (plugin.getConfig().getLong("phase" + plugin.getConfig().getInt("phase") + ".checkAFKMaxTime") * 1000)));
                    plugin.saveConfig();
                    player.showTitle(new Title() {
                        @Override
                        public @NotNull Component title() {
                            return Component.text(ChatColor.GREEN + "Prueba superada");
                        }

                        @Override
                        public @NotNull Component subtitle() {
                            return null;
                        }

                        @Override
                        public @Nullable Times times() {
                            return new Times() {
                                @Override
                                public @NotNull Duration fadeIn() {
                                    return Duration.ofMillis(100);
                                }

                                @Override
                                public @NotNull Duration stay() {
                                    return Duration.ofMillis(4800);
                                }

                                @Override
                                public @NotNull Duration fadeOut() {
                                    return Duration.ofMillis(100);
                                }
                            };
                        }

                        @Override
                        public <T> @UnknownNullability T part(@NotNull TitlePart<T> part) {
                            return null;
                        }
                    });
                } else {
                    if (now.getTime() > (plugin.getConfig().getLong("players." + player.getName() + ".skillCheckEndTime") - 1000)) {
                        damagePlayer(player);
                        plugin.getServer().getScheduler().cancelTask(plugin.getConfig().getInt("players." + player.getName() + ".particlesHandlerID"));
                        plugin.getConfig().set("players." + player.getName() + ".particlesHandlerID", null);
                        plugin.getConfig().set("players." + player.getName() + ".nextSkillCheck", ThreadLocalRandom.current().nextLong(now.getTime() + (plugin.getConfig().getLong( "phase" + plugin.getConfig().getInt("phase") + ".checkAFKMinTime") * 1000), now.getTime() + (plugin.getConfig().getLong("phase" + plugin.getConfig().getInt("phase") + ".checkAFKMaxTime") * 1000)));
                        plugin.saveConfig();
                        player.showTitle(new Title() {
                            @Override
                            public @NotNull Component title() {
                                return Component.text(ChatColor.RED + "Prueba fallida");
                            }

                            @Override
                            public @NotNull Component subtitle() {
                                return null;
                            }

                            @Override
                            public @Nullable Times times() {
                                return new Times() {
                                    @Override
                                    public @NotNull Duration fadeIn() {
                                        return Duration.ofMillis(100);
                                    }

                                    @Override
                                    public @NotNull Duration stay() {
                                        return Duration.ofMillis(4800);
                                    }

                                    @Override
                                    public @NotNull Duration fadeOut() {
                                        return Duration.ofMillis(100);
                                    }
                                };
                            }

                            @Override
                            public <T> @UnknownNullability T part(@NotNull TitlePart<T> part) {
                                return null;
                            }
                        });
                    } else {
                        player.showTitle(new Title() {
                            @Override
                            public @NotNull Component title() {
                                return Component.text(ChatColor.YELLOW + Long.toString((plugin.getConfig().getLong("players." + player.getName() + ".skillCheckEndTime") - now.getTime()) / 1000));
                            }

                            @Override
                            public @NotNull Component subtitle() {
                                return null;
                            }

                            @Override
                            public @Nullable Times times() {
                                return new Times() {
                                    @Override
                                    public @NotNull Duration fadeIn() {
                                        return Duration.ofMillis(0);
                                    }

                                    @Override
                                    public @NotNull Duration stay() {
                                        return Duration.ofMillis(300);
                                    }

                                    @Override
                                    public @NotNull Duration fadeOut() {
                                        return Duration.ofMillis(0);
                                    }
                                };
                            }

                            @Override
                            public <T> @UnknownNullability T part(@NotNull TitlePart<T> part) {
                                return null;
                            }
                        });
                        player.spawnParticle(Particle.FALLING_DUST, blockFaceLocation, 3, Material.REDSTONE_BLOCK.createBlockData());
                    }
                }
            }
        }, 22, 5);
        plugin.getConfig().set("players." + player.getName() + ".particlesHandlerID", particlesHandler.getTaskId());
        plugin.saveConfig();
    }
}