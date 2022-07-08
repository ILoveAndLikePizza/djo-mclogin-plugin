package nl.sverben.mclogin;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class SocketThread extends Thread {
    private Boolean loop = true;
    private MCLogin plug;
    private ServerSocket serverSocket;
    private Socket socket;
    public ArrayList<String> queue = new ArrayList<String>();
    public SocketThread(MCLogin plug) {
        this.plug = plug;
    }

    public void run() {
        try {
            serverSocket = new ServerSocket(1234);
            socket = serverSocket.accept();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        plug.getLogger().info("Client connected!");

        try {
            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            while (loop) {
                String line = reader.readLine();
                synchronized (this) {
                    queue.add(line);
                }
             }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void end() {
        this.loop = false;

        try {
            socket.close();
        } catch (Exception e) {
            // sorry voor dit, was te lui om op te zoeken hoe je 2 exceptions tegelijk kan catchen, exceptions: IOException en NullPointerException
        }
        try {
            serverSocket.close();
        } catch (IOException e) {}
    }
}
