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
package org.sonatype.nexus.repository.tools.datastore;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.NotNull;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobMetrics;
import org.sonatype.nexus.blobstore.api.BlobStoreException;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;
import org.sonatype.nexus.repository.tools.BlobUnavilableException;
import org.sonatype.nexus.repository.tools.DeadBlobFinder;
import org.sonatype.nexus.repository.tools.DeadBlobResult;
import org.sonatype.nexus.repository.tools.MismatchedSHA1Exception;

import com.google.common.base.Stopwatch;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.entity.Continuations.streamOf;
import static org.sonatype.nexus.repository.tools.ResultState.ASSET_DELETED;
import static org.sonatype.nexus.repository.tools.ResultState.DELETED;
import static org.sonatype.nexus.repository.tools.ResultState.MISSING_BLOB_REF;
import static org.sonatype.nexus.repository.tools.ResultState.SHA1_DISAGREEMENT;
import static org.sonatype.nexus.repository.tools.ResultState.UNAVAILABLE_BLOB;
import static org.sonatype.nexus.repository.tools.ResultState.UNKNOWN;
import static org.sonatype.nexus.repository.tools.ResultState.UNREADABLE_BLOB;

/**
 * Examines Asset metadata and confirms the sha1 of all referenced blobs. Reports on any instances where
 * Blob binary is missing or indicates a different sha1 than that stored in the DB.
 *
 * @since 3.25
 */
