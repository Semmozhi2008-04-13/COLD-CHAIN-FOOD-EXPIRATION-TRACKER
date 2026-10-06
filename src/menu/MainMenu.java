package menu;

import model.FoodItem;
import service.InventoryService;
import util.ExpiryAlertThread;
import util.FileManager;
import util.InventoryException;
import util.Validation;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

public class MainMenu {
    private final Scanner sc = new Scanner(System.in);
    private final InventoryService service = new InventoryService();
    private ExpiryAlertThread alertThread;

    public void start() {
        alertThread = new ExpiryAlertThread(service, 30);
        alertThread.start();

        System.out.println("==========================================");
        System.out.println("   🍎 FRESHFLOW - Food Expiration Tracker");
        System.out.println("==========================================");

        boolean exit = false;
        while (!exit) {
            printMenu();
            int choice = readInt("Choose an option: ");
            switch (choice) {
                case 1: addItem(); break;
                case 2: viewSorted(); break;
                case 3: searchMenu(); break;
                case 4: updateMenu(); break;
                case 5: deleteMenu(); break;
                case 6: viewExpiringSoon(); break;
                case 7: generateReport(); break;
                case 8: deleteAllExpired(); break;
                case 9: exit = true; System.out.println("👋 Goodbye!"); break;
                default: System.out.println("⚠️ Invalid option.");
            }
        }
        alertThread.stopThread();
        sc.close();
    }

    private void printMenu() {
        System.out.println("\n---------- MAIN MENU ----------");
        System.out.println("1. ➕ Add New Item");
        System.out.println("2. 📋 View All (Expiring First)");
        System.out.println("3. 🔍 Search Items");
        System.out.println("4. ✏️  Update Item");
        System.out.println("5. ❌ Delete Item");
        System.out.println("6. ⏰ View Expiring Soon");
        System.out.println("7. 📊 Generate Report (CSV)");
        System.out.println("8. 🗑️  Delete All Expired");
        System.out.println("9. 🚪 Exit");
    }

    private void addItem() {
        try {
            System.out.println("\n--- ADD NEW ITEM ---");
            String name = readString("Name: ");
            String cat = readString("Category: ");
            int qty = readInt("Quantity: ");
            String batch = readString("Batch Number: ");
            String mfgStr = readString("Manufacturing Date (yyyy-MM-dd, Enter to skip): ");
            LocalDate mfg = mfgStr.isEmpty() ? null : Validation.parseDate(mfgStr);
            LocalDate exp = Validation.parseDate(readString("Expiry Date (yyyy-MM-dd): "));
            String loc = readString("Storage Location: ");
            String type = readString("Is it Perishable? (yes/no): ");
            boolean isPerishable = type.equalsIgnoreCase("yes") || type.equalsIgnoreCase("y");

            if (service.addItem(name, cat, qty, batch, mfg, exp, loc, isPerishable)) {
                System.out.println("✅ Item added successfully!");
            } else {
                System.out.println("❌ Failed to add.");
            }
        } catch (DateTimeParseException e) {
            System.out.println("❌ Invalid date. Use yyyy-MM-dd.");
        } catch (InventoryException e) {
            System.out.println("❌ " + e.getMessage());
        }
    }

    private void viewSorted() {
        try { printItems(service.viewAllSortedByExpiry()); }
        catch (InventoryException e) { System.out.println("❌ " + e.getMessage()); }
    }

    private void searchMenu() {
        System.out.println("\n--- SEARCH ---");
        System.out.println("1. By Name");
        System.out.println("2. By Category");
        System.out.println("3. By Batch Number");
        System.out.println("4. By Expiry Date Range");
        int ch = readInt("Choose: ");
        try {
            List<FoodItem> results;
            switch (ch) {
                case 1: results = service.search(readString("Name: "), "name"); break;
                case 2: results = service.search(readString("Category: "), "category"); break;
                case 3: results = service.search(readString("Batch: "), "batch"); break;
                case 4:
                    LocalDate from = Validation.parseDate(readString("From (yyyy-MM-dd): "));
                    LocalDate to = Validation.parseDate(readString("To (yyyy-MM-dd): "));
                    results = service.searchByExpiryRange(from, to);
                    break;
                default: System.out.println("Invalid."); return;
            }
            printItems(results);
        } catch (Exception e) { System.out.println("❌ " + e.getMessage()); }
    }

