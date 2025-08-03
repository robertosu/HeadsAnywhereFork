package ru.omashune.headsanywhere.head;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent.Builder;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HeadsManager {
   private static final Rectangle HEAD_RECT = new Rectangle(8, 8, 8, 8);
   private static final Rectangle HELMET_RECT = new Rectangle(40, 8, 8, 8);
   private static final char[] chars = new char[]{'ϧ', 'Ϩ', 'ϩ', 'Ϫ', 'ϫ', 'Ϭ', 'ϭ', 'Ϯ'};
   private final String headsProvider;
   private final boolean enableSkinOverlay;
   private final Cache<String, URL> skinCache;
   private final Cache<String, Component> componentCache;

   public HeadsManager(@NotNull String headsProvider, boolean enableSkinOverlay) {
      this.headsProvider = headsProvider;
      this.enableSkinOverlay = enableSkinOverlay;
      this.skinCache = Caffeine.newBuilder().expireAfterWrite(1L, TimeUnit.HOURS).maximumSize(1000L).build();
      this.componentCache = Caffeine.newBuilder().expireAfterWrite(30L, TimeUnit.MINUTES).maximumSize(1000L).build();
   }

   // Constructor legacy para compatibilidad
   public HeadsManager(@NotNull String headsProvider) {
      this(headsProvider, true);
   }

   public void updateCache(@NotNull String playerName, @Nullable URL skinUrl) {
      if (skinUrl != null) {
         this.skinCache.put(playerName, skinUrl);
         this.componentCache.invalidate(playerName);
      } else {
         URL cachedUrl = (URL)this.skinCache.getIfPresent(playerName);
         URL currentUrl = Bukkit.getOfflinePlayer(playerName).getPlayerProfile().getTextures().getSkin();
         if (cachedUrl != null && !cachedUrl.equals(currentUrl)) {
            this.skinCache.invalidate(playerName);
            this.componentCache.invalidate(playerName);
         }
      }
   }

   public void updateCache(@NotNull String playerName) {
      this.updateCache(playerName, null);
   }

   public Component getHeadComponent(@NotNull String playerName) throws IOException {
      Component cachedComponent = (Component)this.componentCache.getIfPresent(playerName);
      if (cachedComponent != null) {
         return cachedComponent;
      } else {
         URL skinUrl = this.resolveSkinUrl(playerName);
         BufferedImage headImage = this.extractHeadImage(skinUrl);
         Component component = this.buildHeadComponent(headImage);
         this.componentCache.put(playerName, component);
         if (!this.headsProvider.contains(skinUrl.getHost())) {
            this.skinCache.put(playerName, skinUrl);
         }

         return component;
      }
   }

   private BufferedImage extractHeadImage(@NotNull URL skinUrl) throws IOException {
      BufferedImage skinImage = ImageIO.read(skinUrl);
      if (skinImage == null) {
         throw new IOException("Failed to read skin image from URL: " + skinUrl);
      } else if (skinImage.getWidth() == 8 && skinImage.getHeight() == 8) {
         return skinImage;
      } else {
         // Debug para verificar dimensiones (puedes comentar esto en producción)
         // debugSkinRegions(skinImage);

         // Extraer la región de la cara (8x8 desde coordenadas 8,8)
         BufferedImage headImage = this.getImagePart(skinImage, HEAD_RECT);
         if (headImage == null) {
            throw new IOException("Failed to extract head region from skin image");
         }

         // Solo aplicar overlay si está habilitado en configuración
         if (this.enableSkinOverlay) {
            // Verificar si la skin tiene altura suficiente para overlay (64 píxeles)
            if (skinImage.getHeight() >= 64) {
               BufferedImage helmetImage = this.getImagePart(skinImage, HELMET_RECT);
               if (helmetImage != null) {
                  // Aplicar overlay pixel por pixel, respetando la transparencia
                  for (int x = 0; x < 8; x++) {
                     for (int y = 0; y < 8; y++) {
                        int overlayRGB = helmetImage.getRGB(x, y);
                        int alpha = (overlayRGB >> 24) & 0xFF;

                        // Solo aplicar píxeles no transparentes del overlay
                        if (alpha != 0) {
                           headImage.setRGB(x, y, overlayRGB);
                        }
                     }
                  }
               }
            }
         }

         return headImage;
      }
   }

   private Component buildHeadComponent(@NotNull BufferedImage headImage) {
      Builder componentBuilder = Component.text();

      for (int x = 0; x < headImage.getWidth(); x++) {
         Builder line = Component.text();

         for (int y = 0; y < headImage.getHeight(); y++) {
            int rgb = headImage.getRGB(x, y);
            // Mantener el canal alpha si existe
            line.append(Component.text(chars[y]).color(TextColor.color(rgb & 0xFFFFFF)));
         }

         componentBuilder.append(line);
      }

      return componentBuilder.build();
   }

   private URL resolveSkinUrl(@NotNull String playerName) throws IOException {
      try {
         URL skinUrl = Bukkit.getOfflinePlayer(playerName).getPlayerProfile().getTextures().getSkin();
         return skinUrl != null ? skinUrl : new URI(String.format(this.headsProvider, playerName)).toURL();
      } catch (URISyntaxException var3) {
         throw new IOException("Error resolving skin URL for player: " + playerName, var3);
      }
   }

   private BufferedImage getImagePart(@NotNull BufferedImage image, @NotNull Rectangle part) {
      // Verificar que las coordenadas estén dentro de los límites de la imagen
      if (part.x >= 0 && part.y >= 0 &&
              part.x + part.width <= image.getWidth() &&
              part.y + part.height <= image.getHeight()) {
         return image.getSubimage(part.x, part.y, part.width, part.height);
      } else {
         // Log para debugging
         System.out.println("Rectangle " + part + " is out of bounds for image " +
                 image.getWidth() + "x" + image.getHeight());
         return null;
      }
   }

   // Getter para acceder a la configuración de overlay
   public boolean isOverlayEnabled() {
      return this.enableSkinOverlay;
   }

   /**
    * Método de debugging para verificar la extracción de regiones
    */
   private void debugSkinRegions(@NotNull BufferedImage skinImage) {
      System.out.println("=== DEBUG SKIN REGIONS ===");
      System.out.println("Skin dimensions: " + skinImage.getWidth() + "x" + skinImage.getHeight());
      System.out.println("Head region (8,8,8,8): " + HEAD_RECT);
      System.out.println("Helmet region (40,8,8,8): " + HELMET_RECT);
      System.out.println("Overlay enabled: " + this.enableSkinOverlay);

      // Verificar si las regiones están dentro de los límites
      boolean headValid = HEAD_RECT.x + HEAD_RECT.width <= skinImage.getWidth() &&
              HEAD_RECT.y + HEAD_RECT.height <= skinImage.getHeight();
      boolean helmetValid = HELMET_RECT.x + HELMET_RECT.width <= skinImage.getWidth() &&
              HELMET_RECT.y + HELMET_RECT.height <= skinImage.getHeight();

      System.out.println("Head region valid: " + headValid);
      System.out.println("Helmet region valid: " + helmetValid);
      System.out.println("=========================");
   }
}