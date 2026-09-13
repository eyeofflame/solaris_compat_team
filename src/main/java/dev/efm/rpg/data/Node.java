package dev.efm.rpg.data;

import java.util.List;

public record Node(String id, String text, List<Choice> choices, String nextId) {
}
