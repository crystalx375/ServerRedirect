package net.kaikk.mc.serverredirect.bukkit;

import org.bukkit.entity.Player;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.md_5.bungee.api.ChatColor;
import org.jetbrains.annotations.NotNull;

public class PlaceholderExpansionImpl extends PlaceholderExpansion {
	@Override
	public @NotNull String getIdentifier() {
		return "serverredirect";
	}

	@Override
	public @NotNull String getAuthor() {
		return "KaiNoMood";
	}

	@Override
	public @NotNull String getVersion() {
		return "1.41.4";
	}

	@Override
	public String onPlaceholderRequest(Player player, String params) {
		boolean b = ServerRedirect.isUsingServerRedirect(player);
		switch(params) {
		case "yesno":
			return b ? "Yes" : "No";
		case "yesnof1":
			return b ? "&aYes" : "&cNo";
		case "yesnof2":
			return b ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No";
		}
		
		return b ? "1" : "0";
	}
}
