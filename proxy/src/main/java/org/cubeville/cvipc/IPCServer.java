package org.cubeville.cvipc;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.scheduler.ScheduledTask;

// TODO: outstream and cancelled might need some locking (though concurrent access is extremely unlikely)

public class IPCServer implements Runnable
{
    private int port;
    private String serverName;
    private boolean pcmd;
    private String source;
    private List<Pattern> whitelist;
    
    private CVIPC plugin;

    private ServerSocket server;
    private Socket socket;
    DataOutputStream outstream;

    private boolean cancelled = false;

    public IPCServer(CVIPC plugin, String serverName, int port, String source, boolean pcmd, List<String> whitelist) {
        this.port = port;
        this.plugin = plugin;
        this.serverName = serverName;
        this.source = source;
        this.pcmd = pcmd;
	
	this.whitelist = new ArrayList<>();
	for(String p: whitelist) {
	    this.whitelist.add(Pattern.compile(p));
	}
	
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
            if(source == null) {
                server = new ServerSocket(port, 2, InetAddress.getByName(null));
            }
            else {
                server = new ServerSocket(port, 2);
            }
        }
        catch(IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
        
        while(!cancelled) {
            try {
                socket = server.accept();
		if(!socket.getInetAddress().toString().equals("/127.0.0.1")) {
		    if(!socket.getInetAddress().toString().equals(source)) {
			System.out.println("Invalid access from IP address " + socket.getInetAddress().toString());
			socket.close();
			continue;
		    }
		}
		System.out.println("IPC server " + serverName + " connected from: " + socket.getInetAddress().toString() + ", pcmd is " + (pcmd ? "enabled." : "disabled."));

                DataInputStream in = new DataInputStream(socket.getInputStream());
                outstream = new DataOutputStream(socket.getOutputStream());
                plugin.onServerConnect(serverName);
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
                try {} catch (Exception ed) {}
            }
	    finally {
		try {
		    socket.close();
		}
		catch(IOException e) {}
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

    public boolean getPcmd() {
        return pcmd;
    }

    public boolean isInWhitelist(String s) {
	for(Pattern p: whitelist) {
	    if(p.matcher(s).matches()) return true;
	}
	return false;
    }

}
