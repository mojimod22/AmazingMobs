package me.zygotecode.amazingmobs.config.validation;

/**
 * A single validation finding tied to a config path.
 *
 * @param level   severity
 * @param path    dotted config path the issue concerns (e.g. {@code stats.health}); may be empty
 * @param message human-readable explanation, ideally stating what was done about it
 */
public record Issue(IssueLevel level, String path, String message) {

    @Override
    public String toString() {
        String loc = (path == null || path.isEmpty()) ? "" : (" @ " + path);
        return "[" + level + "]" + loc + " " + message;
    }
}
