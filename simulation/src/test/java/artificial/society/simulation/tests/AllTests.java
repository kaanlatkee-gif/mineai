package artificial.society.simulation.tests;

import java.util.List;

/**
 * Test runner: executes every simulation test and reports failures.
 *
 * <p>Used both by {@code gradle :simulation:build} (JavaExec task
 * {@code simulationTest}) and by {@code scripts/offline-verify.sh}.</p>
 *
 * <p>Failure signalling is done by throwing (rather than {@code System.exit})
 * so the suite also runs on restricted JVM subsets - an uncaught error always
 * produces a non-zero process exit code.</p>
 */
public final class AllTests {

    private AllTests() {
    }

    public static void main(String[] args) {
        List<SimTest> tests = List.of(
                new BrainDecisionTest(),
                new NeedsTest(),
                new PersonalityTest(),
                new RelationshipTest(),
                new MemoryTest(),
                new PersistenceRoundTripTest(),
                new DecisionVariationTest());

        int passed = 0;
        int failed = 0;
        for (SimTest test : tests) {
            long start = System.nanoTime();
            try {
                test.run();
                long ms = (System.nanoTime() - start) / 1_000_000L;
                System.out.println("  PASS  " + test.name() + " (" + ms + " ms)");
                passed++;
            } catch (Throwable t) {
                System.out.println("  FAIL  " + test.name());
                System.out.println("        " + t);
                t.printStackTrace();
                failed++;
            }
        }

        System.out.println();
        System.out.println("Simulation tests: " + passed + " passed, " + failed + " failed, "
                + tests.size() + " total");

        if (failed > 0) {
            throw new IllegalStateException(failed + " simulation test(s) failed");
        }
    }
}
