package com.example.guestme;

public class HostModel {
    private String id;
    private String fullName;
    private String description;
    private String city;
    private String country;
    private String photoUrl;
    private double matchPercentage;

    public HostModel(String id, String fullName, String description, String city, 
            String country, String photoUrl, double matchPercentage) {
        this.id = id;
        this.fullName = fullName;
        this.description = description;
        this.city = city;
        this.country = country;
        this.photoUrl = photoUrl;
        this.matchPercentage = matchPercentage;
    }

    // Getters
    public String getId() { return id; }
    public String getFullName() { return fullName; }
    public String getDescription() { return description; }
    public String getCity() { return city; }
    public String getCountry() { return country; }
    public String getPhotoUrl() { return photoUrl; }
    public double getMatchPercentage() { return matchPercentage; }
} 