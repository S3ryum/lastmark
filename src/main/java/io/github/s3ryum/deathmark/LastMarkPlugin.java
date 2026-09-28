package io.github.s3ryum.deathmark;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LastMarkPlugin extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    private final Map<UUID, DeathRecord> records = new HashMap<>();
    private File dataFile;
    private ExecutorService storage;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            throw new IllegalStateException("Could not create the LastMark data folder.");
        }

        dataFile = new File(getDataFolder(), "deaths.yml");
        storage = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "LastMark storage");
            thread.setDaemon(true);
            return thread;
        });
        loadRecords();
        getServer().getPluginManager().registerEvents(this, this);

        PluginCommand command = getCommand("lastmark");
        if (command == null) {
            throw new IllegalStateException("The lastmark command is missing from plugin.yml.");
        }
        command.setExecutor(this);
        command.setTabCompleter(this);
        getLogger().info("Loaded " + records.size() + " saved death location(s).");
    }

    @Override
    public void onDisable() {
        if (storage == null) {
            return;
        }

        queueSave();
        storage.shutdown();
        try {
            if (!storage.awaitTermination(5, TimeUnit.SECONDS)) {
                storage.shutdownNow();
                getLogger().warning("Timed out while saving death locations during shutdown.");
            }
        } catch (InterruptedException exception) {
            storage.shutdownNow();
            Thread.currentThread().interrupt();
            getLogger().warning("Interrupted while saving death locations during shutdown.");
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Location location = player.getLocation();
        World world = location.getWorld();
        if (world == null) {
            return;
        }

        records.put(player.getUniqueId(), new DeathRecord(
                world.getUID(), world.getName(), location.getX(), location.getY(), location.getZ(),
                System.currentTimeMillis()));
        queueSave();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command is for players in game.");
            return true;
        }

        if (args.length == 0) {
            showLocation(player);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "compass" -> pointCompass(player);
            case "clear" -> clearLocation(player);
            default -> player.sendMessage(color("&eUsage: /" + label + " [compass|clear]"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return List.of("compass", "clear").stream()
                .filter(option -> option.startsWith(prefix))
                .toList();
    }

    private void showLocation(Player player) {
        DeathRecord record = records.get(player.getUniqueId());
        if (record == null) {
            player.sendMessage(color("&eNo saved death location yet. Your next death will be recorded."));
            return;
        }

        player.sendMessage(color("&6Last death &7(" + record.worldName() + "): &f"
                + block(record.x()) + ", " + block(record.y()) + ", " + block(record.z())));
        player.sendMessage(color("&7Use &f/lastmark compass &7while holding a compass in the same world."));
    }

    private void pointCompass(Player player) {
        DeathRecord record = records.get(player.getUniqueId());
        if (record == null) {
            player.sendMessage(color("&eNo saved death location yet. Your next death will be recorded."));
            return;
        }

        World world = Bukkit.getWorld(record.worldId());
        if (world == null) {
            player.sendMessage(color("&cThe world containing that death location is not loaded."));
            return;
        }
        if (!player.getWorld().getUID().equals(record.worldId())) {
            player.sendMessage(color("&cTravel to " + record.worldName() + " before setting the compass."));
            return;
        }

        ItemStack compass = player.getInventory().getItemInMainHand();
        boolean offHand = false;
        if (compass.getType() != org.bukkit.Material.COMPASS) {
            compass = player.getInventory().getItemInOffHand();
            offHand = true;
        }
        if (compass.getType() != org.bukkit.Material.COMPASS) {
            player.sendMessage(color("&eHold a compass in either hand first."));
            return;
        }

        ItemStack updated = compass.clone();
        CompassMeta meta = (CompassMeta) updated.getItemMeta();
        meta.setLodestone(new Location(world, record.x(), record.y(), record.z()));
        meta.setLodestoneTracked(false);
        updated.setItemMeta(meta);
        if (offHand) {
            player.getInventory().setItemInOffHand(updated);
        } else {
            player.getInventory().setItemInMainHand(updated);
        }
        player.sendMessage(color("&aCompass is now pointing to your last death."));
    }

    private void clearLocation(Player player) {
        if (records.remove(player.getUniqueId()) == null) {
            player.sendMessage(color("&eYou do not have a saved death location."));
            return;
        }
        queueSave();
        player.sendMessage(color("&aYour saved death location was cleared."));
    }

    private void loadRecords() {
        if (!dataFile.isFile()) {
            return;
        }

        YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection players = data.getConfigurationSection("players");
        if (players == null) {
            return;
        }

        for (String key : players.getKeys(false)) {
            ConfigurationSection entry = players.getConfigurationSection(key);
            if (entry == null) {
                continue;
            }
            try {
                UUID playerId = UUID.fromString(key);
                UUID worldId = UUID.fromString(entry.getString("world-id", ""));
                String worldName = entry.getString("world-name", "unknown");
                double x = entry.getDouble("x");
                double y = entry.getDouble("y");
                double z = entry.getDouble("z");
                long recordedAt = entry.getLong("recorded-at");
                if (worldName.isBlank() || !Double.isFinite(x) || !Double.isFinite(y)
                        || !Double.isFinite(z) || recordedAt <= 0) {
                    continue;
                }
                records.put(playerId, new DeathRecord(worldId, worldName, x, y, z, recordedAt));
            } catch (IllegalArgumentException exception) {
                getLogger().warning("Skipped invalid death record for key " + key + ".");
            }
        }
    }

    private void queueSave() {
        Map<UUID, DeathRecord> snapshot = Map.copyOf(records);
        storage.execute(() -> saveSnapshot(snapshot));
    }

    private void saveSnapshot(Map<UUID, DeathRecord> snapshot) {
        YamlConfiguration data = new YamlConfiguration();
        for (Map.Entry<UUID, DeathRecord> entry : snapshot.entrySet()) {
            String path = "players." + entry.getKey();
            DeathRecord record = entry.getValue();
            data.set(path + ".world-id", record.worldId().toString());
            data.set(path + ".world-name", record.worldName());
            data.set(path + ".x", record.x());
            data.set(path + ".y", record.y());
            data.set(path + ".z", record.z());
            data.set(path + ".recorded-at", record.recordedAt());
        }

        File temporaryFile = new File(getDataFolder(), "deaths.yml.tmp-" + Thread.currentThread().threadId());
        try {
            data.save(temporaryFile);
            try {
                Files.move(temporaryFile.toPath(), dataFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile.toPath(), dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            getLogger().severe("Could not save death locations: " + exception.getMessage());
        } finally {
            try {
                Files.deleteIfExists(temporaryFile.toPath());
            } catch (IOException exception) {
                getLogger().warning("Could not remove a temporary data file: " + exception.getMessage());
            }
        }
    }

    private static String block(double coordinate) {
        return Integer.toString((int) Math.floor(coordinate));
    }

    private static Component color(String message) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(message);
    }
}
