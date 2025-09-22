package com.example.speedrunnerswap.task;

public enum TaskType {
    // Special
    DIE_ON_BEDROCK_FALL,
    KILL_GOLEM_NETHER_BED,
    KILL_ALL_SHEEP_IRON_SHOVEL,

    // Generic typed tasks
    ENTER_NETHER,
    ENTER_END,
    CRAFT_ITEM,
    MINE_BLOCK,
    KILL_ENTITY,
    DIE_TO_CAUSE,
    PLACE_BLOCK,
    SHEAR_COLORED_SHEEP,
    TAME_ENTITY,
    BREED_ENTITY,
    CONSUME_ITEM,
    FISH_ITEM,
    REACH_Y_LEVEL,
    COMPLEX_TASK  // For tasks requiring custom detection logic or manual verification
}
