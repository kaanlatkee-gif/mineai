package artificial.society.simulation.needs;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Mutable container for the seven need values of one NPC.
 *
 * <p>All values are clamped to {@code [0, 100]}. Decay and contextual
 * adjustments are applied by {@code NpcBrain#advanceTicks}, which supplies the
 * personality multipliers and the current {@code WorldSnapshot}; this class
 * itself stays free of any environmental knowledge.</p>
 */
public final class Needs {

    private final Map<NeedType, Double> values = new EnumMap<>(NeedType.class);

    public Needs() {
        for (NeedType type : NeedType.values()) {
            values.put(type, type.initialSatisfaction());
        }
    }

    /** Creates a needs container from raw, possibly persisted values (validated/clamped). */
    public static Needs fromArray(double[] raw) {
        Needs needs = new Needs();
        needs.setAll(raw);
        return needs;
    }

    /** Overwrites all values from a raw array (validated/clamped, missing entries keep defaults). */
    public void setAll(double[] raw) {
        if (raw == null) {
            return;
        }
        NeedType[] types = NeedType.values();
        for (int i = 0; i < types.length && i < raw.length; i++) {
            values.put(types[i], clamp(raw[i]));
        }
    }

    public double get(NeedType type) {
        return values.get(type);
    }

    public void set(NeedType type, double value) {
        values.put(type, clamp(value));
    }

    /** Raises satisfaction, e.g. after eating or resting. */
    public void satisfy(NeedType type, double amount) {
        set(type, get(type) + Math.max(0.0, amount));
    }

    /** Lowers satisfaction, e.g. while performing exhausting work. */
    public void drain(NeedType type, double amount) {
        set(type, get(type) - Math.max(0.0, amount));
    }

    /** The most unsatisfied need — useful for debugging output. */
    public NeedType mostUrgent() {
        NeedType worst = NeedType.HUNGER;
        double worstValue = Double.MAX_VALUE;
        for (NeedType type : NeedType.values()) {
            double v = get(type);
            if (v < worstValue) {
                worstValue = v;
                worst = type;
            }
        }
        return worst;
    }

    /** Exports values in {@link NeedType#values()} order for persistence. */
    public double[] toArray() {
        double[] out = new double[NeedType.values().length];
        NeedType[] types = NeedType.values();
        for (int i = 0; i < types.length; i++) {
            out[i] = get(types[i]);
        }
        return out;
    }

    public Needs copy() {
        return fromArray(toArray());
    }

    private static double clamp(double v) {
        if (Double.isNaN(v)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(100.0, v));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Needs other)) {
            return false;
        }
        for (NeedType type : NeedType.values()) {
            if (Math.abs(get(type) - other.get(type)) > 1.0e-9) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(java.util.Arrays.hashCode(toArray()));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Needs{");
        boolean first = true;
        for (NeedType type : NeedType.values()) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            sb.append(type.name()).append('=').append(String.format("%.0f", get(type)));
        }
        return sb.append('}').toString();
    }
}
