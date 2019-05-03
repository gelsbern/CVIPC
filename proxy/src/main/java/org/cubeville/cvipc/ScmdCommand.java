package org.cubeville.cvipc;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

public class ScmdCommand extends Command
{
    public ScmdCommand() {
        super("scmd", "cvchat.dispatch.scmd");
    }

    public void execute(CommandSender sender, String[] args) {
        if(args.length < 3) {
            sender.sendMessage("/scmd <server> <player> <command>");
            return;
        }

        String server = args[0];
        String player = args[1];
        String command = "";
        for(int i = 2; i < args.length; i++) {
            if(command.length() > 0) command += " ";
            command += args[i];
        }

        try {
            CVIPC.getInstance().sendMessage(server, "cmd|" + player + "|" + command);
        }
        catch(IllegalArgumentException e) {
            sender.sendMessage(e.getMessage());
        }
    }
}
