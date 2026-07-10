package me.zygotecode.amazingmobs.horde;

import java.util.List;

/**
 * One wave of a horde: a set of {@link WaveEntry} lines plus pacing/feedback. Advances when the
 * alive fraction drops to/below {@code 1 - clearThreshold} (i.e. enough killed) or its duration
 * elapses. Pure data.
 */
public final class Wave {

    private final int index;
    private final String label;
    private final List<WaveEntry> entries;
    private final long startDelayTicks;   // pause before this wave begins spawning
    private final long durationTicks;     // hard cap before forced advance (0 = none)
    private final double clearThreshold;  // fraction that must be killed to advance (0..1)
    private final String message;
    private final String title;
    private final String subtitle;
    private final String sound;

    private Wave(Builder b) {
        this.index = b.index;
        this.label = b.label;
        this.entries = b.entries == null ? List.of() : List.copyOf(b.entries);
        this.startDelayTicks = b.startDelayTicks;
        this.durationTicks = b.durationTicks;
        this.clearThreshold = b.clearThreshold;
        this.message = b.message;
        this.title = b.title;
        this.subtitle = b.subtitle;
        this.sound = b.sound;
    }

    public int index() { return index; }
    public String label() { return label; }
    public List<WaveEntry> entries() { return entries; }
    public long startDelayTicks() { return startDelayTicks; }
    public long durationTicks() { return durationTicks; }
    public double clearThreshold() { return clearThreshold; }
    public String message() { return message; }
    public String title() { return title; }
    public String subtitle() { return subtitle; }
    public String sound() { return sound; }

    public boolean hasBoss() {
        for (WaveEntry e : entries) if (e.boss()) return true;
        return false;
    }

    public boolean hasObjectives() {
        for (WaveEntry e : entries) if (e.objective()) return true;
        return false;
    }

    public static Builder builder(int index) { return new Builder(index); }

    public static final class Builder {
        private final int index;
        private String label;
        private List<WaveEntry> entries;
        private long startDelayTicks = 0;
        private long durationTicks = 0;
        private double clearThreshold = 1.0;
        private String message, title, subtitle, sound;

        public Builder(int index) { this.index = index; this.label = "Wave " + (index + 1); }
        public Builder label(String v) { if (v != null) this.label = v; return this; }
        public Builder entries(List<WaveEntry> v) { this.entries = v; return this; }
        public Builder startDelayTicks(long v) { this.startDelayTicks = v; return this; }
        public Builder durationTicks(long v) { this.durationTicks = v; return this; }
        public Builder clearThreshold(double v) { this.clearThreshold = v; return this; }
        public Builder message(String v) { this.message = v; return this; }
        public Builder title(String v) { this.title = v; return this; }
        public Builder subtitle(String v) { this.subtitle = v; return this; }
        public Builder sound(String v) { this.sound = v; return this; }
        public Wave build() { return new Wave(this); }
    }
}
