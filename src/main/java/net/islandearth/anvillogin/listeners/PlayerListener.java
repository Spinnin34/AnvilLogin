package net.islandearth.anvillogin.listeners;

import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.core.PremiumStatus;
import com.google.common.base.Enums;
import fr.xephi.authme.api.v3.AuthMeApi;
import net.islandearth.anvillogin.AnvilLogin;
import net.islandearth.anvillogin.translation.Translations;
import net.islandearth.anvillogin.util.ItemStackBuilder;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlayerListener implements Listener {

    private final AnvilLogin plugin;

    public PlayerListener(AnvilLogin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onJoin(PlayerJoinEvent pje) {
        Player myPlayer = pje.getPlayer();
        if ((plugin.getConfig().getBoolean("disable-op-bypass", true)
                || !myPlayer.hasPermission("AnvilLogin.bypass"))
                && !plugin.getLoggedIn().contains(myPlayer.getUniqueId())) {
            if (plugin.isAuthme()
                    && (AuthMeApi.getInstance().isAuthenticated(myPlayer) || AuthMeApi.getInstance().isUnrestricted(myPlayer))) {
                return;
            }

            if (plugin.getConfig().getBoolean("fastlogin")) {
                if (Bukkit.getPluginManager().getPlugin("FastLogin") != null) {
                    FastLoginBukkit fastLogin = (FastLoginBukkit) Bukkit.getPluginManager().getPlugin("FastLogin");
                    if (fastLogin != null) {
                        PremiumStatus premiumStatus = fastLogin.getStatus(myPlayer.getUniqueId());
                        if (premiumStatus == PremiumStatus.PREMIUM) {
                            if (plugin.debug()) {
                                plugin.getLogger().info("Saltar jugador " + myPlayer.getName() + " porque son premium.");
                            }
                            return;
                        }
                    }
                }
            }

            plugin.getNotLoggedIn().add(myPlayer.getUniqueId());

            final LegacyComponentSerializer legacy = BukkitComponentSerializer.legacy();

            List<String> colouredLeftLore = new ArrayList<>();
            for (Component leftLore : Translations.GUI_LEFT_SLOT_LORE.get(myPlayer)) {
                colouredLeftLore.add(legacy.serialize(leftLore));
            }

            List<String> colouredRightLore = new ArrayList<>();
            for (Component rightLore : Translations.GUI_RIGHT_SLOT_LORE.get(myPlayer)) {
                colouredRightLore.add(legacy.serialize(rightLore));
            }

            final ItemStack leftItem = ItemStackBuilder.of(Enums.getIfPresent(Material.class, plugin.getConfig().getString("left_slot.type", "ANVIL")).or(Material.ANVIL))
                    .addFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .withLore(colouredLeftLore)
                    .withModel(plugin.getConfig().getInt("left_slot.model")).build();

            final Material rightType = Enums.getIfPresent(Material.class, plugin.getConfig().getString("right_slot.type", "AIR")).or(Material.AIR);
            final ItemStack rightItem = rightType == Material.AIR ? new ItemStack(Material.AIR) : ItemStackBuilder.of(rightType)
                    .addFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .withLore(colouredRightLore)
                    .withName(Translations.GUI_RIGHT_SLOT_NAME.get(myPlayer).get(0))
                    .withModel(plugin.getConfig().getInt("right_slot.model")).build();

            AnvilGUI.Builder anvilGUI = new AnvilGUI.Builder()
                    .onClick((slot, snapshot) -> {
                        if (slot != AnvilGUI.Slot.OUTPUT) {
                            return Collections.emptyList();
                        }

                        final Player player = snapshot.getPlayer();
                        final String text = snapshot.getText();
                        if (plugin.isAuthme() && plugin.getConfig().getBoolean("register") && !AuthMeApi.getInstance().isRegistered(player.getName())) {
                            AuthMeApi.getInstance().forceRegister(player, text, true);
                            plugin.getLoggedIn().add(player.getUniqueId());
                            plugin.getNotLoggedIn().remove(player.getUniqueId());
                            if (plugin.getConfig().getBoolean("login_messages")) {
                                Translations.LOGGED_IN.send(player);
                            }
                            return List.of(AnvilGUI.ResponseAction.close());
                        }

                        if (text.equalsIgnoreCase(plugin.getConfig().getString("Password"))
                                || (plugin.isAuthme() && AuthMeApi.getInstance().checkPassword(player.getName(), text))) {
                            plugin.getLoggedIn().add(player.getUniqueId());
                            plugin.getNotLoggedIn().remove(player.getUniqueId());
                            if (plugin.getConfig().getBoolean("login_messages")) {
                                Translations.LOGGED_IN.send(player);
                            }
                            if (plugin.isAuthme()) AuthMeApi.getInstance().forceLogin(player);
                            player.setLevel(player.getLevel());
                            return List.of(AnvilGUI.ResponseAction.close());
                        } else {
                            return List.of(AnvilGUI.ResponseAction.replaceInputText(legacy.serialize(Translations.GUI_WRONG.get(myPlayer).get(0))));
                        }
                    })
                    .preventClose()
                    .text(legacy.serialize(Translations.GUI_TEXT.get(myPlayer).get(0)))
                    .itemLeft(leftItem)
                    .itemRight(rightItem)
                    .title(legacy.serialize(Translations.GUI_TITLE.get(myPlayer).get(0)))  //only works in 1.14+
                    .plugin(plugin);
            Bukkit.getScheduler().runTaskLater(plugin, () -> anvilGUI.open(myPlayer), 20L);

            if (plugin.getConfig().getBoolean("Timeout")) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!plugin.getLoggedIn().contains(myPlayer.getUniqueId())) {
                        myPlayer.kickPlayer(legacy.serialize(Component.join(JoinConfiguration.newlines(), Translations.KICKED.get(myPlayer))));
                    }
                }, plugin.getConfig().getLong("Time"));
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (plugin.getNotLoggedIn().contains(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Player player = (Player) event.getView().getPlayer();
        if (plugin.getNotLoggedIn().contains(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player) {
            if (plugin.getNotLoggedIn().contains(event.getEntity().getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            if (plugin.getNotLoggedIn().contains(event.getEntity().getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent pqe) {
        Player player = pqe.getPlayer();
        plugin.getLoggedIn().remove(player.getUniqueId());
        plugin.getNotLoggedIn().remove(player.getUniqueId());
    }
}
