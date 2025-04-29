package com.itemsadder.converter;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class Mapping
{
    private final List<MappingEntry> entries = new ArrayList<>();
    private final String baseSection;
    private final List<Rule> customRules = new ArrayList<>();

    public Mapping(String baseSection)
    {
        this.baseSection = baseSection;
    }

    public static Mapping create(String baseSection)
    {
        return new Mapping(baseSection);
    }

    public MappingBuilder map(String... fromPaths)
    {
        return new MappingBuilder(this, Arrays.asList(fromPaths));
    }

    public MappingBuilder mapFirst(List<String> fromPaths)
    {
        return new MappingBuilder(this, fromPaths);
    }

    protected void addEntry(MappingEntry entry)
    {
        this.entries.add(entry);
    }

    public void apply(ConfigurationSection source, ConfigurationSection dest)
    {
        ConfigurationSection baseSource = source.getConfigurationSection(baseSection);
        if (baseSource == null) return;

        for (String key : baseSource.getKeys(false))
        {
            ConfigurationSection sourceItem = baseSource.getConfigurationSection(key);
            if (sourceItem == null) continue;
            ConfigurationSection destItem = dest.createSection(key);

            for (MappingEntry entry : entries)
            {
                for (String from : entry.sourcePaths)
                {
                    if (sourceItem.contains(from))
                    {
                        Object value = sourceItem.get(from);
                        if (entry.condition.test(value))
                        {
                            Object transformed = entry.transform.apply(value);
                            destItem.set(entry.targetPath, transformed);
                            break;
                        }
                    }
                }
            }

            for (Rule rule : customRules)
            {
                rule.accept(sourceItem, destItem);
            }
        }
    }

    public Mapping custom(Rule handler)
    {
        this.customRules.add(handler);
        return this;
    }

    public record MappingEntry(List<String> sourcePaths, String targetPath, Function<Object, Object> transform, Predicate<Object> condition) {}

    @FunctionalInterface
    public interface Rule
    {
        void accept(ConfigurationSection source, ConfigurationSection dest);
    }
}
