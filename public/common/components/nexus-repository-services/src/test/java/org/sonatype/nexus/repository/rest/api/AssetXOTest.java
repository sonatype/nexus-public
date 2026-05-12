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

import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.manager.internal.RepositoryImpl;
import org.sonatype.nexus.repository.rest.api.SimpleApiRepositoryAdapterTest.SimpleConfiguration;
import org.sonatype.nexus.repository.search.AssetSearchResult;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.HostedType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.mockito.Mockito;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

class AssetXOTest
{

  @BeforeEach
  void setup() {
    BaseUrlHolder.set("https://nexus-url", "");
  }

  @ParameterizedTest
  @CsvSource({
      "hosted, /path/to/resource, /hosted/path/to/resource",
      "hosted, path/to/resource, /hosted/path/to/resource"
  })
  void testFrom(String repositoryName, String path, String expectedUrl) throws Exception {
    Repository repository = createRepository(new HostedType(), repositoryName);
    AssetSearchResult assetSearchResult = Mockito.mock(AssetSearchResult.class);
    when(assetSearchResult.getRepository()).thenReturn(repositoryName);
    when(assetSearchResult.getPath()).thenReturn(path);
    when(assetSearchResult.getId()).thenReturn("resource-id");
    when(assetSearchResult.getFormat()).thenReturn("test-format");
    AssetXO assetXO = AssetXO.from(assetSearchResult, repository, null);
    assertThat(assetXO.getDownloadUrl(), containsString(expectedUrl));
  }

  @Test
  void testFromForGroup() throws Exception {
    Repository repository = createRepository(new GroupType(), "group");
    AssetSearchResult assetSearchResult = Mockito.mock(AssetSearchResult.class);
    when(assetSearchResult.getRepository()).thenReturn("hosted");
    when(assetSearchResult.getPath()).thenReturn("/path/to/resource");
    when(assetSearchResult.getId()).thenReturn("resource-id");
    when(assetSearchResult.getFormat()).thenReturn("test-format");
    AssetXO assetXO = AssetXO.from(assetSearchResult, repository, null);
    assertThat(assetXO.getDownloadUrl(), containsString("/group/path/to/resource"));
    // it should use the repository where it lives, not the group repository
    assertThat(assetXO.getRepository(), is("hosted"));
  }

  @Test
  void testFromIncludesBlobStoreName() throws Exception {
    Repository repository = createRepositoryWithBlobStore(new HostedType(), "hosted", "my-blob-store");
    AssetSearchResult assetSearchResult = Mockito.mock(AssetSearchResult.class);
    when(assetSearchResult.getRepository()).thenReturn("hosted");
    when(assetSearchResult.getPath()).thenReturn("/path/to/resource");
    when(assetSearchResult.getId()).thenReturn("resource-id");
    when(assetSearchResult.getFormat()).thenReturn("test-format");

    AssetXO assetXO = AssetXO.from(assetSearchResult, repository, null);

    assertThat(assetXO.getBlobStoreName(), is("my-blob-store"));
  }

  @Test
  void testFromIncludesBlobCreated() throws Exception {
    Repository repository = createRepository(new HostedType(), "hosted");
    AssetSearchResult assetSearchResult = Mockito.mock(AssetSearchResult.class);
    when(assetSearchResult.getRepository()).thenReturn("hosted");
    when(assetSearchResult.getPath()).thenReturn("/path/to/resource");
    when(assetSearchResult.getId()).thenReturn("resource-id");
    when(assetSearchResult.getFormat()).thenReturn("test-format");
    java.util.Date blobCreatedDate = new java.util.Date();
    when(assetSearchResult.getBlobCreated()).thenReturn(blobCreatedDate);

    AssetXO assetXO = AssetXO.from(assetSearchResult, repository, null);

    assertThat(assetXO.getBlobCreated(), is(blobCreatedDate));
  }

  @Test
  void testFromIncludesBlobUpdated() throws Exception {
    Repository repository = createRepository(new HostedType(), "hosted");
    AssetSearchResult assetSearchResult = Mockito.mock(AssetSearchResult.class);
    when(assetSearchResult.getRepository()).thenReturn("hosted");
    when(assetSearchResult.getPath()).thenReturn("/path/to/resource");
    when(assetSearchResult.getId()).thenReturn("resource-id");
    when(assetSearchResult.getFormat()).thenReturn("test-format");
    java.util.Date blobUpdatedDate = new java.util.Date();
    when(assetSearchResult.getBlobUpdated()).thenReturn(blobUpdatedDate);

    AssetXO assetXO = AssetXO.from(assetSearchResult, repository, null);

    assertThat(assetXO.getBlobUpdated(), is(blobUpdatedDate));
  }

  @Test
  void testFromIncludesBlobRef() throws Exception {
    Repository repository = createRepository(new HostedType(), "hosted");
    AssetSearchResult assetSearchResult = Mockito.mock(AssetSearchResult.class);
    when(assetSearchResult.getRepository()).thenReturn("hosted");
    when(assetSearchResult.getPath()).thenReturn("/path/to/resource");
    when(assetSearchResult.getId()).thenReturn("resource-id");
    when(assetSearchResult.getFormat()).thenReturn("test-format");
    String blobRefString = "default@051ae249-9d2d-4807-85d0-9c920198b3b7@2025-11-13T09:31";
    when(assetSearchResult.getBlobRef()).thenReturn(blobRefString);

    AssetXO assetXO = AssetXO.from(assetSearchResult, repository, null);

    assertThat(assetXO.getBlobRef(), is(blobRefString));
  }

