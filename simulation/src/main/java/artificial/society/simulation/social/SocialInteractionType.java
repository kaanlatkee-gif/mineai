package artificial.society.simulation.social;

/**
 * The nine kinds of social interaction two NPCs can have. Every interaction
 * produces relationship deltas and at least one memory — dialogue is never
 * purely cosmetic.
 */
public enum SocialInteractionType {
    GREETING,
    CONVERSATION,
    SHARING,
    HELPING,
    TRADING,
    COOPERATION,
    DISAGREEMENT,
    COMPETITION,
    CELEBRATION
}
