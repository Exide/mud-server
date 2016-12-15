package org.arabellan.utils;

import java.util.concurrent.ThreadFactory;

public class ThreadUtils {

    public static ThreadFactory setThreadName(String name) {
        return (Runnable r) -> new Thread(r, name);
    }

    public static ThreadFactory setThreadNameWithID(String name) {
        return (Runnable r) -> new Thread(r, String.format("%s-%s", name, r.hashCode()));
    }
}
