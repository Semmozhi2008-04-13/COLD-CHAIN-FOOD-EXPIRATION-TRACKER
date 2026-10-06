package util;

import model.FoodItem;
import service.InventoryService;

import java.util.List;

public class ExpiryAlertThread extends Thread {
    private final InventoryService service;
    private volatile boolean running = true;
    private final int intervalSeconds;

    public ExpiryAlertThread(InventoryService service, int intervalSeconds) {
        this.service = service;
        this.intervalSeconds = intervalSeconds;
        setDaemon(true);
        setName("ExpiryAlertThread");
    }

    @Override
    public void run() {
        while (running) {
            try {
                List<FoodItem> expiring = service.viewExpiringSoon(2);
                if (!expiring.isEmpty()) {
                    System.out.println("\n⚠️  [ALERT THREAD] " + expiring.size() +
                                       " item(s) expiring within 2 days! (Check option 6)");
                    FileManager.log("ALERT: " + expiring.size() + " items expiring soon.");
                }
                Thread.sleep(intervalSeconds * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Alert thread error: " + e.getMessage());
            }
        }
    }

    public void stopThread() {
        running = false;
    }
}