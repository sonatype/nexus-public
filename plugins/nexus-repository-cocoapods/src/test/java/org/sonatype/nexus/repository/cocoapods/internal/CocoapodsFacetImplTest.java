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
package org.sonatype.nexus.repository.cocoapods.internal;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.cocoapods.CocoapodsFacet;
import org.sonatype.nexus.repository.cocoapods.internal.pod.PodPathParser;
import org.sonatype.nexus.repository.cocoapods.internal.proxy.SpecTransformer;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.DefaultComponent;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.transaction.TransactionModule;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @since 3.19
 */
public class CocoapodsFacetImplTest
    extends TestSupport
{
  private static final String NAME = "MyCheckWalletUI";

  private static final String VERSION = "1.2.3";

  private static final String PATH_URI = "https/api.github.com/repos/mycheck888/MyCheckWalletUI/tarball/1.2.3.tar.gz";

  private static final String PATH = "pods/" + NAME + "/" + VERSION + "/" + PATH_URI;

  @Mock
  private Repository repository;

  @Mock
  private StorageFacet storageFacet;

  @Mock
  private Content content;

  @Mock
  private Format format;

  @Mock
  private Bucket bucket;

  @Mock
  private AssetBlob assetBlob;

  @Mock
  private Blob blob;

  @Mock
  private EventManager eventManager;

  @Mock
  private SpecTransformer specTransformer;

  @Mock
  private StorageTx storageTx;

  private PodPathParser podPathParser;

  private CocoapodsFacet cocoapodsFacet;

  private Asset asset;

  private Component component;

  private AttributesMap attributesMap;

  @Before
  public void init() throws Exception {
    UnitOfWork.begin(() -> storageTx);

    configTestObject();
    configCommonMocks();
  }

  @After
  public void tearDown() {
    UnitOfWork.end();
  }

  @Test
  public void shouldStorePodAssetAndComponent() throws IOException {
    when(storageTx.findAssetWithProperty(any(), any(), eq(bucket))).thenReturn(null);
    when(storageTx.findComponents(any(), any())).thenReturn(Collections.emptyList());
    component = spy(new DefaultComponent());
    when(storageTx.createComponent(any(), any())).thenReturn(component);
    asset = spy(new Asset());
    configAttributeMap();
    when(storageTx.createAsset(any(), eq(component))).thenReturn(asset);

    cocoapodsFacet.getOrCreateAsset(PATH, content, true);

    verify(storageTx, times(1)).createAsset(bucket, component);
    verify(storageTx, times(1)).saveAsset(asset);
    verify(storageTx, times(1)).createComponent(bucket, format);
    verify(storageTx, times(1)).saveComponent(component);

    assertEquals(PATH, asset.name());
    assertEquals(NAME, component.name());
    assertEquals(VERSION, component.version());
  }

  @Test
  public void shouldStoreMetadataAsset() throws IOException {
    when(storageTx.findAssetWithProperty(any(), any(), eq(bucket))).thenReturn(null);
    asset = spy(new Asset());
    configAttributeMap();
    when(storageTx.createAsset(any(), eq(format))).thenReturn(asset);

    cocoapodsFacet.getOrCreateAsset(PATH, content, false);

    verify(storageTx, times(1)).createAsset(bucket, format);
    verify(storageTx, times(1)).saveAsset(asset);
    verify(storageTx, times(0)).createComponent(bucket, format);
    verify(storageTx, times(0)).saveComponent(component);

    assertEquals(PATH, asset.name());
  }

  @Test
  public void shouldStorePodAssetAndGetPodComponent() throws IOException {
    when(storageTx.findAssetWithProperty(any(), any(), eq(bucket))).thenReturn(null);
    component = spy(new DefaultComponent()).name(NAME).version(VERSION);
    when(storageTx.findComponents(any(), any())).thenReturn(Collections.singletonList(component));
    asset = spy(new Asset());
    configAttributeMap();
    when(storageTx.createAsset(any(), eq(component))).thenReturn(asset);

    cocoapodsFacet.getOrCreateAsset(PATH, content, true);

    verify(storageTx, times(1)).createAsset(bucket, component);
    verify(storageTx, times(1)).saveAsset(asset);
    verify(storageTx, times(0)).createComponent(bucket, format);
    verify(storageTx, times(0)).saveComponent(component);

    assertEquals(PATH, asset.name());
    assertEquals(NAME, component.name());
    assertEquals(VERSION, component.version());
  }

  @Test
  public void shouldGetPodAsset() throws IOException {
    asset = spy(new Asset()).name(PATH);
    configAttributeMap();
    when(storageTx.findAssetWithProperty(any(), any(), eq(bucket))).thenReturn(asset);

    cocoapodsFacet.getOrCreateAsset(PATH, content, true);

    verify(storageTx, times(0)).createAsset(bucket, component);
    verify(storageTx, times(1)).saveAsset(asset);
    verify(storageTx, times(0)).createComponent(bucket, format);
    verify(storageTx, times(0)).saveComponent(component);

    assertEquals(PATH, asset.name());
  }

  @Test
  public void shouldGetMetadataAsset() throws IOException {
    asset = spy(new Asset()).name(PATH);
    configAttributeMap();
    when(storageTx.findAssetWithProperty(any(), any(), eq(bucket))).thenReturn(asset);

    cocoapodsFacet.getOrCreateAsset(PATH, content, false);

    verify(storageTx, times(0)).createAsset(bucket, format);
    verify(storageTx, times(1)).saveAsset(asset);
    verify(storageTx, times(0)).createComponent(bucket, format);
    verify(storageTx, times(0)).saveComponent(component);

    assertEquals(PATH, asset.name());
  }

  private void configTestObject() throws Exception {
    podPathParser = new PodPathParser("https://api.github.com");
    attributesMap = new NestedAttributesMap("", new HashMap<>());
    cocoapodsFacet = Guice.createInjector(new TransactionModule(), new AbstractModule()
    {
      @Override
      protected void configure() {
        bind(EventManager.class).toInstance(eventManager);
        bind(PodPathParser.class).toInstance(podPathParser);
        bind(SpecTransformer.class).toInstance(specTransformer);
      }
    }).getInstance(CocoapodsFacetImpl.class);
    cocoapodsFacet.attach(repository);
    cocoapodsFacet.init();
  }

  private void configCommonMocks() throws IOException {
    when(storageTx.findBucket(any())).thenReturn(bucket);
    when(repository.facet(StorageFacet.class)).thenReturn(storageFacet);
    when(repository.getFormat()).thenReturn(format);
    when(assetBlob.getBlob()).thenReturn(blob);
    when(storageTx.setBlob(any(), any(), any(), any(), any(), any(), anyBoolean())).thenReturn(assetBlob);
    when(content.getAttributes()).thenReturn(attributesMap);
  }

  private void configAttributeMap() {
    doReturn(attributesMap).when(asset).attributes();
    doReturn(new DateTime()).when(asset).blobUpdated();
    doReturn(attributesMap).when(asset).formatAttributes();
    doReturn(null).when(asset).requireContentType();
    doReturn(null).when(asset).getChecksums(any());
  }
}
