package net.islandearth.anvillogin;

import com.convallyria.languagy.api.adventure.AdventurePlatform;
import com.convallyria.languagy.api.language.Language;
import com.convallyria.languagy.api.language.Translator;
import com.google.common.base.Enums;
import net.islandearth.anvillogin.api.AnvilLoginAPI;
import net.islandearth.anvillogin.listeners.PlayerListener;
import net.islandearth.anvillogin.translation.Translations;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.wesjd.anvilgui.version.VersionMatcher;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AnvilLogin extends JavaPlugin implements AnvilLoginAPI {
    
    private final List<UUID> loggedIn = new ArrayList<>();

    public List<UUID> getLoggedIn() {
        return loggedIn;
    }

    public List<UUID> getNotLoggedIn() {
        return notLoggedIn;
    }

    @Override
    public Translator getTranslator() {
        return translator;
    }

    public boolean isAuthme() {
        return authme;
    }

    private final List<UUID> notLoggedIn = new ArrayList<>();

    private BukkitAudiences adventure;
    private MiniMessage miniMessage;
    private Translator translator;

    @Override
    public @NonNull BukkitAudiences adventure() {
        if(this.adventure == null) {
            throw new IllegalStateException("¡Intenté acceder a Adventure cuando el complemento estaba deshabilitado!");
        }
        return this.adventure;
    }

    private boolean authme;
    private static AnvilLogin plugin;

    @Override
    public void onEnable() {
        try {
            new VersionMatcher().match();
        } catch (RuntimeException e) {
            this.getLogger().severe("¡La versión de tu servidor no es compatible! ¡Actualice a la última versión!");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        
        if (Bukkit.getPluginManager().getPlugin("AuthMe") != null) {
            this.getLogger().info("Found authme!");
            this.authme = true;
        } else this.authme = false;

        plugin = this;
        createFiles();

        this.adventure = BukkitAudiences.create(this);
        this.miniMessage = MiniMessage.miniMessage();

        final Language defaultLanguage = Enums.getIfPresent(Language.class, getConfig().getString("default_language", "ESPAÑOL")).or(Language.ECUADORIAN_SPANISH);
        this.translator = Translator.of(this, "lang", defaultLanguage, debug(), AdventurePlatform.create(miniMessage, adventure));

        registerListeners();
        this.getLogger().info("LoginKR habilitado");
    }

    @Override
    public void onDisable() {
        if (translator != null) translator.close();
        if (this.adventure != null) {
            this.adventure.close();
            this.adventure = null;
        }
    }
    
    private void createFiles() {
        saveDefaultConfig();
        Translations.generateLang(this);
    }
    
    private void registerListeners() {
        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new PlayerListener(this), this);
    }

    public boolean debug() {
        return this.getConfig().getBoolean("debug");
    }

    public static AnvilLoginAPI getAPI() {
        return plugin;
    }
}
