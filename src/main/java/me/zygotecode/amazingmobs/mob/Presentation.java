package me.zygotecode.amazingmobs.mob;

/**
 * Visual / audible presentation. Pure data; the runtime applies glow, boss bar and ambient FX.
 * Strings (colours, particle/sound names) are resolved at runtime so they stay configurable.
 */
public final class Presentation {

    public static final Presentation DEFAULT = builder().build();

    private final boolean glow;
    private final String glowColor;        // NamedTextColor name, nullable => no team colour
    private final boolean nameVisible;
    private final boolean bossBar;
    private final String bossBarColor;     // BarColor name
    private final String bossBarTitle;     // MiniMessage; null => use display name
    private final String ambientParticle;  // particle name, nullable
    private final String ambientSound;     // sound name, nullable

    private Presentation(Builder b) {
        this.glow = b.glow;
        this.glowColor = b.glowColor;
        this.nameVisible = b.nameVisible;
        this.bossBar = b.bossBar;
        this.bossBarColor = b.bossBarColor;
        this.bossBarTitle = b.bossBarTitle;
        this.ambientParticle = b.ambientParticle;
        this.ambientSound = b.ambientSound;
    }

    public boolean glow() { return glow; }
    public String glowColor() { return glowColor; }
    public boolean nameVisible() { return nameVisible; }
    public boolean bossBar() { return bossBar; }
    public String bossBarColor() { return bossBarColor; }
    public String bossBarTitle() { return bossBarTitle; }
    public String ambientParticle() { return ambientParticle; }
    public String ambientSound() { return ambientSound; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private boolean glow = false;
        private String glowColor = null;
        private boolean nameVisible = true;
        private boolean bossBar = false;
        private String bossBarColor = "RED";
        private String bossBarTitle = null;
        private String ambientParticle = null;
        private String ambientSound = null;

        public Builder glow(boolean v) { this.glow = v; return this; }
        public Builder glowColor(String v) { this.glowColor = v; return this; }
        public Builder nameVisible(boolean v) { this.nameVisible = v; return this; }
        public Builder bossBar(boolean v) { this.bossBar = v; return this; }
        public Builder bossBarColor(String v) { this.bossBarColor = v; return this; }
        public Builder bossBarTitle(String v) { this.bossBarTitle = v; return this; }
        public Builder ambientParticle(String v) { this.ambientParticle = v; return this; }
        public Builder ambientSound(String v) { this.ambientSound = v; return this; }
        public Presentation build() { return new Presentation(this); }
    }
}
