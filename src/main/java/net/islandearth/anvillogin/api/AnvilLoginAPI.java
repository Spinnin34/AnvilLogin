package net.islandearth.anvillogin.api;

import com.convallyria.languagy.api.language.Translator;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;

import java.util.logging.Logger;

public interface AnvilLoginAPI {

	BukkitAudiences adventure();

	/**
	 * Gets the translator provided by Languagy
	 * @return Translator
	 */
	Translator getTranslator();

	Logger getLogger();
}
