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
package org.sonatype.nexus.repository.content.tasks;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobAttributes;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.common.entity.Continuations;
import org.sonatype.nexus.logging.task.ProgressLogIntervalHelper;
import org.sonatype.nexus.logging.task.TaskLogging;
import org.sonatype.nexus.repository.MissingBlobException;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryParallelTaskSupport;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.types.HostedType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Stream.empty;
import static org.sonatype.nexus.blobstore.api.BlobStore.REPO_NAME_HEADER;
import static org.sonatype.nexus.logging.task.TaskLogType.TASK_LOG_ONLY;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.BLOB_STORE_NAME;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.STORAGE;

/**
 * This task fixes an issue where a blob's properties may have the wrong repositoryName set
 */
@Component
@TaskLogging(TASK_LOG_ONLY)
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class BlobRepositoryMismatchTask
    extends RepositoryParallelTaskSupport
{
  private final BlobStoreManager blobStoreManager;

  private final AtomicLong fixedBlobsCount = new AtomicLong();

  private final boolean skipProcessing;

  @Autowired
  public BlobRepositoryMismatchTask(
      final BlobStoreManager blobStoreManager,
      @Value("${blob.repository.name.mismatch.concurrencyLimit:5}") final int concurrencyLimit,
      @Value("${blob.repository.name.mismatch.skipProcessing:false}") final boolean skipProcessing)
  {
    super(concurrencyLimit, 1);
    this.blobStoreManager = checkNotNull(blobStoreManager);
    this.skipProcessing = skipProcessing;
  }

  @Override
  protected Object result() {
    return fixedBlobsCount.get();
  }

  @Override
  protected Stream<Runnable> jobStream(final ProgressLogIntervalHelper progress) {
    log.info("Starting blob repository mismatch check");
    return super.jobStream(progress);
  }

  @Override
  protected Stream<Runnable> jobStream(final ProgressLogIntervalHelper progress, final Repository repository) {
    if (skipProcessing) {
      log.debug("Skip processing is enabled, skipping blob mismatch processing for repository: {}",
          repository.getName());
      return empty();
    }

    log.info("Processing repository: {}", repository.getName());

    BlobStore blobstore = getBlobStore(repository).orElse(null);
    if (blobstore == null) {
      log.error("Unable to obtain blobstore for {}", repository);
      return Stream.of();
    }

    return Continuations.streamOf(repository.facet(ContentFacet.class).assets()::browseEager)
        .filter(FluentAsset::hasBlob)
        .map(asset -> createJob(repository, asset));
  }

  private Runnable createJob(final Repository repository, final FluentAsset asset) {
    return () -> {
      BlobData blobData;
      try {
        blobData = getBlobData(asset);
      }
      catch (MissingBlobException | IOException e) {
        log.warn("Missing blob data for asset {}", asset, e);
        return;
      }

      if (blobData == null) {
        return;
      }

      if (checkBlobRepositoryHeaderMatch(blobData.blob, repository.getName())) {
        log.debug("Ignoring asset {} because of repository property and blob header match", asset);
        return;
      }

      try {
        // Fix the blob's REPO_NAME_HEADER by updating the blob attributes directly
        fixBlobRepositoryHeader(blobData.blobStore, blobData.blob, repository.getName());
        log.debug("Repository {} mismatch fixed for asset {}", repository.getName(), asset.path());
        fixedBlobsCount.incrementAndGet();
      }
      catch (Exception e) {
        log.warn("Failed to fix blob header of asset {}", asset, e);
      }
    };
  }

  @Override
  protected boolean appliesTo(final Repository repository) {
    Optional<BlobStore> blobstore = getBlobStore(repository);

    if (blobstore.isEmpty()) {
      log.error("Unable to identify blobstore type for repository {}", repository);
      return false;
    }

    return HostedType.NAME.equals(repository.getType().toString());
  }

  private Optional<BlobStore> getBlobStore(final Repository repository) {
    return Optional.ofNullable(repository.getConfiguration())
        .map(Configuration::getAttributes)
        .map(attr -> attr.get(STORAGE))
        .filter(Map.class::isInstance)
        .map(Map.class::cast)
        .map(storage -> storage.get(BLOB_STORE_NAME))
        .filter(Objects::nonNull)
        .filter(String.class::isInstance)
        .map(String.class::cast)
        .map(blobStoreManager::get);
  }

  private BlobData getBlobData(final FluentAsset asset) throws IOException, MissingBlobException {
    if (asset.blob().isEmpty()) {
      log.warn("Missing blob for asset: {}", asset.path());
      return null;
    }

    AssetBlob assetBlob = asset.blob().get();
    BlobRef blobRef = assetBlob.blobRef();
    String blobStoreName = blobRef.getStore();

    BlobStore sourceBlobStore = blobStoreManager.get(blobStoreName);
    if (sourceBlobStore == null) {
      log.error("Blob store not found: {}", blobStoreName);
      throw new IOException("Blob store not found: " + blobStoreName);
    }

    Blob sourceBlob = sourceBlobStore.get(blobRef.getBlobId());
    if (sourceBlob == null) {
      log.error("Missing blob reference: {}", blobRef.getBlobId());
      throw new MissingBlobException(blobRef);
    }

    return new BlobData(sourceBlobStore, sourceBlob);
  }

  private boolean checkBlobRepositoryHeaderMatch(final Blob blob, final String repositoryName) {
    return Optional.ofNullable(blob)
        .map(Blob::getHeaders)
        .map(headers -> headers.get(REPO_NAME_HEADER))
        .map(repositoryName::equals)
        .orElse(true);
  }

  private void fixBlobRepositoryHeader(
      final BlobStore blobStore,
      final Blob blob,
      final String correctRepositoryName) throws IOException
  {
    BlobId blobId = blob.getId();

    // Get existing blob attributes
    Map<String, String> headers = blob.getHeaders();
    if (headers == null || headers.isEmpty()) {
      log.error("Blob attributes not found for blob: {}", blobId);
      throw new IOException("Blob attributes not found: " + blobId);
    }

    // Update the REPO_NAME_HEADER in headers
    Map<String, String> headersCopy = new HashMap<>(headers);
    headersCopy.put(REPO_NAME_HEADER, correctRepositoryName);

    // Create new BlobAttributes with updated headers
    BlobAttributes newAttributes = blobStore.createBlobAttributesInstance(
        blobId,
        headersCopy,
        blob.getMetrics());

    // Persist the updated attributes to the blob's .properties file
    blobStore.setBlobAttributes(blobId, newAttributes);
  }

  private record BlobData(
      BlobStore blobStore,
      Blob blob)
  {
  }

  @Override
  public String getMessage() {
    return "Searching for blob properties mismatching " + REPO_NAME_HEADER;
  }
}
