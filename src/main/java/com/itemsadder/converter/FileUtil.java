package com.itemsadder.converter;

import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Set;

public class FileUtil
{
    /**
     * Copies the contents of a directory into another, skipping files with specific extensions.
     *
     * @param sourceDir        the directory to copy from (not copied itself, only its contents)
     * @param targetDir        the destination directory
     * @param ignoredExtensions a set of extensions to skip (e.g., Set.of("zip", "jar"))
     * @throws IOException if something fails during copy
     */
    public static void copyDirectoryContentsSkippingExtensions(File sourceDir, File targetDir, Set<String> ignoredExtensions) throws IOException
    {
        if (!sourceDir.isDirectory()) throw new IllegalArgumentException("Source is not a directory");

        if (!targetDir.exists()) FileUtils.forceMkdir(targetDir);

        for (File file : sourceDir.listFiles())
        {
            if (file.isDirectory())
            {
                File dest = new File(targetDir, file.getName());
                if (file.listFiles().length > 0) // Skip empty directories
                {
                    copyDirectoryContentsSkippingExtensions(file, dest, ignoredExtensions);
                }
            }
            else
            {
                String name = file.getName().toLowerCase();
                boolean skip = ignoredExtensions.stream().anyMatch(ext -> name.endsWith("." + ext.toLowerCase()));
                if (skip) continue;

                File dest = new File(targetDir, file.getName());
                FileUtils.copyFile(file, dest);
            }
        }
    }
}
