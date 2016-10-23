package com.chemsense.travisbrannen.chemsenseapp.displays;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * <p/>
 */
public class DisplayModes {

    /**
     * An array of sample (dummy) items.
     */
    public static List<DisplayItem> ITEMS = new ArrayList<DisplayItem>();

    /**
     * A map of sample (dummy) items, by ID.
     */
    public static Map<String, DisplayItem> ITEM_MAP = new HashMap<String, DisplayItem>();

    static {
        // Add 3 sample items.
        addItem(new DisplayItem("Graph", "Concentration Display"));
        addItem(new DisplayItem("Options", "Options"));
        addItem(new DisplayItem("About", "About ChemSense"));
    }

    private static void addItem(DisplayItem item) {
        ITEMS.add(item);
        ITEM_MAP.put(item.id, item);
    }

    /**
     * A dummy item representing a piece of content.
     */
    public static class DisplayItem {
        public String id;
        public String content;

        public DisplayItem(String id, String content) {
            this.id = id;
            this.content = content;
        }

        @Override
        public String toString() {
            return content;
        }
    }
}
