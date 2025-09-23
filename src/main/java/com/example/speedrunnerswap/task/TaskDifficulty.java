package com.example.speedrunnerswap.task;

public enum TaskDifficulty {
    EASY,
    MEDIUM,
    HARD;

    public TaskDifficulty next() {
        return switch (this) {
            case EASY -> MEDIUM;
            case MEDIUM -> HARD;
            case HARD -> HARD;
        };
    }

    public TaskDifficulty prev() {
        return switch (this) {
            case HARD -> MEDIUM;
            case MEDIUM -> EASY;
            case EASY -> EASY;
        };
    }

    public String shortName() {
        return switch (this) {
            case EASY -> "E";
            case MEDIUM -> "M";
            case HARD -> "H";
        };
    }
}
