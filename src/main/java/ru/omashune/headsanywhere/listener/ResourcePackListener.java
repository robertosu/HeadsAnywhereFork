package ru.omashune.headsanywhere.listener;

import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

public class ResourcePackListener implements Listener {
   private final ConfigurationSection resourcePack;

   public ResourcePackListener(ConfigurationSection resourcePack) {
      this.resourcePack = resourcePack;
   }

   @EventHandler
   public void onPlayerJoin(PlayerJoinEvent event) {
      Player player = event.getPlayer();
      String url = this.resourcePack.getString("url");
      String hash = this.resourcePack.getString("hash");
      if (url != null && hash != null) {
         player.setResourcePack(url, hash.getBytes());
      }
   }

   @EventHandler
   public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
      Player player = event.getPlayer();
      switch (event.getStatus()) {
         case SUCCESSFULLY_LOADED:
            this.sendMessageIfEnabled(player, "successfully-loaded");
            break;
         case FAILED_DOWNLOAD:
            this.kickPlayerOrSendMessageIfEnabled(player, "failed-download");
            break;
         case DECLINED:
            this.kickPlayerOrSendMessageIfEnabled(player, "declined");
      }
   }

   private void kickPlayerOrSendMessageIfEnabled(Player player, String key) {
      if (this.resourcePack.getBoolean("kick-player")) {
         player.kick(Component.text(this.resourcePack.getString(key)));
      } else {
         this.sendMessageIfEnabled(player, key);
      }
   }

   private void sendMessageIfEnabled(Player player, String key) {
      if (this.resourcePack.getBoolean("messaging")) {
         player.sendMessage(this.resourcePack.getString(key));
      }
   }
}
