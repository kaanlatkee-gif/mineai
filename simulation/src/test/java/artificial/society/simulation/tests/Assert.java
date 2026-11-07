package artificial.society.simulation.tests;

import java.util.Arrays;

/**
 * Minimal assertion helpers for the plain-Java test suite.
 *
 * <p>The suite deliberately has no external test-framework dependency so it
 * can run in fully offline environments (see {@code scripts/offline-verify.sh});
 * {@code gradle :simulation:build} executes the same classes through
 * {@link AllTests}.</p>
 */
public final class Assert {

    private Assert() {
    }

    public static void isTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void isFalse(boolean condition, String message) {
        if (condition) {
            throw new AssertionError(message);
        }
    }

    public static void notNull(Object value, String message) {
        if (value == null) {
            throw new AssertionError(message + " (was null)");
        }
    }

    public static void equals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + " (expected " + expected + ", got " + actual + ")");
        }
    }

    public static void equals(double expected, double actual, double tolerance, String message) {
        if (Math.abs(expected - actual) > tolerance) {
            throw new AssertionError(message + " (expected " + expected + " ± " + tolerance + ", got " + actual + ")");
        }
    }

    public static void arrayEquals(double[] expected, double[] actual, double tolerance, String message) {
        if (expected.length != actual.length) {
            throw new AssertionError(message + " (length " + expected.length + " vs " + actual.length + ")");
        }
        for (int i = 0; i < expected.length; i++) {
            if (Math.abs(expected[i] - actual[i]) > tolerance) {
                throw new AssertionError(message + " (index " + i + ": expected " + expected[i]
                        + ", got " + actual[i] + "; arrays=" + Arrays.toString(actual) + ")");
            }
        }
    }

    public static void fail(String message) {
        throw new AssertionError(message);
    }
}
