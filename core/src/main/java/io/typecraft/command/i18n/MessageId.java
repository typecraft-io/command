package io.typecraft.command.i18n;

import lombok.Data;
import lombok.With;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@With
@Data
public class MessageId {
    private final String id;
    private final String message;
    public static final MessageId commandNotExists = MessageId.of("command-not-exists");
    public static final MessageId commandWrongUsage = MessageId.of("command-wrong-usage");
    public static final MessageId typeString = MessageId.of("string");
    public static final MessageId typeInt = MessageId.of("int");
    public static final MessageId typeLong = MessageId.of("long");
    public static final MessageId typeFloat = MessageId.of("float");
    public static final MessageId typeDouble = MessageId.of("double");
    public static final MessageId typeBool = MessageId.of("bool");
    public static final MessageId typeStrings = MessageId.of("strings");
    public static final Map<Locale, Map<String, String>> defaultLangs = getDefaultLangs();

    private MessageId(String id, String message) {
        this.id = id;
        this.message = message;
    }

    public static MessageId of(String id) {
        return new MessageId(id, "");
    }

    public static MessageId ofEmpty() {
        return new MessageId("", "");
    }

    public String getMessage(Map<String, String> messages) {
        return messages.getOrDefault(getId(), getMessage().isEmpty() ? getId() : getMessage());
    }

    public static Map<String, String> getDefaultMessages(Locale locale) {
        Locale key = defaultLangs.containsKey(locale) ? locale : Locale.ENGLISH;
        return defaultLangs.getOrDefault(key, Collections.emptyMap());
    }

    private static Map<Locale, Map<String, String>> getDefaultLangs() {
        Map<Locale, Map<String, String>> map = new HashMap<>();
        map.put(Locale.KOREAN, getKoreanMessages());
        map.put(Locale.ENGLISH, getEnglishMessages());
        return map;
    }

    private static Map<String, String> getKoreanMessages() {
        Map<String, String> map = new HashMap<>();
        // commands
        map.put(commandNotExists.getId(), "'%s' ???????????? ???????????? ????????????!");
        map.put(commandWrongUsage.getId(), "????????? ??????????????????!");
        // types
        map.put(typeString.getId(), "??????");
        map.put(typeInt.getId(), "??????");
        map.put(typeLong.getId(), "??????");
        map.put(typeFloat.getId(), "??????");
        map.put(typeDouble.getId(), "??????");
        map.put(typeBool.getId(), "bool");
        return map;
    }

    private static Map<String, String> getEnglishMessages() {
        Map<String, String> map = new HashMap<>();
        // commands
        map.put(commandNotExists.getId(), "Command '%s' doesn't exists!");
        map.put(commandWrongUsage.getId(), "Wrong command!");
        // types
        map.put(typeString.getId(), "name");
        map.put(typeInt.getId(), "int");
        map.put(typeLong.getId(), "long");
        map.put(typeFloat.getId(), "float");
        map.put(typeDouble.getId(), "double");
        map.put(typeBool.getId(), "bool");
        return map;
    }
}
