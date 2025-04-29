package com.itemsadder.converter;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Command implements CommandExecutor, TabCompleter
{
    public void register()
    {
        PluginCommand command = Main.inst().getCommand("iaconvert");
        assert command != null;
        command.setExecutor(this);
        command.setTabCompleter(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, org.bukkit.command.@NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args)
    {
        if(args.length == 0)
        {
            commandSender.sendMessage(ChatColor.RED + "Please specify a folder name.");
            return false;
        }

        new NexoConverter(args[0]).convert();
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, org.bukkit.command.@NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args)
    {
        if(args.length == 1)
            return NexoConverter.getFoldersNames(args[0]);
        return List.of();
    }
}