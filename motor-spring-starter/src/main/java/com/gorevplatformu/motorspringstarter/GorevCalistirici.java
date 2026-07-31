package com.gorevplatformu.motorspringstarter;

import com.gorevplatformu.motorcekirdek.GorevBaglamiImpl;
import com.gorevplatformu.motorcekirdek.GorevHandler;
import com.gorevplatformu.motorcekirdek.GorevZamanAsimiException;
import jakarta.annotation.PreDestroy;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class GorevCalistirici {

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public <P> Object calistir(GorevHandler<P> handler, P payload, GorevBaglamiImpl baglam, Duration zamanAsimi)
            throws Exception {
        // MDC thread-local'dir, handler ayri bir sanal thread'de calistigi icin cagiran thread'in
        // (GorevMesajIsleyici.isle()'nin koydugu traceId dahil) MDC baglami buraya elle tasinmali -
        // yoksa handler icindeki loglar trace id'siz kalir.
        Map<String, String> mdcBaglam = MDC.getCopyOfContextMap();
        Future<Object> gelecek = executor.submit(() -> {
            if (mdcBaglam != null) {
                MDC.setContextMap(mdcBaglam);
            }
            try {
                return handler.calistir(payload, baglam);
            } finally {
                MDC.clear();
            }
        });
        try {
            return gelecek.get(zamanAsimi.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            baglam.iptalIste();
            gelecek.cancel(true);
            throw new GorevZamanAsimiException(zamanAsimi);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof Exception ex) {
                throw ex;
            }
            throw e;
        }
    }

    @PreDestroy
    public void kapat() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
