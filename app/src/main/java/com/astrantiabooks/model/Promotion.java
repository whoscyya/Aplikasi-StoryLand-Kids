package com.astrantiabooks.models;

public class Promotion {
    private String id;
    private String title;
    private String subtitle;
    private String imageUrl;
    private String targetBookId; // ID Buku yang dibuka saat tombol diklik

    public Promotion() {}

    public Promotion(String id, String title, String subtitle, String imageUrl, String targetBookId) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.imageUrl = imageUrl;
        this.targetBookId = targetBookId;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public String getImageUrl() { return imageUrl; }
    public String getTargetBookId() { return targetBookId; }
}