package chat.octet.app.utils;


import chat.octet.app.core.enums.LanguageType;
import io.github.palexdev.materialfx.i18n.I18N;
import io.github.palexdev.materialfx.i18n.Language;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public final class MessageI18N {

    private static final ObjectProperty<Locale> locale = new SimpleObjectProperty<>();

    static {
        locale.addListener(invalidated -> Locale.setDefault(getLocale()));
    }

    private MessageI18N() {
    }

    public static String get(String key, Object... args) {
        ResourceBundle bundle = getBundle(getLocale());
        return MessageFormat.format(bundle.getString(key), args);
    }

    public static String get(String key) {
        ResourceBundle bundle = getBundle(getLocale());
        return bundle.getString(key);
    }

    public static ResourceBundle getBundle(Locale locale) {
        return ResourceBundle.getBundle(getBundleBaseName(), locale);
    }

    public static Locale getLocale() {
        return locale.get();
    }

    public static ObjectProperty<Locale> localeProperty() {
        return locale;
    }

    public static void setLanguage(LanguageType language) {
        locale.set(language.getLocale());
        switch (language) {
            case SIMPLIFIED_CHINESE:
                I18N.setLanguage(Language.SIMPLIFIED_CHINESE);
                break;
            case ENGLISH:
                I18N.setLanguage(Language.ENGLISH);
                break;
            case RUSSIAN:
                I18N.setLanguage(Language.RUSSIAN);
                break;
        }
    }

    public static String getBundleBaseName() {
        return "i18n/messages";
    }
}
