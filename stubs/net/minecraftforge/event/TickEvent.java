package net.minecraftforge.event;
public class TickEvent {
    public enum Phase { START, END }
    public static class ServerTickEvent extends TickEvent { public Phase phase; }
}
