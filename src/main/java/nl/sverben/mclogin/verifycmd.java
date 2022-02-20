package nl.sverben.mclogin;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import java.io.IOException;

public class verifycmd implements CommandExecutor {
    MCLogin plug;
    Gson g = new Gson();

    public verifycmd(MCLogin plug) {
        this.plug = plug;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player p = (Player) sender;
        if (!p.hasMetadata("LoggingIn")) return true;
        if (!p.getMetadata("LoggingIn").get(0).asBoolean()) return true;
        String state = p.getMetadata("state").get(0).asString();

        JsonObject json = null;
        try {
            json = g.fromJson(plug.get("http://mclogin:3000/api/checkState?state=" + state), JsonObject.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (json.get("ownerShip").isJsonNull()) {
            p.sendMessage(ChatColor.RED + "Log in first!");
            return true;
        }
        if (json.get("ownerShip").getAsBoolean()) {
            p.teleport(plug.getServer().getWorld("lobby").getSpawnLocation());
            plug.getServer().broadcastMessage(ChatColor.YELLOW + p.getName() + " logged into the server");
            p.setMetadata("LoggingIn", new FixedMetadataValue(plug, false));
            return true;
        } else {
            p.kickPlayer("Invalid login state!");
            return true;
        }
    }
}
