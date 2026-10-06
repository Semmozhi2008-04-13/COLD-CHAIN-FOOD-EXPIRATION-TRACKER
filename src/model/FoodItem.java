package model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public abstract class FoodItem {
    private int id;
    private String name;
    private String category;
    private int quantity;
    private String batchNumber;
    private LocalDate manufacturingDate;
    private LocalDate expiryDate;
    private String storageLocation;

    // Constructor for NEW items (no ID)
    public FoodItem(String name, String category, int quantity, String batchNumber,
                    LocalDate manufacturingDate, LocalDate expiryDate, String storageLocation) {
        this.name = name;
        this.category = category;
        this.quantity = quantity;
        this.batchNumber = batchNumber;
        this.manufacturingDate = manufacturingDate;
        this.expiryDate = expiryDate;
        this.storageLocation = storageLocation;
    }

    // Constructor for items FROM database (has ID)
    public FoodItem(int id, String name, String category, int quantity, String batchNumber,
                    LocalDate manufacturingDate, LocalDate expiryDate, String storageLocation) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.quantity = quantity;
        this.batchNumber = batchNumber;
        this.manufacturingDate = manufacturingDate;
        this.expiryDate = expiryDate;
        this.storageLocation = storageLocation;
    }

    // ABSTRACT METHOD - subclasses MUST implement
    public abstract String getItemType();

    public boolean isExpired() {
        return LocalDate.now().isAfter(expiryDate);
    }

    public long daysUntilExpiry() {
        return ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
    }

    public boolean isExpiringSoon() {
        long d = daysUntilExpiry();
        return d >= 0 && d <= 2;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String c) { this.category = c; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int q) { this.quantity = q; }
    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String b) { this.batchNumber = b; }
    public LocalDate getManufacturingDate() { return manufacturingDate; }
    public void setManufacturingDate(LocalDate d) { this.manufacturingDate = d; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate d) { this.expiryDate = d; }
    public String getStorageLocation() { return storageLocation; }
    public void setStorageLocation(String s) { this.storageLocation = s; }

    @Override
    public String toString() {
        return String.format("%-4d | %-18s | %-14s | %-5d | %-10s | %-12s | %s",
                id, name, category, quantity, batchNumber, expiryDate, storageLocation);
    }
}