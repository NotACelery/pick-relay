package dev.maicra.pickrelay.session;

public enum RelayWorkMode {
    UNTIL_BROKEN,
    DURABILITY,
    BLOCKS;

    public RelayWorkMode next() {
        RelayWorkMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
