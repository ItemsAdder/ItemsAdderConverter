package com.itemsadder.converter;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractConverter
{
    private static final List<String> TYPES_FOLDERS = List.of("nexo", "oraxen");

    protected void warn(String s, ConfigurationSection sourceItem, File file)
    {
        String message = s + " in file " + file.getName() + " for item " + sourceItem.getName();
        Main.inst().getLogger().warning(message);
    }

    @NotNull
    public static List<String> getFoldersNames(String partialName)
    {
        List<String> folders = new ArrayList<>();

        File convertFolderBase = new File("convert");
        if (!convertFolderBase.exists())
            return folders;

        for(String typeFolderName : TYPES_FOLDERS)
        {
            File folder = new File(convertFolderBase, typeFolderName);
            if(!folder.exists())
                continue;

            String completeStr = typeFolderName;

            File[] entries = folder.listFiles();
            if (entries == null)
                return folders;

            for (File entry : entries)
            {
                completeStr += "/" + entry.getName();
                if (entry.isDirectory() && completeStr.contains(partialName))
                {
                    folders.add(completeStr);
                }
            }
        }
        return folders;
    }

    public static void createFolders()
    {
        TYPES_FOLDERS.forEach(typeFolderName ->
        {
            File folder = new File(Main.inst().getDataFolder(), typeFolderName);
            if (!folder.exists())
                folder.mkdirs();
        });
    }

    public abstract void convert();
    protected abstract void convertItems(File file, FileConfiguration config, FileConfiguration outputConfig);

}
