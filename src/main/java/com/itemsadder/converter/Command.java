package com.itemsadder.converter;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
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
    public boolean onCommand(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args)
    {
        String folder = null;
        boolean toIaContents = false;

        for (int i = 0; i < args.length; i++)
        {
            switch (args[i])
            {
                case "--folder" ->
                {
                    if (i + 1 < args.length)
                    {
                        folder = args[i + 1];
                        i++; // skip next
                    }
                }
                case "--to-ia-contents" -> toIaContents = true;
            }
        }

        if (folder == null)
        {
            sender.sendMessage(ChatColor.RED + "Please specify a folder name using --folder <name>.");
            return false;
        }

        if (folder.startsWith("nexo"))
        {
            new NexoConverter(folder, toIaContents).convert();
        }
        else if (folder.startsWith("oraxen"))
        {
            new OraxenConverter(folder, toIaContents).convert();
        }
        else
        {
            sender.sendMessage(ChatColor.RED + "Invalid type. Use 'nexo' or 'oraxen'.");
            return false;
        }

        if(toIaContents)
            sender.sendMessage(ChatColor.GREEN + "Conversion completed. Saved into ItemsAdder/contents folder.");
        else
            sender.sendMessage(ChatColor.GREEN + "Conversion completed. Saved into ItemsAdderConverter/converted folder.");

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args)
    {
        List<String> completions = new ArrayList<>();

        boolean hasFolderFlag = false;
        boolean expectingFolderValue = false;
        boolean hasToIaContents = false;

        for (int i = 0; i < args.length; i++)
        {
            String arg = args[i];
            if (arg.equals("--folder"))
            {
                hasFolderFlag = true;
                if (i == args.length - 2) expectingFolderValue = true; // if next is being typed
            }
            if (arg.equals("--to-ia-contents")) hasToIaContents = true;
        }

        String lastArg = args[args.length - 1];

        if (expectingFolderValue)
        {
            completions.addAll(NexoConverter.getFoldersNames(lastArg));
            completions.addAll(OraxenConverter.getFoldersNames(lastArg));
        }
        else
        {
            if (!hasFolderFlag && "--folder".startsWith(lastArg)) completions.add("--folder");
            if (!hasToIaContents && "--to-ia-contents".startsWith(lastArg)) completions.add("--to-ia-contents");
        }

        return completions;
    }
}