  @Test
  void testFromExtractsLastVerified() throws Exception {
    Repository repository = createRepository(new HostedType(), "hosted");
    AssetSearchResult assetSearchResult = Mockito.mock(AssetSearchResult.class);
    when(assetSearchResult.getRepository()).thenReturn("hosted");
    when(assetSearchResult.getPath()).thenReturn("/path/to/resource");
    when(assetSearchResult.getId()).thenReturn("resource-id");
    when(assetSearchResult.getFormat()).thenReturn("test-format");

    // Set up attributes with cache.last_verified
    Map<String, Object> attributes = new HashMap<>();
    Map<String, Object> cacheAttributes = new HashMap<>();
    long lastVerifiedTimestamp = System.currentTimeMillis();
    cacheAttributes.put("last_verified", lastVerifiedTimestamp);
    attributes.put("cache", cacheAttributes);
    when(assetSearchResult.getAttributes()).thenReturn(attributes);

    AssetXO assetXO = AssetXO.from(assetSearchResult, repository, null);

    assertThat(assetXO.getLastVerified(), is(notNullValue()));
    assertThat(assetXO.getLastVerified().getTime(), is(lastVerifiedTimestamp));
  }

  @Test
  void testFromHandlesMissingLastVerified() throws Exception {
    Repository repository = createRepository(new HostedType(), "hosted");
    AssetSearchResult assetSearchResult = Mockito.mock(AssetSearchResult.class);
    when(assetSearchResult.getRepository()).thenReturn("hosted");
    when(assetSearchResult.getPath()).thenReturn("/path/to/resource");
    when(assetSearchResult.getId()).thenReturn("resource-id");
    when(assetSearchResult.getFormat()).thenReturn("test-format");
    when(assetSearchResult.getAttributes()).thenReturn(new HashMap<>());

    AssetXO assetXO = AssetXO.from(assetSearchResult, repository, null);

    // Should not throw exception, lastVerified should be null
    assertThat(assetXO.getLastVerified(), is(nullValue()));
  }

  @Test
  void testGetExpandedAttributes_withExposedKeys() {
    Map<String, Object> attributes = new HashMap<>();
    Map<String, Object> formatAttributes = new HashMap<>();
    formatAttributes.put("key1", "value1");
    formatAttributes.put("key2", "value2");
    attributes.put("test-format", formatAttributes);

    Map<String, AssetXODescriptor> assetDescriptors = new HashMap<>();
    AssetXODescriptor descriptor = new TestAssetXODescriptor(Set.of("key1"));
    assetDescriptors.put("test-format", descriptor);

    Map<String, Object> result = AssetXO.getExpandedAttributes(attributes, "test-format", assetDescriptors);

    assertThat(result.size(), is(1));
    assertThat(result, hasKey("test-format"));
    Map<String, Object> resultFormatAttributes = (Map<String, Object>) result.get("test-format");
    assertThat(resultFormatAttributes.size(), is(1));
    assertThat(resultFormatAttributes.get("key1"), is("value1"));
  }

  @Test
  void testGetExpandedAttributes_withoutExposedKeys() {
    Map<String, Object> attributes = new HashMap<>();
    Map<String, Object> formatAttributes = new HashMap<>();
    formatAttributes.put("key1", "value1");
    formatAttributes.put("key2", "value2");
    attributes.put("test-format", formatAttributes);

    Map<String, AssetXODescriptor> assetDescriptors = new HashMap<>();
    AssetXODescriptor descriptor = new TestAssetXODescriptor(Set.of());
    assetDescriptors.put("test-format", descriptor);

    Map<String, Object> result = AssetXO.getExpandedAttributes(attributes, "test-format", assetDescriptors);

    assertThat(result.size(), is(1));
    assertThat(result, hasKey("test-format"));
    Map<String, Object> resultFormatAttributes = (Map<String, Object>) result.get("test-format");
    assertThat(resultFormatAttributes.isEmpty(), is(true));
  }

  @Test
  void testGetExpandedAttributes_withNullDescriptors() {
    Map<String, Object> attributes = new HashMap<>();
    Map<String, Object> formatAttributes = new HashMap<>();
    formatAttributes.put("key1", "value1");
    formatAttributes.put("key2", "value2");
    attributes.put("test-format", formatAttributes);

    Map<String, Object> result = AssetXO.getExpandedAttributes(attributes, "test-format", null);

    assertThat(result.size(), is(1));
    assertThat(result, hasKey("test-format"));
    Map<String, Object> resultFormatAttributes = (Map<String, Object>) result.get("test-format");
    assertThat(resultFormatAttributes.isEmpty(), is(true));
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

  private static Repository createRepositoryWithBlobStore(
      final Type type,
      String repositoryName,
      String blobStoreName) throws Exception
  {
    Repository repository = new RepositoryImpl(
        Mockito.mock(EventManager.class),
        type,
        new Format("test-format")
        {
        });
    Configuration configuration = configWithBlobStore(repositoryName, blobStoreName);
    repository.init(configuration);
    return repository;
  }

  private static Configuration configWithBlobStore(final String repositoryName, final String blobStoreName) {
    Configuration configuration = new SimpleConfiguration();
    configuration.setOnline(true);
    configuration.setRepositoryName(repositoryName);
    configuration.attributes("storage").set("blobStoreName", blobStoreName);
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
