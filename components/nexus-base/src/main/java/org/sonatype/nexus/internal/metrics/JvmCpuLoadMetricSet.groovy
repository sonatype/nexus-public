package org.sonatype.nexus.internal.metrics

import java.lang.management.ManagementFactory
import java.lang.management.OperatingSystemMXBean

import javax.annotation.Nullable
import javax.inject.Named
import javax.inject.Singleton

import com.codahale.metrics.Gauge
import com.codahale.metrics.Metric
import com.codahale.metrics.MetricSet
import groovy.util.logging.Slf4j

import static com.google.common.base.Preconditions.checkNotNull

/**
 * JVM CPU load {@link MetricSet}.
 *
 * @since 3.2
 */
@Slf4j
@Named("jvm.cpu_load")
@Singleton
class JvmCpuLoadMetricSet
  implements MetricSet
{
  private final OperatingSystemMXBean os

  JvmCpuLoadMetricSet(final OperatingSystemMXBean os) {
    this.os = checkNotNull(os);
  }

  JvmCpuLoadMetricSet() {
    this(ManagementFactory.operatingSystemMXBean)
  }

  @Override
  Map<String, Metric> getMetrics() {
    return [
      'system': gauge('getSystemCpuLoad'),
      'process': gauge('getProcessCpuLoad')
    ]
  }

  @Nullable
  private Gauge<Double> gauge(final String name) {
    return {
      try {
        def method = os.getClass().getDeclaredMethod(name)
        method.accessible = true
        return method.invoke(os)
      }
      catch (Exception e) {
        log.warn("Failed to fetch OperatingSystem MBean value: $name", e)
        return null
      }
    }
  }
}
