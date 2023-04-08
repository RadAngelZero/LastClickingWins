package com.radangelzero.lastclickingwins;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.BlockFace;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

public final class LastClickingWins extends JavaPlugin implements Listener, TabCompleter {

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().log(Level.INFO, getClass().getName() + " Loaded");
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("lastclickingwins").setExecutor(new SubCommandsHandler(this));
        getCommand("lastclickingwins").setTabCompleter(new SubCommandsTabCompletion(this));
        if (getConfig().get("enabled") == null) {
            getConfig().set("enabled", true);
            getConfig().set("started", false);
            Location firstLocation = new Location(getServer().getWorld("world"), 0, 70, 0);
            getConfig().set("losersSpawn", firstLocation);
            getConfig().set("block", "BEDROCK");
            getConfig().set("totalPlayers", 0);
            getConfig().set("leftPlayers", 0);
            getConfig().set("checkAFKMinTime", 120);
            getConfig().set("checkAFKMaxTime", 180);
            getConfig().set("phase", 1);
            saveConfig();
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic

        getLogger().log(Level.INFO, getClass().getName() + " Stopped, saving config");
        saveConfig();
        if (getConfig().get("mainTaskID") == null) {
            return;
        }
        getServer().getScheduler().cancelTask(getConfig().getInt("mainTaskID"));
        getConfig().set("mainTaskID", null);
        saveConfig();

    }

