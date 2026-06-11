package pl.kloper.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.kloper.manager.HologramManager;
import pl.kloper.utils.ColorUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HologramCommand implements CommandExecutor, TabCompleter {

    private final HologramManager manager;

    public HologramCommand(HologramManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Ta komenda jest dostępna tylko dla graczy!");
            return true;
        }

        if (!player.hasPermission("vercode.command.hologram")) {
            player.sendMessage(ColorUtil.color("&x&F&F&0&0&0&0&l✘ &8⁑ &cNie posiadasz uprawnien do tej komendy!"));
            return true;
        }

        if (args.length < 1) {
            sendUsage(player);
            return true;
        }

        String action = args[0].toLowerCase();

        if (action.equals("reload")) {
            manager.reloadAll();
            player.sendMessage(ColorUtil.color("&2§l✔ &8⁑ &aPomyślnie przeładowano wszystkie hologramy oraz konfiguracje!"));
            return true;
        }

        if (args.length < 2) {
            sendUsage(player);
            return true;
        }

        String name = args[1].toLowerCase();

        if (action.equals("create")) {
            if (args.length < 3) {
                player.sendMessage(ColorUtil.color("&x&F&F&0&0&0&0&l✘ &8⁑ &cUżyj: /hologram create <nazwa> <tekst>"));
                return true;
            }
            String text = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            manager.create(name, player.getLocation(), text);
            player.sendMessage(ColorUtil.color("&2§l✔ &8⁑ &aPomyślnie utworzono hologram o nazwie: &e" + name));
            return true;
        }

        if (action.equals("movehere")) {
            if (!manager.getHologramNames().contains(name)) {
                player.sendMessage(ColorUtil.color("&x&F&F&0&0&0&0&l✘ &8⁑ &cHologram o takiej nazwie nie istnieje!"));
                return true;
            }
            manager.moveHere(name, player.getLocation());
            player.sendMessage(ColorUtil.color("&2§l✔ &8⁑ &aPrzeniesiono hologram &e" + name + " &ana Twoją obecną pozycję!"));
            return true;
        }

        if (action.equals("remove") || action.equals("delete")) {
            if (!manager.getHologramNames().contains(name)) {
                player.sendMessage(ColorUtil.color("&x&F&F&0&0&0&0&l✘ &8⁑ &cHologram o takiej nazwie nie istnieje!"));
                return true;
            }
            manager.remove(name);
            player.sendMessage(ColorUtil.color("&2§l✔ &8⁑ &aPomyślnie usunięto hologram o nazwie: &e" + name));
            return true;
        }

        player.sendMessage(ColorUtil.color("&x&F&F&0&0&0&0&l✘ &8⁑ &cNieznana podkomenda! Wybierz 'create', 'movehere', 'remove' lub 'reload'."));
        return true;
    }

    private void sendUsage(Player player) {
        player.sendMessage(ColorUtil.color("&x&F&F&0&0&0&0&l✘ &8⁑ &cPoprawne użycie:"));
        player.sendMessage(ColorUtil.color("&8» &4/hologram create <nazwa> <tekst>"));
        player.sendMessage(ColorUtil.color("&8» &4/hologram movehere <nazwa>"));
        player.sendMessage(ColorUtil.color("&8» &4/hologram remove <nazwa>"));
        player.sendMessage(ColorUtil.color("&8» &4/hologram reload"));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("givemc.coregen.admin.command.hologram")) return List.of();
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], Arrays.asList("create", "movehere", "remove", "reload"), completions);
        } else if (args.length == 2) {
            if (!args[0].equalsIgnoreCase("reload")) {
                StringUtil.copyPartialMatches(args[1], manager.getHologramNames(), completions);
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            completions.add("[tekst]");
        }

        return completions;
    }
}