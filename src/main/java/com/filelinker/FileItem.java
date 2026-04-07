package com.filelinker;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class FileItem {
    private final File file;
    private final boolean isDirectory;
    private final String name;
    private String sizeFormatted;
    private String dateFormatted;
    
    private boolean linked = false;
    private String linkColor = "";

    public FileItem(File file) {
        this.file = file;
        this.isDirectory = file.isDirectory();
        this.name = file.getName();
        
        updateInfo();
    }

    public void updateInfo() {
        try {
            BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            if (!this.isDirectory) {
                long sizeBytes = attr.size();
                sizeFormatted = formatSize(sizeBytes);
            } else {
                sizeFormatted = "";
            }
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            dateFormatted = formatter.format(attr.lastModifiedTime().toInstant().atZone(ZoneId.systemDefault()));
        } catch (Exception e) {
            sizeFormatted = "Unknown";
            dateFormatted = "Unknown";
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int z = (63 - Long.numberOfLeadingZeros(bytes)) / 10;
        return String.format("%.1f %sB", (double)bytes / (1L << (z * 10)), " KMGTPE".charAt(z));
    }

    public File getFile() { return file; }
    public boolean isDirectory() { return isDirectory; }
    public String getName() { return name; }
    public String getSizeFormatted() { return sizeFormatted; }
    public String getDateFormatted() { return dateFormatted; }

    public boolean isLinked() { return linked; }
    public void setLinked(boolean linked) { this.linked = linked; }

    public String getLinkColor() { return linkColor; }
    public void setLinkColor(String linkColor) { this.linkColor = linkColor; }
}
