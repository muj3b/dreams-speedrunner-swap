package com.example.speedrunnerswap.task;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class TaskMetadataTest {
    @Test
    public void classifiesObviousHardDefaultsAsHard() {
        assertEquals(TaskDifficulty.HARD,
                TaskMetadata.inferDefaultDifficulty("kill_ender_dragon_punch",
                        "Deal the killing blow to the ender dragon with your fist"));
        assertEquals(TaskDifficulty.HARD,
                TaskMetadata.inferDefaultDifficulty("create_nether_highway_1000",
                        "Create a Nether highway spanning 1000 blocks"));
    }

    @Test
    public void classifiesSimpleDefaultsAsEasy() {
        assertEquals(TaskDifficulty.EASY,
                TaskMetadata.inferDefaultDifficulty("make_cake", "Craft and place a cake"));
        assertEquals(TaskDifficulty.EASY,
                TaskMetadata.inferDefaultDifficulty("collect_16_glowstone", "Collect 16 glowstone dust"));
    }

    @Test
    public void preservesExplicitCategories() {
        TaskDefinition def = new TaskDefinition("custom_test", "Custom test", TaskType.COMPLEX_TASK, List.of(),
                TaskDifficulty.MEDIUM, List.of("custom", "nether"), true);

        assertEquals(List.of("custom", "nether"), TaskMetadata.inferDefaultCategories(def));
    }

    @Test
    public void infersProgressionCategories() {
        TaskDefinition nether = new TaskDefinition("kill_blaze_water_bucket", "Kill a blaze using a water bucket",
                TaskType.COMPLEX_TASK);
        TaskDefinition end = new TaskDefinition("collect_chorus_fruit", "Collect chorus fruit in the End",
                TaskType.COMPLEX_TASK);

        assertTrue(TaskMetadata.inferDefaultCategories(nether).contains("nether"));
        assertTrue(TaskMetadata.inferDefaultCategories(end).contains("end"));
    }
}