@Named
public class DatastoreDeadBlobFinder
    extends ComponentSupport
    implements DeadBlobFinder<Asset>
{
  private BlobStoreManager blobStoreManager;

  @Inject
  public DatastoreDeadBlobFinder(final BlobStoreManager blobStoreManager) {
    this.blobStoreManager = blobStoreManager;
  }

  public void findAndProcessBatch(
      @NotNull final Repository repository,
      final boolean ignoreMissingBlobRefs,
      final int batchSize,
      final Consumer<DeadBlobResult<Asset>> resultProcessor) {
    checkNotNull(repository);
    checkNotNull(resultProcessor);

    FluentAssets fluentAssets = repository.facet(ContentFacet.class).assets();
    Continuation<FluentAsset> assets = fluentAssets.browse(batchSize, null);
    long deadBlobCandidateCount = 0;
    long deadBlobCount = 0;
    Stopwatch sw = Stopwatch.createStarted();

    while (!assets.isEmpty()) {
      List<DeadBlobResult<Asset>> deadBlobCandidates = identifySuspects(repository, ignoreMissingBlobRefs,
          assets.stream(), true);
      List<DeadBlobResult<Asset>> deadBlobResults = verifySuspects(deadBlobCandidates, repository, true);
      deadBlobCandidateCount += deadBlobCandidates.size();
      deadBlobCount += deadBlobResults.size();

      for (DeadBlobResult<Asset> deadBlobResult : deadBlobResults) {
        resultProcessor.accept(deadBlobResult);
      }
      assets = fluentAssets.browse(batchSize, assets.nextContinuationToken());
    }
    log.info("Inspection of repository {} took {}ms for " + "{} assets and identified {} incorrect Assets",
        repository.getName(), sw.elapsed(TimeUnit.MILLISECONDS), deadBlobCandidateCount, deadBlobCount);
  }

  /**
   * Based on the db metadata, confirm that all Blobs exist and sha1 values match. Can optionally ignore any records
   * that don't have a blobRef, which is expected for NuGet search results.
   * @parem repository  The Repository to inspect
   * @param ignoreMissingBlobRefs (defaults to true)
   */
  @Override
  public List<DeadBlobResult<Asset>> find(@NotNull final Repository repository, final boolean ignoreMissingBlobRefs) {
    checkNotNull(repository);

    FluentAssets fluentAssets = repository.facet(ContentFacet.class).assets();

    List<DeadBlobResult<Asset>> deadBlobCandidates = identifySuspects(repository, ignoreMissingBlobRefs,
        streamOf(fluentAssets::browse), false);
    return verifySuspects(deadBlobCandidates, repository, false);
  }

  /**
   * Identify any potentially incorrect data by inspecting all Assets in the system. It is expected that
   * in a highly concurrent system some of the blob related data may already be stale after being loaded in
   * memory, so we collect those for later inspection.
   */
  private List<DeadBlobResult<Asset>> identifySuspects(
      final Repository repository,
      final boolean ignoreMissingBlobRefs,
      final Stream<FluentAsset> fluentAssets,
      final boolean batchMode)
  {
    Stopwatch sw = Stopwatch.createStarted();
    AtomicLong blobsExamined = new AtomicLong();

    List<DeadBlobResult<Asset>> deadBlobCandidates = fluentAssets
        .peek(a -> blobsExamined.incrementAndGet())
        .map(asset -> {
            if (!asset.blob().isPresent() && ignoreMissingBlobRefs) {
              log.trace("Set to ignore missing blobRef on {}", asset);
              return null;
            }
            else {
              return checkAsset(repository.getName(), asset);
            }
          })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());

    if (!batchMode) {
      log.debug(
          "Inspecting repository {} took {}ms for {}  assets and identified {} potentially incorrect Assets for followup assessment",
          repository.getName(), sw.elapsed(TimeUnit.MILLISECONDS), blobsExamined, deadBlobCandidates.size());
    }
    return deadBlobCandidates;
  }

  /**
   * Load each potentially incorrect Asset individually to see if it is truly out of sync with its referenced Blob.
   * If data still appears in an incorrect state, log all pertinent details, including whether or not the Asset
   * remains in the same state as when originally inspected.
   */
  private List<DeadBlobResult<Asset>> verifySuspects(
      final List<DeadBlobResult<Asset>> deadBlobCandidates,
      final Repository repository,
      final boolean batchMode)
  {
    if (!deadBlobCandidates.isEmpty()) {
      Stopwatch sw = Stopwatch.createStarted();
      ContentFacet content = repository.facet(ContentFacet.class);
      List<DeadBlobResult<Asset>> deadBlobs = deadBlobCandidates.stream()
          .map(candidateResult -> {
            DeadBlobResult<Asset> deadBlobResult = checkAsset(repository.getName(),
                content.assets().path(candidateResult.getAsset().path()).find().orElse(null));
            if (deadBlobResult != null) {
              logResults(candidateResult, deadBlobResult);
              return deadBlobResult;
            }
            else {
              log.debug(
                  "Asset {} corrected from error state {} during inspection", candidateResult.getAsset().path(), candidateResult.getResultState());
              return null;
            }
          })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());

      if (!batchMode) {
        log.info("Followup inspection of repository {} took {}ms for " + "{} assets and identified {} incorrect Assets",
            repository.getName(), sw.elapsed(TimeUnit.MILLISECONDS), deadBlobCandidates.size(), deadBlobs.size());
      }

      return deadBlobs;
    }
    return Collections.emptyList();
  }

  /**
   * Verify the Asset metadata against the attached Blob, categorizing any errors encountered.
   */
  private DeadBlobResult<Asset> checkAsset(final String repositoryName, final Asset asset) {
    if (asset == null) {
      return new DeadBlobResult<>(repositoryName, null, ASSET_DELETED, "Asset was deleted during inspection");
    }
    try {
      OffsetDateTime createdTime = asset.blob().map(AssetBlob::datePath).orElse(null);
      Blob blob = asset.blob().map(AssetBlob::blobRef)
          .map(blobRef -> blobStoreManager.get(blobRef.getStore()).get(blobRef.getBlobId(createdTime)))
          .orElseThrow(() -> new IllegalStateException("Blob not found."));
      verifyBlob(blob, asset);
    }
    catch (IllegalStateException ise) {
      return new DeadBlobResult<>(repositoryName, asset, MISSING_BLOB_REF, ise.getMessage());
    }
    catch (BlobStoreException bse) {
      if (bse.getCause() instanceof IOException) {
        return new DeadBlobResult<>(repositoryName, asset, UNREADABLE_BLOB, bse.getMessage());
      }
      return new DeadBlobResult<>(repositoryName, asset, DELETED, bse.getMessage()); // check for specific message?
    }
    catch (MismatchedSHA1Exception pae) {
      return new DeadBlobResult<>(repositoryName, asset, SHA1_DISAGREEMENT, pae.getMessage());
    }
    catch (BlobUnavilableException e) {
      return new DeadBlobResult<>(repositoryName, asset, UNAVAILABLE_BLOB, e.getMessage() == null ? "Blob inputstream unavailable" : e.getMessage());
    }
    catch (Exception e) {
      return new DeadBlobResult<>(repositoryName, asset, UNKNOWN, e.getMessage());
    }
    return null;
  }

  /**
   * Verify that the Blob exists and is in agreement with the stored Asset metadata.;
   */
  private void verifyBlob(final Blob blob, final Asset asset) throws MismatchedSHA1Exception, BlobUnavilableException, IOException {
    BlobMetrics metrics = blob.getMetrics();

    String assetChecksum =
        asset.blob().map(AssetBlob::checksums).map(checksums -> checksums.get(HashAlgorithm.SHA1.name())).orElse(null);
    if (!metrics.getSha1Hash().equals(assetChecksum)) {
      throw new MismatchedSHA1Exception();
    }

    try (InputStream blobstream = blob.getInputStream()) {
      if (metrics.getContentSize() > 0 && blobstream.available() == 0) {
        throw new BlobUnavilableException();
      }
    }
  }

  /**
   * Log details about an incorrect result, including if state changed between inspections.;
   */
  private void logResults(final DeadBlobResult<Asset> firstResult, final DeadBlobResult<Asset> secondResult) {
    log.info("Possible bad data found in Asset: {}", secondResult.getAsset());
    if (lastUpdated(firstResult) != lastUpdated(secondResult)) {
      log.info("Asset metadata was updated during inspection between {} and {}",  lastUpdated(firstResult), lastUpdated(secondResult));
    }
    if (firstResult.getResultState() != secondResult.getResultState()) {
      log.info("Error state changed from {} to {} during inspection", firstResult.getResultState(), secondResult.getResultState());
    }
    if (blobUpdated(firstResult) != blobUpdated(secondResult)) {
      log.info("Asset blob was updated during inspection between {} and {}", blobUpdated(firstResult), blobUpdated(secondResult));
    }
  }

  private static OffsetDateTime blobUpdated(final DeadBlobResult<Asset> deadBlobResult) {
    return Optional.ofNullable(deadBlobResult.getAsset()).flatMap(Asset::blob).map(AssetBlob::blobCreated).orElse(null);
  }

  private static OffsetDateTime lastUpdated(final DeadBlobResult<Asset> deadBlobResult) {
    return Optional.ofNullable(deadBlobResult.getAsset()).map(Asset::lastUpdated).orElse(null);
  }
}
