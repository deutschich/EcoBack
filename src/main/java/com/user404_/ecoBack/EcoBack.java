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
    private BukkitTask updateCheckTask;  // Task for periodic update checks
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

        // Schedule periodic update checks
        startUpdateScheduler();

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
        if (updateCheckTask != null) {
            updateCheckTask.cancel();
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

    /**
     * Starts the periodic update checker based on config settings.
     * Only runs if update checking is enabled in the build (release builds).
     */
    private void startUpdateScheduler() {
        // Only run if update checker is enabled in this build (release builds)
        if (!BuildConfig.UPDATE_CHECKER_ENABLED) {
            getLogger().info("Update checking is disabled in this build.");
            return;
        }

        int interval = getConfig().getInt("update-check-interval", 30);
        if (interval <= 0) {
            getLogger().info("Periodic update checking is disabled (interval <= 0).");
            return;
        }

        String unit = getConfig().getString("update-check-unit", "minutes").toLowerCase();
        long ticks;
        if (unit.equals("minutes")) {
            ticks = interval * 60 * 20L;
        } else { // hours (default)
            ticks = interval * 60 * 60 * 20L;
        }

        // Check once immediately
        updateChecker.checkForUpdates();

        // Then schedule periodic checks
        updateCheckTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateChecker.checkForUpdates();
            }
        }.runTaskTimerAsynchronously(this, ticks, ticks);
    }

    public void reloadPlugin() {
        reloadConfig();
        languageManager.reload();

        // Restart backup scheduler
        if (backupTask != null) {
            backupTask.cancel();
        }
        startBackupScheduler();

        // Restart update checker scheduler
        if (updateCheckTask != null) {
            updateCheckTask.cancel();
        }
        startUpdateScheduler();
    }

    // Getters
    public static EcoBack getInstance() { return instance; }
    public Economy getEconomy() { return economy; }
    public BackupManager getBackupManager() { return backupManager; }
    public LanguageManager getLanguageManager() { return languageManager; }
}