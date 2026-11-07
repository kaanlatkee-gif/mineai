package artificial.society.simulation.tests;

/** A single simulation test case executed by {@link AllTests}. */
public interface SimTest {

    /** Human readable test name. */
    String name();

    /** Runs the test; throws {@link AssertionError} (or any exception) on failure. */
    void run() throws Exception;
}
