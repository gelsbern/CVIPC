package org.cubeville.cvipc;

import java.io.IOException;
import java.io.File;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

public class CVIPC extends Plugin {
    Map<String, IPCServer> ipcServers;
    Map<String, IPCInterface> ipcInterfaces;

    ProxyServer server;
    
    private static CVIPC instance;
    public static CVIPC getInstance() {
        return instance;
    }

    public void onEnable() {
        instance = this;
        server = ProxyServer.getInstance();

        ipcServers = new HashMap<>();
        ipcInterfaces = new HashMap<>();

        try {
            File configFile = new File(getDataFolder(), "config.yml");
            Configuration config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
            Configuration ipcList = (Configuration) config.get("ipc");
            Collection<String> servers = ipcList.getKeys();
            for(String server: servers) {
                Configuration sc = (Configuration) ipcList.get(server);
                int port = ((Configuration) ipcList.get(server)).getInt("port");
		String source = ((Configuration) ipcList.get(server)).getString("source");
		boolean pcmd = ((Configuration) ipcList.get(server)).getBoolean("pcmd");
		List<String> whitelist = ((Configuration) ipcList.get(server)).getStringList("whitelist");
                ipcServers.put(server, new IPCServer(this, server, port, source, pcmd, whitelist));
            }

            getProxy().getPluginManager().registerCommand(this, new ScmdCommand());
        }
        catch (IOException e) {
            System.out.println("Unable to load config.yml, no IPC available.");
        }
    }

    public void onDisable() {
        for(String s: ipcServers.keySet()) {
            ipcServers.get(s).stop();
        }
        instance = null;
    }

    public void onServerConnect(String server) {
        IPCInterface i = ipcInterfaces.get("serverconnect");
        if(i != null) i.process(server, "serverconnect", "");
    }
    
    public void sendMessage(String server, String message) {
        IPCServer ipcServer = ipcServers.get(server);
        if(ipcServer == null) throw new IllegalArgumentException("Server " + server + " is not registerd: " + message);
        ipcServer.send(message);
    }

    public void broadcastMessage(String message) {
        for(IPCServer ipcServer: ipcServers.values()) {
            ipcServer.send(message);
        }
    }
    
    public void registerInterface(String channel, IPCInterface ipcInterface) {
        ipcInterfaces.put(channel, ipcInterface);
    }

    public void deregisterInterface(String channel) {
        ipcInterfaces.remove(channel);
    }

    protected void processRemoteMessage(String serverName, String message) {
	{
	    IPCServer s = ipcServers.get(serverName);
	    if(s.getPcmd() == false) {
		if(s.isInWhitelist(message) == false) {
		    System.out.println("Denied IPC request from " + serverName + ": " + message);
		    return;
		}
	    }
	}
	
        if(message.indexOf("|") == -1) return;

        String channel = getPart(message);
        message = getRest(message);

        if(channel.equals("cmd")) { // Built-in command dispatcher channel
            String player = getPart(message);
            message = getRest(message);
            CommandSender sender;
            if(player.equals("console")) sender = server.getConsole();
            else sender = server.getPlayer(UUID.fromString(player));
            if(sender != null) server.getPluginManager().dispatchCommand(sender, message);
        }
        else if(channel.equals("fwd")) { // Built-in command forwarder channel
            String server = getPart(message);
            message = getRest(message);
            sendMessage(server, message);
        }
        else if(channel.equals("server")) { // Built-in move-player-to-server
            UUID playerId = UUID.fromString(getPart(message));
            String targetServer = getRest(message);
            ProxiedPlayer player = server.getPlayer(playerId);
            if(player == null) return;
            ServerInfo serverInfo = server.getServerInfo(targetServer);
            if(serverInfo == null) return;
            player.connect(serverInfo);
        }
        else {
            IPCInterface i = ipcInterfaces.get(channel);
            if (i != null) i.process(serverName, channel, message);
        }
    }

    private String getPart(String m) {
        if(m.indexOf("|") == -1) return "";
        return m.substring(0, m.indexOf("|"));
    }

    private String getRest(String m) {
        if(m.indexOf("|") == -1) return m;
        return m.substring(m.indexOf("|") + 1);
    }
}
