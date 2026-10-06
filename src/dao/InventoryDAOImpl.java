package dao;

import model.FoodItem;
import model.NonPerishableItem;
import model.PerishableItem;
import util.DBConnection;
import util.InventoryException;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class InventoryDAOImpl implements InventoryDAO {

    // -------- CREATE --------
    @Override
    public boolean addItem(FoodItem item) throws InventoryException {
        String sql = "INSERT INTO food_items (name, category, item_type, quantity, batch_number, " +
                     "manufacturing_date, expiry_date, storage_location) VALUES (?,?,?,?,?,?,?,?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, item.getName());
            ps.setString(2, item.getCategory());
            ps.setString(3, item.getItemType());
            ps.setInt(4, item.getQuantity());
            ps.setString(5, item.getBatchNumber());
            ps.setDate(6, item.getManufacturingDate() != null ? Date.valueOf(item.getManufacturingDate()) : null);
            ps.setDate(7, Date.valueOf(item.getExpiryDate()));
            ps.setString(8, item.getStorageLocation());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new InventoryException("Add failed: " + e.getMessage(), e);
        }
    }

    // -------- READ --------
    @Override
    public List<FoodItem> getAllItems() throws InventoryException {
        return query("SELECT * FROM food_items ORDER BY id ASC");
    }

    @Override
    public List<FoodItem> getAllItemsSortedByExpiry() throws InventoryException {
        return query("SELECT * FROM food_items ORDER BY expiry_date ASC");
    }

    @Override
    public List<FoodItem> getExpiringItems(int days) throws InventoryException {
        String sql = "SELECT * FROM food_items WHERE expiry_date BETWEEN CURDATE() " +
                     "AND DATE_ADD(CURDATE(), INTERVAL ? DAY) ORDER BY expiry_date ASC";
        List<FoodItem> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, days);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new InventoryException("Fetch failed: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public FoodItem getItemById(int id) throws InventoryException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM food_items WHERE id=?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return map(rs);
        } catch (SQLException e) {
            throw new InventoryException("Fetch failed: " + e.getMessage(), e);
        }
        return null;
    }

    // -------- SEARCH --------
    @Override
    public List<FoodItem> searchByName(String name) throws InventoryException {
        return searchField("name", name);
    }

    @Override
    public List<FoodItem> searchByCategory(String category) throws InventoryException {
        return searchField("category", category);
    }

    @Override
    public List<FoodItem> searchByBatch(String batch) throws InventoryException {
        return searchField("batch_number", batch);
    }

    @Override
    public List<FoodItem> searchByExpiryRange(LocalDate from, LocalDate to) throws InventoryException {
        String sql = "SELECT * FROM food_items WHERE expiry_date BETWEEN ? AND ? ORDER BY expiry_date ASC";
        List<FoodItem> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new InventoryException("Search failed: " + e.getMessage(), e);
        }
        return list;
    }

    private List<FoodItem> searchField(String column, String value) throws InventoryException {
        String sql = "SELECT * FROM food_items WHERE " + column + " LIKE ? ORDER BY expiry_date ASC";
        List<FoodItem> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "%" + value + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new InventoryException("Search failed: " + e.getMessage(), e);
        }
        return list;
    }

    // -------- UPDATE --------
    @Override
    public boolean updateItem(FoodItem item) throws InventoryException {
        String sql = "UPDATE food_items SET name=?, category=?, quantity=?, batch_number=?, " +
                     "manufacturing_date=?, expiry_date=?, storage_location=? WHERE id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, item.getName());
            ps.setString(2, item.getCategory());
            ps.setInt(3, item.getQuantity());
            ps.setString(4, item.getBatchNumber());
            ps.setDate(5, item.getManufacturingDate() != null ? Date.valueOf(item.getManufacturingDate()) : null);
            ps.setDate(6, Date.valueOf(item.getExpiryDate()));
            ps.setString(7, item.getStorageLocation());
            ps.setInt(8, item.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new InventoryException("Update failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean updateQuantity(int id, int newQty) throws InventoryException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE food_items SET quantity=? WHERE id=?")) {
            ps.setInt(1, newQty);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new InventoryException("Update failed: " + e.getMessage(), e);
        }
    }

    // -------- DELETE --------
    @Override
    public boolean deleteItem(int id) throws InventoryException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM food_items WHERE id=?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new InventoryException("Delete failed: " + e.getMessage(), e);
        }
    }

    @Override
    public int deleteExpiredItems() throws InventoryException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM food_items WHERE expiry_date < CURDATE()")) {
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new InventoryException("Bulk delete failed: " + e.getMessage(), e);
        }
    }

    // -------- HELPERS --------
    private List<FoodItem> query(String sql) throws InventoryException {
        List<FoodItem> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new InventoryException("Query failed: " + e.getMessage(), e);
        }
        return list;
    }

    private FoodItem map(ResultSet rs) throws SQLException {
        String type = rs.getString("item_type");
        int id = rs.getInt("id");
        String name = rs.getString("name");
        String cat = rs.getString("category");
        int qty = rs.getInt("quantity");
        String batch = rs.getString("batch_number");
        Date mfgD = rs.getDate("manufacturing_date");
        Date expD = rs.getDate("expiry_date");
        String loc = rs.getString("storage_location");

        LocalDate mfg = mfgD != null ? mfgD.toLocalDate() : null;
        LocalDate exp = expD.toLocalDate();

        if ("NON_PERISHABLE".equalsIgnoreCase(type)) {
            return new NonPerishableItem(id, name, cat, qty, batch, mfg, exp, loc);
        }
        return new PerishableItem(id, name, cat, qty, batch, mfg, exp, loc);
    }
}