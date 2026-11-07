package artificial.society.simulation.personality;

/**
 * The ten stable personality traits generated for every NPC at creation.
 *
 * <p>Traits are numbers in {@code [0, 1]}. They bias decisions without
 * determining them, and they drift only slowly through lived experience.</p>
 */
public enum TraitType {
    KINDNESS("Kindness"),
    AGGRESSION("Aggression"),
    CURIOSITY("Curiosity"),
    BRAVERY("Bravery"),
    SOCIABILITY("Sociability"),
    PATIENCE("Patience"),
    GENEROSITY("Generosity"),
    AMBITION("Ambition"),
    INDEPENDENCE("Independence"),
    TRUSTFULNESS("Trustfulness");

    private final String displayName;

    TraitType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
