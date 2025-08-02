package ru.omashune.headsanywhere.hook;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import net.skinsrestorer.api.PropertyUtils;
import net.skinsrestorer.api.SkinsRestorerProvider;
import net.skinsrestorer.api.event.SkinApplyEvent;
import net.skinsrestorer.api.property.SkinProperty;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationOptions;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import ru.omashune.headsanywhere.HeadsAnywhere;
import ru.omashune.headsanywhere.head.HeadsManager;

public class SkinsRestorerHook
implements PluginMessageListener {
    private static final String SR_CHANNEL = "sr:messagechannel";
    private final HeadsAnywhere plugin;
    private final HeadsManager headsManager;

    public SkinsRestorerHook(@NotNull HeadsAnywhere plugin, @NotNull HeadsManager headsManager) {
        this.plugin = plugin;
        this.headsManager = headsManager;
        if (this.isProxy()) {
            Bukkit.getMessenger().registerIncomingPluginChannel((Plugin)plugin, SR_CHANNEL, (PluginMessageListener)this);
            return;
        }
        SkinsRestorerProvider.get().getEventBus().subscribe((Object)plugin, SkinApplyEvent.class, this::handleSkinApplyEvent);
    }

    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull byte[] message) {
        if (!channel.equals(SR_CHANNEL)) {
            return;
        }
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(message));){
            if (!input.readUTF().equals("SkinUpdateV2")) {
                return;
            }
            String base64 = input.readUTF();
            URL skinURL = URI.create(PropertyUtils.getSkinTextureUrl((String)base64)).toURL();
            this.headsManager.updateCache(player.getName(), skinURL);
        }
        catch (IOException e) {
            this.plugin.getLogger().severe("Failed to process plugin message: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void handleSkinApplyEvent(SkinApplyEvent event) {
        try {
            Player player = (Player)event.getPlayer(Player.class);
            URL skinURL = URI.create(PropertyUtils.getSkinTextureUrl((SkinProperty)event.getProperty())).toURL();
            this.headsManager.updateCache(player.getName(), skinURL);
        }
        catch (MalformedURLException e) {
            this.plugin.getLogger().severe("Failed to process skin URL: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private boolean isProxy() {
        return true;
    }
}
