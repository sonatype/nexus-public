package org.sonatype.nexus.internal.metrics;

import com.codahale.metrics.JvmAttributeGaugeSet;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.jvm.BufferPoolMetricSet;
import com.codahale.metrics.jvm.ClassLoadingGaugeSet;
import com.codahale.metrics.jvm.FileDescriptorRatioGauge;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.google.common.collect.ImmutableMap;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.management.MBeanServer;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * JVM {@link MetricSet}.
 *
 * @since 3.2
 */
@Named("jvm")
@Singleton
public class JvmMetricSet
  implements MetricSet
{
  private final MBeanServer server;

  public JvmMetricSet(final MBeanServer server) {
    this.server = checkNotNull(server);
  }

  public JvmMetricSet() {
    this(ManagementFactory.getPlatformMBeanServer());
  }

  @Override
  public Map<String, Metric> getMetrics() {
    Map<String, Metric> metrics = new HashMap<>();

    metrics.put("vm", new JvmAttributeGaugeSet());
    metrics.put("buffers", new BufferPoolMetricSet(server));
    metrics.put("classes", new ClassLoadingGaugeSet());
    metrics.put("fd_usage", new FileDescriptorRatioGauge());
    metrics.put("garbage-collectors", new GarbageCollectorMetricSet());
    metrics.put("memory", new MemoryUsageGaugeSet());
    metrics.put("thread-states", new ThreadStatesGaugeSet());

    return ImmutableMap.copyOf(metrics);
  }
}
