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
package org.sonatype.nexus.internal.atlas.customizers;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.TabularData;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singleton;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JmxCustomizerTest
    extends TestSupport
{
  @Mock
  private MBeanServer mBeanServer;

  private JmxCustomizer underTest;

  @Before
  public void setUp() {
    underTest = new JmxCustomizer(mBeanServer);
  }

  @Test
  public void testRenderString() {
    String input = "test";
    Object result = underTest.render(input);
    assertThat(result, is("test"));
  }

  @Test
  public void testRenderTabularData() {
    TabularData tabularData = mock(TabularData.class);
    CompositeData compositeData = mock(CompositeData.class);
    when(compositeData.getCompositeType()).thenReturn(mock(CompositeType.class));
    when(compositeData.getCompositeType().keySet()).thenReturn(singleton("key"));
    when(compositeData.get("key")).thenReturn("value");
    doReturn(singleton(newArrayList("key1", "key1a"))).when(tabularData).keySet();
    when(tabularData.get(newArrayList("key1", "key1a").toArray())).thenReturn(compositeData);
    Object result = underTest.render(tabularData);
    assertThat(result, instanceOf(List.class));
    assertThat((List<?>) result, hasSize(1));
    assertThat(((List<?>) result).get(0), instanceOf(Map.class));
    assertThat((Map<?, ?>) ((List<?>) result).get(0), hasEntry("key", "value"));
  }

  @Test
  public void testRenderCompositeData() {
    CompositeData compositeData = mock(CompositeData.class);
    when(compositeData.getCompositeType()).thenReturn(mock(CompositeType.class));
    when(compositeData.getCompositeType().keySet()).thenReturn(singleton("key"));
    when(compositeData.get("key")).thenReturn("value");
    Object result = underTest.render(compositeData);
    assertThat(result, instanceOf(Map.class));
    assertThat((Map<?, ?>) result, hasEntry("key", "value"));
  }

  @Test
  public void testRenderObjectName() {
    ObjectName objectName = mock(ObjectName.class);
    when(objectName.getCanonicalName()).thenReturn("canonicalName");
    Object result = underTest.render(objectName);
    assertThat(result, is("canonicalName"));
  }

  @Test
  public void testRenderCollection() {
    Collection<String> collection = Arrays.asList("one", "two");
    Object result = underTest.render(collection);
    assertThat(result, instanceOf(List.class));
    assertThat((List<?>) result, contains("one", "two"));
  }

  @Test
  public void testRenderObjectArray() {
    Object[] array = {"one", "two"};
    Object result = underTest.render(array);
    assertThat(result, instanceOf(List.class));
    assertThat((List<?>) result, contains("one", "two"));
  }

  @Test
  public void testRenderStringArray() {
    String[] array = {"one", "two"};
    Object result = underTest.render(array);
    assertThat(result, instanceOf(List.class));
    assertThat((List<?>) result, contains("one", "two"));
  }

  @Test
  public void testRenderMap() {
    Map<String, String> map = new HashMap<>();
    map.put("key", "value");
    Object result = underTest.render(map);
    assertThat(result, instanceOf(Map.class));
    assertThat((Map<?, ?>) result, hasEntry("key", "value"));
  }

  @Test
  public void testRenderDouble() {
    Double value = 1.0;
    Object result = underTest.render(value);
    assertThat(result, is(1.0));
  }

  @Test
  public void testRenderFloat() {
    Float value = 1.0f;
    Object result = underTest.render(value);
    assertThat(result, is(1.0f));
  }

  @Test
  public void testRenderEnum() {
    TestEnum value = TestEnum.VALUE;
    Object result = underTest.render(value);
    assertThat(result, is("VALUE"));
  }

  @Test
  public void testRenderNull() {
    Object result = underTest.render(null);
    assertThat(result, nullValue());
  }

  private enum TestEnum
  {
    VALUE
  }
}
