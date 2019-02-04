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
package org.sonatype.nexus.blobstore.file;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.goodies.testsupport.concurrent.ConcurrentRunner;
import org.sonatype.nexus.blobstore.DefaultBlobIdLocationResolver;
import org.sonatype.nexus.blobstore.MetricsInputStream;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobMetrics;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreException;
import org.sonatype.nexus.blobstore.file.internal.FileBlobStoreMetricsStore;
import org.sonatype.nexus.blobstore.file.internal.FileOperations;
import org.sonatype.nexus.blobstore.file.internal.SimpleFileOperations;
import org.sonatype.nexus.scheduling.internal.PeriodicJobServiceImpl;
import org.sonatype.nexus.blobstore.quota.BlobStoreQuotaService;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.log.DryRunPrefix;
import org.sonatype.nexus.common.node.NodeAccess;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.io.ByteStreams.nullOutputStream;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.sonatype.nexus.blobstore.api.BlobStore.BLOB_NAME_HEADER;
import static org.sonatype.nexus.blobstore.api.BlobStore.CREATED_BY_HEADER;

/**
 * {@link FileBlobStore} concurrency tests.
 */
public class FileBlobStoreConcurrencyIT
    extends TestSupport
{
  public static final ImmutableMap<String, String> TEST_HEADERS = ImmutableMap.of(
      CREATED_BY_HEADER, "test",
      BLOB_NAME_HEADER, "test/randomData.bin"
  );

  public static final int BLOB_MAX_SIZE_BYTES = 5_000_000;

  private static final int QUOTA_CHECK_INTERVAL = 1;

  private FileBlobStore underTest;

  private FileBlobStoreMetricsStore metricsStore;

  @Mock
  NodeAccess nodeAccess;

  @Mock
  DryRunPrefix dryRunPrefix;

  @Mock
  BlobStoreQuotaService quotaService;

  @Mock
  FileOperations fileOperations;

  @Before
  public void setUp() throws Exception {
    Path root = util.createTempDir().toPath();
    Path content = root.resolve("content");

    when(nodeAccess.getId()).thenReturn(UUID.randomUUID().toString());
    when(dryRunPrefix.get()).thenReturn("");

    ApplicationDirectories applicationDirectories = mock(ApplicationDirectories.class);
    when(applicationDirectories.getWorkDirectory(anyString())).thenReturn(root.toFile());

    final BlobStoreConfiguration config = new BlobStoreConfiguration();
    config.attributes(FileBlobStore.CONFIG_KEY).set(FileBlobStore.PATH_KEY, root.toString());

    metricsStore = spy(
        new FileBlobStoreMetricsStore(new PeriodicJobServiceImpl(), nodeAccess, quotaService, QUOTA_CHECK_INTERVAL,
            fileOperations));

    this.underTest = new FileBlobStore(content,
        new DefaultBlobIdLocationResolver(),
        new SimpleFileOperations(),
        metricsStore,
        config,
        applicationDirectories, nodeAccess, dryRunPrefix);
    underTest.start();
  }

  @After
  public void tearDown() throws Exception {
    if (underTest != null) {
      underTest.stop();
    }
  }

  @Test
  public void concurrencyTest() throws Exception {

    final Random random = new Random();

    int numberOfCreators = 10;
    int numberOfDeleters = 5;
    int numberOfReaders = 30;
    int numberOfCompactors = 1;
    int numberOfShufflers = 3;

    final Queue<BlobId> blobIdsInTheStore = new ConcurrentLinkedDeque<>();

    final Set<BlobId> deletedIds = new HashSet<>();

    int numberOfIterations = 15;
    int timeoutMinutes = 5;
    final ConcurrentRunner runner = new ConcurrentRunner(numberOfIterations, timeoutMinutes * 60);

    runner.addTask(numberOfCreators, () -> {
      final byte[] data = new byte[random.nextInt(BLOB_MAX_SIZE_BYTES) + 1];
      random.nextBytes(data);
      final Blob blob = underTest.create(new ByteArrayInputStream(data), TEST_HEADERS);

      blobIdsInTheStore.add(blob.getId());
    });

    runner.addTask(numberOfReaders, () -> {
          final BlobId blobId = blobIdsInTheStore.peek();

          log("Attempting to read " + blobId);

          if (blobId == null) {
            return;
          }

          final Blob blob = underTest.get(blobId);
          if (blob == null) {
            log("Attempted to obtain blob, but it was deleted:" + blobId);
            return;
          }

          try (InputStream inputStream = blob.getInputStream()) {
            readContentAndValidateMetrics(blobId, inputStream, blob.getMetrics());
          }
          catch (BlobStoreException e) {
            checkState(deletedIds.contains(e.getBlobId()));
            // This is normal operation if another thread deletes your blob after you obtain a Blob reference
            log("Concurrent deletion suspected while calling blob.getInputStream().", e);
          }
        }
    );

    runner.addTask(numberOfDeleters, () -> {
          final BlobId blobId = blobIdsInTheStore.poll();
          if (blobId == null) {
            log("deleter: null blob id");
            return;
          }
          log("Deleting {}", blobId);

          // There's a race condition here, we need to note that we're attempting to delete this before the deletion
          // goes through, otherwise we may fail the check, above.
          deletedIds.add(blobId);
          underTest.delete(blobId, "Testing concurrency");
        }
    );

    // Shufflers pull blob IDs off the front of the queue and stick them on the back, to make the blobID queue a bit less orderly
    runner.addTask(numberOfShufflers, () -> {
      final BlobId blobId = blobIdsInTheStore.poll();
      if (blobId != null) {
        blobIdsInTheStore.add(blobId);
      }
    });


    runner.addTask(numberOfCompactors, () -> underTest.compact(null));

    runner.go();

    verify(metricsStore).setBlobStore(underTest);
    verify(quotaService, atLeastOnce()).checkQuota(underTest);
  }

  /**
   * Read all the content from a blob, and compare it with the metrics on file in the blob store.
   *
   * @throws RuntimeException if there is any deviation
   */
  private void readContentAndValidateMetrics(final BlobId blobId,
                                             final InputStream inputStream,
                                             final BlobMetrics metadataMetrics)
      throws NoSuchAlgorithmException, IOException
  {
    final MetricsInputStream measured = new MetricsInputStream(inputStream);
    ByteStreams.copy(measured, nullOutputStream());

    checkEqual("stream length", metadataMetrics.getContentSize(), measured.getSize(), blobId);
    checkEqual("SHA1 hash", metadataMetrics.getSha1Hash(), measured.getMessageDigest(), blobId);
  }

  private void checkEqual(final String propertyName, final Object expected, final Object measured,
                          final BlobId blobId)
  {
    if (!Objects.equal(measured, expected)) {
      throw new RuntimeException(
          "Blob " + blobId + "'s measured " + propertyName + " differed from its metadata. Expected " + expected +
              " but was " + measured + "."
      );
    }
  }
}
