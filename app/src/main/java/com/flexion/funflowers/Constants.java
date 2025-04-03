package com.flexion.funflowers;

public class Constants {
    /**
     * Item ID for the seeds item
     */
    public static final String ITEM_ID_SEEDS = "1023608";

    /**
     * The number of displayed seeds that a purchase of one "seeds" item corresponds to.
     * e.g. If the player buys one "seeds" item then they will receive this many 'seeds'
     * to grow flowers with
     */
    public static final int SEEDS_PER_PURCHASE = 20;

    /**
     * The number of seeds that the player starts with when they first run the game
     */
    public static final int PLAYER_STARTING_SEEDS = 20;

    /**
     * A key value used to reference a stored variable that records the player's
     * available number of seeds
     */
    public static final String KEY_PLAYER_SEEDS = "key_player_seeds";
}
