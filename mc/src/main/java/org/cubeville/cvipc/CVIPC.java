package org.cubeville.cvipc;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class CVIPC extends JavaPlugin
{
    private IPCClient ipcClient;
    Map<String, IPCInterface> ipcInterfaces;
    
    public void onEnable() {
        Integer port = getConfig().getInt("ipc_server_port");
        ipcClient = new IPCClient(this, port);
        ipcInterfaces = new HashMap<>();
    }

    public void onDisable() {
        ipcClient.stop();
    }
    
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if(label.equals("pcmd")) {
            String pcmd = "";
            for(int i = 0; i < args.length; i++) {
                if(pcmd.length() > 0) pcmd += " ";
                pcmd += args[i];
            }
            sendMessage("cmd|console|" + pcmd);
        }
        else if(label.equals("reconnect")) {
            ipcClient.reconnect();
        }
        return false;
    }

    public void sendMessage(String message) {
        if(ipcClient == null) return;
        ipcClient.send(message);
    }

    public void sendMessage(String channel, String message) {
        if(ipcClient == null) return;
        ipcClient.send(channel + "|" + message);
    }

    public void registerInterface(String channel, IPCInterface ipcInterface) {
        ipcInterfaces.put(channel, ipcInterface);
    }

    public void deregisterInterface(String channel) {
        ipcInterfaces.remove(channel);
    }

    public void processRemoteMessage(String message) {
        if(message.indexOf("|") == -1) return;

        String channel = getPart(message);
        message = getRest(message);

        if(channel.equals("cmd")) { // Built-in command dispatcher channel
            String player = getPart(message);
            message = getRest(message);
            CommandSender sender;
            if(player.equals("console")) sender = getServer().getConsoleSender();
            else sender = getServer().getPlayer(UUID.fromString(player));
            if(sender != null) getServer().dispatchCommand(sender, message);
        }
        else {
            IPCInterface i = ipcInterfaces.get(channel);
            if (i != null) i.process(channel, message);
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

