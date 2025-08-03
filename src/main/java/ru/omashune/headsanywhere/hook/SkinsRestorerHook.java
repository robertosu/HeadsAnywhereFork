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
import org.bukkit.Bukkit;
import org.bukkit.Server.Spigot;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import ru.omashune.headsanywhere.HeadsAnywhere;
import ru.omashune.headsanywhere.head.HeadsManager;

public class SkinsRestorerHook implements PluginMessageListener {
   private static final String SR_CHANNEL = "sr:messagechannel";
   private final HeadsAnywhere plugin;
   private final HeadsManager headsManager;

   public SkinsRestorerHook(@NotNull HeadsAnywhere plugin, @NotNull HeadsManager headsManager) {
      this.plugin = plugin;
      this.headsManager = headsManager;
      if (this.isProxy()) {
         Bukkit.getMessenger().registerIncomingPluginChannel(plugin, "sr:messagechannel", this);
      } else {
         SkinsRestorerProvider.get().getEventBus().subscribe(plugin, SkinApplyEvent.class, this::handleSkinApplyEvent);
      }
   }

   public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull byte[] message) {
      if (channel.equals("sr:messagechannel")) {
         try {
            DataInputStream input = new DataInputStream(new ByteArrayInputStream(message));

            label34: {
               try {
                  if (!input.readUTF().equals("SkinUpdateV2")) {
                     break label34;
                  }

                  String base64 = input.readUTF();
                  URL skinURL = URI.create(PropertyUtils.getSkinTextureUrl(base64)).toURL();
                  this.headsManager.updateCache(player.getName(), skinURL);
               } catch (Throwable var8) {
                  try {
                     input.close();
                  } catch (Throwable var7) {
                     var8.addSuppressed(var7);
                  }

                  throw var8;
               }

               input.close();
               return;
            }

            input.close();
         } catch (IOException var9) {
            this.plugin.getLogger().severe("Failed to process plugin message: " + var9.getMessage());
            throw new RuntimeException(var9);
         }
      }
   }

   private void handleSkinApplyEvent(SkinApplyEvent event) {
      try {
         Player player = (Player)event.getPlayer(Player.class);
         URL skinURL = URI.create(PropertyUtils.getSkinTextureUrl(event.getProperty())).toURL();
         this.headsManager.updateCache(player.getName(), skinURL);
      } catch (MalformedURLException var4) {
         this.plugin.getLogger().severe("Failed to process skin URL: " + var4.getMessage());
         throw new RuntimeException(var4);
      }
   }

   private boolean isProxy() {
      Spigot spigot = Bukkit.spigot();
      YamlConfiguration paperConfig = spigot.getPaperConfig();
      return spigot.getConfig().getBoolean("settings.bungeecord")
         || paperConfig.getBoolean("settings.velocity-support.enabled")
         || paperConfig.getBoolean("proxies.velocity.enabled");
   }
}
