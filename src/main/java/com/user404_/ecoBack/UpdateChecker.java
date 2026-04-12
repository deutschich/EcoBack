package com.user404_.ecoBack;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;

public class UpdateChecker {

    private final EcoBack plugin;
    private static final String VERSION_URL = "https://raw.githubusercontent.com/deutschich/EcoBack/master/version.json";
    private static final String PROJECT_URL = "https://github.com/deutschich/EcoBack/releases/latest";

    private boolean updateAvailable = false;
    private String latestVersion;
    private String downloadURL;

    public UpdateChecker(EcoBack plugin) {
        this.plugin = plugin;
    }

    public void checkForUpdates() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(VERSION_URL);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(5000);
                    connection.setRequestProperty("User-Agent", "EcoBack/" + plugin.getDescription().getVersion());

                    if (connection.getResponseCode() == 200) {
                        try (InputStreamReader reader = new InputStreamReader(connection.getInputStream())) {
                            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                            latestVersion = json.get("version").getAsString();
                            downloadURL = json.get("download").getAsString();
                            String current = plugin.getDescription().getVersion();

                            if (!current.equalsIgnoreCase(latestVersion)) {
                                updateAvailable = true;
                                plugin.getLogger().info("A new update is available: v" + latestVersion + " (current: v" + current + ")");
                            } else {
                                updateAvailable = false;
                            }
                        }
                    } else {
                        plugin.getLogger().warning("Update check failed: HTTP " + connection.getResponseCode());
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Could not check for updates", e);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public void sendUpdateMessage(Player player) {
        if (!updateAvailable) return;

        String prefix = plugin.getLanguageManager().getMessage("prefix");
        String current = plugin.getDescription().getVersion();
        player.sendMessage(prefix + " §eNew update available! §av" + latestVersion + " §e(current: §av" + current + "§e)");
        player.sendMessage(prefix + " §eDownload:");
        // Clickable link for Spigot/Paper
        net.md_5.bungee.api.chat.TextComponent link = new net.md_5.bungee.api.chat.TextComponent(prefix + " §b§nDownload now!");
        link.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.OPEN_URL, downloadURL));
        player.spigot().sendMessage(link);
    }
}