package com.example.speedrunnerswap.gui;

public class ControlGuiHolder implements org.bukkit.inventory.InventoryHolder {
    public enum Type { MAIN, RUNNER_SELECTOR, ABOUT }
    private final Type type;

    public ControlGuiHolder(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    @Override
    public org.bukkit.inventory.Inventory getInventory() {
        return null; // not used; marker holder only
    }
}

