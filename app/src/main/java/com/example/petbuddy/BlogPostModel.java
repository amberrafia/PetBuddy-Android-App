package com.example.petbuddy;

import java.io.Serializable;

public class BlogPostModel implements Serializable {
    private String postId;
    private String title;
    private String content;
    private String author;
    private String category;
    private String imageUrl;
    private long timestamp;
    private long lastModified;
    private int likes;
    private int comments;
    private boolean isPublished;

    public BlogPostModel() {
        // Default constructor required for Firebase
    }

    public BlogPostModel(String title, String content, String author, String category, String imageUrl) {
        this.title = title;
        this.content = content;
        this.author = author;
        this.category = category;
        this.imageUrl = imageUrl;
        this.timestamp = System.currentTimeMillis();
        this.lastModified = System.currentTimeMillis();
        this.likes = 0;
        this.comments = 0;
        this.isPublished = true;
    }

    // Getters and Setters
    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public long getLastModified() { return lastModified; }
    public void setLastModified(long lastModified) { this.lastModified = lastModified; }

    public int getLikes() { return likes; }
    public void setLikes(int likes) { this.likes = likes; }

    public int getComments() { return comments; }
    public void setComments(int comments) { this.comments = comments; }

    public boolean isPublished() { return isPublished; }
    public void setPublished(boolean published) { isPublished = published; }

    public String getCategoryEmoji() {
        switch (category.toLowerCase()) {
            case "pet care tips": return "🐾";
            case "pet stories": return "📖";
            case "community news": return "📰";
            case "pet health": return "🏥";
            case "training tips": return "🎓";
            default: return "💬";
        }
    }
}