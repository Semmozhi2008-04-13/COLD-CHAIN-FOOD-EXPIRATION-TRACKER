package model;

import java.time.LocalDate;

public class PerishableItem extends FoodItem {

    public PerishableItem(String name, String category, int qty, String batch,
                          LocalDate mfg, LocalDate exp, String loc) {
        super(name, category, qty, batch, mfg, exp, loc);
    }

    public PerishableItem(int id, String name, String category, int qty, String batch,
                          LocalDate mfg, LocalDate exp, String loc) {
        super(id, name, category, qty, batch, mfg, exp, loc);
    }

    @Override
    public String getItemType() {
        return "PERISHABLE";
    }

    @Override
    public String toString() {
        return "[PERISHABLE]     " + super.toString();
    }
}