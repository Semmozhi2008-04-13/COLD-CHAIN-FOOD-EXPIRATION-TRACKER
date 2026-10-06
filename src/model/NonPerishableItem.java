package model;

import java.time.LocalDate;

public class NonPerishableItem extends FoodItem {

    public NonPerishableItem(String name, String category, int qty, String batch,
                             LocalDate mfg, LocalDate exp, String loc) {
        super(name, category, qty, batch, mfg, exp, loc);
    }

    public NonPerishableItem(int id, String name, String category, int qty, String batch,
                             LocalDate mfg, LocalDate exp, String loc) {
        super(id, name, category, qty, batch, mfg, exp, loc);
    }

    @Override
    public String getItemType() {
        return "NON_PERISHABLE";
    }

    @Override
    public String toString() {
        return "[NON-PERISHABLE] " + super.toString();
    }
}