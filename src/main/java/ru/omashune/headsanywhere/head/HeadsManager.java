package ru.omashune.headsanywhere.head;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HeadsManager {
    private static final Rectangle HEAD_RECT = new Rectangle(8, 8, 8, 8);
    private static final Rectangle HELMET_RECT = new Rectangle(40, 8, 8, 8);
    private static final char[] chars = new char[]{'ϧ', 'Ϩ', 'ϩ', 'Ϫ', 'ϫ', 'Ϭ', 'ϭ', 'Ϯ'};
    private final String headsProvider;
    private final Cache<String, URL> skinCache;
    private final Cache<String, Component> componentCache;

    public HeadsManager(@NotNull String headsProvider) {
        this.headsProvider = headsProvider;
        this.skinCache = Caffeine.newBuilder().expireAfterWrite(1L, TimeUnit.HOURS).maximumSize(1000L).build();
        this.componentCache = Caffeine.newBuilder().expireAfterWrite(30L, TimeUnit.MINUTES).maximumSize(1000L).build();
    }

    public void updateCache(@NotNull String playerName, @Nullable URL skinUrl) {
        if (skinUrl != null) {
            this.skinCache.put(playerName, skinUrl);
            this.componentCache.invalidate(playerName);
            return;
        }
        URL cachedUrl = this.skinCache.getIfPresent((playerName));
        URL currentUrl = Bukkit.getOfflinePlayer(playerName).getPlayerProfile().getTextures().getSkin();
        if (cachedUrl != null && !cachedUrl.equals(currentUrl)) {
            this.skinCache.invalidate(playerName);
            this.componentCache.invalidate(playerName);
        }
    }

    public void updateCache(@NotNull String playerName) {
        this.updateCache(playerName, null);
    }

    public Component getHeadComponent(@NotNull String playerName) throws IOException {
        Component cachedComponent = (Component)this.componentCache.getIfPresent(playerName);
        if (cachedComponent != null) {
            return cachedComponent;
        }
        URL skinUrl = this.resolveSkinUrl(playerName);
        BufferedImage headImage = this.extractHeadImage(skinUrl);
        Component component = this.buildHeadComponent(headImage);
        this.componentCache.put(playerName, component);
        if (!this.headsProvider.contains(skinUrl.getHost())) {
            this.skinCache.put(playerName, skinUrl);
        }
        return component;
    }

    private BufferedImage extractHeadImage(@NotNull URL skinUrl) throws IOException {
        BufferedImage skinImage = ImageIO.read(skinUrl);
        if (skinImage == null) {
            throw new IOException("Failed to read skin image from URL: " + String.valueOf(skinUrl));
        }
        if (skinImage.getWidth() == 8 && skinImage.getHeight() == 8) {
            return skinImage;
        }
        BufferedImage headImage = this.getImagePart(skinImage, HEAD_RECT);
        BufferedImage helmetImage = this.getImagePart(skinImage, HELMET_RECT);
        if (helmetImage != null) {
            Graphics2D g = headImage.createGraphics();
            g.drawImage((Image)helmetImage, 0, 0, null);
            g.dispose();
        }
        return headImage;
    }

    private Component buildHeadComponent(@NotNull BufferedImage headImage) {
        TextComponent.Builder componentBuilder = Component.text();
        for (int x = 0; x < headImage.getWidth(); ++x) {
            TextComponent.Builder line = Component.text();
            for (int y = 0; y < headImage.getHeight(); ++y) {
                line.append(Component.text((char)chars[y]).color(TextColor.color((int)headImage.getRGB(x, y))));
            }
            componentBuilder.append(line);
        }
        return componentBuilder.build();
    }

    private URL resolveSkinUrl(@NotNull String playerName) throws IOException {
        try {
            URL skinUrl = Bukkit.getOfflinePlayer((String)playerName).getPlayerProfile().getTextures().getSkin();
            return skinUrl != null ? skinUrl : new URI(String.format(this.headsProvider, playerName)).toURL();
        }
        catch (URISyntaxException e) {
            throw new IOException("Error resolving skin URL for player: " + playerName, e);
        }
    }

    private BufferedImage getImagePart(@NotNull BufferedImage image, @NotNull Rectangle part) {
        if (part.x < 0 || part.y < 0 || part.x + part.width > image.getWidth() || part.y + part.height > image.getHeight()) {
            throw new IllegalArgumentException(String.format("Rectangle %s is out of bounds for image %dx%d", part, image.getWidth(), image.getHeight()));
        }
        return image.getSubimage(part.x, part.y, part.width, part.height);
    }
}