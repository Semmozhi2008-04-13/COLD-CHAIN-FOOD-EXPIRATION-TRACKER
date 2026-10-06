import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class WebServer {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/freshflow_db";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "12345"; // 🔴 CHANGE THIS

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", WebServer::serveIndex);
        server.createContext("/api/items", WebServer::getItems);
        server.createContext("/api/add", WebServer::addItem);
        server.createContext("/api/update", WebServer::updateItem);
        server.createContext("/api/delete", WebServer::deleteItem);
        server.createContext("/api/stats", WebServer::getStats);
        server.createContext("/api/lowstock", WebServer::getLowStock);
        server.createContext("/api/expiring", WebServer::getExpiring);
        server.createContext("/api/export", WebServer::exportCSV);
        server.setExecutor(null);
        server.start();

        System.out.println("\n========================================");
        System.out.println("  FRESHFLOW WEB STARTED!");
        System.out.println("  Open: http://localhost:8080");
        System.out.println("========================================\n");
    }

    private static void serveIndex(HttpExchange ex) throws IOException {
        File f = new File("index.html");
        String html = f.exists() ? new String(new FileInputStream(f).readAllBytes()) : "<h1>index.html missing</h1>";
        sendResponse(ex, 200, "text/html", html);
    }

    private static void getItems(HttpExchange ex) throws IOException {
        String json = queryItems("SELECT * FROM food_items ORDER BY expiry_date ASC");
        sendResponse(ex, 200, "application/json", json);
    }

    private static void getLowStock(HttpExchange ex) throws IOException {
        String json = queryItems("SELECT * FROM food_items WHERE quantity <= 10 ORDER BY quantity ASC");
        sendResponse(ex, 200, "application/json", json);
    }

    private static void getExpiring(HttpExchange ex) throws IOException {
        String json = queryItems("SELECT * FROM food_items WHERE expiry_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 3 DAY) ORDER BY expiry_date ASC");
        sendResponse(ex, 200, "application/json", json);
    }

    private static void addItem(HttpExchange ex) throws IOException {
        String body = new String(ex.getRequestBody().readAllBytes());
        Map<String, String> p = parseForm(body);
        String sql = "INSERT INTO food_items (name, category, quantity, batch_number, expiry_date, storage_location) VALUES (?,?,?,?,?,?)";
        try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.get("name"));
            ps.setString(2, p.get("category"));
            ps.setInt(3, Integer.parseInt(p.get("quantity")));
            ps.setString(4, p.get("batchNumber"));
            ps.setDate(5, java.sql.Date.valueOf(p.get("expiryDate")));
            ps.setString(6, p.get("storageLocation"));
            ps.executeUpdate();
            sendResponse(ex, 200, "application/json", "{\"message\":\"Added\"}");
        } catch (Exception e) {
            sendResponse(ex, 500, "application/json", "{\"error\":\"" + esc(e.getMessage()) + "\"}");
        }
    }

    private static void updateItem(HttpExchange ex) throws IOException {
        String body = new String(ex.getRequestBody().readAllBytes());
        Map<String, String> p = parseForm(body);
        String sql = "UPDATE food_items SET name=?, category=?, quantity=?, batch_number=?, expiry_date=?, storage_location=? WHERE id=?";
        try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.get("name"));
            ps.setString(2, p.get("category"));
            ps.setInt(3, Integer.parseInt(p.get("quantity")));
            ps.setString(4, p.get("batchNumber"));
            ps.setDate(5, java.sql.Date.valueOf(p.get("expiryDate")));
            ps.setString(6, p.get("storageLocation"));
            ps.setInt(7, Integer.parseInt(p.get("id")));
            ps.executeUpdate();
            sendResponse(ex, 200, "application/json", "{\"message\":\"Updated\"}");
        } catch (Exception e) {
            sendResponse(ex, 500, "application/json", "{\"error\":\"" + esc(e.getMessage()) + "\"}");
        }
    }

    private static void deleteItem(HttpExchange ex) throws IOException {
        String id = ex.getRequestURI().getQuery().split("=")[1];
        try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = c.prepareStatement("DELETE FROM food_items WHERE id=?")) {
            ps.setInt(1, Integer.parseInt(id));
            ps.executeUpdate();
            sendResponse(ex, 200, "application/json", "{\"message\":\"Deleted\"}");
        } catch (Exception e) {
            sendResponse(ex, 500, "application/json", "{\"error\":\"" + esc(e.getMessage()) + "\"}");
        }
    }

    private static void getStats(HttpExchange ex) throws IOException {
        int total = 0, expired = 0, expiring = 0, ok = 0, totalQty = 0, lowStock = 0;
        try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM food_items")) {
            while (rs.next()) {
                total++;
                totalQty += rs.getInt("quantity");
                if (rs.getInt("quantity") <= 10) lowStock++;
                LocalDate exp = rs.getDate("expiry_date").toLocalDate();
                long d = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), exp);
                if (LocalDate.now().isAfter(exp)) expired++;
                else if (d <= 2) expiring++;
                else ok++;
            }
        } catch (Exception e) {
            sendResponse(ex, 500, "application/json", "{\"error\":\"" + esc(e.getMessage()) + "\"}");
            return;
        }
        sendResponse(ex, 200, "application/json",
            String.format("{\"total\":%d,\"expired\":%d,\"expiringSoon\":%d,\"ok\":%d,\"totalQuantity\":%d,\"lowStock\":%d}",
                total, expired, expiring, ok, totalQty, lowStock));
    }

    private static void exportCSV(HttpExchange ex) throws IOException {
        StringBuilder csv = new StringBuilder("ID,Name,Category,Quantity,Batch,Expiry,Location,Status\n");
        try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM food_items ORDER BY expiry_date ASC")) {
            while (rs.next()) {
                LocalDate exp = rs.getDate("expiry_date").toLocalDate();
                long d = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), exp);
                String status = LocalDate.now().isAfter(exp) ? "EXPIRED" : (d <= 2 ? "EXPIRING" : "OK");
                csv.append(rs.getInt("id")).append(",")
                   .append(rs.getString("name")).append(",")
                   .append(rs.getString("category")).append(",")
                   .append(rs.getInt("quantity")).append(",")
                   .append(rs.getString("batch_number")).append(",")
                   .append(exp).append(",")
                   .append(rs.getString("storage_location")).append(",")
                   .append(status).append("\n");
            }
        } catch (Exception e) {
            sendResponse(ex, 500, "text/plain", "Error: " + e.getMessage());
            return;
        }
        ex.getResponseHeaders().set("Content-Type", "text/csv; charset=UTF-8");
        ex.getResponseHeaders().set("Content-Disposition", "attachment; filename=freshflow_report_" + LocalDate.now() + ".csv");
        byte[] bytes = csv.toString().getBytes("UTF-8");
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private static String queryItems(String sql) {
        StringBuilder json = new StringBuilder("[");
        try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                first = false;
                LocalDate exp = rs.getDate("expiry_date").toLocalDate();
                long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), exp);
                String status = LocalDate.now().isAfter(exp) ? "EXPIRED" : (days <= 2 ? "EXPIRING_SOON" : "OK");
                json.append("{")
                    .append("\"id\":").append(rs.getInt("id")).append(",")
                    .append("\"name\":\"").append(esc(rs.getString("name"))).append("\",")
                    .append("\"category\":\"").append(esc(rs.getString("category"))).append("\",")
                    .append("\"quantity\":").append(rs.getInt("quantity")).append(",")
                    .append("\"batchNumber\":\"").append(esc(rs.getString("batch_number"))).append("\",")
                    .append("\"expiryDate\":\"").append(exp).append("\",")
                    .append("\"storageLocation\":\"").append(esc(rs.getString("storage_location"))).append("\",")
                    .append("\"daysUntilExpiry\":").append(days).append(",")
                    .append("\"status\":\"").append(status).append("\"}");
            }
        } catch (Exception e) {
            return "{\"error\":\"" + esc(e.getMessage()) + "\"}";
        }
        json.append("]");
        return json.toString();
    }

    private static void sendResponse(HttpExchange ex, int code, String type, String body) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type + "; charset=UTF-8");
        byte[] bytes = body.getBytes("UTF-8");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static Map<String, String> parseForm(String body) {
        Map<String, String> m = new HashMap<>();
        for (String pair : body.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                try { m.put(kv[0], java.net.URLDecoder.decode(kv[1], "UTF-8")); }
                catch (Exception e) { m.put(kv[0], kv[1]); }
            }
        }
        return m;
    }
}