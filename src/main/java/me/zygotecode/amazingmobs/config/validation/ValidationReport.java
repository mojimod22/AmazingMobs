package me.zygotecode.amazingmobs.config.validation;

import java.util.ArrayList;
import java.util.List;

/**
 * Accumulates {@link Issue}s produced while parsing one source (a file, a section, or an
 * in-game session). Pure: no Bukkit, so parsers + their reports are fully unit-testable.
 */
public final class ValidationReport {

    private final String source;
    private final List<Issue> issues = new ArrayList<>();

    public ValidationReport(String source) {
        this.source = source == null ? "?" : source;
    }

    public String source() {
        return source;
    }

    public void add(IssueLevel level, String path, String message) {
        issues.add(new Issue(level, path == null ? "" : path, message));
    }

    public void error(String path, String message) { add(IssueLevel.ERROR, path, message); }
    public void warn(String path, String message)  { add(IssueLevel.WARN, path, message); }
    public void info(String path, String message)  { add(IssueLevel.INFO, path, message); }

    public List<Issue> issues() {
        return List.copyOf(issues);
    }

    public boolean hasErrors() {
        return count(IssueLevel.ERROR) > 0;
    }

    public boolean isClean() {
        return issues.isEmpty();
    }

    public int count(IssueLevel level) {
        int n = 0;
        for (Issue i : issues) if (i.level() == level) n++;
        return n;
    }

    /** Highest severity present, or {@code null} if clean. */
    public IssueLevel worst() {
        IssueLevel worst = null;
        for (Issue i : issues) {
            if (worst == null || i.level().ordinal() > worst.ordinal()) worst = i.level();
        }
        return worst;
    }

    /** Merge another report's issues into this one, prefixing their paths. */
    public void merge(ValidationReport other, String pathPrefix) {
        if (other == null) return;
        String pre = (pathPrefix == null || pathPrefix.isEmpty()) ? "" : pathPrefix + ".";
        for (Issue i : other.issues) {
            String p = i.path().isEmpty() ? pathPrefix == null ? "" : pathPrefix : pre + i.path();
            issues.add(new Issue(i.level(), p, i.message()));
        }
    }

    /** One-line summary, e.g. {@code "phantom.yml: 0 errors, 2 warnings, 0 info"}. */
    public String summaryLine() {
        return source + ": " + count(IssueLevel.ERROR) + " errors, "
                + count(IssueLevel.WARN) + " warnings, " + count(IssueLevel.INFO) + " info";
    }

    public List<String> lines() {
        List<String> out = new ArrayList<>();
        for (Issue i : issues) out.add(i.toString());
        return out;
    }
}
