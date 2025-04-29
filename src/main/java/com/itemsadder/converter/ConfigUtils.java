package com.itemsadder.converter;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConfigUtils
{
    public static List<Map<String, Object>> getSectionList(ConfigurationSection section, String path)
    {
        Object raw = section.get(path);
        List<Map<String, Object>> result = new ArrayList<>();

        if (raw instanceof List<?> list)
        {
            for (Object item : list)
            {
                if (item instanceof Map<?, ?> map)
                {
                    // safe cast da Map<?, ?> a Map<String, Object>
                    Map<String, Object> stringMap = new java.util.LinkedHashMap<>();
                    for (Map.Entry<?, ?> entry : map.entrySet())
                    {
                        if (entry.getKey() instanceof String key)
                        {
                            stringMap.put(key, entry.getValue());
                        }
                    }
                    result.add(stringMap);
                }
            }
        }

        return result;
    }
}
