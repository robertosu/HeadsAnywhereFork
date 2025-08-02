package ru.omashune.headsanywhere;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import ru.omashune.headsanywhere.head.HeadsManager;
import ru.omashune.headsanywhere.hook.PlaceholderAPIHook;
import ru.omashune.headsanywhere.hook.SkinsRestorerHook;
import ru.omashune.headsanywhere.listener.ResourcePackListener;

public class HeadsAnywhere
        extends JavaPlugin
        implements Listener {
    private HeadsManager headsManager;

    public void onEnable() {
        this.saveDefaultConfig();
        this.headsManager = new HeadsManager(this.getConfig().getString("heads-provider"));
        ConfigurationSection resourcePack = this.getConfig().getConfigurationSection("resource-pack");
        if (resourcePack.getBoolean("enabled")) {
            Bukkit.getPluginManager().registerEvents((Listener)new ResourcePackListener(resourcePack), (Plugin)this);
        }
        Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)this);
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PlaceholderAPIHook(this.headsManager).register();
        }
        if (Bukkit.getPluginManager().isPluginEnabled("SkinsRestorer")) {
            new SkinsRestorerHook(this, this.headsManager);
        }
    }

    @EventHandler
    public void on(PlayerJoinEvent event) {
        this.headsManager.updateCache(event.getPlayer().getName());
    }
}
