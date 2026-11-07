package artificial.society.simulation.perception;

import artificial.society.simulation.core.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * An immutable, Minecraft-free picture of the world around one NPC at one
 * moment in time.
 *
 * <p>This is the <b>only</b> environmental input a brain ever receives. The
 * Minecraft module builds snapshots by scanning the real world; tests build
 * them by hand. Neither a brain nor any of its helpers can tell the
 * difference — which is exactly what makes the simulation testable without
 * launching Minecraft.</p>
 */
public final class WorldSnapshot {

    /** Minecraft day length in ticks. */
    public static final long DAY_LENGTH = 24_000L;

    private final long tick;
    private final long timeOfDayTicks;
    private final int lightLevel;
    private final Vec3 selfPosition;
    private final Vec3 homePosition;
    private final List<PerceivedAgent> agents;
    private final List<PerceivedItem> items;
    private final List<PerceivedThreat> threats;

    private WorldSnapshot(Builder b) {
        this.tick = b.tick;
        this.timeOfDayTicks = b.timeOfDayTicks;
        this.lightLevel = b.lightLevel;
        this.selfPosition = b.selfPosition;
        this.homePosition = b.homePosition;
        this.agents = List.copyOf(b.agents);
        this.items = List.copyOf(b.items);
        this.threats = List.copyOf(b.threats);
    }

    public static Builder builder() {
        return new Builder();
    }

    public long tick() {
        return tick;
    }

    public long timeOfDayTicks() {
        return timeOfDayTicks;
    }

    public int lightLevel() {
        return lightLevel;
    }

    public Vec3 selfPosition() {
        return selfPosition;
    }

    public Vec3 homePosition() {
        return homePosition;
    }

    public List<PerceivedAgent> agents() {
        return agents;
    }

    public List<PerceivedItem> items() {
        return items;
    }

    public List<PerceivedThreat> threats() {
        return threats;
    }

    public boolean isNight() {
        long t = timeOfDayTicks % DAY_LENGTH;
        return t >= 13_000L && t < 23_000L;
    }

    public boolean hasThreats() {
        return !threats.isEmpty();
    }

    public Optional<PerceivedThreat> nearestThreat() {
        return threats.stream().min((a, b) -> Double.compare(a.distance(), b.distance()));
    }

    public Optional<PerceivedItem> nearestFood() {
        return items.stream().min((a, b) -> Double.compare(a.distance(), b.distance()));
    }

    public List<PerceivedItem> foodWithin(double radius) {
        List<PerceivedItem> out = new ArrayList<>();
        for (PerceivedItem item : items) {
            if (item.distance() <= radius) {
                out.add(item);
            }
        }
        return Collections.unmodifiableList(out);
    }

    public Optional<PerceivedAgent> nearestAgent() {
        return agents.stream().min((a, b) -> Double.compare(a.distance(), b.distance()));
    }

    /** Distance from the snapshot's self position back home. */
    public double distanceFromHome() {
        return selfPosition.horizontalDistanceTo(homePosition);
    }

    public static final class Builder {
        private long tick;
        private long timeOfDayTicks;
        private int lightLevel = 15;
        private Vec3 selfPosition = Vec3.ZERO;
        private Vec3 homePosition = Vec3.ZERO;
        private final List<PerceivedAgent> agents = new ArrayList<>();
        private final List<PerceivedItem> items = new ArrayList<>();
        private final List<PerceivedThreat> threats = new ArrayList<>();

        public Builder tick(long tick) {
            this.tick = tick;
            return this;
        }

        public Builder timeOfDayTicks(long timeOfDayTicks) {
            this.timeOfDayTicks = timeOfDayTicks;
            return this;
        }

        public Builder lightLevel(int lightLevel) {
            this.lightLevel = lightLevel;
            return this;
        }

        public Builder selfPosition(Vec3 pos) {
            this.selfPosition = pos;
            return this;
        }

        public Builder homePosition(Vec3 pos) {
            this.homePosition = pos;
            return this;
        }

        public Builder agent(PerceivedAgent agent) {
            this.agents.add(agent);
            return this;
        }

        public Builder item(PerceivedItem item) {
            this.items.add(item);
            return this;
        }

        public Builder threat(PerceivedThreat threat) {
            this.threats.add(threat);
            return this;
        }

        public WorldSnapshot build() {
            return new WorldSnapshot(this);
        }
    }
}
