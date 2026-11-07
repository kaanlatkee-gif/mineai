package artificial.society.simulation.personality;

import java.util.EnumMap;
import java.util.Map;

/**
 * Immutable personality of one NPC: a value in {@code [0, 1]} for each
 * {@link TraitType}. Two NPCs with identical needs make different decisions
 * because these numbers bias every utility score differently.
 */
public final class Personality {

    private final Map<TraitType, Double> traits = new EnumMap<>(TraitType.class);

    private Personality(Map<TraitType, Double> source) {
        for (TraitType type : TraitType.values()) {
            double v = source.getOrDefault(type, 0.5);
            traits.put(type, clamp(v));
        }
    }

    /** Builds a personality from raw (e.g. persisted) values; missing entries fall back to 0.5. */
    public static Personality fromArray(double[] raw) {
        Map<TraitType, Double> map = new EnumMap<>(TraitType.class);
        TraitType[] types = TraitType.values();
        for (int i = 0; i < types.length; i++) {
            map.put(types[i], i < raw.length ? raw[i] : 0.5);
        }
        return new Personality(map);
    }

    public static Personality of(Map<TraitType, Double> values) {
        return new Personality(values);
    }

    public double get(TraitType type) {
        return traits.get(type);
    }

    /** Exports values in {@link TraitType#values()} order for persistence. */
    public double[] toArray() {
        double[] out = new double[TraitType.values().length];
        TraitType[] types = TraitType.values();
        for (int i = 0; i < types.length; i++) {
            out[i] = get(types[i]);
        }
        return out;
    }

    private static double clamp(double v) {
        if (Double.isNaN(v)) {
            return 0.5;
        }
        return Math.max(0.0, Math.min(1.0, v));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Personality{");
        boolean first = true;
        for (TraitType type : TraitType.values()) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            sb.append(type.name()).append('=').append(String.format("%.2f", get(type)));
        }
        return sb.append('}').toString();
    }
}