    private void updateMenu() {
        try {
            int id = readInt("Enter ID to update: ");
            FoodItem item = service.getItem(id);
            if (item == null) { System.out.println("❌ Item not found."); return; }
            System.out.println("Current: " + item);

            String name = readString("New name (Enter to keep): ");
            if (!name.isEmpty()) item.setName(name);
            String qtyStr = readString("New quantity (Enter to keep): ");
            if (!qtyStr.isEmpty()) item.setQuantity(Integer.parseInt(qtyStr));
            String loc = readString("New location (Enter to keep): ");
            if (!loc.isEmpty()) item.setStorageLocation(loc);
            String expStr = readString("New expiry (yyyy-MM-dd, Enter to keep): ");
            if (!expStr.isEmpty()) item.setExpiryDate(Validation.parseDate(expStr));

            if (service.updateItem(item)) System.out.println("✅ Updated!");
            else System.out.println("❌ Failed.");
        } catch (Exception e) { System.out.println("❌ " + e.getMessage()); }
    }

    private void deleteMenu() {
        try {
            int id = readInt("Enter ID to delete: ");
            String confirm = readString("Are you sure? (yes/no): ");
            if (confirm.equalsIgnoreCase("yes")) {
                if (service.deleteItem(id)) System.out.println("✅ Deleted.");
                else System.out.println("❌ Not found.");
            } else System.out.println("Cancelled.");
        } catch (InventoryException e) { System.out.println("❌ " + e.getMessage()); }
    }

    private void viewExpiringSoon() {
        try {
            List<FoodItem> items = service.viewExpiringSoon(2);
            if (items.isEmpty()) System.out.println("✅ No items expiring within 2 days.");
            else { System.out.println("⚠️  EXPIRING SOON:"); printItems(items); }
        } catch (InventoryException e) { System.out.println("❌ " + e.getMessage()); }
    }

    private void generateReport() {
        try {
            List<FoodItem> items = service.viewAllSortedByExpiry();
            StringBuilder sb = new StringBuilder();
            sb.append("ID,Name,Category,Type,Quantity,Batch,Expiry,Location,Status\n");
            for (FoodItem item : items) {
                String status = item.isExpired() ? "EXPIRED" :
                                item.isExpiringSoon() ? "EXPIRING_SOON" : "OK";
                sb.append(item.getId()).append(",")
                  .append(item.getName()).append(",")
                  .append(item.getCategory()).append(",")
                  .append(item.getItemType()).append(",")
                  .append(item.getQuantity()).append(",")
                  .append(item.getBatchNumber()).append(",")
                  .append(item.getExpiryDate()).append(",")
                  .append(item.getStorageLocation()).append(",")
                  .append(status).append("\n");
            }
            String fname = "report_" + LocalDate.now() + ".csv";
            FileManager.writeReport(fname, sb.toString());
            System.out.println("✅ Report saved: data/reports/" + fname);
        } catch (Exception e) { System.out.println("❌ " + e.getMessage()); }
    }

    private void deleteAllExpired() {
        try {
            String confirm = readString("Delete ALL expired items? (yes/no): ");
            if (confirm.equalsIgnoreCase("yes")) {
                int n = service.deleteAllExpired();
                System.out.println("✅ Deleted " + n + " item(s).");
            }
        } catch (InventoryException e) { System.out.println("❌ " + e.getMessage()); }
    }

    private void printItems(List<FoodItem> items) {
        if (items.isEmpty()) { System.out.println("📭 No items found."); return; }
        System.out.println("ID   | Name               | Category       | Qty   | Batch      | Expiry       | Location");
        System.out.println("-------------------------------------------------------------------------------------------");
        for (FoodItem item : items) {
            String flag = item.isExpired() ? "🔴 " :
                          item.isExpiringSoon() ? "🟡 " : "   ";
            System.out.println(flag + item);
        }
        System.out.println("Total: " + items.size() + " item(s)");
    }

    private String readString(String p) {
        System.out.print(p);
        return sc.nextLine().trim();
    }

    private int readInt(String p) {
        while (true) {
            System.out.print(p);
            try { return Integer.parseInt(sc.nextLine().trim()); }
            catch (NumberFormatException e) { System.out.println("❌ Enter a number."); }
        }
    }
}