/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.extender.modules.internal;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.sonatype.goodies.testsupport.TestSupport;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.annotation.CachedGauge;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.palominolabs.metrics.guice.DefaultMetricNamer;
import com.palominolabs.metrics.guice.GaugeInjectionListener;
import com.palominolabs.metrics.guice.annotation.MethodAnnotationResolver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class CachedGaugeTypeListenerTest
    extends TestSupport
{
  private final TypeLiteral<TestClass> testTypeLiteral = new TypeLiteral<TestClass>() { };

  private final Map<String, String> mockProperties = new HashMap<>();

  @Captor
  ArgumentCaptor<InjectionListener<? super TestClass>> injectionListenerArgumentCaptor;

  @Mock
  private MetricRegistry mockMetricRegistry;

  @Mock
  private TypeEncounter<TestClass> mockTypeEncounter;

  private CachedGaugeTypeListener undertest;

  @Before
  public void setup() {
    undertest =
        spy(new CachedGaugeTypeListener(mockMetricRegistry, new DefaultMetricNamer(), new MethodAnnotationResolver(),
            mockProperties));
  }

  @Test
  public void testHearNoOverride() {
    undertest.hear(testTypeLiteral, mockTypeEncounter);
    verify(mockTypeEncounter, times(1)).register(isA(CachedGaugeInjectionListener.class));
  }

  @Test
  public void testHearAllCacheDisable() {
    mockProperties.put("nexus.analytics.cache.disableAll", "true");
    undertest.hear(testTypeLiteral, mockTypeEncounter);
    verify(mockTypeEncounter, times(1)).register(isA(GaugeInjectionListener.class));
  }

  @Test
  public void testHearSingleCacheDisable() {
    mockProperties.put("nexus.analytics.cache.disable.test.annotated", "true");
    undertest.hear(testTypeLiteral, mockTypeEncounter);
    verify(mockTypeEncounter, times(1)).register(isA(GaugeInjectionListener.class));
  }

  @Test
  public void testHearSingleCacheDisableFalse() {
    mockProperties.put("nexus.analytics.cache.disable.test.annotated", "false");
    undertest.hear(testTypeLiteral, mockTypeEncounter);
    verify(mockTypeEncounter, times(1)).register(isA(CachedGaugeInjectionListener.class));
  }

  @Test
  public void testHearTimeoutOverride() throws Exception {
    mockProperties.put("test.annotated.cache.timeout", "42");
    undertest.hear(testTypeLiteral, mockTypeEncounter);
    verify(mockTypeEncounter, times(1)).register(injectionListenerArgumentCaptor.capture());
    assertThat(getTimeout(injectionListenerArgumentCaptor.getValue()), is(42L));
  }

  @Test
  public void testHearTimeUnitOverride() throws Exception {
    mockProperties.put("test.annotated.cache.timeUnit", "minutes");
    undertest.hear(testTypeLiteral, mockTypeEncounter);
    verify(mockTypeEncounter, times(1)).register(injectionListenerArgumentCaptor.capture());
    assertThat(getTimeUnit(injectionListenerArgumentCaptor.getValue()), is(TimeUnit.MINUTES));
  }

  @Test
  public void testHearAnalyticDisable() {
    mockProperties.put("test.annotated.disable", "true");
    undertest.hear(testTypeLiteral, mockTypeEncounter);
    verify(mockTypeEncounter, times(0)).register((InjectionListener<? super TestClass>) isNotNull());
  }

  private long getTimeout(InjectionListener<? super TestClass> listener) throws Exception {
    Field timeoutField = CachedGaugeInjectionListener.class.getDeclaredField("timeout");
    timeoutField.setAccessible(true);
    return timeoutField.getLong(listener);
  }

  private TimeUnit getTimeUnit(InjectionListener<?> listener) throws Exception {
    Field timeUnitField = CachedGaugeInjectionListener.class.getDeclaredField("timeUnit");
    timeUnitField.setAccessible(true);
    return (TimeUnit) timeUnitField.get(listener);
  }

  public static class TestClass
  {
    @CachedGauge(name = "test.annotated", timeout = 10, timeoutUnit = TimeUnit.SECONDS, absolute = true)
    public String annotated() {
      return null;
    }
  }
}
