package com.itemsadder.converter;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

// https://docs.nexomc.com/
public class NexoConverter extends AbstractConverter
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

    @Override
    public void convert()
    {
        File itemsFolder = new File(sourceFolder, "items");
        if (itemsFolder.exists())
        {
            File[] itemsFiles = itemsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
            if (itemsFiles == null)
                return;

            for (File file : itemsFiles)
            {
                FileConfiguration sourceConfig = YamlConfiguration.loadConfiguration(file);
                FileConfiguration outputConfig = new YamlConfiguration();

                convertItems(file, sourceConfig, outputConfig);

                // TODO recipes.
                //  "Recipes can be created directly in the relevant file within the `Nexo/recipes` directory"
                //  https://docs.nexomc.com/general-usage/recipes


                // TODO test if the models actually convert correctly or if I have to dynamically fix the namespaces inside the
                //  models json files themselves.

                // TODO armors
                // TODO glyphs
                // TODO create the /ia menu entries for the items.

                // TODO: iterate the items, find furniture mechanic, convert loot into separate ItemsAdder loot section.


                // TODO light mechanic (block)

                // TODO farming mechanic. (once it also will be implemented into itemsadder itself).
                // TODO bed mechanic. (once it also will be implemented into itemsadder itself).

                // TODO other various Nexo mechanics
                //  https://docs.nexomc.com/mechanics/all-mechanics

                // TODO: convert sounds to the json variant.

                // TODO: custom blocks
                //   - log_strip mechanic. ItemsAdder can do that with events and replace block.
                //     https://docs.nexomc.com/mechanics/custom-block-mechanics/noteblock-mechanic/stripped-log-mechanic
                //   - directional blocks. ItemsAdder handles them in a different and more optimal way: https://itemsadder.devs.beer/plugin-usage/adding-content/advanced-block-properties/directional-blocks
                //     https://docs.nexomc.com/mechanics/custom-block-mechanics/noteblock-mechanic/directional-mechanic

                outputConfig.set("info.namespace", "nexo");
                outputConfig.set("info.converted_from", "nexo");

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
                    Main.inst().getLogger().warning("Failed to save converted file: " + file.getName());
                    e.printStackTrace();
                }
            }
        }
        else
        {
            Main.inst().getLogger().warning("No 'items' folder found in " + sourceFolder.getPath() + ". Skipping conversion.");
        }
    }

    @Override
    protected void convertItems(File file, FileConfiguration config, FileConfiguration outputConfig)
    {
        Mapping mapping = Mapping.create()
                .mapFirst(List.of("customname", "displayname", "itemname")).to("name").end()
                .map("lore").to("lore").end()
                .map("permission").to("permission_suffix").end()
                .map("disable_enchanting").to("blocked_enchants").transform(v -> List.of("all")).end()
                .map("unbreakable").to("durability.unbreakable").end()
                .map("ItemFlags").to("item_flags").end()
                .map("Enchantments").to("enchantments").end()
                .map("Pack.custom_model_data").to("resource.custom_model_data").end()
                .map("material").to("resource.material").end()
                .map("hide_tooltip").to("hide_tooltip").end()
                .map("enchantment_glint_override").to("glint").end()
                // unsupported stuff
                .custom((source, dest) -> {
                    // TODO: add support in the future, seems like a niche feature.
                    if (source.contains("PersistentData"))
                        warn("'PersistentData' support not implemented yet.", source, file);

                    // damage_resistant not implemented in ItemsAdder yet
                    if (source.contains("damage_resistant"))
                        warn("'damage_resistant' support not implemented yet.", source, file);

                    // enchantable not implemented in ItemsAdder yet
                    if (source.contains("enchantable"))
                        warn("'enchantable' support not implemented yet.", source, file);

                    // use_remainder not implemented in ItemsAdder yet
                    if (source.contains("use_remainder"))
                        warn("'use_remainder' support not implemented yet.", source, file);

                    // jukebox_playable not implemented in ItemsAdder yet
                    if (source.contains("jukebox_playable"))
                        warn("'jukebox_playable' support not implemented yet.", source, file);

                    // repairable not implemented in ItemsAdder yet
                    if (source.contains("repairable")) // TODO, implement automatic creation of the recipe.
                        warn("'repairable' support not implemented yet. You should manually create an ItemsAdder anvil recipe.", source, file);
                })
                // tooltip_display
                .custom((source, dest) -> {
                    ConfigurationSection components = source.getConfigurationSection("Components");
                    if (components != null && components.contains("tooltip_display"))
                    {
                        List<String> tooltipDisplay = components.getStringList("tooltip_display");
                        if (!tooltipDisplay.isEmpty())
                        {
                            dest.set("hide_tooltip", true);

                            // Warning perché IA non supporta nascondere solo alcuni componenti
                            warn("'Components.tooltip_display' found but ItemsAdder does not support hiding specific components. Forced full tooltip hiding instead.", source, null);
                        }
                    }
                })
                // tooltip_style
                .custom((source, dest) -> {
                    ConfigurationSection components = source.getConfigurationSection("Components");
                    if (components != null && components.contains("tooltip_style"))
                    {
                        String style = components.getString("tooltip_style");
                        if (style != null && !style.isEmpty())
                        {
                            dest.set("tooltip_style", style);
                        }
                    }
                })
                // attribute_modifiers
                .custom((source, dest) -> {
                    if (!source.contains("AttributeModifiers")) return;

                    Map<String, Object> slotMap = new HashMap<>();

                    List<Map<String, Object>> modifiers = ConfigUtils.getSectionList(source, "AttributeModifiers");

                    for (Map<String, Object> entry : modifiers)
                    {
                        Object attrRaw = entry.get("attribute");
                        Object amountRaw = entry.get("amount");
                        Object opRaw = entry.getOrDefault("operation", 0);
                        Object slotRaw = entry.getOrDefault("slot", "HAND");

                        if (!(attrRaw instanceof String attr) || !(amountRaw instanceof Number amount)) continue;

                        attr = attr.toLowerCase().replace("generic_", "");
                        double value = amount.doubleValue();

                        String operation = switch (String.valueOf(opRaw))
                        {
                            case "0" -> "add";
                            case "1" -> "multiply";
                            case "2" -> "multiply_base";
                            default -> null;
                        };

                        String slot = switch (String.valueOf(slotRaw).toLowerCase())
                        {
                            case "hand", "mainhand" -> "mainhand";
                            case "offhand" -> "offhand";
                            case "feet" -> "feet";
                            case "legs" -> "legs";
                            case "chest" -> "chest";
                            case "head" -> "head";
                            default -> null;
                        };

                        if (slot == null || operation == null) continue;

                        Map<String, Object> attrMap = new LinkedHashMap<>();
                        attrMap.put("operation", operation);
                        attrMap.put("value", value);

                        @SuppressWarnings("unchecked")
                        Map<String, Object> slotAttributes = (Map<String, Object>) slotMap.computeIfAbsent(slot, k -> new LinkedHashMap<>());
                        slotAttributes.put(attr, attrMap);
                    }

                    if (!slotMap.isEmpty())
                    {
                        dest.set("attribute_modifiers", slotMap);
                    }
                })
                // template, variant_of
                .custom((source, dest) -> {
                    if (source.contains("template"))
                    {
                        if (source.get("template") instanceof String)
                            dest.set("variant_of", source.getString("template"));
                        else if (source.get("template") instanceof Boolean)
                            dest.set("template", source.getBoolean("template"));
                    }
                })
                // durability
                .custom((source, dest) -> {
                    // https://docs.nexomc.com/configuration/items-advanced
                    //     durability: 10
                    //    # if the material above isnt a normal tool, but say PAPER
                    //    # The item will not have its durability lowered by actions by default
                    //    # Example of making the tool lower its durability from hitting entities and breaking blocks
                    //    #durability:
                    //    #  value: 10
                    //    #  damage_block_break: true
                    //    # damage_entity_hit: true
                    if (source.get("durability", source.get("durability.value")) instanceof Integer durability)
                    {
                        Map<String, Object> durabilityMap = new HashMap<>();
                        durabilityMap.put("max_custom_durability", durability);
                        dest.set("durability", durabilityMap);
                    }

                    if (source.contains("durability.damage_block_break"))
                        warn("'durability.damage_block_break' support not supported.", source, file);
                    if (source.contains("durability.damage_entity_hit"))
                        warn("'durability.damage_entity_hit' support not supported.", source, file);
                })
                // pack
                .custom((source, dest) -> {
                    ConfigurationSection pack = source.getConfigurationSection("Pack");
                    if (pack == null) return;

                    ConfigurationSection resource = dest.createSection("resource");

                    // Material fallback
                    String material = source.getString("material", "PAPER");
                    resource.set("material", material);

                    // model_path
                    if (pack.contains("model_path"))
                    {
                        resource.set("generate", true);
                        resource.set("model_path", pack.getString("model_path"));
                        if (pack.contains("parent_model"))
                            resource.set("parent", pack.getString("parent_model"));
                    }
                    else
                    {
                        resource.set("generate", false);
                    }

                    // texture (basic)
                    if (pack.contains("texture") && pack.get("texture") instanceof String)
                    {
                        resource.set("texture", pack.getString("texture"));
                        return;
                    }

                    // textures (basic)
                    if (pack.contains("textures") && pack.get("textures") instanceof List<?> textures)
                    {
                        resource.set("textures", textures);
                        return;
                    }

                    // texture from parent_model
                    String parentModel = pack.getString("parent_model", "");
                    List<String> blockTextures = new ArrayList<>();

                    // TODO: check if this shit is actually working good or not.
                    switch (parentModel)
                    {
                        case "block/cube_top" ->
                        {
                            String top = pack.getString("textures.top");
                            String side = pack.getString("textures.side");
                            if (top != null && side != null)
                            {
                                blockTextures.add(side); // down
                                blockTextures.add(side); // east
                                blockTextures.add(side); // north
                                blockTextures.add(side); // south
                                blockTextures.add(top);  // up
                                blockTextures.add(side); // west
                            }
                        }
                        case "block/cube_all" ->
                        {
                            String texture = pack.getString("texture");
                            if (texture != null)
                            {
                                for (int i = 0; i < 6; i++) blockTextures.add(texture);
                            }
                        }
                        case "block/cube_column" ->
                        {
                            String side = pack.getString("textures.side");
                            String end = pack.getString("textures.end");
                            if (side != null && end != null)
                            {
                                blockTextures.add(end);   // down
                                blockTextures.add(side);  // east
                                blockTextures.add(side);  // north
                                blockTextures.add(side);  // south
                                blockTextures.add(end);   // up
                                blockTextures.add(side);  // west
                            }
                        }
                        case "block/orientable" ->
                        {
                            String top = pack.getString("textures.top");
                            if (top != null)
                            {
                                for (int i = 0; i < 6; i++) blockTextures.add(top);
                            }
                        }
                        case "block/orientable_vertical" ->
                        {
                            String side = pack.getString("textures.side");
                            String front = pack.getString("textures.front");
                            if (side != null && front != null)
                            {
                                blockTextures.add(side);   // down
                                blockTextures.add(side);   // east
                                blockTextures.add(side);   // north
                                blockTextures.add(side);   // south
                                blockTextures.add(front);  // up
                                blockTextures.add(side);   // west
                            }
                        }
                    }

                    if (!blockTextures.isEmpty())
                    {
                        resource.set("textures", blockTextures);
                    }
                    else if (resource.contains("textures") && !(resource.get("textures") instanceof List<?>))
                    {
                        warn("'textures' particular settings support not implemented yet.", source, file); // TODO
                    }

                    dest.set("resource", resource);
                })
                // equippable
                .custom((source, dest) -> {
                    ConfigurationSection pack = source.getConfigurationSection("Pack");
                    if (pack == null) return;

                    if (pack.contains("Components.equippable"))
                    {
                        ConfigurationSection equippable = dest.createSection("equippable");

                        // Mapping diretto dei campi
                        equippable.set("slot", pack.getString("Components.equippable.slot"));

                        String model = pack.getString("Components.equippable.model");
                        if (model != null)
                        {
                            equippable.set("id", model);
                        }

                        String cameraOverlay = pack.getString("Components.equippable.camera_overlay");
                        if (cameraOverlay != null)
                        {
                            equippable.set("camera_overlay", cameraOverlay);
                        }

                        String equipSound = pack.getString("Components.equippable.equip_sound");
                        if (equipSound != null)
                        {
                            equippable.set("equip_sound", equipSound);
                        }

                        if (pack.contains("Components.equippable.allowed_entities"))
                        {
                            equippable.set("allowed_entities", pack.get("Components.equippable.allowed_entities"));
                        }

                        if (pack.contains("Components.equippable.dispensable"))
                        {
                            equippable.set("dispensable", pack.get("Components.equippable.dispensable"));
                        }

                        if (pack.contains("Components.equippable.swappable"))
                        {
                            equippable.set("swappable", pack.get("Components.equippable.swappable"));
                        }

                        if (pack.contains("Components.equippable.damage_on_hurt"))
                        {
                            equippable.set("damage_on_hurt", pack.get("Components.equippable.damage_on_hurt"));
                        }

                        if (source.contains("Components.glider"))
                        {
                            equippable.set("glider", source.get("Components.glider"));
                        }
                    }
                })
                // furniture
                .custom((source, dest) -> {
                    ConfigurationSection sourceFurniture = source.getConfigurationSection("Mechanics.furniture");
                    if (sourceFurniture == null) return;

                    ConfigurationSection furniture = dest.createSection("behaviours.furniture");

                    // rotatable
                    if (source.contains("rotatable"))
                    {
                        boolean rotatable = source.getBoolean("rotatable");
                        furniture.set("fixed_rotation", !rotatable);
                    }

                    // limited placing
                    if (source.contains("limited_placing.roof"))
                    {
                        furniture.set("placeable_on.ceiling", source.getBoolean("limited_placing.roof"));
                    }
                    if (source.contains("limited_placing.floor"))
                    {
                        furniture.set("placeable_on.floor", source.getBoolean("limited_placing.floor"));
                    }
                    if (source.contains("limited_placing.wall"))
                    {
                        furniture.set("placeable_on.walls", source.getBoolean("limited_placing.wall"));
                    }

                    // type/entity
                    String type = source.getString("type");
                    if (type != null)
                    {
                        switch (type.toLowerCase())
                        {
                            case "display_entity", "item_display" -> furniture.set("entity", "item_display");
                            case "armor_stand" -> furniture.set("entity", "armor_stand");
                            case "item_frame" -> furniture.set("entity", "item_frame");
                        }
                    }

                    // Oraxen legacy shit
                    if (source.contains("display_entity_properties.display_transform"))
                    {
                        furniture.set("display_transformation.transform", source.get("display_entity_properties.display_transform"));
                    }
                    if (source.contains("properties.display_transform"))
                    {
                        furniture.set("display_transformation.transform", source.get("properties.display_transform"));
                    }

                    // Warn about hitbox not implemented
                    if (source.contains("hitbox")) // TODO
                    {
                        warn("'hitbox' support not implemented yet.", source, file);
                    }

                    // tracking_rotation not implemented
                    if (source.contains("tracking_rotation")) // TODO
                    {
                        warn("'tracking_rotation' support not implemented yet.", source, file);
                    }

                    // Block Sounds
                    if (sourceFurniture.contains("block_sounds"))
                    {
                        ConfigurationSection blockSounds = sourceFurniture.getConfigurationSection("block_sounds");
                        if (blockSounds != null)
                        {
                            ConfigurationSection soundSection = furniture.createSection("sound");

                            if (blockSounds.contains("place_sound"))
                            {
                                ConfigurationSection place = soundSection.createSection("place");
                                place.set("name", blockSounds.getString("place_sound"));
                                place.set("volume", 0);
                                place.set("pitch", 0);
                            }

                            if (blockSounds.contains("break_sound"))
                            {
                                ConfigurationSection breakS = soundSection.createSection("break");
                                breakS.set("name", blockSounds.getString("break_sound"));
                                breakS.set("volume", 0);
                                breakS.set("pitch", 0);
                            }

                            if (blockSounds.contains("hit_sound"))
                            {
                                warn("'hit_sound' is defined but is not supported by ItemsAdder furniture yet.", source, file);
                            }
                            if (blockSounds.contains("step_sound"))
                            {
                                warn("'step_sound' is defined but is not supported by ItemsAdder furniture yet.", source, file);
                            }
                            if (blockSounds.contains("fall_sound"))
                            {
                                warn("'fall_sound' is defined but is not supported by ItemsAdder furniture yet.", source, file);
                            }
                        }
                    }
                })
                // furniture_sit
                .custom((source, dest) -> {
                    if (!source.contains("Mechanics.furniture")) return;

                    ConfigurationSection furniture = source.getConfigurationSection("Mechanics.furniture");
                    if (furniture == null || !furniture.contains("seats")) return;

                    // Warning: Nexo supports multiple seats with specific coordinates,
                    // ItemsAdder only supports a single sit point via sit_height.
                    warn("Nexo 'seats' defined, but ItemsAdder only supports a single sitting point via sit_height. Outcome could be different.", source, file);

                    // TODO: maybe in the future implement precise seats locations also in ItemsAdder.

                    ConfigurationSection behaviours = dest.contains("behaviours")
                            ? dest.getConfigurationSection("behaviours")
                            : dest.createSection("behaviours");

                    ConfigurationSection sit = behaviours.createSection("furniture_sit");

                    // Try to extract Y coordinate from first seat if present
                    List<String> seats = furniture.getStringList("seats");
                    if (!seats.isEmpty())
                    {
                        String[] coords = seats.get(0).split(",");
                        if (coords.length == 3)
                        {
                            try
                            {
                                double y = Double.parseDouble(coords[1].trim());
                                sit.set("sit_height", y);
                            }
                            catch (NumberFormatException e)
                            {
                                sit.set("sit_height", 0.5); // fallback
                            }
                        }
                    }

                    // Default options
                    sit.set("opposite_direction", false);
                    sit.set("sit_all_solid_blocks", true);
                })

                // ItemsAdder uses the same property for both food and consumable
                .custom((source, dest) -> {
                    boolean hasFood = source.contains("food");
                    boolean hasConsumable = source.contains("consumable");

                    if (!hasFood && !hasConsumable) return;

                    ConfigurationSection consumable = dest.createSection("consumable");

                    // Part 1: Map food values
                    if (hasFood)
                    {
                        ConfigurationSection food = source.getConfigurationSection("food");
                        if (food != null)
                        {
                            consumable.set("nutrition", food.getInt("nutrition", 1));
                            consumable.set("saturation", food.getInt("saturation", 0));
                            consumable.set("can_always_eat", food.getBoolean("can_always_eat", false));
                        }
                    }

                    // Part 2: Map consumable base values
                    if (hasConsumable)
                    {
                        ConfigurationSection sourceConsumable = source.getConfigurationSection("consumable");
                        if (sourceConsumable != null)
                        {
                            consumable.set("sound", sourceConsumable.getString("sound", "entity.generic.eat"));
                            consumable.set("particles", sourceConsumable.getBoolean("consume_particles", true));
                            consumable.set("consume_seconds", sourceConsumable.getDouble("consume_seconds", 1.6));

                            String animation = sourceConsumable.getString("animation", "EAT").toLowerCase();
                            consumable.set("animation", switch (animation)
                            {
                                case "block" -> "block";
                                case "drink" -> "drink";
                                default -> "eat";
                            });

                            if (sourceConsumable.contains("effects"))
                            {
                                ConfigurationSection effectsSection = sourceConsumable.getConfigurationSection("effects");
                                if (effectsSection == null)
                                    return;

                                ConfigurationSection effectsDest = consumable.createSection("effects");

                                // APPLY_EFFECTS
                                if (effectsSection.contains("APPLY_EFFECTS"))
                                {
                                    ConfigurationSection applyEffects = effectsSection.getConfigurationSection("APPLY_EFFECTS");
                                    if (applyEffects != null)
                                    {
                                        ConfigurationSection applyStatusEffects = effectsDest.createSection("apply_status_effects");
                                        ConfigurationSection effectList = applyStatusEffects.createSection("effects");

                                        int id = 1;
                                        for (String effectName : applyEffects.getKeys(false))
                                        {
                                            ConfigurationSection singleEffect = applyEffects.getConfigurationSection(effectName);
                                            if (singleEffect != null)
                                            {
                                                ConfigurationSection effectEntry = effectList.createSection("effect_" + id);
                                                effectEntry.set("potion", effectName.toUpperCase());
                                                effectEntry.set("duration", singleEffect.getInt("duration", 20));
                                                effectEntry.set("amplifier", singleEffect.getInt("amplifier", 0));
                                                effectEntry.set("ambient", singleEffect.getBoolean("ambient", false));
                                                effectEntry.set("particles", singleEffect.getBoolean("particles", true));
                                                effectEntry.set("icon", singleEffect.getBoolean("icon", true));
                                                id++;
                                            }
                                        }

                                        applyStatusEffects.set("probability", 1); // Always 1 for now
                                    }
                                }

                                // REMOVE_EFFECTS
                                if (effectsSection.contains("REMOVE_EFFECTS"))
                                {
                                    List<String> removeEffects = effectsSection.getStringList("REMOVE_EFFECTS");
                                    if (!removeEffects.isEmpty())
                                    {
                                        ConfigurationSection removeEffectsSection = effectsDest.createSection("remove_status_effects");
                                        removeEffectsSection.set("effects", removeEffects.stream()
                                                .map(String::toUpperCase)
                                                .toList());
                                    }
                                }

                                // CLEAR_ALL_EFFECTS
                                if (effectsSection.contains("CLEAR_ALL_EFFECTS"))
                                {
                                    // ItemsAdder uses a boolean: effects.clear_status_effects = true
                                    effectsDest.set("clear_status_effects", true);
                                }

                                // TELEPORT_RANDOMLY
                                if (effectsSection.contains("TELEPORT_RANDOMLY"))
                                {
                                    ConfigurationSection teleport = effectsSection.getConfigurationSection("TELEPORT_RANDOMLY");
                                    if (teleport != null)
                                    {
                                        ConfigurationSection teleportDest = effectsDest.createSection("teleport_randomly");
                                        teleportDest.set("diameter", teleport.getDouble("diameter", 5.0));
                                    }
                                }

                                // PLAY_SOUND
                                if (effectsSection.contains("PLAY_SOUND"))
                                {
                                    ConfigurationSection playSound = effectsSection.getConfigurationSection("PLAY_SOUND");
                                    if (playSound != null)
                                    {
                                        ConfigurationSection soundDest = effectsDest.createSection("play_sound");
                                        soundDest.set("sound", playSound.getString("sound", "entity.generic.eat"));
                                    }
                                }
                            }
                        }
                    }
                })
                // use_cooldown
                .custom((source, dest) -> {
                    if (!source.contains("use_cooldown")) return;

                    ConfigurationSection useCooldown = source.getConfigurationSection("use_cooldown");
                    if (useCooldown == null) return;

                    ConfigurationSection eventsSettings = dest.createSection("events_settings");
                    ConfigurationSection cooldown = eventsSettings.createSection("cooldown");

                    double seconds = useCooldown.getDouble("seconds", 1.0);

                    // In ItemsAdder, the cooldown is always enforced server-side to prevent client hacking.
                    // Also, the cooldown group is automatically managed by using the item's ID as the default group identifier.
                    // Users do not need to manually specify the cooldown group.
                    cooldown.set("indicator", "VANILLA"); // Always show vanilla cooldown animation
                    cooldown.set("ticks", (int) (seconds * 20)); // Convert seconds to ticks (1s = 20 ticks)
                })
                // color
                .custom((source, dest) -> {
                    if (!source.contains("color")) return;

                    Object colorObj = source.get("color");
                    if (!(colorObj instanceof String) && !(colorObj instanceof List<?>)) return;

                    // Default: fallback to string split or list
                    int[] rgb = new int[3];
                    try
                    {
                        if (colorObj instanceof String str)
                        {
                            String[] parts = str.split(",");
                            if (parts.length == 3)
                            {
                                rgb[0] = Integer.parseInt(parts[0].trim());
                                rgb[1] = Integer.parseInt(parts[1].trim());
                                rgb[2] = Integer.parseInt(parts[2].trim());
                            }
                        }
                        else
                        {
                            List<?> list = (List<?>) colorObj;
                            if (list.size() == 3)
                            {
                                rgb[0] = ((Number) list.get(0)).intValue();
                                rgb[1] = ((Number) list.get(1)).intValue();
                                rgb[2] = ((Number) list.get(2)).intValue();
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        warn("Invalid RGB format in 'color'", source, file);
                        return;
                    }

                    String hexColor = String.format("%02x%02x%02x", rgb[0], rgb[1], rgb[2]);

                    String material = source.getString("material", "PAPER").toUpperCase(Locale.ROOT);

                    ConfigurationSection specific = dest.contains("specific_properties")
                            ? dest.getConfigurationSection("specific_properties")
                            : dest.createSection("specific_properties");

                    switch (material)
                    {
                        case "LEATHER_HELMET", "LEATHER_CHESTPLATE", "LEATHER_LEGGINGS", "LEATHER_BOOTS" ->
                        {
                            ConfigurationSection armor = specific.contains("armor")
                                    ? specific.getConfigurationSection("armor")
                                    : specific.createSection("armor");
                            armor.set("slot", switch (material)
                            {
                                case "LEATHER_HELMET" -> "HEAD";
                                case "LEATHER_CHESTPLATE" -> "CHEST";
                                case "LEATHER_LEGGINGS" -> "LEGS";
                                case "LEATHER_BOOTS" -> "FEET";
                                default -> "HEAD"; // fallback
                            });
                            armor.set("color", hexColor);
                        }
                        case "LEATHER_HORSE_ARMOR" ->
                        {
                            ConfigurationSection horse = specific.createSection("leather_horse_armor");
                            horse.set("color", hexColor);
                        }
                        case "POTION", "SPLASH_POTION", "LINGERING_POTION", "TIPPED_ARROW" ->
                        {
                            ConfigurationSection potion = specific.createSection("potion");
                            potion.set("color", hexColor);
                        }
                        default -> warn("Color defined but material '" + material + "' is not supported for coloring in ItemsAdder", source, file);
                    }
                })
                // furniture lights
                .custom((source, dest) -> {
                    if (!source.contains("Mechanics.furniture")) return;

                    ConfigurationSection furniture = source.getConfigurationSection("Mechanics.furniture");
                    if (furniture == null || !furniture.contains("lights")) return;

                    ConfigurationSection behaviours = dest.isConfigurationSection("behaviours")
                            ? dest.getConfigurationSection("behaviours")
                            : dest.createSection("behaviours");

                    ConfigurationSection iaFurniture = behaviours.isConfigurationSection("furniture")
                            ? behaviours.getConfigurationSection("furniture")
                            : behaviours.createSection("furniture");

                    List<String> lights = furniture.getStringList("lights");
                    if (!lights.isEmpty())
                    {
                        // Get the highest light level from defined lights
                        int maxLevel = getMaxLightLevel(lights);
                        iaFurniture.set("light_level", maxLevel);
                    }

                    // Warn about unsupported toggling system
                    if (furniture.contains("toggleable"))
                        warn("'toggleable' is not supported by ItemsAdder furniture.", source, file);
                    if (furniture.contains("toggled_model"))
                        warn("'toggled_model' is not supported by ItemsAdder furniture.", source, file);
                    if (furniture.contains("toggled_item_model"))
                        warn("'toggled_item_model' is not supported by ItemsAdder furniture.", source, file);
                })
                // potion effects
                .custom((source, dest) -> {
                    if (!source.contains("PotionEffects")) return;

                    List<Map<?, ?>> effects = (List<Map<?, ?>>) source.getList("PotionEffects");
                    if (effects == null || effects.isEmpty()) return;

                    ConfigurationSection specific = dest.isConfigurationSection("specific_properties")
                            ? dest.getConfigurationSection("specific_properties")
                            : dest.createSection("specific_properties");

                    ConfigurationSection potion = specific.isConfigurationSection("potion")
                            ? specific.getConfigurationSection("potion")
                            : specific.createSection("potion");

                    ConfigurationSection potionEffects = potion.createSection("effects");

                    int i = 1;
                    for (Map<?, ?> effect : effects)
                    {
                        String type = (String) effect.get("type");
                        if (type == null) continue;

                        ConfigurationSection effectEntry = potionEffects.createSection("effect_" + i++);
                        effectEntry.set("type", type.toUpperCase());
                        if(effect.containsKey("duration"))
                            effectEntry.set("duration", effect.get("duration"));
                        if(effect.containsKey("amplifier"))
                            effectEntry.set("amplifier", effect.get("amplifier"));
                        if(effect.containsKey("ambient"))
                            effectEntry.set("ambient", effect.get("ambient"));

                        if (effect.containsKey("particles"))
                            warn("'particles' property in PotionEffects is ignored in ItemsAdder.", source, file);
                        if (effect.containsKey("icon"))
                            warn("'icon' property in PotionEffects is ignored in ItemsAdder.", source, file);
                    }
                })
                ;

        mapping.apply(config, outputConfig.createSection("items"));
    }

    private static int getMaxLightLevel(List<String> lights)
    {
        int maxLevel = 0;
        for (String entry : lights)
        {
            String[] parts = entry.split("\\s+");
            if (parts.length == 4)
            {
                try
                {
                    int level = Integer.parseInt(parts[3].trim());
                    maxLevel = Math.max(maxLevel, level);
                }
                catch (NumberFormatException ignored) {}
            }
        }
        return maxLevel;
    }

    // TODO: implement converter logic that can convert ingame items and placed furniture.
}
