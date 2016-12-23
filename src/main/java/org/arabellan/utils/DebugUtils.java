package org.arabellan.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.Collection;

@Slf4j
public class DebugUtils {

    public static void traceCollectionSize(String name, Collection collection) {
        String message = name + ": " + collection.size();
        log.trace(message);
    }
}
