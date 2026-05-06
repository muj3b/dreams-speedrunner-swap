package com.example.speedrunnerswap.task;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class TaskMetadata {
    private TaskMetadata() {
    }

    static TaskDifficulty inferDefaultDifficulty(String id, String description) {
        String s = ((id == null ? "" : id) + " " + (description == null ? "" : description))
                .toLowerCase(Locale.ROOT);
        if (s.contains("dragon") || s.contains("wither") || s.contains("warden") || s.contains("elder guardian")
                || s.contains("ravager") || s.contains("piglin brute") || s.contains("void")
                || s.contains("elytra") || s.contains("stronghold") || s.contains("fortress")
                || s.contains("nether roof") || s.contains("lava ocean") || s.contains("ancient debris")
                || s.contains("shulker") || s.contains("end crystal") || s.contains("end spawn")
                || s.contains("all advancements") || s.contains("three bosses") || s.contains("raid")
                || s.contains("beacon") || s.contains("1000") || s.contains("10000")
                || s.contains("64 blaze") || s.contains("64 ender") || s.contains("full diamond")
                || s.contains("max-level") || s.contains("maximum level")) {
            return TaskDifficulty.HARD;
        }
        if (s.contains("craft") || s.contains("cake") || s.contains("paintings") || s.contains("glowstone")
                || s.contains("magma cream") || s.contains("trade 16") || s.contains("brew fire")
                || s.contains("music disc") || s.contains("2 saddles") || s.contains("sleep in a bed")
                || s.contains("sunset") || s.contains("collect one") || s.contains("totem")
                || s.contains("infinite water") || s.contains("bucket") || s.contains("fish")) {
            return TaskDifficulty.EASY;
        }
        return TaskDifficulty.MEDIUM;
    }

    static List<String> inferDefaultCategories(TaskDefinition def) {
        if (def.categories() != null && !def.categories().isEmpty()) {
            return def.categories();
        }
        String s = (def.id() + " " + def.description()).toLowerCase(Locale.ROOT);
        List<String> inferred = new ArrayList<>();
        if (s.contains("nether") || s.contains("blaze") || s.contains("piglin") || s.contains("ghast")
                || s.contains("hoglin") || s.contains("strider") || s.contains("wither skeleton")
                || s.contains("ancient debris") || s.contains("magma") || s.contains("soul sand")) {
            inferred.add("nether");
        }
        if (s.contains(" end") || s.contains("ender") || s.contains("dragon") || s.contains("shulker")
                || s.contains("elytra") || s.contains("chorus") || s.contains("void")) {
            inferred.add("end");
        }
        if (inferred.isEmpty()) {
            inferred.add("overworld");
        }
        return inferred;
    }
}
