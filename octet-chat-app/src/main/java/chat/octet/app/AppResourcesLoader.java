package chat.octet.app;


import java.io.InputStream;
import java.net.URL;

public class AppResourcesLoader {

    private AppResourcesLoader() {
    }

    public static URL loadURL(String path) {
        return AppResourcesLoader.class.getResource(path);
    }

    public static String load(String path) {
        return loadURL(path).toString();
    }

    public static InputStream loadStream(String name) {
        return AppResourcesLoader.class.getResourceAsStream(name);
    }
}
