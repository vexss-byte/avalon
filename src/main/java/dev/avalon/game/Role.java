package dev.avalon.game;

public enum Role {
    MERLIN("Merlin", true),
    PERCIVAL("Percival", true),
    KINGS_DAUGHTER("King's Daughter", true),
    LOYAL_SERVANT("Loyal Servant", true),
    ASSASSIN("Assassin", false),
    MORGANA("Morgana", false),
    MORDRED("Mordred", false),
    MINION("Minion of Mordred", false);

    public final String display;
    public final boolean good;

    Role(String display, boolean good) {
        this.display = display;
        this.good = good;
    }

    public boolean evil() {
        return !good;
    }
}
