package io.github.cjustinn.specialisedworkforce;

public class PotionDurationData {
    private int base;
    private int upgraded;
    private int extended;

    public PotionDurationData(int _b, int _u, int _e) {
        this.base = _b;
        this.upgraded = _u;
        this.extended = _e;
    }

    public int GetBaseDuration() {
        return this.base;
    }

    public int GetUpgradedDuration() {
        return this.upgraded;
    }

    public int GetExtendedDuration() {
        return this.extended;
    }
}
