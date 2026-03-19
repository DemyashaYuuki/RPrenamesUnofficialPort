package com.hiword9.rprenames.client.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.item.ItemAsset;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class RenameCatalog {
    private static final Map<Item, List<String>> BY_ITEM = new ConcurrentHashMap<>();

    private RenameCatalog() {
    }

    public static void rebuild(Map<Identifier, ItemAsset> itemAssets) {
        Map<Item, Set<String>> collected = new ConcurrentHashMap<>();

        itemAssets.forEach((id, asset) -> {
            if (!Registries.ITEM.containsId(id)) {
                return;
            }

            Item item = Registries.ITEM.get(id);
            Set<String> names = new LinkedHashSet<>();
            ItemModelNameCollector.collect(asset.model(), asset, new ArrayList<>(), names);
            if (!names.isEmpty()) {
                collected.computeIfAbsent(item, ignored -> new LinkedHashSet<>()).addAll(names);
            }
        });

        BY_ITEM.clear();
        collected.forEach((item, names) -> {
            List<String> sorted = new ArrayList<>(names);
            sorted.sort(String.CASE_INSENSITIVE_ORDER);
            BY_ITEM.put(item, Collections.unmodifiableList(sorted));
        });
    }

    public static List<String> get(Item item) {
        return BY_ITEM.getOrDefault(item, List.of());
    }

    public static int size() {
        return BY_ITEM.size();
    }

    public static List<String> filter(Item item, String query, int limit) {
        List<String> source = get(item);
        if (source.isEmpty()) {
            return List.of();
        }

        String lowered = query == null ? "" : query.toLowerCase();
        List<String> exactPrefix = new ArrayList<>();
        List<String> contains = new ArrayList<>();

        for (String entry : source) {
            String entryLower = entry.toLowerCase();
            if (lowered.isBlank() || entryLower.startsWith(lowered)) {
                exactPrefix.add(entry);
            } else if (entryLower.contains(lowered)) {
                contains.add(entry);
            }
        }

        List<String> result = new ArrayList<>(Math.min(limit, exactPrefix.size() + contains.size()));
        appendLimited(result, exactPrefix, limit);
        appendLimited(result, contains, limit);
        return result;
    }

    private static void appendLimited(List<String> target, Collection<String> entries, int limit) {
        for (String entry : entries) {
            if (target.size() >= limit) {
                return;
            }
            target.add(entry);
        }
    }
}
