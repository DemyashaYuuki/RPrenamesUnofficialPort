package com.hiword9.rprenames.client.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.client.item.ItemAsset;
import net.minecraft.client.render.item.model.CompositeItemModel;
import net.minecraft.client.render.item.model.ConditionItemModel;
import net.minecraft.client.render.item.model.ItemModel;
import net.minecraft.client.render.item.model.RangeDispatchItemModel;
import net.minecraft.client.render.item.model.SelectItemModel;
import net.minecraft.client.render.item.property.select.ComponentSelectProperty;
import net.minecraft.client.render.item.property.select.SelectProperty;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.text.Text;

public final class ItemModelNameCollector {
    private ItemModelNameCollector() {
    }

    public static void collect(ItemModel.Unbaked model, ItemAsset asset, List<String> currentNames, Set<String> output) {
        switch (model) {
            case CompositeItemModel.Unbaked composite -> {
                for (ItemModel.Unbaked child : composite.models()) {
                    collect(child, asset, currentNames, output);
                }
            }
            case ConditionItemModel.Unbaked condition -> {
                collect(condition.onTrue(), asset, currentNames, output);
                collect(condition.onFalse(), asset, currentNames, output);
            }
            case SelectItemModel.Unbaked select -> collectSelect(select, asset, currentNames, output);
            case RangeDispatchItemModel.Unbaked range -> {
                for (RangeDispatchItemModel.Entry entry : range.entries()) {
                    collect(entry.model(), asset, currentNames, output);
                }
                range.fallback().ifPresent(fallback -> collect(fallback, asset, currentNames, output));
            }
            default -> {
                if (!currentNames.isEmpty()) {
                    output.addAll(currentNames);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <P extends SelectProperty<T>, T> void collectSelect(
            SelectItemModel.Unbaked select,
            ItemAsset asset,
            List<String> currentNames,
            Set<String> output
    ) {
        SelectItemModel.UnbakedSwitch<P, T> unbakedSwitch = (SelectItemModel.UnbakedSwitch<P, T>) select.unbakedSwitch();
        boolean isCustomNameProperty = unbakedSwitch.property() instanceof ComponentSelectProperty<?>(ComponentType<?> componentType)
                && componentType.equals(DataComponentTypes.CUSTOM_NAME);

        for (var switchCase : unbakedSwitch.cases()) {
            List<String> propagated = currentNames;
            if (isCustomNameProperty) {
                propagated = new ArrayList<>(currentNames);
                for (T value : switchCase.values()) {
                    if (value instanceof Text text) {
                        String name = text.getString();
                        if (!name.isBlank()) {
                            propagated.add(name);
                        }
                    }
                }
            }
            collect(switchCase.model(), asset, propagated, output);
        }

        select.fallback().ifPresent(fallback -> collect(fallback, asset, currentNames, output));
    }
}
