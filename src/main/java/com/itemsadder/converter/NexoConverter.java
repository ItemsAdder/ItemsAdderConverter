package com.itemsadder.converter;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;

// https://docs.nexomc.com/
public class NexoConverter
{
    private final File sourceFolder;
    private final String folderName;

    public NexoConverter(String folderName) throws IllegalArgumentException
    {
        this.folderName = folderName;

        File convertFolderBase = new File("convert" + File.separator + "nexo");
        if (!convertFolderBase.exists())
        {
            //noinspection ResultOfMethodCallIgnored
            convertFolderBase.mkdirs();
        }

        this.sourceFolder = new File(Main.inst().getDataFolder(), new File(convertFolderBase, folderName).getPath());
        if (!sourceFolder.exists())
        {
            throw new IllegalArgumentException("Folder " + folderName + " does not exist in the converter directory.");
        }
    }

    @NotNull
    public static List<String> getFoldersNames(String partialName)
    {
        File convertFolderBase = new File("convert" + File.separator + "nexo");
        if (!convertFolderBase.exists())
        {
            //noinspection ResultOfMethodCallIgnored
            convertFolderBase.mkdirs();
        }

        List<String> folders = new ArrayList<>();
        File[] files = convertFolderBase.listFiles();
        if (files == null)
            return folders;
        for (File file : files)
        {
            if (file.isDirectory() && file.getName().contains(partialName))
            {
                folders.add(file.getName());
            }
        }
        return folders;
    }

    public void convert()
    {
        File[] files = sourceFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null)
            return;

