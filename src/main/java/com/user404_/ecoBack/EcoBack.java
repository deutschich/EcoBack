package com.user404_.ecoBack;

import com.user404_.ecoBack.BuildConfig;
import com.user404_.ecoBack.commands.EcobackCommand;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.logging.Logger;

public final class EcoBack extends JavaPlugin {

    private static EcoBack instance;
    private Economy economy;
    private BackupManager backupManager;
    private LanguageManager languageManager;
    private UpdateChecker updateChecker;
    private BukkitTask backupTask;
    private Logger logger;

    @Override
    public void onEnable() {
        instance = this;
        logger = getLogger();



        // Save default config and language files
        saveDefaultConfig();
        saveResource("lang/lang_en.yml", false);
        saveResource("lang/lang_de.yml", false);

        // Load language manager
        languageManager = new LanguageManager(this);

        // Setup Vault economy
        if (!setupEconomy()) {
            getLogger().severe("Vault economy not found! Disabling plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize managers
        backupManager = new BackupManager(this);
        updateChecker = new UpdateChecker(this);

        // Register commands
        EcobackCommand commandExecutor = new EcobackCommand(this);
        getCommand("ecoback").setExecutor(commandExecutor);
        getCommand("ecoback").setTabCompleter(commandExecutor);

        // Schedule automatic backups
        startBackupScheduler();

        // Check for updates
        updateChecker.checkForUpdates();

        // Notify ops on join
        Bukkit.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
                if (event.getPlayer().hasPermission("ecoback.update.notify")) {
                    updateChecker.sendUpdateMessage(event.getPlayer());
                }
            }
        }, this);

        // PREVIEW BUILD CHANGES: Show warning if this is a preview build
        if ("preview".equals(BuildConfig.BUILD_TYPE)) {
            logger.warning("=========================================");
            logger.warning("This is a PREVIEW build (commit: " + BuildConfig.COMMIT_ID + ").");
            logger.warning("It may contain bugs and should not be used in production.");
            logger.warning("Update checking is disabled.");
            logger.warning("=========================================");
        }

        logger.info("EcoBack enabled successfully.");
    }

    @Override
    public void onDisable() {
        if (backupTask != null) {
            backupTask.cancel();
        }
        getLogger().info("EcoBack disabled.");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    private void startBackupScheduler() {
        int interval = getConfig().getInt("backup-interval", 0);
        if (interval <= 0) return;

        String unit = getConfig().getString("interval-unit", "minutes").toLowerCase();
        long ticks;
        if (unit.equals("hours")) {
            ticks = interval * 60 * 60 * 20L;
        } else {
            ticks = interval * 60 * 20L;
        }

        backupTask = new BukkitRunnable() {
            @Override
            public void run() {
                backupManager.createBackup();
            }
        }.runTaskTimerAsynchronously(this, ticks, ticks);
    }

    public void reloadPlugin() {
        reloadConfig();
        languageManager.reload();
        if (backupTask != null) {
            backupTask.cancel();
        }
        startBackupScheduler();
    }

    // Getters
    public static EcoBack getInstance() { return instance; }
    public Economy getEconomy() { return economy; }
    public BackupManager getBackupManager() { return backupManager; }
    public LanguageManager getLanguageManager() { return languageManager; }
}