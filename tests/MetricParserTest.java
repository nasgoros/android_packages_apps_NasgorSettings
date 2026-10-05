import com.nasgoros.settings.performance.MetricParser;

public final class MetricParserTest {
    private static void check(boolean value, String name) {
        if (!value) throw new AssertionError(name);
    }
    public static void main(String[] args) {
        check(MetricParser.cpuCount("0-7") == 8, "eight cores");
        check(MetricParser.cpuCount("0-3,6,8-9") == 7, "sparse cores");
        check(MetricParser.cpuCount("0") == 1, "single core");
        for (String bad : new String[]{"", "oops", "-1", "4-0", "0-3,2", "0-999999"})
            check(MetricParser.cpuCount(bad) == -1, "bad cpulist: " + bad);
        long[] a = MetricParser.cpuTicks("cpu  100 20 30 400 10 5 5 0 25 10");
        check(a[0] == 570 && a[1] == 410, "guest ticks not counted twice");
        long[] b = MetricParser.cpuTicks("cpu 150 20 30 450 10 5 5 0 25 10");
        check(MetricParser.cpuPercent(a,b) == 50, "50 percent CPU");
        check(Double.isNaN(MetricParser.cpuPercent(null,b)), "first reading unavailable");
        check(Double.isNaN(MetricParser.cpuPercent(a,a)), "no elapsed ticks");
        check(Double.isNaN(MetricParser.cpuPercent(b,a)), "counter reset");
        check(MetricParser.cpuTicks("cpu0 1 2 3 4") == null, "reject per-core line");
        check(MetricParser.cpuTicks("cpu 1 2 X 4") == null, "malformed counters");
        check(MetricParser.cpuTicks("cpu 1 2 3 -4") == null, "negative counters");
        check(MetricParser.cpuTicks(null) == null, "permission denied fallback");
        System.out.println("PASS: CPU list, guest accounting, deltas, reset and missing-data cases");
    }
}
