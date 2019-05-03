package org.cubeville.cvipc;

public interface IPCInterface
{
    public void process(String server, String channel, String message);
}
