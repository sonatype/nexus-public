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
package org.sonatype.nexus.repository.content.store.internal;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.sonatype.nexus.audit.AuditData;
import org.sonatype.nexus.audit.AuditRecorder;
import org.sonatype.nexus.blobstore.DefaultBlobIdLocationResolver;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.common.event.EventHelper;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.AttributeChangeSet.AttributeChange;
import org.sonatype.nexus.repository.content.AttributeOperation;
import org.sonatype.nexus.repository.content.event.asset.AssetAttributesEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetCreatedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetDeletedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetPurgedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetRestoredEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentPurgedEvent;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.store.AssetData;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class AssetAuditorTest
{
  private static final String REPO_NAME = "test-repo";

  private static final String ASSET_PATH = "/org/example/artifact-1.0.jar";

  private static final String ASSET_KIND = "ARTIFACT";

  private static final String BLOB_STORE_NAME = "default";

  private static final String BLOB_ID_STRING = "86e20baa-1a02-40e5-8542-2b2b8787e30b";

  @Mock
  private DefaultBlobIdLocationResolver blobIdLocationResolver;

  @Mock
  private AuditRecorder auditRecorder;

  @Mock
  private Repository repository;

  private AssetAuditor underTest;

  @Before
  public void setUp() {
    when(repository.getName()).thenReturn(REPO_NAME);
    when(blobIdLocationResolver.getLocation(any(BlobId.class))).thenReturn("vol-01/chap-01/" + BLOB_ID_STRING);
  }

  private AssetAuditor createAuditor(final boolean attributeChangesDetailEnabled, final boolean recorderEnabled) {
    AssetAuditor auditor = spy(new AssetAuditor(attributeChangesDetailEnabled, blobIdLocationResolver));
    when(auditRecorder.isEnabled()).thenReturn(recorderEnabled);
    auditor.setAuditRecorder(auditRecorder);
    return auditor;
  }

  private AssetData createAssetData() {
    AssetData assetData = new AssetData();
    assetData.setRepositoryId(1);
    assetData.setPath(ASSET_PATH);
    assetData.setKind(ASSET_KIND);
    return assetData;
  }

  @Test
  public void testConstruction() {
    underTest = new AssetAuditor(true, blobIdLocationResolver);
    assertThat(underTest, is(notNullValue()));
  }

  @Test
  public void testDomainConstant() {
    assertThat(AssetAuditor.DOMAIN, is("repository.asset"));
  }

  @Test
  public void testOnAssetPurgedEvent_whenRecording_noAssets_fallsBackToIds() {
    underTest = createAuditor(true, true);
    int[] assetIds = {1, 2, 3};

    AssetPurgedEvent event = mock(AssetPurgedEvent.class);
    when(event.getAssetIds()).thenReturn(assetIds);
    when(event.getAssets()).thenReturn(null);
    when(event.getRepository()).thenReturn(Optional.of(repository));

    try (MockedStatic<EventHelper> mockedStatic = mockStatic(EventHelper.class)) {
      mockedStatic.when(EventHelper::isReplicating).thenReturn(false);
      underTest.on(event);
    }

    ArgumentCaptor<AuditData> captor = ArgumentCaptor.forClass(AuditData.class);
    verify(auditRecorder).record(captor.capture());

    AuditData auditData = captor.getValue();
    assertThat(auditData.getDomain(), is("repository.asset"));
    assertThat(auditData.getType(), is("purged"));
    assertThat(auditData.getContext(), is(REPO_NAME));

    Map<String, Object> attributes = auditData.getAttributes();
    assertThat(attributes, hasEntry("repository.name", REPO_NAME));
    assertThat(attributes, hasEntry("assetIds", Arrays.toString(assetIds)));
  }

  @Test
  public void testOnAssetPurgedEvent_whenRecording_withAssets_logsPerAssetDetails() {
    underTest = createAuditor(true, true);
    int[] assetIds = {1, 2};

    AssetBlob blob1 = mock(AssetBlob.class);
    BlobRef blobRef1 = new BlobRef(BLOB_STORE_NAME, BLOB_ID_STRING);
    when(blob1.blobRef()).thenReturn(blobRef1);

    Asset asset1 = mock(Asset.class);
    when(asset1.path()).thenReturn(ASSET_PATH);
    when(asset1.kind()).thenReturn(ASSET_KIND);
    when(asset1.blob()).thenReturn(Optional.of(blob1));

    Asset asset2 = mock(Asset.class);
    when(asset2.path()).thenReturn("/other/asset.jar");
    when(asset2.kind()).thenReturn(ASSET_KIND);
    when(asset2.blob()).thenReturn(Optional.empty());

    AssetPurgedEvent event = mock(AssetPurgedEvent.class);
    when(event.getAssetIds()).thenReturn(assetIds);
    when(event.getAssets()).thenReturn(List.of(asset1, asset2));
    when(event.getRepository()).thenReturn(Optional.of(repository));

    try (MockedStatic<EventHelper> mockedStatic = mockStatic(EventHelper.class)) {
      mockedStatic.when(EventHelper::isReplicating).thenReturn(false);
      underTest.on(event);
    }

    ArgumentCaptor<AuditData> captor = ArgumentCaptor.forClass(AuditData.class);
    verify(auditRecorder, times(2)).record(captor.capture());

    List<AuditData> capturedData = captor.getAllValues();

    AuditData data1 = capturedData.get(0);
    assertThat(data1.getDomain(), is("repository.asset"));
    assertThat(data1.getType(), is("purged"));
    assertThat(data1.getContext(), is(ASSET_PATH));
    assertThat(data1.getAttributes(), hasEntry("repository.name", REPO_NAME));
    assertThat(data1.getAttributes(), hasEntry("path", ASSET_PATH));
    assertThat(data1.getAttributes(), hasEntry("kind", ASSET_KIND));
    assertThat((String) data1.getAttributes().get("blob.path"), containsString(BLOB_STORE_NAME + "/content/"));

    AuditData data2 = capturedData.get(1);
    assertThat(data2.getContext(), is("/other/asset.jar"));
    assertThat(data2.getAttributes(), hasEntry("path", "/other/asset.jar"));
    assertThat(data2.getAttributes().get("blob.path"), is("unknown/content/unknown"));
  }

  @Test
  public void testOnComponentPurgedEvent_withAssets_logsPerAssetDetails() {
    underTest = createAuditor(true, true);

    AssetBlob blob = mock(AssetBlob.class);
    BlobRef blobRef = new BlobRef(BLOB_STORE_NAME, BLOB_ID_STRING);
    when(blob.blobRef()).thenReturn(blobRef);

    FluentAsset fluentAsset = mock(FluentAsset.class);
    when(fluentAsset.path()).thenReturn(ASSET_PATH);
    when(fluentAsset.kind()).thenReturn(ASSET_KIND);
    when(fluentAsset.blob()).thenReturn(Optional.of(blob));

    ComponentPurgedEvent event = mock(ComponentPurgedEvent.class);
    when(event.getComponentIds()).thenReturn(new int[]{10});
    when(event.getAssets()).thenReturn(List.of(fluentAsset));
    when(event.getRepository()).thenReturn(Optional.of(repository));

    try (MockedStatic<EventHelper> mockedStatic = mockStatic(EventHelper.class)) {
      mockedStatic.when(EventHelper::isReplicating).thenReturn(false);
      underTest.on(event);
    }

    ArgumentCaptor<AuditData> captor = ArgumentCaptor.forClass(AuditData.class);
    verify(auditRecorder).record(captor.capture());

    AuditData auditData = captor.getValue();
    assertThat(auditData.getDomain(), is("repository.asset"));
    assertThat(auditData.getType(), is("purged"));
    assertThat(auditData.getContext(), is(ASSET_PATH));

    Map<String, Object> attributes = auditData.getAttributes();
    assertThat(attributes, hasEntry("repository.name", REPO_NAME));
    assertThat(attributes, hasEntry("path", ASSET_PATH));
    assertThat(attributes, hasEntry("kind", ASSET_KIND));
    assertThat((String) attributes.get("blob.path"), containsString(BLOB_STORE_NAME + "/content/"));
  }

  @Test
  public void testOnComponentPurgedEvent_withoutAssets_fallsBackToComponentIds() {
    underTest = createAuditor(true, true);
    int[] componentIds = {10};

    ComponentPurgedEvent event = mock(ComponentPurgedEvent.class);
    when(event.getComponentIds()).thenReturn(componentIds);
    when(event.getAssets()).thenReturn(null);
    when(event.getRepository()).thenReturn(Optional.of(repository));

    try (MockedStatic<EventHelper> mockedStatic = mockStatic(EventHelper.class)) {
      mockedStatic.when(EventHelper::isReplicating).thenReturn(false);
      underTest.on(event);
    }

    ArgumentCaptor<AuditData> captor = ArgumentCaptor.forClass(AuditData.class);
    verify(auditRecorder).record(captor.capture());

    AuditData auditData = captor.getValue();
    assertThat(auditData.getDomain(), is("repository.asset"));
    assertThat(auditData.getType(), is("purged"));
    assertThat(auditData.getContext(), is(REPO_NAME));

    Map<String, Object> attributes = auditData.getAttributes();
    assertThat(attributes, hasEntry("repository.name", REPO_NAME));
    assertThat(attributes, hasEntry("componentIds", Arrays.toString(componentIds)));
  }

  @Test
  public void testOnComponentPurgedEvent_withEmptyAssets_fallsBackToComponentIds() {
    underTest = createAuditor(true, true);
    int[] componentIds = {10};

    ComponentPurgedEvent event = mock(ComponentPurgedEvent.class);
    when(event.getComponentIds()).thenReturn(componentIds);
    when(event.getAssets()).thenReturn(List.of());
    when(event.getRepository()).thenReturn(Optional.of(repository));

    try (MockedStatic<EventHelper> mockedStatic = mockStatic(EventHelper.class)) {
      mockedStatic.when(EventHelper::isReplicating).thenReturn(false);
      underTest.on(event);
    }

    ArgumentCaptor<AuditData> captor = ArgumentCaptor.forClass(AuditData.class);
    verify(auditRecorder).record(captor.capture());

    AuditData auditData = captor.getValue();
    assertThat(auditData.getDomain(), is("repository.asset"));
    assertThat(auditData.getType(), is("purged"));
    assertThat(auditData.getContext(), is(REPO_NAME));

    Map<String, Object> attributes = auditData.getAttributes();
    assertThat(attributes, hasEntry("repository.name", REPO_NAME));
    assertThat(attributes, hasEntry("componentIds", Arrays.toString(componentIds)));
  }

  @Test
  public void testOnComponentPurgedEvent_notRecording_noAuditRecord() {
    underTest = createAuditor(true, false);

    FluentAsset fluentAsset = mock(FluentAsset.class);
    when(fluentAsset.path()).thenReturn(ASSET_PATH);
    when(fluentAsset.kind()).thenReturn(ASSET_KIND);
    when(fluentAsset.blob()).thenReturn(Optional.empty());

    ComponentPurgedEvent event = mock(ComponentPurgedEvent.class);
    when(event.getComponentIds()).thenReturn(new int[]{10});
    when(event.getAssets()).thenReturn(List.of(fluentAsset));
    when(event.getRepository()).thenReturn(Optional.of(repository));

    try (MockedStatic<EventHelper> mockedStatic = mockStatic(EventHelper.class)) {
      mockedStatic.when(EventHelper::isReplicating).thenReturn(false);
      underTest.on(event);
    }

    verify(auditRecorder, never()).record(any(AuditData.class));
  }

  @Test
  public void testOnAssetPurgedEvent_whenNotRecording() {
    underTest = createAuditor(true, false);

    AssetPurgedEvent event = mock(AssetPurgedEvent.class);
    when(event.getAssetIds()).thenReturn(new int[]{1, 2});
    when(event.getRepository()).thenReturn(Optional.of(repository));

    try (MockedStatic<EventHelper> mockedStatic = mockStatic(EventHelper.class)) {
      mockedStatic.when(EventHelper::isReplicating).thenReturn(false);
      underTest.on(event);
    }

    verify(auditRecorder, never()).record(any(AuditData.class));
  }

  @Test
  public void testOnAssetCreatedEvent_withBlob() {
    underTest = createAuditor(true, true);

    AssetBlob assetBlob = mock(AssetBlob.class);
    BlobRef blobRef = new BlobRef(BLOB_STORE_NAME, BLOB_ID_STRING);
    when(assetBlob.blobRef()).thenReturn(blobRef);

    Asset asset = mock(Asset.class);
    when(asset.path()).thenReturn(ASSET_PATH);
    when(asset.kind()).thenReturn(ASSET_KIND);
    when(asset.blob()).thenReturn(Optional.of(assetBlob));

    AssetCreatedEvent event = mock(AssetCreatedEvent.class);
    when(event.getAsset()).thenReturn(asset);
    when(event.getRepository()).thenReturn(Optional.of(repository));

    try (MockedStatic<EventHelper> mockedStatic = mockStatic(EventHelper.class)) {
      mockedStatic.when(EventHelper::isReplicating).thenReturn(false);
      underTest.on(event);
    }

    ArgumentCaptor<AuditData> captor = ArgumentCaptor.forClass(AuditData.class);
    verify(auditRecorder).record(captor.capture());

    AuditData auditData = captor.getValue();
    assertThat(auditData.getDomain(), is("repository.asset"));
    assertThat(auditData.getType(), is("created"));
    assertThat(auditData.getContext(), is(ASSET_PATH));

    Map<String, Object> attributes = auditData.getAttributes();
    assertThat(attributes, hasEntry("repository.name", REPO_NAME));
    assertThat(attributes, hasEntry("path", ASSET_PATH));
    assertThat(attributes, hasEntry("kind", ASSET_KIND));
    assertThat((String) attributes.get("blob.path"), containsString(BLOB_STORE_NAME + "/content/"));
    assertThat((String) attributes.get("blob.path"), containsString(BLOB_ID_STRING));
  }

  @Test
  public void testOnAssetDeletedEvent_withNoBlob() {
    underTest = createAuditor(true, true);

    Asset asset = mock(Asset.class);
    when(asset.path()).thenReturn(ASSET_PATH);
    when(asset.kind()).thenReturn(ASSET_KIND);
    when(asset.blob()).thenReturn(Optional.empty());

    AssetDeletedEvent event = mock(AssetDeletedEvent.class);
    when(event.getAsset()).thenReturn(asset);
    when(event.getRepository()).thenReturn(Optional.of(repository));

    try (MockedStatic<EventHelper> mockedStatic = mockStatic(EventHelper.class)) {
      mockedStatic.when(EventHelper::isReplicating).thenReturn(false);
      underTest.on(event);
    }

    ArgumentCaptor<AuditData> captor = ArgumentCaptor.forClass(AuditData.class);
    verify(auditRecorder).record(captor.capture());

    AuditData auditData = captor.getValue();
    assertThat(auditData.getDomain(), is("repository.asset"));
    assertThat(auditData.getType(), is("deleted"));
    assertThat(auditData.getContext(), is(ASSET_PATH));

    Map<String, Object> attributes = auditData.getAttributes();
    assertThat(attributes, hasEntry("repository.name", REPO_NAME));
    assertThat(attributes, hasEntry("path", ASSET_PATH));
    assertThat(attributes, hasEntry("kind", ASSET_KIND));
    assertThat((String) attributes.get("blob.path"), is("unknown/content/unknown"));
  }

  @Test
  public void testOnAssetRestoredEvent_auditsAsRestored() {
    // A restore (un-trash) must audit under the dedicated "restored" type, not the raw lowercased class name
    // ("assetrestoredevent"). AssetRestoredEvent is handled by the generic on(AssetEvent) path (NEXUS-55233).
    underTest = createAuditor(true, true);

    Asset asset = mock(Asset.class);
    when(asset.path()).thenReturn(ASSET_PATH);
    when(asset.kind()).thenReturn(ASSET_KIND);
    when(asset.blob()).thenReturn(Optional.empty());

    AssetRestoredEvent event = mock(AssetRestoredEvent.class);
    when(event.getAsset()).thenReturn(asset);
    when(event.getRepository()).thenReturn(Optional.of(repository));

    try (MockedStatic<EventHelper> mockedStatic = mockStatic(EventHelper.class)) {
      mockedStatic.when(EventHelper::isReplicating).thenReturn(false);
      underTest.on(event);
    }

    ArgumentCaptor<AuditData> captor = ArgumentCaptor.forClass(AuditData.class);
    verify(auditRecorder).record(captor.capture());

    AuditData auditData = captor.getValue();
    assertThat(auditData.getDomain(), is("repository.asset"));
    assertThat(auditData.getType(), is("restored"));
    assertThat(auditData.getContext(), is(ASSET_PATH));
  }

  @Test
  public void testOnAssetAttributesEvent_whenAttributeChangesDetailEnabled() {
    underTest = createAuditor(true, true);

    AssetData assetData = createAssetData();

    AttributeChange change1 = mock(AttributeChange.class);
    when(change1.getOperation()).thenReturn(AttributeOperation.SET);
    when(change1.getKey()).thenReturn("checksum.sha1");
    when(change1.getValue()).thenReturn("abc123");

    AttributeChange change2 = mock(AttributeChange.class);
    when(change2.getOperation()).thenReturn(AttributeOperation.REMOVE);
    when(change2.getKey()).thenReturn("obsolete.key");
    when(change2.getValue()).thenReturn(null);

    AssetAttributesEvent event = mock(AssetAttributesEvent.class);
    when(event.getAsset()).thenReturn(assetData);
    when(event.getRepository()).thenReturn(Optional.of(repository));
    when(event.getChanges()).thenReturn(Arrays.asList(change1, change2));

    try (MockedStatic<EventHelper> mockedStatic = mockStatic(EventHelper.class)) {
      mockedStatic.when(EventHelper::isReplicating).thenReturn(false);
      underTest.on(event);
    }

    ArgumentCaptor<AuditData> captor = ArgumentCaptor.forClass(AuditData.class);
    verify(auditRecorder).record(captor.capture());

    AuditData auditData = captor.getValue();
    assertThat(auditData.getDomain(), is("repository.asset"));
    assertThat(auditData.getType(), is("updated-attribute"));
    assertThat(auditData.getContext(), is(ASSET_PATH));

    Map<String, Object> attributes = auditData.getAttributes();
    assertThat(attributes, hasKey("attribute.changes"));

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> changes = (List<Map<String, Object>>) attributes.get("attribute.changes");
    assertThat(changes.size(), is(2));

    Map<String, Object> firstChange = changes.get(0);
    assertThat(firstChange.get("operation"), is(AttributeOperation.SET));
    assertThat(firstChange.get("key"), is("checksum.sha1"));
    assertThat(firstChange.get("value"), is("abc123"));

    Map<String, Object> secondChange = changes.get(1);
    assertThat(secondChange.get("operation"), is(AttributeOperation.REMOVE));
    assertThat(secondChange.get("key"), is("obsolete.key"));
  }

  @Test
  public void testOnAssetAttributesEvent_whenAttributeChangesDetailDisabled() {
    underTest = createAuditor(false, true);

    AssetData assetData = createAssetData();

    AssetAttributesEvent event = mock(AssetAttributesEvent.class);
    when(event.getAsset()).thenReturn(assetData);
    when(event.getRepository()).thenReturn(Optional.of(repository));

    try (MockedStatic<EventHelper> mockedStatic = mockStatic(EventHelper.class)) {
      mockedStatic.when(EventHelper::isReplicating).thenReturn(false);
      underTest.on(event);
    }

    ArgumentCaptor<AuditData> captor = ArgumentCaptor.forClass(AuditData.class);
    verify(auditRecorder).record(captor.capture());

    AuditData auditData = captor.getValue();
    assertThat(auditData.getDomain(), is("repository.asset"));
    assertThat(auditData.getType(), is("updated-attribute"));

    Map<String, Object> attributes = auditData.getAttributes();
    assertThat(attributes, not(hasKey("attribute.changes")));

    verify(event, never()).getChanges();
  }

  @Test
  public void testOnAssetEvent_whenNotRecording() {
    underTest = createAuditor(true, false);

    Asset asset = mock(Asset.class);

    AssetCreatedEvent event = mock(AssetCreatedEvent.class);
    when(event.getAsset()).thenReturn(asset);

    try (MockedStatic<EventHelper> mockedStatic = mockStatic(EventHelper.class)) {
      mockedStatic.when(EventHelper::isReplicating).thenReturn(false);
      underTest.on(event);
    }

    verify(auditRecorder, never()).record(any(AuditData.class));
    verify(asset, never()).path();
  }

  @Test
  public void testOnAssetEvent_whenRepositoryIsEmpty() {
    underTest = createAuditor(true, true);

    Asset asset = mock(Asset.class);
    when(asset.path()).thenReturn(ASSET_PATH);
    when(asset.kind()).thenReturn(ASSET_KIND);
    when(asset.blob()).thenReturn(Optional.empty());

    AssetCreatedEvent event = mock(AssetCreatedEvent.class);
    when(event.getAsset()).thenReturn(asset);
    when(event.getRepository()).thenReturn(Optional.empty());

    try (MockedStatic<EventHelper> mockedStatic = mockStatic(EventHelper.class)) {
      mockedStatic.when(EventHelper::isReplicating).thenReturn(false);
      underTest.on(event);
    }

    ArgumentCaptor<AuditData> captor = ArgumentCaptor.forClass(AuditData.class);
    verify(auditRecorder).record(captor.capture());

    AuditData auditData = captor.getValue();
    Map<String, Object> attributes = auditData.getAttributes();
    assertThat(attributes, hasEntry("repository.name", "Unknown"));
  }

  @Test
  public void testOnAssetPurgedEvent_whenRepositoryIsEmpty() {
    underTest = createAuditor(true, true);

    int[] assetIds = {10, 20};
    AssetPurgedEvent event = mock(AssetPurgedEvent.class);
    when(event.getAssetIds()).thenReturn(assetIds);
    when(event.getRepository()).thenReturn(Optional.empty());

    try (MockedStatic<EventHelper> mockedStatic = mockStatic(EventHelper.class)) {
      mockedStatic.when(EventHelper::isReplicating).thenReturn(false);
      underTest.on(event);
    }

    ArgumentCaptor<AuditData> captor = ArgumentCaptor.forClass(AuditData.class);
    verify(auditRecorder).record(captor.capture());

    AuditData auditData = captor.getValue();
    assertThat(auditData.getContext(), is("Unknown"));

    Map<String, Object> attributes = auditData.getAttributes();
    assertThat(attributes, hasEntry("repository.name", "Unknown"));
  }

  @Test
  public void testOnAssetPurgedEvent_notRecordingWhenReplicating() {
    underTest = createAuditor(false, true);

    int[] assetIds = {5};
    AssetPurgedEvent event = mock(AssetPurgedEvent.class);
    when(event.getAssetIds()).thenReturn(assetIds);
    when(event.getRepository()).thenReturn(Optional.of(repository));

    try (MockedStatic<EventHelper> mockedStatic = mockStatic(EventHelper.class)) {
      mockedStatic.when(EventHelper::isReplicating).thenReturn(true);
      underTest.on(event);
    }

    verify(auditRecorder, never()).record(any(AuditData.class));
  }

  @Test
  public void testOnAssetEvent_notRecordingWhenReplicating() {
    underTest = createAuditor(true, true);

    Asset asset = mock(Asset.class);

    AssetCreatedEvent event = mock(AssetCreatedEvent.class);
    when(event.getAsset()).thenReturn(asset);

    try (MockedStatic<EventHelper> mockedStatic = mockStatic(EventHelper.class)) {
      mockedStatic.when(EventHelper::isReplicating).thenReturn(true);
      underTest.on(event);
    }

    verify(auditRecorder, never()).record(any(AuditData.class));
    verify(asset, never()).path();
  }

  @Test
  public void testOnAssetCreatedEvent_blobPathComputedCorrectly() {
    underTest = createAuditor(true, true);

    String expectedBlobStorePath = "vol-01/chap-01/" + BLOB_ID_STRING;
    when(blobIdLocationResolver.getLocation(any(BlobId.class))).thenReturn(expectedBlobStorePath);

    AssetBlob assetBlob = mock(AssetBlob.class);
    BlobRef blobRef = new BlobRef(BLOB_STORE_NAME, BLOB_ID_STRING);
    when(assetBlob.blobRef()).thenReturn(blobRef);

    Asset asset = mock(Asset.class);
    when(asset.path()).thenReturn(ASSET_PATH);
    when(asset.kind()).thenReturn(ASSET_KIND);
    when(asset.blob()).thenReturn(Optional.of(assetBlob));

    AssetCreatedEvent event = mock(AssetCreatedEvent.class);
    when(event.getAsset()).thenReturn(asset);
    when(event.getRepository()).thenReturn(Optional.of(repository));

    try (MockedStatic<EventHelper> mockedStatic = mockStatic(EventHelper.class)) {
      mockedStatic.when(EventHelper::isReplicating).thenReturn(false);
      underTest.on(event);
    }

    ArgumentCaptor<AuditData> captor = ArgumentCaptor.forClass(AuditData.class);
    verify(auditRecorder).record(captor.capture());

    Map<String, Object> attributes = captor.getValue().getAttributes();
    assertThat(attributes.get("blob.path"), is(BLOB_STORE_NAME + "/content/" + expectedBlobStorePath));
  }
}
