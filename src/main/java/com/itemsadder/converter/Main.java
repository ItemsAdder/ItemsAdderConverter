package com.itemsadder.converter;

import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin
{
    private static Main instance;

    public static Main inst()
    {
        return instance;
    }

    @Override
    public void onEnable()
    {
        instance = this;
    }
}
