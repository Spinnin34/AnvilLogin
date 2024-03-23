package net.islandearth.anvillogin.translation;

import com.convallyria.languagy.api.language.Language;
import com.convallyria.languagy.api.language.key.LanguageKey;
import com.convallyria.languagy.api.language.key.TranslationKey;
import com.convallyria.languagy.api.language.translation.Translation;
import me.clip.placeholderapi.PlaceholderAPI;
import net.islandearth.anvillogin.AnvilLogin;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UnknownFormatConversionException;
import java.util.concurrent.CompletionException;

public enum Translations {
	KICKED(TranslationKey.of("kicked")),
	LOGGED_IN(TranslationKey.of("logged_in")),
	GUI_TITLE(TranslationKey.of("gui_title")),
	GUI_TEXT(TranslationKey.of("gui_text")),
	GUI_WRONG(TranslationKey.of("gui_wrong")),
	GUI_LEFT_SLOT_LORE(TranslationKey.of("gui_left_slot_lore")),
	GUI_RIGHT_SLOT_NAME(TranslationKey.of("gui_right_slot_name")),
	GUI_RIGHT_SLOT_LORE(TranslationKey.of("gui_right_slot_lore"));

	private final TranslationKey key;

	Translations(TranslationKey key) {
		this.key = key;
	}

	public void send(Player player, Object... values) {
		get(player, values).forEach((component) -> AnvilLogin.getAPI().adventure().player(player).sendMessage(component));
	}

	public List<Component> get(Player player, Object... values) {
		final Translation translation = AnvilLogin.getAPI().getTranslator().getTranslationFor(player, key);
		try {
			translation.format(values);
		} catch (CompletionException | UnknownFormatConversionException e) {
			AnvilLogin.getAPI().getLogger().warning("Translation key '" + this.name() +
					"' is using legacy variable format. " +
					"Some variables may not show correctly. Please update your language files.");
		}

		translation.getTranslations().replaceAll((translationString) -> this.setPapi(player, translationString));
		return new ArrayList<>(translation.colour());
	}

	public static void generateLang(AnvilLogin plugin) {
		File lang = new File(plugin.getDataFolder() + "/lang/");
		lang.mkdirs();

		for (Language language : Language.values()) {
			final LanguageKey languageKey = language.getKey();
			try {
				plugin.saveResource("lang/" + languageKey.getCode() + ".yml", false);
				plugin.getLogger().info("Generated " + languageKey.getCode() + ".yml");
			} catch (IllegalArgumentException ignored) { }

			File file = new File(plugin.getDataFolder() + "/lang/" + languageKey.getCode() + ".yml");
			if (file.exists()) {
				FileConfiguration config = YamlConfiguration.loadConfiguration(file);
				for (Translations key : values()) {
					if (config.get(key.toString().toLowerCase()) == null) {
						plugin.getLogger().warning("No value in translation file for key "
								+ key + " was found. Please regenerate or edit your language files with new values!");
					}
				}
			}
		}
	}

	@NotNull
	private String setPapi(Player player, String message) {
		if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
			return PlaceholderAPI.setPlaceholders(player, message);
		}
		return message;
	}
}
