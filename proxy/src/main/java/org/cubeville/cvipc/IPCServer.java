package org.cubeville.cvipc;

import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import java.net.InetAddress;

// TODO: outstream and cancelled might need some locking (though concurrent access is extremely unlikely)

public class IPCServer implements Runnable
{
    private int port;
    private String serverName;
    
    private CVIPC plugin;

    private ServerSocket server;
    private Socket socket;
    DataOutputStream outstream;
    
    private boolean cancelled = false;
    
    public IPCServer(CVIPC plugin, String serverName, int port) {
        this.port = port;
        this.plugin = plugin;
        this.serverName = serverName;
        cancelled = false;
        ProxyServer.getInstance().getScheduler().runAsync(plugin, this);
    }

    public void stop() {
        cancelled = true;
        try { socket.close(); } catch (Exception e) {}
        try { server.close(); } catch (Exception e) {}
    }
    
    public void run() {
        try {
            server = new ServerSocket(port, 2, InetAddress.getByName(null));
        }
        catch(IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
        
        while(!cancelled) {
            try {
                socket = server.accept();
                DataInputStream in = new DataInputStream(socket.getInputStream());
                outstream = new DataOutputStream(socket.getOutputStream());
                while(!cancelled) {
                    String rd = in.readUTF();
                    ProxyServer.getInstance().getScheduler().runAsync(plugin, new Runnable() {
                            public void run() {
                                plugin.processRemoteMessage(serverName, rd);
                            }
                        });
                }
            }
            catch(IOException e) {
                try { socket.close(); } catch (Exception ed) {}
            }
        }
    }

    public synchronized void send(String message) {
        ProxyServer.getInstance().getScheduler().runAsync(plugin, new Runnable() {
                public void run() {
                    doSend(message);
                }
            });
    }

    private synchronized void doSend(String message) {
        if(outstream == null) return;
        try {
            outstream.writeUTF(message);
        }
        catch(IOException e) {}
    }
}
