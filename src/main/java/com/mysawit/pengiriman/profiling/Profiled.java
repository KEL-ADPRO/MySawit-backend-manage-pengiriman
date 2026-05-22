package com.mysawit.pengiriman.profiling;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation untuk mengaktifkan profiling otomatis pada method.
 *
 * <p>Ketika digunakan, {@link ProfilingAspect} akan:
 * <ul>
 *   <li>Mengukur waktu eksekusi (execution time)</li>
 *   <li>Mencatat metrik ke Micrometer/Prometheus</li>
 *   <li>Menghitung jumlah pemanggilan dan error rate</li>
 * </ul>
 *
 * <p>Gunakan {@code category} untuk mengelompokkan metrik berdasarkan layer (service, usecase, grpc).
 *
 * <pre>{@code
 * @Profiled(category = "shipment.command")
 * public ShipmentResponse createShipment(CreateShipmentRequest request) { ... }
 * }</pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Profiled {

    /**
     * Nama kategori/group untuk metrik, mis. "shipment.query", "shipment.command", "grpc".
     * Default diambil dari nama class.
     */
    String category() default "";
}
