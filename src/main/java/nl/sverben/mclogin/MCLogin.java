package nl.sverben.mclogin;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public final class MCLogin extends JavaPlugin implements Listener {
    World empty;
    Gson g = new Gson();
    SocketThread thread;

    @Override
    public void onEnable() {
        empty = getServer().getWorld("empty");

        if (empty == null) {
            WorldCreator wc = new WorldCreator("empty");

            wc.type(WorldType.FLAT);
            wc.generator(new VoidGenerator());

            empty = wc.createWorld();
        }

        getCommand("verify").setExecutor(new verifycmd(this));
        getServer().getPluginManager().registerEvents(this, this);

        thread = new SocketThread(this);
        thread.start();

        MCLogin plug = this;
        getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {

            @Override
            public void run() {
                for (String name : thread.queue) {
                    if (name == null || name.isEmpty()) {
                        continue;
                    }
                    Player p = getServer().getPlayer(name);
                    p.teleport(getServer().getWorld("lobby").getSpawnLocation());
                    getServer().broadcastMessage(ChatColor.YELLOW + p.getName() + " logged into the server");
                    p.setMetadata("LoggingIn", new FixedMetadataValue(plug, false));
                }
                synchronized (this) {
                    thread.queue.clear();
                }
            }
        }, 20L, 20L);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        thread.end();
    }

    @EventHandler
    public void playerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();

        p.setMetadata("LoggingIn", new FixedMetadataValue(this, true));
        p.teleport(new Location(empty, 0, 0, 0));
        p.setGameMode(GameMode.SPECTATOR);

        String result = "{}";
        try {
            result = get("http://mclogin:3000/api/genState?username=" + p.getName());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        getServer().getLogger().info(result);
        JsonObject json = g.fromJson(result, JsonObject.class);

        String url = json.get("auth").getAsString();
        String state = json.get("state").getAsString();

        p.sendMessage("Log in to DJO Minecraft on " + ChatColor.BOLD + url);
        p.setMetadata("state", new FixedMetadataValue(this, state));
    }

    public static String get(String url) throws IOException {
        HttpURLConnection httpClient =
                (HttpURLConnection) new URL(url).openConnection();

        httpClient.setRequestProperty("User-Agent", "Mozilla/5.0");

        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(httpClient.getInputStream()))) {

            StringBuilder response = new StringBuilder();
            String line;

            while ((line = in.readLine()) != null) {
                response.append(line);
            }

            return response.toString();

        } catch(Exception e) {
            e.printStackTrace();
        }

        return "{}";
    }

    @EventHandler
    public void cmdProcess(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();
        if (e.getMessage().equals("/verify")) return;
        if (!p.hasMetadata("LoggingIn")) return;
        if (!p.getMetadata("LoggingIn").get(0).asBoolean()) return;

        e.setCancelled(true);
    }
}
