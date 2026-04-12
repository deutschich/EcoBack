package com.user404_.ecoBack;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class LanguageManager {

    private final EcoBack plugin;
    private YamlConfiguration langConfig;
    private String language;

    public LanguageManager(EcoBack plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        language = plugin.getConfig().getString("language", "en");
        File langFile = new File(plugin.getDataFolder(), "lang/lang_" + language + ".yml");
        if (!langFile.exists()) {
            plugin.getLogger().warning("Language file for '" + language + "' not found, falling back to 'en'.");
            langFile = new File(plugin.getDataFolder(), "lang/lang_en.yml");
        }
        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    public String getMessage(String key) {
        String msg = langConfig.getString(key, "&cMissing message: " + key);
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    public String getMessage(String key, String... replacements) {
        String msg = getMessage(key);
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                msg = msg.replace(replacements[i], replacements[i + 1]);
            }
        }
        return msg;
    }
}