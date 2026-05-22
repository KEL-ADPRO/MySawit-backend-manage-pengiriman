package com.mysawit.pengiriman.profiling;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test untuk {@link ProfilingAspect}.
 *
 * <p>Memverifikasi bahwa:
 * <ul>
 *   <li>Metrik {@code shipment.method.calls.total} di-increment setiap pemanggilan</li>
 *   <li>Metrik {@code shipment.method.errors.total} di-increment saat exception</li>
 *   <li>Exception tetap di-propagate ke caller</li>
 *   <li>Hasil method dikembalikan dengan benar</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class ProfilingAspectTest {

    private MeterRegistry meterRegistry;
    private ProfilingAspect profilingAspect;

    @Mock
    private ProceedingJoinPoint pjp;

    @Mock
    private MethodSignature methodSignature;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        profilingAspect = new ProfilingAspect(meterRegistry);
    }

    @Test
    @DisplayName("Successful method call increments calls counter and returns result")
    void profile_successfulCall_incrementsCounterAndReturnsResult() throws Throwable {
        // Arrange
        final String expectedResult = "shipment-data";
        final Method method = SampleService.class.getDeclaredMethod("doQuery");
        when(pjp.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(pjp.getTarget()).thenReturn(new SampleService());
        when(pjp.proceed()).thenReturn(expectedResult);

        // Act
        final Object result = profilingAspect.profile(pjp);

        // Assert
        assertThat(result).isEqualTo(expectedResult);

        final Counter callsCounter = meterRegistry.find("shipment.method.calls.total")
            .tag("method", "doQuery")
            .tag("success", "true")
            .counter();
        assertThat(callsCounter).isNotNull();
        assertThat(callsCounter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Failed method call increments error counter and re-throws exception")
    void profile_failedCall_incrementsErrorCounterAndRethrows() throws Throwable {
        // Arrange
        final RuntimeException expectedException = new RuntimeException("DB down");
        final Method method = SampleService.class.getDeclaredMethod("doQuery");
        when(pjp.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(pjp.getTarget()).thenReturn(new SampleService());
        when(pjp.proceed()).thenThrow(expectedException);

        // Act & Assert
        assertThatThrownBy(() -> profilingAspect.profile(pjp))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("DB down");

        final Counter errorsCounter = meterRegistry.find("shipment.method.errors.total")
            .tag("method", "doQuery")
            .tag("exception", "RuntimeException")
            .counter();
        assertThat(errorsCounter).isNotNull();
        assertThat(errorsCounter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Successful call also records to Timer metric")
    void profile_successfulCall_recordsTimerMetric() throws Throwable {
        // Arrange
        final Method method = SampleService.class.getDeclaredMethod("doQuery");
        when(pjp.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(pjp.getTarget()).thenReturn(new SampleService());
        when(pjp.proceed()).thenReturn("ok");

        // Act
        profilingAspect.profile(pjp);

        // Assert — timer should have recorded exactly 1 observation
        final var timer = meterRegistry.find("shipment.method.duration")
            .tag("method", "doQuery")
            .timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Category resolved from @Profiled annotation takes precedence")
    void profile_withProfiledAnnotation_usesCategoryFromAnnotation() throws Throwable {
        // Arrange — method with @Profiled(category="custom.category")
        final Method method = SampleService.class.getDeclaredMethod("doProfiledQuery");
        when(pjp.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(pjp.getTarget()).thenReturn(new SampleService());
        when(pjp.proceed()).thenReturn("ok");

        // Act
        profilingAspect.profile(pjp);

        // Assert
        final Counter counter = meterRegistry.find("shipment.method.calls.total")
            .tag("category", "custom.category")
            .counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Multiple calls accumulate counter correctly")
    void profile_multipleCalls_accumulatesCounter() throws Throwable {
        // Arrange
        final Method method = SampleService.class.getDeclaredMethod("doQuery");
        when(pjp.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(pjp.getTarget()).thenReturn(new SampleService());
        when(pjp.proceed()).thenReturn("ok");

        // Act
        profilingAspect.profile(pjp);
        profilingAspect.profile(pjp);
        profilingAspect.profile(pjp);

        // Assert
        final Counter callsCounter = meterRegistry.find("shipment.method.calls.total")
            .tag("method", "doQuery")
            .counter();
        assertThat(callsCounter).isNotNull();
        assertThat(callsCounter.count()).isEqualTo(3.0);
    }

    /** Sample class untuk mock target. */
    static class SampleService {
        public String doQuery() { return "data"; }

        @Profiled(category = "custom.category")
        public String doProfiledQuery() { return "profiled-data"; }
    }
}
