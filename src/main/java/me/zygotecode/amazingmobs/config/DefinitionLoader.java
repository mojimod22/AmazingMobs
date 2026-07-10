package me.zygotecode.amazingmobs.config;

import me.zygotecode.amazingmobs.config.validation.ValidationReport;
import me.zygotecode.amazingmobs.horde.HordeDefinition;
import me.zygotecode.amazingmobs.horde.HordeParser;
import me.zygotecode.amazingmobs.horde.HordeRegistry;
import me.zygotecode.amazingmobs.mob.MobDefinition;
import me.zygotecode.amazingmobs.mob.MobParser;
import me.zygotecode.amazingmobs.mob.MobRegistry;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Loads folders of {@code .yml} definitions into fresh registries, collecting a per-file
 * {@link ValidationReport}. A file with ERRORs is rejected (not registered) but never aborts the
 * batch — every other file still loads. Reload builds new registries and only swaps them in if the
 * batch is usable, so a corrupt edit can never wipe a working set.
 */
public final class DefinitionLoader {

    private DefinitionLoader() {}

    public static LoadResult loadMobs(File dir, MobRegistry target, Set<String> knownSkillIds, Set<String> knownTraitIds) {
        List<LoadResult.FileResult> results = new ArrayList<>();
        for (File f : ymlFiles(dir)) {
            String base = f.getName();
            String fileId = stripExt(base);
            ValidationReport report = new ValidationReport(base);
            ConfigSection root = ConfigSource.load(f, report);
            Optional<MobDefinition> parsed = report.hasErrors()
                    ? Optional.empty() : MobParser.parse(fileId, root, knownSkillIds, knownTraitIds, report);
            boolean ok = parsed.isPresent() && !report.hasErrors();
            String id = ok ? parsed.get().id() : fileId;
            if (ok) target.register(parsed.get());
            results.add(new LoadResult.FileResult(base, id, ok, report));
        }
        return new LoadResult(results);
    }

    public static LoadResult loadHordes(File dir, HordeRegistry target, Set<String> knownMobIds) {
        List<LoadResult.FileResult> results = new ArrayList<>();
        for (File f : ymlFiles(dir)) {
            String base = f.getName();
            String fileId = stripExt(base);
            ValidationReport report = new ValidationReport(base);
            ConfigSection root = ConfigSource.load(f, report);
            Optional<HordeDefinition> parsed = report.hasErrors()
                    ? Optional.empty() : HordeParser.parse(fileId, root, knownMobIds, report);
            boolean ok = parsed.isPresent() && !report.hasErrors();
            String id = ok ? parsed.get().id() : fileId;
            if (ok) target.register(parsed.get());
            results.add(new LoadResult.FileResult(base, id, ok, report));
        }
        return new LoadResult(results);
    }

    private static List<File> ymlFiles(File dir) {
        if (dir == null || !dir.isDirectory()) return List.of();
        File[] arr = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".yml"));
        if (arr == null) return List.of();
        List<File> files = new ArrayList<>(Arrays.asList(arr));
        files.sort(Comparator.comparing(File::getName));
        return files;
    }

    private static String stripExt(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}