        for (File file : files)
        {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            FileConfiguration outputConfig = new YamlConfiguration();

            if(config.contains("items"))
            {
                @SuppressWarnings("DataFlowIssue")
                Set<String> keys = config.getConfigurationSection("items").getKeys(false);
                for (String key : keys)
                {
                    @Nullable ConfigurationSection sourceItem = config.getConfigurationSection(key);
                    assert sourceItem != null;

                    @NotNull ConfigurationSection destItem = outputConfig.createSection(key);

                    if (sourceItem.contains("template"))
                    {
                        if (sourceItem.get("template") instanceof String)
                            destItem.set("variant_of", sourceItem.getString("template"));
                        else if (sourceItem.get("template") instanceof Boolean)
                            destItem.set("template", sourceItem.getBoolean("template"));
                    }

                    destItem.set("name", sourceItem.getString("customname", sourceItem.getString("displayname", sourceItem.getString("itemname"))));

                    if (sourceItem.contains("permission"))
                        destItem.set("permission_suffix", sourceItem.get("permission"));

                    if (sourceItem.contains("lore"))
                        destItem.set("lore", sourceItem.get("lore"));

                    if (sourceItem.contains("disable_enchanting"))
                        destItem.set("blocked_enchants", List.of("all"));

                    if (sourceItem.contains("unbreakable"))
                        destItem.set("durability.unbreakable", sourceItem.get("unbreakable"));

                    // ItemFlags
                    if (sourceItem.contains("ItemFlags"))
                        destItem.set("item_flags", sourceItem.get("ItemFlags"));

                    // NOTE: excludeFromInventory unsupported.

                    if (sourceItem.contains("PersistentData"))
                        warn("'PersistentData' support not implemented yet.", sourceItem, file);

                    // TODO potion effects
                    // https://docs.nexomc.com/configuration/items-advanced#potioneffects

                    // TODO: colors
                    // https://docs.nexomc.com/configuration/items-advanced#color

                    if (sourceItem.contains("Enchantments"))
                        destItem.set("enchantments", sourceItem.get("Enchantments"));

                    @NotNull ConfigurationSection resource = destItem.createSection("resource");

                    if (sourceItem.contains("Pack.custom_model_data"))
                        destItem.set("resource.custom_model_data", sourceItem.get("Pack.custom_model_data"));


                    // Durability
                    if (sourceItem.contains("Components"))
                    {
                        Object componentsObj = sourceItem.get("Components");
                        if (componentsObj instanceof Map<?, ?> components)
                        {
                            if (components.containsKey("durability"))
                            {
                                Object durabilityObj = ((Map<?, ?>) components.get("durability")).get("value");
                                if (durabilityObj != null)
                                {
                                    Map<String, Object> durability = new HashMap<>();
                                    durability.put("max_custom_durability", durabilityObj);
                                    destItem.set("durability", durability);
                                }
                            }
                        }
                    }

                    // Resource
                    resource.set("material", sourceItem.get("material", "PAPER"));

                    ConfigurationSection sourcePack = sourceItem.getConfigurationSection("Pack");
                    assert sourcePack != null;

                    if (sourcePack.contains("texture") && sourcePack.get("texture") instanceof String)
                    {
                        resource.set("texture", sourcePack.getString("texture"));
                    }
                    else if (sourcePack.contains("textures") && sourcePack.get("textures") instanceof List<?> sourceTexturesList)
                    {
                        resource.set("textures", sourceTexturesList);
                    }
                    else
                    {
                        // Convert blocks special cases.
                        // ItemsAdder configuration:
                        // textures:
                        // - block/block_down.png
                        // - block/block_east.png
                        // - block/block_north.png
                        // - block/block_south.png
                        // - block/block_up.png
                        // - block/block_west.png
                        if (sourcePack.contains("parent_model"))
                        {
                            String parentModel = sourcePack.getString("parent_model");
                            if (parentModel == null)
                                parentModel = "";

                            switch (parentModel)
                            {
                                case "block/cube_top" ->
                                {
                                    if (sourcePack.contains("texture") && sourcePack.get("texture") instanceof String)
                                    {
                                        resource.set("texture", sourcePack.getString("texture"));
                                    }
                                    else if (sourcePack.contains("textures") && sourcePack.get("textures") instanceof List<?> sourceTexturesList)
                                    {
                                        resource.set("textures", sourceTexturesList);
                                    }
                                    else if (sourcePack.contains("textures"))
                                    {
                                        Map<String, String> blockTextures = new HashMap<>();
                                        if (sourcePack.contains("textures.top"))
                                        {
                                            blockTextures.put("up", sourcePack.getString("textures.top"));
                                        }
                                        if (sourcePack.contains("textures.side"))
                                        {
                                            blockTextures.put("down", sourcePack.getString("textures.side"));
                                            blockTextures.put("east", sourcePack.getString("textures.side"));
                                            blockTextures.put("west", sourcePack.getString("textures.side"));
                                            blockTextures.put("north", sourcePack.getString("textures.side"));
                                            blockTextures.put("south", sourcePack.getString("textures.side"));
                                        }

                                        List<String> blockTexturesList = new ArrayList<>();
                                        blockTexturesList.add(blockTextures.get("down"));
                                        blockTexturesList.add(blockTextures.get("east"));
                                        blockTexturesList.add(blockTextures.get("north"));
                                        blockTexturesList.add(blockTextures.get("south"));
                                        blockTexturesList.add(blockTextures.get("up"));
                                        blockTexturesList.add(blockTextures.get("west"));

                                        resource.set("textures", blockTexturesList);
                                    }
                                }
                                case "block/cube_all" ->
                                {
                                    if (sourcePack.contains("texture") && sourcePack.get("texture") instanceof String)
                                    {
                                        List<String> blockTexturesList = new ArrayList<>();
                                        String texture = sourcePack.getString("texture");
                                        for (int i = 0; i < 6; i++)
                                        {
                                            blockTexturesList.add(texture);
                                        }
                                        resource.set("textures", blockTexturesList);
                                    }
                                }
                                case "block/cube_column" ->
                                {
                                    if (sourcePack.contains("textures"))
                                    {
                                        List<String> blockTexturesList = new ArrayList<>();
                                        if (sourcePack.contains("textures.side") && sourcePack.contains("textures.end"))
                                        {
                                            String side = sourcePack.getString("textures.side");
                                            String end = sourcePack.getString("textures.end");

                                            blockTexturesList.add(end);   // down
                                            blockTexturesList.add(side);  // east
                                            blockTexturesList.add(side);  // north
                                            blockTexturesList.add(side);  // south
                                            blockTexturesList.add(end);   // up
                                            blockTexturesList.add(side);  // west

                                            resource.set("textures", blockTexturesList);
                                        }
                                    }
                                }
                                case "block/orientable" ->
                                {
                                    if (sourcePack.contains("textures.top"))
                                    {
                                        List<String> blockTexturesList = new ArrayList<>();
                                        String top = sourcePack.getString("textures.top");

                                        for (int i = 0; i < 6; i++)
                                        {
                                            blockTexturesList.add(top);
                                        }

                                        resource.set("textures", blockTexturesList);
                                    }
                                }
                                case "block/orientable_vertical" ->
                                {
                                    if (sourcePack.contains("textures"))
                                    {
                                        List<String> blockTexturesList = new ArrayList<>();
                                        if (sourcePack.contains("textures.side") && sourcePack.contains("textures.front"))
                                        {
                                            String side = sourcePack.getString("textures.side");
                                            String front = sourcePack.getString("textures.front");

                                            blockTexturesList.add(side);   // down
                                            blockTexturesList.add(side);   // east
                                            blockTexturesList.add(side);   // north
                                            blockTexturesList.add(side);   // south
                                            blockTexturesList.add(front);  // up
                                            blockTexturesList.add(side);   // west

                                            resource.set("textures", blockTexturesList);
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (sourceItem.contains("Pack.model_path"))
                    {
                        resource.set("generate", true);
                        resource.set("model_path", sourceItem.getString("Pack.model"));
                        if (sourceItem.contains("Pack.parent_model"))
                            resource.set("parent", sourceItem.getString("Pack.parent_model"));
                    }
                    else
                    {
                        resource.set("generate", false);
                        if (resource.contains("texture") && resource.get("texture") instanceof String)
                        {
                            resource.set("texture", sourceItem.getString("Pack.texture"));
                        }
                        else if (resource.contains("textures") && resource.get("textures") instanceof List<?> sourceTexturesList)
                        {
                            resource.set("textures", sourceTexturesList);
                        }
                        else if (resource.contains("textures"))
                        {
                            warn("'textures' support not implemented yet.", sourceItem, file);
                        }
                    }

                    destItem.set("resource", resource);

                    // Map attribute modifiers
                    if (sourceItem.contains("AttributeModifiers"))
                    {
                        Map<String, Object> attributeModifiers = new HashMap<>();
                        Map<String, Object> head = new HashMap<>();

                        // Assuming it's always an array
                        Object listObj = sourceItem.get("AttributeModifiers");
                        if (listObj instanceof Iterable<?>)
                        {
                            for (Object entryObj : (Iterable<?>) listObj)
                            {
                                if (entryObj instanceof Map<?, ?> entry)
                                {
                                    String attribute = ((String) entry.get("attribute")).toLowerCase().replace("generic_", "");
                                    head.put(attribute, entry.get("amount"));
                                }
                            }
                        }
                        attributeModifiers.put("head", head);
                        destItem.set("attribute_modifiers", attributeModifiers);
                    }

                    // Equippables
                    if (sourcePack.contains("Components.equippable"))
                    {
                        ConfigurationSection equippable = destItem.createSection("equippable");
                        equippable.set("slot", sourcePack.getString("Components.equippable.slot"));

                        String model = sourcePack.getString("Components.equippable.model");
                        if (model != null)
                            equippable.set("id", model);

                        String cameraOverlay = sourcePack.getString("Components.equippable.camera_overlay");
                        if (cameraOverlay != null)
                            equippable.set("camera_overlay", cameraOverlay);

                        String equipSound = sourcePack.getString("Components.equippable.equip_sound");
                        if (equipSound != null)
                            equippable.set("equip_sound", equipSound);

                        if (sourcePack.contains("Components.equippable.allowed_entities"))
                            equippable.set("allowed_entities", sourcePack.get("Components.equippable.allowed_entities"));

                        if (sourcePack.contains("Components.equippable.dispensable"))
                            equippable.set("dispensable", sourcePack.get("Components.equippable.dispensable"));

                        if (sourcePack.contains("Components.equippable.swappable"))
                            equippable.set("swappable", sourcePack.get("Components.equippable.swappable"));

                        if (sourcePack.contains("Components.equippable.damage_on_hurt"))
                            equippable.set("damage_on_hurt", sourcePack.get("Components.equippable.damage_on_hurt"));
                    }

                    // TODO food
                    // TODO consumable
                    // TODO: damage_resistant
                    // todo: glider
                    // todo tooltip

                    // TODO furniture
                    // TODO light mechanic (for furniture)

                    // TODO farming mechanic. (once it also will be implemented into itemsadder itself).
                    // TODO bed mechanic. (once it also will be implemented into itemsadder itself).

                    // TODO seat mechanic (furniture_sit)

                    // TODO various Nexo mechanics

                    // TODO test if the models actually convert correctly or if I have to dynamically fix the namespaces inside the
                    //  models json files themselves.

                    outputConfig.set(key, destItem);
                }
            }

            // TODO recipes
            // TODO armors
            // TODO glyphs
            // TODO create the /ia menu entries for the items.

            try
            {
                File outputFolder = new File(Main.inst().getDataFolder(), "contents" + File.separator + folderName);
                if (!outputFolder.exists())
                {
                    //noinspection ResultOfMethodCallIgnored
                    outputFolder.mkdirs();
                }
                outputConfig.save(new File(outputFolder, "converted_itemsadder.yml"));
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    private void warn(String s, ConfigurationSection sourceItem, File file)
    {
        String message = s + " in file " + file.getName() + " for item " + sourceItem.getName();
        Main.inst().getLogger().warning(message);
    }
}
