package service;

import dao.InventoryDAO;
import dao.InventoryDAOImpl;
import model.FoodItem;
import model.NonPerishableItem;
import model.PerishableItem;
import util.FileManager;
import util.InventoryException;
import util.Validation;

import java.time.LocalDate;
import java.util.List;

public class InventoryService {
    private final InventoryDAO dao;

    public InventoryService() {
        this.dao = new InventoryDAOImpl();
    }

    public boolean addItem(String name, String category, int qty, String batch,
                           LocalDate mfg, LocalDate exp, String loc, boolean isPerishable)
                           throws InventoryException {

        if (!Validation.isValidName(name)) throw new InventoryException("Invalid name.");
        if (!Validation.isValidQuantity(qty)) throw new InventoryException("Quantity must be > 0.");
        if (!Validation.isValidBatch(batch)) throw new InventoryException("Invalid batch number.");
        if (exp == null) throw new InventoryException("Expiry date required.");

        FoodItem item;
        if (isPerishable) {
            item = new PerishableItem(name, category, qty, batch, mfg, exp, loc);
        } else {
            item = new NonPerishableItem(name, category, qty, batch, mfg, exp, loc);
        }

        boolean ok = dao.addItem(item);
        if (ok) FileManager.log("ADDED: " + name + " (Exp: " + exp + ")");
        return ok;
    }

    public List<FoodItem> viewAllSortedByExpiry() throws InventoryException {
        return dao.getAllItemsSortedByExpiry();
    }

    public List<FoodItem> viewExpiringSoon(int days) throws InventoryException {
        return dao.getExpiringItems(days);
    }

    public List<FoodItem> search(String keyword, String type) throws InventoryException {
        switch (type.toLowerCase()) {
            case "name":     return dao.searchByName(keyword);
            case "category": return dao.searchByCategory(keyword);
            case "batch":    return dao.searchByBatch(keyword);
            default: throw new InventoryException("Unknown search type.");
        }
    }

    public List<FoodItem> searchByExpiryRange(LocalDate from, LocalDate to) throws InventoryException {
        return dao.searchByExpiryRange(from, to);
    }

    public FoodItem getItem(int id) throws InventoryException {
        return dao.getItemById(id);
    }

    public boolean updateItem(FoodItem item) throws InventoryException {
        boolean ok = dao.updateItem(item);
        if (ok) FileManager.log("UPDATED: ID " + item.getId() + " - " + item.getName());
        return ok;
    }

    public boolean updateQuantity(int id, int qty) throws InventoryException {
        if (!Validation.isValidQuantity(qty)) throw new InventoryException("Invalid quantity.");
        return dao.updateQuantity(id, qty);
    }

    public boolean deleteItem(int id) throws InventoryException {
        boolean ok = dao.deleteItem(id);
        if (ok) FileManager.log("DELETED: ID " + id);
        return ok;
    }

    public int deleteAllExpired() throws InventoryException {
        int n = dao.deleteExpiredItems();
        FileManager.log("BULK DELETE: " + n + " items.");
        return n;
    }
}