package ru.omashune.headsanywhere.hook;

import java.io.IOException;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.omashune.headsanywhere.head.HeadsManager;

public class PlaceholderAPIHook extends PlaceholderExpansion {
   private final LegacyComponentSerializer serializer = LegacyComponentSerializer.builder()
      .character('§')
      .hexColors()
      .useUnusualXRepeatedCharacterHexFormat()
      .build();
   private final HeadsManager headsManager;

   public PlaceholderAPIHook(HeadsManager headsManager) {
      this.headsManager = headsManager;
   }

   @NotNull
   public String getIdentifier() {
      return "headsanywhere";
   }

   @NotNull
   public String getAuthor() {
      return "omashune";
   }

   @NotNull
   public String getVersion() {
      return "1.4";
   }

   @Nullable
   public String onPlaceholderRequest(Player player, @NotNull String params) {
      if (!params.startsWith("head")) {
         return null;
      } else {
         int index = params.indexOf("_");
         String name = index == -1 ? (player == null ? "" : player.getName()) : params.substring(index + 1);
         if (name.isEmpty()) {
            return null;
         } else {
            try {
               Component headComponent = this.headsManager.getHeadComponent(name);
               return this.serializer.serialize(headComponent) + "§r";
            } catch (IOException var6) {
               return null;
            }
         }
      }
   }
}
