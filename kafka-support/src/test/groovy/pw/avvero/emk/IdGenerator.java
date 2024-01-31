package pw.avvero.emk;

import java.util.concurrent.atomic.AtomicInteger;

public class IdGenerator {

    private static final AtomicInteger id = new AtomicInteger();

    public static String getNext() {
        return "" + id.incrementAndGet();
    }

}
