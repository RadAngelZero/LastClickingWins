package com.radangelzero.lastclickingwins;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SubCommandsTabCompletion implements TabCompleter {

    private final LastClickingWins plugin;

    public SubCommandsTabCompletion(LastClickingWins plugin) {
        this.plugin = plugin;
    }

    private static final String[] commands = {
            "join",
            "forcejoin",
            "leave",
            "forceleave",
            "help",
            "enable",
            "start",
            "stop",
            "disable",
            "setloserspawn",
            "loserspawn",
            "setblock",
            "reload",
            "checkafktimeinterval",
            "setcheckafktimeinterval",
            "joinall",
            "leaveall",
            "joinspectator",
            "pause",
            "resume",
    };

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1 && plugin.getConfig().getBoolean("enabled")) {
            List<String> completions = new ArrayList<>();
            StringUtil.copyPartialMatches(args[0], Arrays.stream(commands).toList(), completions);
            return completions;
        }
        if (args.length == 1 && !plugin.getConfig().getBoolean("enabled")) {
            List<String> completions = new ArrayList<>();
            completions.add("enable");
            return completions;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("setblock")) {
            List<String> blocks = Arrays.stream(Arrays.stream(Material.values()).map(Enum::name).toArray(String[]::new)).filter(mat -> Material.getMaterial(mat).isBlock()).toList().stream().filter(block -> block.toLowerCase().contains(args[1].toLowerCase())).toList();
//            blocks.sort(Comparator.comparing((String block) -> !block.startsWith("BEDROCK"))
//                    .thenComparing(block -> !block.endsWith("STONE"))
//                    .thenComparingInt(String::length)
//                    .thenComparing(Comparator.naturalOrder()));
            return blocks;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("setplayerlives")) {
            List<String> livesOptions = new ArrayList<>();
            livesOptions.add("1");
            livesOptions.add("3");
            livesOptions.add("10");
            livesOptions.add("20");
            return livesOptions;
        }
        if ((args.length == 2 || args.length == 3) && args[0].equalsIgnoreCase("setcheckafktimeinterval")) {
            List<String> livesOptions = new ArrayList<>();
            livesOptions.add("60");
            livesOptions.add("120");
            livesOptions.add("300");
            livesOptions.add("600");
            return livesOptions;
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("forcejoin") || args[0].equalsIgnoreCase("forceleave"))) {
            List<String> playerNames = new ArrayList<>();
            Player[] players = new Player[plugin.getServer().getOnlinePlayers().size()];
            plugin.getServer().getOnlinePlayers().toArray(players);
            for (Player player : players) {
                if (args[0].equalsIgnoreCase("forcejoin")) {
                    if (plugin.getConfig().get("players." + player.getName()) == null)
                        playerNames.add(player.getName());
                }
                if (args[0].equalsIgnoreCase("forceleave")) {
                    if (plugin.getConfig().get("players." + player.getName()) != null)
                        playerNames.add(player.getName());
                }
            }
            return playerNames;
        }
        return new ArrayList<String>();
    }
}
