package me.zygotecode.amazingmobs.config.validation;

/**
 * Severity of a configuration {@link Issue}. Ordinal order is significant: ERROR &gt; WARN &gt; INFO.
 *
 * <ul>
 *   <li>{@link #INFO} — informational note (e.g. an accepted deprecated alias).</li>
 *   <li>{@link #WARN} — loaded, but a value was missing/invalid and was defaulted or corrected.</li>
 *   <li>{@link #ERROR} — the definition cannot load and is rejected.</li>
 * </ul>
 */
public enum IssueLevel {
    INFO, WARN, ERROR
}
