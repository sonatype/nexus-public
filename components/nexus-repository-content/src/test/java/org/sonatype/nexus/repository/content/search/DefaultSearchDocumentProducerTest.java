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
package org.sonatype.nexus.repository.content.search;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.internal.FluentAssetImpl;
import org.sonatype.nexus.repository.content.store.AssetData;
import org.sonatype.nexus.repository.search.index.SearchConstants;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultSearchDocumentProducerTest
    extends TestSupport
{
  private static final String REPO_NAME = "repo";

  private static final String GROUP = "group";

  private static final String NAME = "name";

  private static final String VERSION = "1.2.3";

  private final ObjectMapper mapper = new ObjectMapper();

  @Mock
  private FluentComponent component;

  @Mock
  private SearchDocumentExtension searchDocumentExtension;

  private DefaultSearchDocumentProducer underTest;

  private Map<String, Object> commonFields;

  @Before
  public void setup() {
    NestedAttributesMap attributes = new NestedAttributesMap(SearchConstants.ATTRIBUTES, new HashMap<>());

    when(component.namespace()).thenReturn(GROUP);
    when(component.name()).thenReturn(NAME);
    when(component.version()).thenReturn(VERSION);
    when(component.attributes()).thenReturn(attributes);
    when(component.assets()).thenReturn(ImmutableList.of());

    commonFields = ImmutableMap.of(
        SearchConstants.REPOSITORY_NAME, REPO_NAME,
        SearchConstants.FORMAT, "format-id",
        "foo", "bar");

    underTest = new DefaultSearchDocumentProducer(ImmutableSet.of(searchDocumentExtension));
  }

  @Test
  public void testGetMetadata() throws IOException {

    FluentAsset asset = mockAsset(NAME, 1);
    when(component.assets()).thenReturn(ImmutableList.of(asset));

    String result = underTest.getDocument(component, commonFields);

    JsonNode json = mapper.readTree(result);

    assertValue(json, SearchConstants.FORMAT, "format-id");
    assertValue(json, SearchConstants.NAME, NAME);
    assertValue(json, SearchConstants.GROUP, GROUP);
    assertValue(json, SearchConstants.VERSION, VERSION);
    assertValue(json, SearchConstants.IS_PRERELEASE_KEY, "false");
    assertThat(json.get(SearchConstants.LAST_BLOB_UPDATED_KEY), equalTo(null));
    assertThat(json.get(SearchConstants.LAST_DOWNLOADED_KEY), equalTo(null));
    assertValue(json, "foo", "bar");

    JsonNode jsonAssets = json.get(SearchConstants.ASSETS);
    assertTrue(jsonAssets.isArray());
    assertThat(jsonAssets.size(), equalTo(1));
    JsonNode jsonAsset = jsonAssets.get(0);
    assertValue(jsonAsset, SearchConstants.NAME, NAME);

    verify(searchDocumentExtension).getFields(any(FluentComponent.class));
  }

  @Test
  public void testMissingVersion() throws IOException {

    FluentAsset asset = mockAsset(NAME, 1);
    when(component.assets()).thenReturn(ImmutableList.of(asset));
    when(component.version()).thenReturn("");

    String result = underTest.getDocument(component, emptyMap());

    JsonNode json = mapper.readTree(result);

    assertValue(json, SearchConstants.VERSION, "");
    assertValue(json, SearchConstants.NORMALIZED_VERSION, "");
  }

  @Test
  public void testNormalizedVersion() throws IOException {

    FluentAsset asset = mockAsset(NAME, 1);
    when(component.assets()).thenReturn(ImmutableList.of(asset));
    when(component.version()).thenReturn("11.11.11-10");

    String result = underTest.getDocument(component, emptyMap());

    JsonNode json = mapper.readTree(result);

    assertValue(json, SearchConstants.NORMALIZED_VERSION, "000000011.000000011.000000011-000000010");
  }

  @Test
  public void testLongNormalizedVersion() throws IOException {

    FluentAsset asset = mockAsset(NAME, 1);
    when(component.assets()).thenReturn(ImmutableList.of(asset));
    when(component.version()).thenReturn("v1-rev20181217-1.27.0123456789123456789123456789");

    String result = underTest.getDocument(component, emptyMap());

    JsonNode json = mapper.readTree(result);

    assertValue(json, SearchConstants.NORMALIZED_VERSION,
        "v000000001-rev020181217-000000001.000000027.0123456789123456789123456789");
  }

  @Test
  public void defaultLastBlobUpdatedUsesNewestUpload() throws Exception {
    OffsetDateTime expected = OffsetDateTime.now();

    FluentAsset asset1 = mockAsset(NAME, 1);
    AssetBlob blob1 = mockBlob(expected.minus(1, MILLIS));
    when(asset1.blob()).thenReturn(Optional.of(blob1));

    FluentAsset asset2 = mockAsset(NAME, 2);
    AssetBlob blob2 = mockBlob(expected);
    when(asset2.blob()).thenReturn(Optional.of(blob2));

    FluentAsset asset3 = mockAsset(NAME, 3);
    AssetBlob blob3 = mockBlob(expected.minus(2, MILLIS));
    when(asset3.blob()).thenReturn(Optional.of(blob3));

    Collection<Asset> assets = ImmutableList.of(asset1, asset2, asset3);

    OffsetDateTime actual = underTest.lastBlobUpdated(assets).get();

    assertThat(actual, is(expected));
  }

  @Test
  public void defaultLastDownloadedWhenNoDownloadsUsesNewestDownloadDate() throws Exception {
    OffsetDateTime expected = OffsetDateTime.now();

    FluentAsset asset1 = mockAsset(NAME, 1);
    AssetBlob blob1 = mockBlob(expected.minus(1, MILLIS));
    when(asset1.blob()).thenReturn(Optional.of(blob1));

    FluentAsset asset2 = mockAsset(NAME, 2);
    AssetBlob blob2 = mockBlob(expected);
    when(asset2.blob()).thenReturn(Optional.of(blob2));

    FluentAsset asset3 = mockAsset(NAME, 3);
    AssetBlob blob3 = mockBlob(expected.minus(2, MILLIS));
    when(asset3.blob()).thenReturn(Optional.of(blob3));

    Collection<Asset> assets = ImmutableList.of(asset1, asset2, asset3);

    assertThat(underTest.lastDownloaded(assets).isPresent(), is(false));
  }

  @Test
  public void defaultLastDownloadedWhenSingleAssetMarkedAsDownloaded() throws Exception {
    OffsetDateTime expected = OffsetDateTime.now();

    FluentAsset asset1 = mockAsset(NAME, 1);
    AssetBlob blob1 = mockBlob(expected.minus(1, MILLIS));
    when(asset1.blob()).thenReturn(Optional.of(blob1));
    when(asset1.lastDownloaded()).thenReturn(Optional.of(expected.minus(1, MILLIS)));

    FluentAsset asset2 = mockAsset(NAME, 2);
    AssetBlob blob2 = mockBlob(expected);
    when(asset2.blob()).thenReturn(Optional.of(blob2));
    when(asset2.lastDownloaded()).thenReturn(empty());

    FluentAsset asset3 = mockAsset(NAME, 3);
    AssetBlob blob3 = mockBlob(expected.minus(2, MILLIS));
    when(asset3.blob()).thenReturn(Optional.of(blob3));
    when(asset3.lastDownloaded()).thenReturn(Optional.of(expected));

    Collection<Asset> assets = ImmutableList.of(asset1, asset2, asset3);

    OffsetDateTime actual = underTest.lastDownloaded(assets).get();

    assertThat(actual, is(expected));
  }

  private static void assertValue(final JsonNode json, final String jsonAttribute, final String value) {
    assertThat(json.get(jsonAttribute).asText(), equalTo(value));
  }

  private static FluentAssetImpl mockAsset(final String path, final int id) {
    NestedAttributesMap attributes = new NestedAttributesMap(SearchConstants.ATTRIBUTES, new HashMap<>());

    FluentAssetImpl asset = mock(FluentAssetImpl.class);
    AssetData assetData = new AssetData();
    assetData.setAssetId(id);
    when(asset.unwrap()).thenReturn(assetData);
    when(asset.path()).thenReturn(path);
    when(asset.attributes()).thenReturn(attributes);
    when(asset.blob()).thenReturn(empty());
    when(asset.lastDownloaded()).thenReturn(empty());

    return asset;
  }

  private static AssetBlob mockBlob(final OffsetDateTime created) {

    AssetBlob assetBlob = mock(AssetBlob.class);
    when(assetBlob.blobCreated()).thenReturn(created);

    return assetBlob;
  }
}
