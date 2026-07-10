package me.zygotecode.amazingmobs.config;

import me.zygotecode.amazingmobs.config.validation.IssueLevel;
import me.zygotecode.amazingmobs.config.validation.ValidationReport;

import java.util.List;

/** Aggregated outcome of loading a folder of definitions, with per-file validation reports. */
public record LoadResult(List<FileResult> files) {

    /** One file's outcome. */
    public record FileResult(String fileName, String id, boolean loaded, ValidationReport report) {}

    public int total() { return files.size(); }

    public long loaded() { return files.stream().filter(FileResult::loaded).count(); }

    public long rejected() { return files.stream().filter(f -> !f.loaded()).count(); }

    public long withWarnings() {
        return files.stream().filter(f -> f.report().count(IssueLevel.WARN) > 0).count();
    }

    public long totalWarnings() {
        return files.stream().mapToLong(f -> f.report().count(IssueLevel.WARN)).sum();
    }
}
