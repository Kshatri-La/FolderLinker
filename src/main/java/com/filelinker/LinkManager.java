// A singleton tracking which specific folder names are globally designated as 'Linked' and their assigned colors.
package com.filelinker;

import java.util.HashMap;
import java.util.Map;

public class LinkManager {
    private final Map<String, String> linkedFolders = new HashMap<>();
    
    private final String[] colors = {
        "#007BFF",
        "#17A2B8",
        "#28A745",
        "#6610F2",
        "#E83E8C",
        "#FD7E14",
        "#20B2AA",
        "#4169E1"
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
