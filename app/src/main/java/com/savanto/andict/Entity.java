package com.savanto.andict;

public final class Entity {
    final String name;
    final String description;

    public Entity(String name, String description) {
        this.name = name;
        this.description = description;
    }

    @Override
    public String toString() {
        return String.format("%s [%s]", this.name, this.description);
    }
}
