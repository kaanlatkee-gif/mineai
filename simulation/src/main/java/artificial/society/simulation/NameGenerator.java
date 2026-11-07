package artificial.society.simulation;

import java.util.List;
import java.util.Random;

/**
 * Generates plausible citizen names. Pure data — no Minecraft resources — so
 * names can be created (and re-created after restarts, when the identity seed
 * is known) inside unit tests.
 */
public final class NameGenerator {

    private static final List<String> FIRST_NAMES = List.of(
            "Ada", "Bram", "Cora", "Doran", "Elin", "Finn", "Greta", "Hugo",
            "Iris", "Jonas", "Kira", "Lars", "Mira", "Nils", "Oona", "Piet",
            "Quinn", "Rhea", "Soren", "Tilda", "Ulf", "Vera", "Wim", "Xenia",
            "Yara", "Zeno", "Anke", "Bela", "Caius", "Dara");

    private static final List<String> LAST_NAMES = List.of(
            "Ashford", "Berg", "Copper", "Dunhill", "Elm", "Fisher", "Grove",
            "Hale", "Ivory", "Juniper", "Kestrel", "Larkspur", "Moss", "North",
            "Oakhaven", "Pike", "Quarry", "Rook", "Stone", "Thatcher", "Umber",
            "Vale", "Willow", "Yarrow", "Zeller");

    private NameGenerator() {
    }

    public static String generate(Random random) {
        String first = FIRST_NAMES.get(random.nextInt(FIRST_NAMES.size()));
        String last = LAST_NAMES.get(random.nextInt(LAST_NAMES.size()));
        return first + " " + last;
    }

    /** Deterministic short id such as {@code npc-0042}. */
    public static String idFor(int number) {
        return String.format("npc-%04d", number);
    }
}
