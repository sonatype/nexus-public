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
package org.sonatype.nexus.repository.content.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class FormatAttributesUtilsTest
{
  private static final String FORMAT_NAME = "maven2";

  @Mock
  private Repository repository;

  @Before
  public void setUp() {
    Format format = new Format(FORMAT_NAME)
    {
    };
    when(repository.getFormat()).thenReturn(format);
  }

  @Test
  public void testGetFormatAttributesFromFluentAssetReturnsEmptyMapWhenNoAttributes() {
    FluentAsset fluentAsset = mock(FluentAsset.class);
    when(fluentAsset.repository()).thenReturn(repository);
    NestedAttributesMap attributes = new NestedAttributesMap("attributes", new HashMap<>());
    when(fluentAsset.attributes()).thenReturn(attributes);

    Map<String, Object> result = FormatAttributesUtils.getFormatAttributes(fluentAsset);
    assertThat(result.isEmpty(), is(true));
  }

  @Test
  public void testGetFormatAttributesFromFluentAssetReturnsExistingAttributes() {
    FluentAsset fluentAsset = mock(FluentAsset.class);
    when(fluentAsset.repository()).thenReturn(repository);
    Map<String, Object> backing = new HashMap<>();
    Map<String, Object> formatAttrs = new HashMap<>();
    formatAttrs.put("classifier", "sources");
    backing.put(FORMAT_NAME, formatAttrs);
    NestedAttributesMap attributes = new NestedAttributesMap("attributes", backing);
    when(fluentAsset.attributes()).thenReturn(attributes);

    Map<String, Object> result = FormatAttributesUtils.getFormatAttributes(fluentAsset);
    assertThat(result.get("classifier"), is("sources"));
  }

  @Test
  public void testGetFormatAttributesFromAsset() {
    Asset asset = mock(Asset.class);
    Map<String, Object> backing = new HashMap<>();
    Map<String, Object> formatAttrs = new HashMap<>();
    formatAttrs.put("extension", "jar");
    backing.put("raw", formatAttrs);
    NestedAttributesMap attributes = new NestedAttributesMap("attributes", backing);
    when(asset.attributes()).thenReturn(attributes);

    Map<String, Object> result = FormatAttributesUtils.getFormatAttributes(asset, "raw");
    assertThat(result.get("extension"), is("jar"));
  }

  @Test
  public void testGetFormatAttributesFromFluentComponent() {
    FluentComponent fluentComponent = mock(FluentComponent.class);
    when(fluentComponent.repository()).thenReturn(repository);
    Map<String, Object> backing = new HashMap<>();
    Map<String, Object> formatAttrs = new HashMap<>();
    formatAttrs.put("packaging", "pom");
    backing.put(FORMAT_NAME, formatAttrs);
    NestedAttributesMap attributes = new NestedAttributesMap("attributes", backing);
    when(fluentComponent.attributes()).thenReturn(attributes);

    Map<String, Object> result = FormatAttributesUtils.getFormatAttributes(fluentComponent);
    assertThat(result.get("packaging"), is("pom"));
  }

  @Test
  public void testSetFormatAttributesOnFluentAsset() {
    FluentAsset fluentAsset = mock(FluentAsset.class);
    when(fluentAsset.repository()).thenReturn(repository);
    NestedAttributesMap attributes = new NestedAttributesMap("attributes", new HashMap<>());
    when(fluentAsset.attributes()).thenReturn(attributes);

    Map<String, Object> values = new HashMap<>();
    values.put("newKey", "newValue");
    FormatAttributesUtils.setFormatAttributes(fluentAsset, values);

    verify(fluentAsset).withAttribute(eq(FORMAT_NAME), eq(values));
  }

  @Test
  public void testSetFormatAttributesSingleKeyValue() {
    FluentAsset fluentAsset = mock(FluentAsset.class);
    when(fluentAsset.repository()).thenReturn(repository);
    NestedAttributesMap attributes = new NestedAttributesMap("attributes", new HashMap<>());
    when(fluentAsset.attributes()).thenReturn(attributes);

    FormatAttributesUtils.setFormatAttributes(fluentAsset, "singleKey", "singleValue");

    Map<String, Object> expected = new HashMap<>();
    expected.put("singleKey", "singleValue");
    verify(fluentAsset).withAttribute(eq(FORMAT_NAME), eq(expected));
  }

  @Test
  public void testSetFormatAttributesWithSupplier() {
    FluentAsset fluentAsset = mock(FluentAsset.class);
    when(fluentAsset.repository()).thenReturn(repository);
    NestedAttributesMap attributes = new NestedAttributesMap("attributes", new HashMap<>());
    when(fluentAsset.attributes()).thenReturn(attributes);

    Map<String, Object> suppliedValues = Collections.singletonMap("suppliedKey", "suppliedValue");
    FormatAttributesUtils.setFormatAttributes(fluentAsset, () -> suppliedValues);

    Map<String, Object> expected = new HashMap<>(suppliedValues);
    verify(fluentAsset).withAttribute(eq(FORMAT_NAME), eq(expected));
  }

  @Test
  public void testSetFormatAttributesOnFluentComponent() {
    FluentComponent fluentComponent = mock(FluentComponent.class);
    when(fluentComponent.repository()).thenReturn(repository);
    NestedAttributesMap attributes = new NestedAttributesMap("attributes", new HashMap<>());
    when(fluentComponent.attributes()).thenReturn(attributes);

    Map<String, Object> values = new HashMap<>();
    values.put("groupId", "org.example");
    FormatAttributesUtils.setFormatAttributes(fluentComponent, values);

    verify(fluentComponent).withAttribute(eq(FORMAT_NAME), eq(values));
  }

  @Test
  public void testRemoveFormatAttributesFromFluentAsset() {
    FluentAsset fluentAsset = mock(FluentAsset.class);
    when(fluentAsset.repository()).thenReturn(repository);
    Map<String, Object> backing = new HashMap<>();
    Map<String, Object> formatAttrs = new HashMap<>();
    formatAttrs.put("keep", "yes");
    formatAttrs.put("remove", "this");
    backing.put(FORMAT_NAME, formatAttrs);
    NestedAttributesMap attributes = new NestedAttributesMap("attributes", backing);
    when(fluentAsset.attributes()).thenReturn(attributes);

    FormatAttributesUtils.removeFormatAttributes(fluentAsset, Set.of("remove"));

    Map<String, Object> expected = new HashMap<>();
    expected.put("keep", "yes");
    verify(fluentAsset).withAttribute(eq(FORMAT_NAME), eq(expected));
  }

  @Test
  public void testRemoveFormatAttributesSingleKeyFromFluentAsset() {
    FluentAsset fluentAsset = mock(FluentAsset.class);
    when(fluentAsset.repository()).thenReturn(repository);
    Map<String, Object> backing = new HashMap<>();
    Map<String, Object> formatAttrs = new HashMap<>();
    formatAttrs.put("a", "1");
    formatAttrs.put("b", "2");
    backing.put(FORMAT_NAME, formatAttrs);
    NestedAttributesMap attributes = new NestedAttributesMap("attributes", backing);
    when(fluentAsset.attributes()).thenReturn(attributes);

    FormatAttributesUtils.removeFormatAttributes(fluentAsset, "a");

    Map<String, Object> expected = new HashMap<>();
    expected.put("b", "2");
    verify(fluentAsset).withAttribute(eq(FORMAT_NAME), eq(expected));
  }

  @Test
  public void testRemoveFormatAttributesFromFluentComponent() {
    FluentComponent fluentComponent = mock(FluentComponent.class);
    when(fluentComponent.repository()).thenReturn(repository);
    Map<String, Object> backing = new HashMap<>();
    Map<String, Object> formatAttrs = new HashMap<>();
    formatAttrs.put("x", "1");
    backing.put(FORMAT_NAME, formatAttrs);
    NestedAttributesMap attributes = new NestedAttributesMap("attributes", backing);
    when(fluentComponent.attributes()).thenReturn(attributes);

    FormatAttributesUtils.removeFormatAttributes(fluentComponent, Set.of("x"));

    Map<String, Object> expected = new HashMap<>();
    verify(fluentComponent).withAttribute(eq(FORMAT_NAME), eq(expected));
  }

  @Test
  public void testRemoveFormatAttributesSingleKeyFromFluentComponent() {
    FluentComponent fluentComponent = mock(FluentComponent.class);
    when(fluentComponent.repository()).thenReturn(repository);
    Map<String, Object> backing = new HashMap<>();
    Map<String, Object> formatAttrs = new HashMap<>();
    formatAttrs.put("key1", "val1");
    backing.put(FORMAT_NAME, formatAttrs);
    NestedAttributesMap attributes = new NestedAttributesMap("attributes", backing);
    when(fluentComponent.attributes()).thenReturn(attributes);

    FormatAttributesUtils.removeFormatAttributes(fluentComponent, "key1");

    Map<String, Object> expected = new HashMap<>();
    verify(fluentComponent).withAttribute(eq(FORMAT_NAME), eq(expected));
  }

  @Test
  public void testGetFormatAttributesHandlesImmutableMap() {
    Asset asset = mock(Asset.class);
    Map<String, Object> backing = new HashMap<>();
    backing.put("raw", Collections.singletonMap("immutableKey", "immutableValue"));
    NestedAttributesMap attributes = new NestedAttributesMap("attributes", backing);
    when(asset.attributes()).thenReturn(attributes);

    Map<String, Object> result = FormatAttributesUtils.getFormatAttributes(asset, "raw");
    assertThat(result.get("immutableKey"), is("immutableValue"));
    // Should be mutable since it's converted to HashMap
    result.put("newKey", "newValue");
    assertThat(result.get("newKey"), is("newValue"));
  }
}
