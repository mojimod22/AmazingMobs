package me.zygotecode.amazingmobs.clazz;

/**
 * The four macro categories of player skill. {@code BASE} is the universal fallback (the AK-47),
 * {@code ACTIVE} are frequent low/medium-cooldown tools, {@code SPECIAL} are impactful mid-cooldown
 * plays, and {@code HYPER} are the spectacular, long-cooldown near-ultimates.
 */
public enum SkillType {
    BASE("<gray>", "Base", "Right-click the AK-47"),
    ACTIVE("<aqua>", "Active", "Swap-hand key (F)"),
    SPECIAL("<light_purple>", "Special", "Sneak + Swap-hand (Shift+F)"),
    HYPER("<gold>", "Hyper", "Sneak + Drop (Shift+Q)");

    private final String color;   // MiniMessage colour tag
    private final String label;
    private final String trigger; // how the player activates it

    SkillType(String color, String label, String trigger) {
        this.color = color;
        this.label = label;
        this.trigger = trigger;
    }

    public String color() { return color; }
    public String label() { return label; }
    public String trigger() { return trigger; }
}
