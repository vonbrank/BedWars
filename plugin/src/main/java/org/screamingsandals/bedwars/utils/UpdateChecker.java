package org.screamingsandals.bedwars.utils;

import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.screamingsandals.bedwars.Main;
import org.screamingsandals.bedwars.commands.BaseCommand;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.gson.GsonConfigurationLoader;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.screamingsandals.bedwars.lib.lang.I.*;

public class UpdateChecker {
    public static void run() {
        HttpClient.newHttpClient().sendAsync(HttpRequest.newBuilder()
                .uri(URI.create("https://screamingsandals.org/bedwars-zero-update-checker.php?version=" + Main.getVersion()))
                .build(), HttpResponse.BodyHandlers.ofInputStream())
                .thenAccept(inputStreamHttpResponse -> {
                    var loader = GsonConfigurationLoader.builder()
                            .source(() -> new BufferedReader(new InputStreamReader(inputStreamHttpResponse.body())))
                            .build();
                    try {
                        var result = loader.load();

                        if ("ok".equalsIgnoreCase(result.node("status").getString())) {
                            if (result.node("zero_update").getBoolean()) {
                                if (Main.getConfigurator().node("update-checker", "console").getBoolean()) {
                                    mpr("update_checker_zero")
                                            .replace("version", result.node("version").getString())
                                            .send(Bukkit.getConsoleSender());
                                    mpr("update_checker_zero_second")
                                            .replace("url", result.node("zero_download_url").getString())
                                            .send(Bukkit.getConsoleSender());
                                }
                                if (Main.getConfigurator().node("update-checker", "admins").getBoolean()) {
                                    Main.getInstance().registerBedwarsListener(new UpdateListener(result));
                                }
                            }
                        }
                    } catch (ConfigurateException e) {
                        e.printStackTrace();
                    }
                });
    }

    @Data
    public static class UpdateListener implements Listener {
        private final ConfigurationNode result;

        @EventHandler
        public void onPlayerJoin(PlayerJoinEvent event) {
            Player player = event.getPlayer();
            if (BaseCommand.hasPermission(player, BaseCommand.ADMIN_PERMISSION, false)) {
                if (Main.getConfigurator().node("update-checker", "admins").getBoolean() && result.node("zero_update").getBoolean()) {
                    mpr("update_checker_zero")
                            .replace("version", result.node("version").getString())
                            .send(player);
                    mpr("update_checker_zero_second")
                            .replace("url", result.node("zero_download_url").getString())
                            .send(player);
                }
            }
        }
    }
}
