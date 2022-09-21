package io.github.cjustinn.specialisedworkforce;

public class BrewLog {
    private boolean isAlchemist;
    private int playerLevel;
    private String playerUuid;

    public BrewLog(boolean _isAlc, int _level, String _uuid) {
        this.isAlchemist = _isAlc;
        this.playerLevel = _level;
        this.playerUuid = _uuid;
    }

    public boolean brewerIsAlchemist() { return this.isAlchemist; }
    public int getBrewerLevel() { return this.playerLevel; }
    public String getBrewerUuid() { return this.playerUuid; }
}
