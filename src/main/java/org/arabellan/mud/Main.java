package org.arabellan.mud;

import com.google.inject.Guice;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {

    public static void main(String... args) {
        Injector injector = Guice.createInjector(new GuiceModule());
        injector.getInstance(Server.class).run();
    }

}
