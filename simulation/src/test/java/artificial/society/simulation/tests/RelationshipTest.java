package artificial.society.simulation.tests;

import artificial.society.simulation.social.Relationship;
import artificial.society.simulation.social.RelationshipManager;
import artificial.society.simulation.social.SocialInteractionType;

/**
 * Relationships are directional, develop only through interactions, and fade
 * through isolation (spec §2.D and §4.3).
 */
public final class RelationshipTest implements SimTest {

    @Override
    public String name() {
        return "RelationshipTest - directional growth, hostility, isolation decay";
    }

    @Override
    public void run() {
        RelationshipManager aView = new RelationshipManager();
        RelationshipManager bView = new RelationshipManager();

        // Baseline: neutral relationship created on first contact.
        Relationship initial = aView.getOrCreate("npc-b", 100L);
        Assert.equals(10.0, initial.trust(), 1.0e-9, "relationships start neutral");

        // Helping raises trust and friendship for the helper's view of B...
        aView.applyInteraction("npc-b", SocialInteractionType.HELPING, 10.0, true,
                0.8, 0.6, 0.2, 200L);
        Relationship afterHelp = aView.get("npc-b");
        Assert.isTrue(afterHelp.trust() > initial.trust(), "helping must raise trust");
        Assert.isTrue(afterHelp.friendship() > initial.friendship(), "helping must raise friendship");
        Assert.isTrue(afterHelp.familiarity() > 0.0, "interactions raise familiarity");

        // ...but B's view of A starts untouched: relationships are directional.
        Assert.isTrue(!bView.knows("npc-a"),
                "B's store must not change when A decides to help B");
        Assert.equals(10.0, bView.get("npc-a").trust(), 1.0e-9, "B still neutral towards A");

        // Hostility: disagreement erodes trust and stokes rivalry.
        double trustBefore = aView.get("npc-b").trust();
        double rivalryBefore = aView.get("npc-b").rivalry();
        aView.applyInteraction("npc-b", SocialInteractionType.DISAGREEMENT, 10.0, false,
                0.2, 0.5, 0.7, 300L);
        Relationship afterFight = aView.get("npc-b");
        Assert.isTrue(afterFight.trust() < trustBefore, "disagreement must lower trust");
        Assert.isTrue(afterFight.rivalry() > rivalryBefore, "disagreement must raise rivalry");

        // Isolation: long idle periods erode familiarity and friendship.
        RelationshipManager lonely = new RelationshipManager();
        lonely.applyInteraction("npc-x", SocialInteractionType.CONVERSATION, 5.0, true,
                0.5, 0.5, 0.5, 0L);
        double familiarityBefore = lonely.get("npc-x").familiarity();
        double friendshipBefore = lonely.get("npc-x").friendship();
        lonely.decay(24_000L * 10L); // ten in-game days without contact
        Relationship afterIsolation = lonely.get("npc-x");
        Assert.isTrue(afterIsolation.familiarity() < familiarityBefore,
                "familiarity must fade through isolation");
        Assert.isTrue(afterIsolation.friendship() < friendshipBefore,
                "friendship must weaken through prolonged isolation");

        // Trustfulness personality parameter dampens trust gains from helping.
        RelationshipManager cautious = new RelationshipManager();
        cautious.applyInteraction("npc-y", SocialInteractionType.HELPING, 10.0, true,
                0.0, 0.5, 0.5, 100L);
        RelationshipManager trusting = new RelationshipManager();
        trusting.applyInteraction("npc-y", SocialInteractionType.HELPING, 10.0, true,
                1.0, 0.5, 0.5, 100L);
        Assert.isTrue(trusting.get("npc-y").trust() > cautious.get("npc-y").trust(),
                "trustful NPCs gain more trust from the same act");
    }
}
