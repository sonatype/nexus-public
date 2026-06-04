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
package org.sonatype.nexus.repository.content.internal;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobAttributes;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobMetrics;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAssetBuilder;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;
import org.sonatype.nexus.repository.content.handlers.LastDownloadedAttributeHandler;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test cases for {@link LastDownloadedAttributePropertyFileHandler}
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class LastDownloadedAttributePropertyFileHandlerTest
{
  @Mock
  private BlobStoreManager blobStoreManager;

  @Mock
  private BlobStore blobStore;

  @Mock
  private BlobAttributes blobAttributes;

  @Mock
  private BlobMetrics blobMetrics;

  @Mock
  private BlobId blobId;

  @Mock
  private Blob blob;

  @Mock
  private FluentAsset asset;

  private LastDownloadedAttributeHandler underTest;

  private OffsetDateTime assetLastDownloaded = OffsetDateTime.now();

  private Logger handlerLogger;

  private ListAppender<ILoggingEvent> logAppender;

  private Level originalLogLevel;

  @Before
  public void setup() {
    configureHappyPath();
    underTest = new LastDownloadedAttributePropertyFileHandler(blobStoreManager);

    handlerLogger = (Logger) LoggerFactory.getLogger(LastDownloadedAttributePropertyFileHandler.class);
    originalLogLevel = handlerLogger.getLevel();
    logAppender = new ListAppender<>();
    logAppender.start();
    handlerLogger.addAppender(logAppender);
    handlerLogger.setLevel(Level.DEBUG);
  }

  @After
  public void tearDown() {
    handlerLogger.detachAppender(logAppender);
    handlerLogger.setLevel(originalLogLevel);
  }

  @Test
  public void shouldSetAssetLastDownloadedProperty() {
    when(asset.lastDownloaded()).thenReturn(Optional.of(assetLastDownloaded));

    underTest.writeLastDownloadedAttribute(asset);

    verify(asset, times(2)).lastDownloaded();
    verify(blobStore).getBlobAttributes(blobId);
    verify(blobMetrics).setLastDownloaded(asset.lastDownloaded().get());
    verify(blobStore).setBlobAttributes(blobId, blobAttributes);
  }

  @Test
  public void shouldReadAssetLastDownloadedProperty() {
    when(blobMetrics.getLastDownloaded()).thenReturn(assetLastDownloaded);

    OffsetDateTime lastDownloaded = underTest.readLastDownloadedAttribute("default", blob);
    assertNotNull(lastDownloaded);
  }

  @Test
  public void shouldNotUpdatePropertyWhenAssetNotDownloadedYet() {
    when(asset.lastDownloaded()).thenReturn(Optional.empty());

    underTest.writeLastDownloadedAttribute(asset);

    verify(blobStore, never()).getBlobAttributes(blobId);
    verify(blobStore, never()).setBlobAttributes(blobId, blobAttributes);

    assertNull(blobMetrics.getLastDownloaded());
  }

  @Test
  public void shouldLogDebugNotWarnWhenBlobAttributesNullDuringConcurrentAccess() {
    when(asset.lastDownloaded()).thenReturn(Optional.of(assetLastDownloaded));
    when(blobStore.getBlobAttributes(blobId)).thenReturn(null);

    underTest.writeLastDownloadedAttribute(asset);

    verify(blobStore, never()).setBlobAttributes(blobId, blobAttributes);

    List<ILoggingEvent> logs = logAppender.list;
    boolean hasDebugLog = logs.stream()
        .anyMatch(e -> e.getLevel() == Level.DEBUG
            && e.getFormattedMessage().contains("Could not get blob attributes"));
    boolean hasWarnLog = logs.stream()
        .anyMatch(e -> e.getLevel() == Level.WARN
            && e.getFormattedMessage().contains("Could not get blob attributes"));

    assertTrue("Expected DEBUG log when blob attributes are null", hasDebugLog);
    assertFalse("Should NOT log at WARN level when blob attributes are null during concurrent access", hasWarnLog);
  }

  @Test
  public void shouldLogWarnWithBlobStoreNameWhenBlobStoreNotLoaded() {
    when(blobStoreManager.get("unknown-store")).thenReturn(null);

    underTest.readLastDownloadedAttribute("unknown-store", blob);

    List<ILoggingEvent> logs = logAppender.list;
    boolean hasWarnWithName = logs.stream()
        .anyMatch(e -> e.getLevel() == Level.WARN
            && e.getFormattedMessage().contains("unknown-store"));

    assertTrue("Expected WARN log containing the blob store name", hasWarnWithName);
  }

  private void configureHappyPath() {
    when(asset.path()).thenReturn("/test/asset.jar");
    configureContentFacet();

    when(blob.getId()).thenReturn(blobId);
    AssetBlob assetBlob = mock(AssetBlob.class);
    BlobRef blobRef = mock(BlobRef.class);
    when(blobRef.getStore()).thenReturn("test-blobstore");
    when(blobRef.getBlobId()).thenReturn(blobId);
    when(assetBlob.blobRef()).thenReturn(blobRef);
    when(asset.blob()).thenReturn(Optional.of(assetBlob));

    when(blobStoreManager.get("default")).thenReturn(blobStore);
    when(blobStoreManager.get(blobRef.getStore())).thenReturn(blobStore);
    when(blobStore.getBlobAttributes(blobId)).thenReturn(blobAttributes);
    when(blobAttributes.getMetrics()).thenReturn(blobMetrics);
  }

  private void configureContentFacet() {
    ContentFacet contentFacet = mock(ContentFacet.class);
    FluentAssets fluentAssets = mock(FluentAssets.class);
    FluentAssetBuilder builder = mock(FluentAssetBuilder.class);
    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(fluentAssets.path(asset.path())).thenReturn(builder);
    when(builder.find()).thenReturn(Optional.of(asset));

    Repository repository = mock(Repository.class);
    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(asset.repository()).thenReturn(repository);
  }
}
