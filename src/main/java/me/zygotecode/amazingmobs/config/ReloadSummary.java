package me.zygotecode.amazingmobs.config;

/** Combined result of reloading both definition folders. */
public record ReloadSummary(LoadResult mobs, LoadResult hordes) {}
