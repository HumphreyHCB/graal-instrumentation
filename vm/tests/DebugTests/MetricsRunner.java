package DebugTests;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.regex.*;

/**
 * Runs the benchmark twice (debug flags **ON / OFF**) and compares **only the
 * *last* aggregated CSV written in each run directory**, regardless of the
 * isolate-id contained in its `@N` suffix. "Last" is defined as the file
 * with the numerically highest isolate id (aggregated@N.csv, or plain
 * aggregated.csv when N = 0).
 *
 * Uses OFF as the base and ON as the measure.  The diff table now shows
 * clearly labeled OFF / ON columns and reports both total and median %Δ.
 */
public final class MetricsRunner {

    private static final String JAVA =
        System.getenv().getOrDefault("GRAALVM_HOME",
            "/home/hb478/repos/graal-instrumentation/vm/latest_graalvm_home") + "/bin/java";

    private static final List<String> COMMON = List.of(
        "-XX:+UnlockExperimentalVMOptions", "-XX:+UnlockDiagnosticVMOptions",
        "-XX:+EnableJVMCI", "-XX:+UseJVMCICompiler",
        "-XX:-TieredCompilation", "-XX:-BackgroundCompilation",
        "-Djdk.graal.CompilationFailureAction=Diagnose",
        "-Djdk.graal.StrictProfiles=false",
        "-Djdk.graal.LoadProfiles=/home/hb478/repos/GTSlowdownSchedular/FinalDataRefined100/List/List_CompilerReplay"
    );

    private static final String CLASSPATH =
        "/home/hb478/repos/are-we-fast-yet/benchmarks/Java/benchmarks.jar";

    private static final String[] HARNESS = {"Harness", "List", "500", "10000"};

    public static void main(String... args) throws Exception {
        // Run OFF first (base), then ON
        Path offDir = run(false);
        Path onDir  = run(true);

        // pick the last produced aggregate CSV in each
        Path offAgg = latestAggregated(offDir);
        Path onAgg  = latestAggregated(onDir);

        System.out.printf("\nComparing last aggregates: OFF %s  ->  ON %s%n", 
                          offAgg.getFileName(), onAgg.getFileName());

        Map<String,Long> off = readCsv(offAgg);
        Map<String,Long> on  = readCsv(onAgg);
        printDiff(off, on);
    }

    // ──────────────────────────── launch child JVM ────────────────────────────
    private static Path run(boolean debugInfo) throws IOException, InterruptedException {
        String mode = debugInfo ? "on" : "off";
        Path out = Paths.get("vm/tests/DebugTests/Runs/run_" +
                             LocalDateTime.now().toString().replace(':', '_') + "_" + mode);
        Files.createDirectories(out);

        List<String> cmd = new ArrayList<>();
        cmd.add(JAVA);
        cmd.addAll(COMMON);
        cmd.add("-Djdk.graal.Timers=");
        cmd.add("-Djdk.graal.Counters=");
        //cmd.add("-Djdk.graal.MemUseTrackers=");   // enable if desired
        cmd.add("-Djdk.graal.MetricsFile=" + out.resolve("metrics.csv").toAbsolutePath());
        cmd.add("-Djdk.graal.AggregatedMetricsFile=" + out.resolve("aggregated.csv").toAbsolutePath());

        cmd.add(debugInfo ? "-Djdk.graal.LIRAdditionalCompilerDebugInformation=true"
                          : "-Djdk.graal.LIRAdditionalCompilerDebugInformation=false");

        cmd.add("-cp"); cmd.add(CLASSPATH); cmd.addAll(List.of(HARNESS));

        Files.writeString(out.resolve("cmdline.txt"), String.join(" ", cmd));

        ProcessBuilder pb = new ProcessBuilder(cmd)
                .directory(out.toFile())
                .redirectOutput(out.resolve("stdout.log").toFile())
                .redirectError(out.resolve("stderr.log").toFile());

        System.out.printf("➜  [%s] running …%n", mode);
        if (pb.start().waitFor() != 0)
            throw new IllegalStateException("run " + mode + " failed – see " + out);

        if (latestAggregated(out) == null)
            throw new IOException("No aggregated CSV produced in " + out);

        System.out.printf("✓ [%s] finished → %s%n", mode, out);
        return out;
    }

    // ─────────────────────── pick highest-id aggregated file ───────────────────
    private static Path latestAggregated(Path dir) throws IOException {
        Pattern p = Pattern.compile("aggregated(?:@(?<id>\\d+))?\\.csv");
        int best = -1;
        Path bestPath = null;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "aggregated*.csv")) {
            for (Path f : ds) {
                Matcher m = p.matcher(f.getFileName().toString());
                if (m.matches()) {
                    int id = m.group("id") == null ? 0 : Integer.parseInt(m.group("id"));
                    if (id > best) { best = id; bestPath = f; }
                }
            }
        }
        return bestPath;
    }

    // ───────────────────────────── CSV reader ────────────────────────────────
    private static Map<String,Long> readCsv(Path csv) throws IOException {
        Map<String,Long> map = new HashMap<>();
        try (BufferedReader r = Files.newBufferedReader(csv)) {
            for (String ln; (ln = r.readLine()) != null; ) {
                String[] p = ln.split(";");
                if (p.length >= 2)
                    map.put(p[0].trim(), Long.parseLong(p[1].trim()));
            }
        }
        return map;
    }

    // ───────────────────────────── diff printer ───────────────────────────────
    private static void printDiff(Map<String,Long> off, Map<String,Long> on) {
        // Header labels
        System.out.printf("\n%-45s %12s %12s %12s %10s%n", "Metric", "OFF", "ON", "Δ", "%Δ");
        System.out.println(String.join("", Collections.nCopies(85, "-")));

        double sumOff = 0, sumOn = 0;
        List<Double> pctList = new ArrayList<>();

        for (String k : new TreeSet<>(on.keySet())) {
            long vOff = off.getOrDefault(k, 0L);
            long vOn  = on .getOrDefault(k, 0L);
            long d    = vOn - vOff;
            if (d != 0) {
                double pct = vOff == 0 ? 100.0 : (d * 100.0) / vOff;
                pctList.add(pct);
                System.out.printf("%-45s %12d %12d %12d %10.2f%%%n", k, vOff, vOn, d, pct);
            }
            sumOff += vOff;
            sumOn  += vOn;
        }
        double totPct = sumOff == 0 ? 100.0 : ((sumOn - sumOff) * 100.0) / sumOff;
        System.out.printf("%-45s %12.0f %12.0f %12.0f %10.2f%%%n", "TOTAL", sumOff, sumOn, sumOn - sumOff, totPct);

        // Median percentage change
        if (!pctList.isEmpty()) {
            Collections.sort(pctList);
            double medianPct;
            int n = pctList.size();
            if (n % 2 == 0) {
                medianPct = (pctList.get(n/2 - 1) + pctList.get(n/2)) / 2.0;
            } else {
                medianPct = pctList.get(n/2);
            }
            System.out.printf(
            "%-45s %12s %12s %12s %10.2f%%%n",
            "MEDIAN%Δ", "", "", "", medianPct
            );

        }
    }
}
