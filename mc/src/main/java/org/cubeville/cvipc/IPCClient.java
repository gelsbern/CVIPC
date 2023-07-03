package org.cubeville.cvipc;
 
import java.io.IOException;
import java.net.Socket;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

public class IPCClient implements Runnable
{
    private int port;
    private CVIPC plugin;
    private String address;
    
    Socket socket;
    DataOutputStream outstream;
    boolean connected;
    AtomicBoolean active;
    
    public IPCClient(CVIPC plugin, int port, String address) {
        this.port = port;
        this.plugin = plugin;
	if(address == null) {
	    this.address = "127.0.0.1";
	}
	else {
	    this.address = address;
	}
        connected = false;
        active = new AtomicBoolean(true);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this);
    }

    public void stop() {
        active.set(false);
        try { socket.close(); } catch (Exception e) {}
        connected = false;
    }
    
    public void run() {
        DataInputStream instream = null;
        try {
            System.out.println("IPC connect to port " + port);
            socket = new Socket(address, port);
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
            if(active.get()) {
                plugin.getServer().getScheduler(). runTaskLaterAsynchronously(plugin, this, 40);
            }
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
