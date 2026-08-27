package dev.maicra.pickrelay.session;

public enum StopReason {
    MANUAL,
    QUEUE_COMPLETE,
    PLAYER_MOVED,
    PLAYER_DEATH,
    DISCONNECT,
    DIMENSION_CHANGE,
    TOOL_INVALID,
    INVENTORY_DESYNC,
    INTERNAL_SAFETY
}
