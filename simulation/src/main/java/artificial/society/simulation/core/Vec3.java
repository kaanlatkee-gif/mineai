package artificial.society.simulation.core;

/**
 * Plain immutable 3D vector used everywhere inside the simulation engine.
 *
 * <p>The simulation never touches Minecraft's {@code Vec3d}; world data is
 * converted to this type by the Minecraft adapter layer before it reaches a
 * brain. Keeping a tiny value type here is what allows every simulation class
 * to be unit-tested on a bare JVM.</p>
 */
public record Vec3(double x, double y, double z) {

    public static final Vec3 ZERO = new Vec3(0.0, 0.0, 0.0);

    public double distanceTo(Vec3 other) {
        double dx = x - other.x;
        double dy = y - other.y;
        double dz = z - other.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public double horizontalDistanceTo(Vec3 other) {
        double dx = x - other.x;
        double dz = z - other.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    public Vec3 add(double dx, double dy, double dz) {
        return new Vec3(x + dx, y + dy, z + dz);
    }

    /** Direction from this position towards {@code other}, normalised to {@code length}. */
    public Vec3 towards(Vec3 other, double length) {
        double dx = other.x - x;
        double dy = other.y - y;
        double dz = other.z - z;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist < 1.0e-6) {
            return new Vec3(0.0, 0.0, 0.0);
        }
        double scale = length / dist;
        return new Vec3(dx * scale, dy * scale, dz * scale);
    }

    @Override
    public String toString() {
        return String.format("(%.1f, %.1f, %.1f)", x, y, z);
    }
}
