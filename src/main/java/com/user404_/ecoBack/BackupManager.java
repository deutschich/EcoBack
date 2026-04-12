package com.user404_.ecoBack;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;

public class BackupManager {

    private final EcoBack plugin;
    private final File backupFolder;

    public BackupManager(EcoBack plugin) {
        this.plugin = plugin;
        this.backupFolder = new File(plugin.getDataFolder(), "backups");
        if (!backupFolder.exists()) {
            backupFolder.mkdirs();
        }
    }

    /**
     * Creates a backup of all player balances.
     * @return the created backup file, or null if failed
     */
    public File createBackup() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        File backupFile = new File(backupFolder, "backup-" + timeStamp + ".yml");

        YamlConfiguration yaml = new YamlConfiguration();

        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            if (player.getName() == null) continue;
            double balance = plugin.getEconomy().getBalance(player);
            yaml.set("balances." + player.getUniqueId().toString(), balance);
        }

        try {
            yaml.save(backupFile);
            plugin.getLogger().info("Backup created: " + backupFile.getName());
            return backupFile;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create backup", e);
            return null;
        }
    }

    /**
     * Lists all backup files in the backup folder.
     * @return array of file names (without path)
     */
    public List<String> listBackups() {
        List<String> list = new ArrayList<>();
        File[] files = backupFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File f : files) {
                list.add(f.getName());
            }
        }
        Collections.sort(list, Collections.reverseOrder()); // newest first
        return list;
    }

    /**
     * Restores balances from a backup file.
     * @param fileName name of the backup file (e.g., "backup-20250220-153045.yml")
     * @return number of accounts restored, or -1 if error
     */
    public int rollback(String fileName) {
        File backupFile = new File(backupFolder, fileName);
        if (!backupFile.exists()) {
            return -1;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(backupFile);
        if (!yaml.contains("balances")) {
            return -1;
        }

        int count = 0;
        for (String uuidString : yaml.getConfigurationSection("balances").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidString);
                OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                if (player.getName() != null) {
                    double balance = yaml.getDouble("balances." + uuidString);
                    // Withdraw current balance and deposit the backup amount
                    plugin.getEconomy().withdrawPlayer(player, plugin.getEconomy().getBalance(player));
                    plugin.getEconomy().depositPlayer(player, balance);
                    count++;
                }
            } catch (IllegalArgumentException e) {
                // Invalid UUID, skip
            }
        }

        plugin.getLogger().info("Rollback completed: " + count + " accounts restored from " + fileName);
        return count;
    }

    public File getBackupFolder() {
        return backupFolder;
    }
}