package dao;

import model.FoodItem;
import util.InventoryException;

import java.time.LocalDate;
import java.util.List;

public interface InventoryDAO {
    // CREATE
    boolean addItem(FoodItem item) throws InventoryException;

    // READ
    List<FoodItem> getAllItems() throws InventoryException;
    List<FoodItem> getAllItemsSortedByExpiry() throws InventoryException;
    List<FoodItem> getExpiringItems(int days) throws InventoryException;
    FoodItem getItemById(int id) throws InventoryException;

    // SEARCH
    List<FoodItem> searchByName(String name) throws InventoryException;
    List<FoodItem> searchByCategory(String category) throws InventoryException;
    List<FoodItem> searchByBatch(String batch) throws InventoryException;
    List<FoodItem> searchByExpiryRange(LocalDate from, LocalDate to) throws InventoryException;

    // UPDATE
    boolean updateItem(FoodItem item) throws InventoryException;
    boolean updateQuantity(int id, int newQty) throws InventoryException;

    // DELETE
    boolean deleteItem(int id) throws InventoryException;
    int deleteExpiredItems() throws InventoryException;
}