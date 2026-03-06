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
package org.sonatype.nexus.cleanup.internal.content.method;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

import org.sonatype.goodies.testsupport.Test5Support;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.common.db.DatabaseCheck;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.maintenance.ContentMaintenanceFacet;
import org.sonatype.nexus.repository.task.DeletionProgress;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeleteCleanupMethodTest
    extends Test5Support
{
  @Mock
  private Repository repository;

  @Mock
  private BooleanSupplier cancelledCheck;

  @Mock
  private ContentMaintenanceFacet contentMaintenanceFacet;

  @Mock
  private DatabaseCheck databaseCheck;

  private DeleteCleanupMethod underTest;

  private Logger logger;

  private ListAppender<ILoggingEvent> logAppender;

  @BeforeEach
  void setUp() {
    underTest = new DeleteCleanupMethod(databaseCheck);
    when(repository.facet(ContentMaintenanceFacet.class)).thenReturn(contentMaintenanceFacet);

    // Set up log appender to capture log messages
    logger = (Logger) LoggerFactory.getLogger(DeleteCleanupMethod.class);
    logger.setLevel(Level.DEBUG); // Enable DEBUG level to capture debug logs
    logAppender = new ListAppender<>();
    logAppender.start();
    logger.addAppender(logAppender);
  }

  @AfterEach
  void tearDown() {
    // Clean up log appender
    logAppender.list.clear();
    logger.detachAppender(logAppender);
  }

  @Test
  void testRunFailsIfTaskIsCancelled() {
    when(cancelledCheck.getAsBoolean()).thenReturn(true);
    assertThrows(TaskInterruptedException.class,
        () -> underTest.run(repository, getRandomStream(1000), cancelledCheck));
  }

  @Test
  void testRunReBatchStream() {
    when(cancelledCheck.getAsBoolean()).thenReturn(false);
    when(contentMaintenanceFacet.deleteComponents(any(Stream.class)))
        .thenAnswer(invocation -> {
          Stream<FluentComponent> input = invocation.getArgument(0);
          return (int) input.count();
        });

    Stream<FluentComponent> input = getRandomStream(5000);

    DeletionProgress deleted = underTest.run(repository, input, cancelledCheck);

    // validate stream is batched and cancel check is verified for each batch
    verify(contentMaintenanceFacet, times(5)).deleteComponents(any(Stream.class));
    verify(cancelledCheck, times(5)).getAsBoolean();

    assertEquals(5000, deleted.getComponentCount());
  }

  @Test
  void testLogsBlobPathsForAssetsWithBlobs() {
    when(cancelledCheck.getAsBoolean()).thenReturn(false);
    when(contentMaintenanceFacet.deleteComponents(any(Stream.class)))
        .thenAnswer(invocation -> {
          Stream<FluentComponent> input = invocation.getArgument(0);
          return (int) input.count();
        });

    Stream<FluentComponent> input = getStreamWithAssets(3);

    DeletionProgress deleted = underTest.run(repository, input, cancelledCheck);

    // Verify deletion happened
    verify(contentMaintenanceFacet, times(1)).deleteComponents(any(Stream.class));
    assertEquals(3, deleted.getComponentCount());

    // Verify logging happened for each asset with blob
    List<ILoggingEvent> logEvents = logAppender.list;
    List<ILoggingEvent> deletionLogs = logEvents.stream()
        .filter(event -> event.getLevel() == Level.DEBUG)
        .filter(event -> event.getFormattedMessage().contains("Deleting asset"))
        .toList();

    assertEquals(3, deletionLogs.size(), "Should log deletion for all 3 assets");

    // Verify log messages contain expected information
    for (int i = 1; i <= 3; i++) {
      final int assetNum = i;
      boolean foundLog = deletionLogs.stream()
          .anyMatch(event -> {
            String message = event.getFormattedMessage();
            return message.contains("repository: test-repo") &&
                message.contains("path: /path/to/asset" + assetNum) &&
                message.contains("blobRef: default@blob-id-" + assetNum) &&
                message.contains("size: " + (1024 * assetNum) + " bytes");
          });
      assertTrue(foundLog, "Should have log entry for asset " + assetNum + " with all expected information");
    }
  }

  @Test
  void testHandlesAssetsWithoutBlobsGracefully() {
    when(cancelledCheck.getAsBoolean()).thenReturn(false);
    when(contentMaintenanceFacet.deleteComponents(any(Stream.class)))
        .thenAnswer(invocation -> {
          Stream<FluentComponent> input = invocation.getArgument(0);
          return (int) input.count();
        });

    Stream<FluentComponent> input = getStreamWithAssetsWithoutBlobs(2);

    DeletionProgress deleted = underTest.run(repository, input, cancelledCheck);

    // Verify deletion happened even for assets without blobs
    verify(contentMaintenanceFacet, times(1)).deleteComponents(any(Stream.class));
    assertEquals(2, deleted.getComponentCount());

    // Verify no deletion logs for assets without blobs
    List<ILoggingEvent> logEvents = logAppender.list;
    long deletionLogCount = logEvents.stream()
        .filter(event -> event.getLevel() == Level.DEBUG)
        .filter(event -> event.getFormattedMessage().contains("Deleting asset"))
        .count();

    assertEquals(0, deletionLogCount, "Should not log deletion for assets without blobs");
  }

  public Stream<FluentComponent> getRandomStream(final int size) {
    List<FluentComponent> resultList = new ArrayList<>(size);

    for (int i = 1; i <= size; i++) {
      FluentComponent fluentComponent = mock(FluentComponent.class);
      resultList.add(fluentComponent);
    }

    return resultList.stream();
  }

  private Stream<FluentComponent> getStreamWithAssets(final int size) {
    List<FluentComponent> resultList = new ArrayList<>(size);

    for (int i = 1; i <= size; i++) {
      FluentComponent component = mock(FluentComponent.class);
      FluentAsset asset = mock(FluentAsset.class);
      AssetBlob assetBlob = mock(AssetBlob.class);
      BlobRef blobRef = mock(BlobRef.class);
      Repository assetRepository = mock(Repository.class);

      when(assetRepository.getName()).thenReturn("test-repo");
      when(asset.repository()).thenReturn(assetRepository);
      when(asset.path()).thenReturn("/path/to/asset" + i);
      when(asset.blob()).thenReturn(Optional.of(assetBlob));
      when(assetBlob.blobRef()).thenReturn(blobRef);
      when(assetBlob.blobSize()).thenReturn(1024L * i);
      when(blobRef.toString()).thenReturn("default@blob-id-" + i + "@2026-02-06");
      when(component.assets()).thenReturn(List.of(asset));

      resultList.add(component);
    }

    return resultList.stream();
  }

  private Stream<FluentComponent> getStreamWithAssetsWithoutBlobs(final int size) {
    List<FluentComponent> resultList = new ArrayList<>(size);

    for (int i = 1; i <= size; i++) {
      FluentComponent component = mock(FluentComponent.class);
      FluentAsset asset = mock(FluentAsset.class);
      Repository assetRepository = mock(Repository.class);

      when(assetRepository.getName()).thenReturn("test-repo");
      when(asset.repository()).thenReturn(assetRepository);
      when(asset.path()).thenReturn("/path/to/asset" + i);
      when(asset.blob()).thenReturn(Optional.empty());
      when(component.assets()).thenReturn(List.of(asset));

      resultList.add(component);
    }

    return resultList.stream();
  }
}
