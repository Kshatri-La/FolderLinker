package com.filelinker;

import java.util.HashMap;
import java.util.Map;

public class LinkManager {
    // Map Folder Name -> Hex Color String
    private final Map<String, String> linkedFolders = new HashMap<>();
    
    // List of predefined colors for linking (mostly attractive blues/cyans and some random ones)
    private final String[] colors = {
        "#007BFF", // Blue
        "#17A2B8", // Cyan/Teal
        "#28A745", // Green
        "#6610F2", // Purple
        "#E83E8C", // Pink
        "#FD7E14", // Orange
        "#20B2AA", // LightSeaGreen
        "#4169E1", // RoyalBlue
    };
    private int colorIndex = 0;

    private static LinkManager instance;

    private LinkManager() {}

    public static LinkManager getInstance() {
        if (instance == null) {
            instance = new LinkManager();
        }
        return instance;
    }

    public void toggleLink(String folderName) {
        if (linkedFolders.containsKey(folderName)) {
            linkedFolders.remove(folderName);
        } else {
            linkedFolders.put(folderName, colors[colorIndex % colors.length]);
            colorIndex++;
        }
    }

    public boolean isLinked(String folderName) {
        return linkedFolders.containsKey(folderName);
    }

    public String getColor(String folderName) {
        return linkedFolders.get(folderName);
    }
}
