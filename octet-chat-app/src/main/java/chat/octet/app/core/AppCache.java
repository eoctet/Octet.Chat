package chat.octet.app.core;


import com.google.common.collect.Maps;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

public class AppCache {
    private static final String DELIMITER = ":";
    private static final Map<String, Parent> CACHE_MAP;

    static {
        CACHE_MAP = Maps.newConcurrentMap();
    }

    private AppCache() {
    }

    public static void addView(Parent parent, String... id) {
        CACHE_MAP.put(StringUtils.join(id, DELIMITER), parent);
    }

    public static void removeView(String... id) {
        String key = StringUtils.join(id, DELIMITER);
        if (CACHE_MAP.containsKey(key)) {
            BorderPane view = (BorderPane) CACHE_MAP.get(key);
            view.getChildren().clear();
            CACHE_MAP.remove(key);
        }
    }

    public static Parent getView(String... id) {
        String key = StringUtils.join(id, DELIMITER);
        return CACHE_MAP.get(key);
    }

    public static boolean hasCache(String id) {
        return CACHE_MAP.containsKey(id);
    }

}