    @EventHandler
    public void onPLayerJoin(PlayerJoinEvent event) {
        if (getConfig().get("players." + event.getPlayer().getName() + ".disconnected") == null) {
            return;
        }
        if (!getConfig().getBoolean("players." + event.getPlayer().getName() + ".disconnected")) {
            return;
        }
        event.getPlayer().setGameMode(GameMode.SPECTATOR);
        event.getPlayer().teleport(getConfig().getLocation("losersSpawn"));
        event.getPlayer().showTitle(new Title() {
            @Override
            public @NotNull Component title() {
                return Component.text(ChatColor.RED + "Has sido eliminado");
            }

            @Override
            public @NotNull Component subtitle() {
                return Component.text("Por desconexión");
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
        event.getPlayer().teleport(getConfig().getLocation("losersSpawn"));
    }

    @EventHandler
    public void onBreakBlockCancel(BlockDamageAbortEvent event) {
        if (getConfig().get("players." + event.getPlayer().getName()) == null) {
            return;
        }
        getLogger().log(Level.INFO, "Destrucion de bloque cancelada");
        getLogger().log(Level.INFO, event.getPlayer().getName());
        getLogger().log(Level.INFO, event.getBlock().getType().toString());
        getLogger().log(Level.INFO, getConfig().getString("block"));
        if (Objects.equals(event.getBlock().getType().toString(), getConfig().getString("block"))) {
            Date now = new Date();
            getConfig().set("players." + event.getPlayer().getName() + ".isBreaking", false);
            getConfig().set("players." + event.getPlayer().getName() + ".lastBreaking", now.getTime());
            if (getConfig().get("players." + event.getPlayer().getName() + ".particlesHandlerID") != null) {
                getConfig().set("players." + event.getPlayer().getName() + ".nextSkillCheck", ThreadLocalRandom.current().nextLong(now.getTime() + (getConfig().getLong("phase" + getConfig().getInt("phase") + ".checkAFKMinTime") * 1000), now.getTime() + (getConfig().getLong("phase" + getConfig().getInt("phase") + ".checkAFKMaxTime") * 1000)));
                getServer().getScheduler().cancelTask(getConfig().getInt("players." + event.getPlayer().getName() + ".particlesHandlerID"));
                getConfig().set("players." + event.getPlayer().getName() + ".particlesHandlerID", null);
                event.getPlayer().showTitle(new Title() {
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
                damagePlayer(event.getPlayer());
            }
            saveConfig();
        }
    }

    @EventHandler
    public void onHealthRegen(EntityRegainHealthEvent event) {
        if (event.getEntityType() == EntityType.PLAYER) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onHunger(FoodLevelChangeEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntityType() == EntityType.PLAYER) {
            getLogger().log(Level.INFO, event.getCause().toString());
            if (event.getCause() != EntityDamageEvent.DamageCause.CUSTOM) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDamageBlock(BlockDamageEvent event) {
        Date now = new Date();
        if (getConfig().get("players." + event.getPlayer().getName()) == null) {
            return;
        }
        if (!Objects.equals(event.getBlock().getType().toString(), getConfig().getString("block"))) {
            return;
        }
        getConfig().set("players." + event.getPlayer().getName() + ".isBreaking", true);
        getConfig().set("players." + event.getPlayer().getName() + ".blockLocation", event.getBlock().getLocation());
        if (getConfig().getLong("players." + event.getPlayer().getName() + ".nextSkillCheck") < now.getTime()) {
            getConfig().set("players." + event.getPlayer().getName() + ".nextSkillCheck", ThreadLocalRandom.current().nextLong(now.getTime() + (getConfig().getLong("phase" + getConfig().getInt("phase") + ".checkAFKMinTime") * 1000), now.getTime() + (getConfig().getLong("phase" + getConfig().getInt("phase") + ".checkAFKMaxTime") * 1000)));
        }
        saveConfig();
    }

    public void damagePlayer(Player player) {
        double playerHealth = player.getHealth();

        if (getConfig().getDouble("phase" + getConfig().getInt("phase") + ".damage") >= playerHealth) {
            player.setHealth(20f);
            player.setFoodLevel(20);
            getConfig().set("inGamePlayers", getConfig().getInt("inGamePlayers") - 1);
            getConfig().set("players." + player.getName() + ".isSpectator", true);
            saveConfig();
            getServer().getScheduler().runTask(this, new Runnable() {
                @Override
                public void run() {
                    player.setGameMode(GameMode.SPECTATOR);
                    player.teleport(getConfig().getLocation("losersSpawn"));

                }
            });
            if (getConfig().getInt("inGamePlayers") == 1) {
                getServer().getScheduler().cancelTask(getConfig().getInt("mainTaskID"));
                getConfig().set("mainTaskID", null);
                Player winner = getServer().getPlayer("RadAngelZero");
                for (String name : getConfig().getConfigurationSection("players").getKeys(false)) {
                    Player playerCheck = getServer().getPlayer(name);
                    if (playerCheck == null) {
                        continue;
                    }
                    if (getConfig().get("players." + playerCheck.getName() + ".isSpectator") != null) {
                        continue;
                    }
                    winner = playerCheck;
                    break;
                }
                for (String name : getConfig().getConfigurationSection("players").getKeys(false)) {
                    Player playerCheck = getServer().getPlayer(name);
                    if (playerCheck == null) {
                        continue;
                    }
                    if (getConfig().get("players." + playerCheck.getName() + ".isSpectator") != null) {
                        Player finalWinner = winner;
                        playerCheck.sendMessage(ChatColor.GOLD + finalWinner.getName() + " es el ganador");
                        playerCheck.showTitle(new Title() {
                            @Override
                            public @NotNull Component title() {
                                return Component.text(ChatColor.GOLD + finalWinner.getName());
                            }

                            @Override
                            public @NotNull Component subtitle() {
                                return Component.text("Es el ganador");
                            }

                            @Override
                            public @Nullable Times times() {
                                return null;
                            }

                            @Override
                            public <T> @UnknownNullability T part(@NotNull TitlePart<T> part) {
                                return null;
                            }
                        });
                        continue;
                    }
                    playerCheck.sendMessage(ChatColor.GOLD + "Felicitaciones, eres el ganador");
                    playerCheck.showTitle(new Title() {
                        @Override
                        public @NotNull Component title() {
                            return Component.text(ChatColor.GOLD + "Has ganado");
                        }

                        @Override
                        public @NotNull Component subtitle() {
                            return Component.text("Felicitaciones");
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
                }
                return;
            }
            player.sendMessage(ChatColor.RED + "Has sido eliminado");
            player.sendMessage("Quedan " + getConfig().getInt("inGamePlayers") + " en el juego");
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
            for (String name : getConfig().getConfigurationSection("players").getKeys(false)) {
                Player playerPlaying = getServer().getPlayer(name);
                if (playerPlaying == null) {
                    continue;
                }
                if (name.equals(player.getName())) {
                    continue;
                }
                playerPlaying.sendMessage(ChatColor.RED + player.getName() + " ha sido eliminado");
                playerPlaying.sendMessage(getConfig().getInt("inGamePlayers") + " jugadores restantes");
            }
            return;
        }
        getServer().getScheduler().runTask(this, new Runnable() {
            @Override
            public void run() {
                player.damage(getConfig().getDouble("phase" + getConfig().getInt("phase") + ".damage"));
            }
        });
    }
}


