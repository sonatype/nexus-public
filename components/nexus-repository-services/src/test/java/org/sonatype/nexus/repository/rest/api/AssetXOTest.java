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
package org.sonatype.nexus.repository.rest.api;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.manager.internal.RepositoryImpl;
import org.sonatype.nexus.repository.rest.api.SimpleApiRepositoryAdapterTest.SimpleConfiguration;
import org.sonatype.nexus.repository.search.AssetSearchResult;
import org.sonatype.nexus.repository.types.HostedType;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(JUnitParamsRunner.class)
public class AssetXOTest
    extends TestSupport
{

  @Before
  public void setup() {
    BaseUrlHolder.set("https://nexus-url", "");
  }

  @Test
  @Parameters({
      "hosted, /path/to/resource, /hosted/path/to/resource",
      "hosted, path/to/resource, /hosted/path/to/resource"
  })
  public void testFrom(String repositoryName, String path, String expectedUrl) throws Exception {
    Repository repository = createRepository(new HostedType(), repositoryName);
    AssetSearchResult assetSearchResult = Mockito.mock(AssetSearchResult.class);
    when(assetSearchResult.getPath()).thenReturn(path);
    when(assetSearchResult.getId()).thenReturn("resource-id");
    when(assetSearchResult.getFormat()).thenReturn("test-format");
    AssetXO assetXO = AssetXO.from(assetSearchResult, repository, null);
    Assert.assertTrue(assetXO.getDownloadUrl().contains(expectedUrl));
  }

  @Test
  public void testGetExpandedAttributes_withExposedKeys() {
    Map<String, Object> attributes = new HashMap<>();
    Map<String, Object> formatAttributes = new HashMap<>();
    formatAttributes.put("key1", "value1");
    formatAttributes.put("key2", "value2");
    attributes.put("test-format", formatAttributes);

    Map<String, AssetXODescriptor> assetDescriptors = new HashMap<>();
    AssetXODescriptor descriptor = new TestAssetXODescriptor(Set.of("key1"));
    assetDescriptors.put("test-format", descriptor);

    Map<String, Object> result = AssetXO.getExpandedAttributes(attributes, "test-format", assetDescriptors);

    assertEquals(1, result.size());
    assertTrue(result.containsKey("test-format"));
    Map<String, Object> resultFormatAttributes = (Map<String, Object>) result.get("test-format");
    assertEquals(1, resultFormatAttributes.size());
    assertEquals("value1", resultFormatAttributes.get("key1"));
  }

  @Test
  public void testGetExpandedAttributes_withoutExposedKeys() {
    Map<String, Object> attributes = new HashMap<>();
    Map<String, Object> formatAttributes = new HashMap<>();
    formatAttributes.put("key1", "value1");
    formatAttributes.put("key2", "value2");
    attributes.put("test-format", formatAttributes);

    Map<String, AssetXODescriptor> assetDescriptors = new HashMap<>();
    AssetXODescriptor descriptor = new TestAssetXODescriptor(Set.of());
    assetDescriptors.put("test-format", descriptor);

    Map<String, Object> result = AssetXO.getExpandedAttributes(attributes, "test-format", assetDescriptors);

    assertEquals(1, result.size());
    assertTrue(result.containsKey("test-format"));
    Map<String, Object> resultFormatAttributes = (Map<String, Object>) result.get("test-format");
    assertTrue(resultFormatAttributes.isEmpty());
  }

  @Test
  public void testGetExpandedAttributes_withNullDescriptors() {
    Map<String, Object> attributes = new HashMap<>();
    Map<String, Object> formatAttributes = new HashMap<>();
    formatAttributes.put("key1", "value1");
    formatAttributes.put("key2", "value2");
    attributes.put("test-format", formatAttributes);

    Map<String, Object> result = AssetXO.getExpandedAttributes(attributes, "test-format", null);

    assertEquals(1, result.size());
    assertTrue(result.containsKey("test-format"));
    Map<String, Object> resultFormatAttributes = (Map<String, Object>) result.get("test-format");
    assertTrue(resultFormatAttributes.isEmpty());
  }

  private static Repository createRepository(final Type type, String repositoryName) throws Exception {
    Repository repository = new RepositoryImpl(
        Mockito.mock(EventManager.class),
        type,
        new Format("test-format")
        {
        });
    repository.init(config(repositoryName));
    return repository;
  }

  private static Configuration config(final String repositoryName) {
    Configuration configuration = new SimpleConfiguration();
    configuration.setOnline(true);
    configuration.setRepositoryName(repositoryName);
    return configuration;
  }

  static class TestAssetXODescriptor
      implements AssetXODescriptor
  {
    private Set<String> exposedAttributeKeys;

    public TestAssetXODescriptor(Set<String> exposedAttributeKeys) {
      this.exposedAttributeKeys = exposedAttributeKeys;
    }

    @Override
    public Set<String> listExposedAttributeKeys() {
      return exposedAttributeKeys;
    }
  }
}
