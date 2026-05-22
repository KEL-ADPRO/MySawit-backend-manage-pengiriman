package com.mysawit.pengiriman.profiling;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * AOP Aspect untuk profiling performa seluruh layer service dan use case.
 *
 * <p><strong>Cara kerja:</strong>
 * <ol>
 *   <li>Intercept semua method di {@code service}, {@code usecase}, dan {@code integration} package,
 *       ATAU method yang diberi anotasi {@link Profiled}.</li>
 *   <li>Mengukur waktu eksekusi dengan nanosecond precision.</li>
 *   <li>Mendaftarkan 3 jenis metrik ke Micrometer:
 *     <ul>
 *       <li>{@code shipment.method.duration} — Timer (latency histogram)</li>
 *       <li>{@code shipment.method.calls.total} — Counter (total invocations)</li>
 *       <li>{@code shipment.method.errors.total} — Counter (total exceptions)</li>
 *     </ul>
 *   </li>
 *   <li>Log setiap eksekusi ke SLF4J dengan level DEBUG (sukses) atau WARN (lambat/error).</li>
 * </ol>
 *
 * <p><strong>Threshold peringatan:</strong> method dianggap lambat jika melebihi
 * {@value #SLOW_THRESHOLD_MS} ms, dan di-log dengan level WARN beserta tag {@code slow=true}.
 *
 * <p>Metrik dapat diakses via Prometheus di endpoint {@code /actuator/prometheus}.
 */
@Aspect
@Component
public class ProfilingAspect {

    private static final Logger log = LoggerFactory.getLogger(ProfilingAspect.class);

    /** Threshold untuk menandai method sebagai "slow" di log dan metrik. */
    static final long SLOW_THRESHOLD_MS = 500L;

    private final MeterRegistry meterRegistry;

    public ProfilingAspect(final MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Intercept semua public method di package service, usecase, dan integration,
     * serta method yang diberi anotasi {@link Profiled}.
     */
    @Around(
        "(" +
        "  within(com.mysawit.pengiriman.service..*)  ||" +
        "  within(com.mysawit.pengiriman.usecase..*)  ||" +
        "  within(com.mysawit.pengiriman.integration..*)" +
        ") || @annotation(com.mysawit.pengiriman.profiling.Profiled)" +
        "   || @within(com.mysawit.pengiriman.profiling.Profiled)"
    )
    public Object profile(final ProceedingJoinPoint pjp) throws Throwable {
        final MethodSignature sig = (MethodSignature) pjp.getSignature();
        final Method method = sig.getMethod();
        final String className = pjp.getTarget().getClass().getSimpleName();
        final String methodName = method.getName();

        // Tentukan category dari anotasi (jika ada), atau gunakan nama class
        final Profiled annotation = method.getAnnotation(Profiled.class);
        final String category = (annotation != null && !annotation.category().isBlank())
            ? annotation.category()
            : resolveCategory(pjp.getTarget().getClass());

        final long startNs = System.nanoTime();
        boolean success = true;

        try {
            final Object result = pjp.proceed();
            return result;
        } catch (Throwable ex) {
            success = false;
            // Catat error counter
            Counter.builder("shipment.method.errors.total")
                .tag("class", className)
                .tag("method", methodName)
                .tag("category", category)
                .tag("exception", ex.getClass().getSimpleName())
                .description("Total exceptions thrown by shipment methods")
                .register(meterRegistry)
                .increment();
            throw ex;
        } finally {
            final long elapsedNs = System.nanoTime() - startNs;
            final long elapsedMs = TimeUnit.NANOSECONDS.toMillis(elapsedNs);
            final boolean slow = elapsedMs >= SLOW_THRESHOLD_MS;

            // Timer — latency histogram untuk percentile (p50, p95, p99)
            Timer.builder("shipment.method.duration")
                .tag("class", className)
                .tag("method", methodName)
                .tag("category", category)
                .tag("success", String.valueOf(success))
                .tag("slow", String.valueOf(slow))
                .description("Execution time of shipment service methods")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(meterRegistry)
                .record(elapsedNs, TimeUnit.NANOSECONDS);

            // Counter — total invocations
            Counter.builder("shipment.method.calls.total")
                .tag("class", className)
                .tag("method", methodName)
                .tag("category", category)
                .tag("success", String.valueOf(success))
                .description("Total invocations of shipment service methods")
                .register(meterRegistry)
                .increment();

            // Logging
            if (!success) {
                log.warn("[PROFILING][ERROR] {}.{}() failed | {}ms | category={}",
                    className, methodName, elapsedMs, category);
            } else if (slow) {
                log.warn("[PROFILING][SLOW]  {}.{}() completed | {}ms (>{}ms threshold) | category={}",
                    className, methodName, elapsedMs, SLOW_THRESHOLD_MS, category);
            } else {
                log.debug("[PROFILING] {}.{}() completed | {}ms | category={}",
                    className, methodName, elapsedMs, category);
            }
        }
    }

    /**
     * Tentukan kategori berdasarkan package/nama class secara otomatis.
     */
    private String resolveCategory(final Class<?> targetClass) {
        final String pkg = targetClass.getPackageName();
        if (pkg.contains(".service")) {
            return "shipment.service";
        }
        if (pkg.contains(".usecase")) {
            return "shipment.usecase";
        }
        if (pkg.contains(".integration")) {
            return "shipment.integration";
        }
        return "shipment.other";
    }
}
