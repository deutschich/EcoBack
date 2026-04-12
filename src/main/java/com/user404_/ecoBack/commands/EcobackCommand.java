package com.user404_.ecoBack.commands;

import com.user404_.ecoBack.EcoBack;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class EcobackCommand implements CommandExecutor, TabCompleter {

    private final EcoBack plugin;

    public EcobackCommand(EcoBack plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ecoback.admin")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                if (!sender.hasPermission("ecoback.admin")) {
                    sender.sendMessage(plugin.getLanguageManager().getMessage("no-permission"));
                    return true;
                }
                plugin.reloadPlugin();
                sender.sendMessage(plugin.getLanguageManager().getMessage("config-reloaded"));
                break;

            case "backup":
                if (!sender.hasPermission("ecoback.backup")) {
                    sender.sendMessage(plugin.getLanguageManager().getMessage("no-permission"));
                    return true;
                }
                sender.sendMessage(plugin.getLanguageManager().getMessage("backup-starting"));
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                    File backupFile = plugin.getBackupManager().createBackup();
                    if (backupFile != null) {
                        sender.sendMessage(plugin.getLanguageManager().getMessage("backup-success", "{file}", backupFile.getName()));
                    } else {
                        sender.sendMessage(plugin.getLanguageManager().getMessage("backup-failed"));
                    }
                });
                break;

            case "backups":
                if (!sender.hasPermission("ecoback.admin")) {
                    sender.sendMessage(plugin.getLanguageManager().getMessage("no-permission"));
                    return true;
                }
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                    List<String> backups = plugin.getBackupManager().listBackups();
                    if (backups.isEmpty()) {
                        sender.sendMessage(plugin.getLanguageManager().getMessage("backups-empty"));
                    } else {
                        sender.sendMessage(plugin.getLanguageManager().getMessage("backups-header"));
                        for (String name : backups) {
                            sender.sendMessage(" §7- §f" + name);
                        }
                    }
                });
                break;

            case "rollback":
                if (!sender.hasPermission("ecoback.rollback")) {
                    sender.sendMessage(plugin.getLanguageManager().getMessage("no-permission"));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(plugin.getLanguageManager().getMessage("rollback-usage"));
                    return true;
                }
                String fileName = args[1];
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                    int count = plugin.getBackupManager().rollback(fileName);
                    if (count == -1) {
                        sender.sendMessage(plugin.getLanguageManager().getMessage("rollback-file-not-found", "{file}", fileName));
                    } else {
                        sender.sendMessage(plugin.getLanguageManager().getMessage("rollback-success", "{count}", String.valueOf(count), "{file}", fileName));
                    }
                });
                break;

            default:
                sendHelp(sender);
                break;
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.getLanguageManager().getMessage("help-header"));
        sender.sendMessage(" §e/ecoback backup §7- " + plugin.getLanguageManager().getMessage("help-backup"));
        sender.sendMessage(" §e/ecoback backups §7- " + plugin.getLanguageManager().getMessage("help-backups"));
        sender.sendMessage(" §e/ecoback rollback <file> §7- " + plugin.getLanguageManager().getMessage("help-rollback"));
        sender.sendMessage(" §e/ecoback reload §7- " + plugin.getLanguageManager().getMessage("help-reload"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            if (sender.hasPermission("ecoback.backup")) completions.add("backup");
            if (sender.hasPermission("ecoback.admin")) completions.add("backups");
            if (sender.hasPermission("ecoback.rollback")) completions.add("rollback");
            if (sender.hasPermission("ecoback.admin")) completions.add("reload");
            return filter(completions, args[0]);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("rollback")) {
            if (sender.hasPermission("ecoback.rollback")) {
                plugin.getBackupManager().listBackups().forEach(name -> {
                    if (name.toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(name);
                    }
                });
            }
        }
        return completions;
    }

    private List<String> filter(List<String> list, String prefix) {
        List<String> result = new ArrayList<>();
        for (String s : list) {
            if (s.toLowerCase().startsWith(prefix.toLowerCase())) {
                result.add(s);
            }
        }
        return result;
    }
}