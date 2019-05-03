package org.cubeville.cvipc;

import java.io.IOException;
import java.net.Socket;
import java.io.DataInputStream;
import java.io.DataOutputStream;

public class IPCClient implements Runnable
{
    private int port;
    private CVIPC plugin;
    Socket socket;
    DataOutputStream outstream;
    boolean connected;
    
    public IPCClient(CVIPC plugin, int port) {
        this.port = port;
        this.plugin = plugin;
        connected = false;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this);
    }

    public void stop() {
        try { socket.close(); } catch (Exception e) {}
        connected = false;
    }
    
    public void run() {
        DataInputStream instream = null;
        try {
            System.out.println("IPC connect to port " + port);
            socket = new Socket("127.0.0.1", port);
            outstream = new DataOutputStream(socket.getOutputStream());
            instream = new DataInputStream(socket.getInputStream());
            connected = true;
            while(true) {
                String rd = instream.readUTF();
                plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
                        public void run() {
                            plugin.processRemoteMessage(rd);
                        }
                    });
            }
        }
        catch(IOException e) {
            System.out.println("IPC connection broken.");
            try { outstream.close(); } catch (Exception ed) {}
            try { instream.close(); } catch (Exception ed) {}
            try { socket.close(); } catch (Exception ed) {}
            connected = false;
            plugin.getServer().getScheduler(). runTaskLaterAsynchronously(plugin, this, 40);
        }
    }

    public void reconnect() {
        if(!connected) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this);
        }
    }
    
    public void send(String message) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
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
