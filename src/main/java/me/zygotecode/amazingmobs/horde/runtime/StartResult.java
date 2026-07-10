package me.zygotecode.amazingmobs.horde.runtime;

/** Outcome of a horde start attempt: either an instance, or a human-readable reason it was blocked. */
public record StartResult(HordeInstance instance, String error) {

    public static StartResult ok(HordeInstance instance) { return new StartResult(instance, null); }
    public static StartResult fail(String error) { return new StartResult(null, error); }

    public boolean ok() { return instance != null; }
}
