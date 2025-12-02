package com.astrantiabooks.models;
import java.io.Serializable;

public class Buku implements Serializable {
    private String id;
    private String title;
    private String author;
    private String category;
    private String description;
    private String coverUrl;
    private String searchKey;

    public Buku() {}

    public Buku(String id, String title, String author, String category, String description, String coverUrl) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.category = category;
        this.description = description;
        this.coverUrl = coverUrl;
        this.searchKey = title.toLowerCase();
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public String getCoverUrl() { return coverUrl; }

    public void setTitle(String title) { this.title = title; }
    public void setAuthor(String author) { this.author = author; }
    public void setCategory(String category) { this.category = category; }
    public void setDescription(String description) { this.description = description; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
}