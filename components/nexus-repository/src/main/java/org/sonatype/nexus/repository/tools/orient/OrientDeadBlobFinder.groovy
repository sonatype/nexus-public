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
package org.sonatype.nexus.repository.tools.orient

import java.util.concurrent.TimeUnit

import javax.annotation.Priority
import javax.inject.Inject
import javax.inject.Named
import javax.validation.constraints.NotNull

import org.sonatype.goodies.common.ComponentSupport
import org.sonatype.nexus.blobstore.api.Blob
import org.sonatype.nexus.blobstore.api.BlobMetrics
import org.sonatype.nexus.blobstore.api.BlobStoreException
import org.sonatype.nexus.common.hash.HashAlgorithm
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.storage.Asset
import org.sonatype.nexus.repository.storage.AssetEntityAdapter
import org.sonatype.nexus.repository.storage.StorageFacet
import org.sonatype.nexus.repository.storage.StorageTx
import org.sonatype.nexus.repository.tools.BlobUnavilableException
import org.sonatype.nexus.repository.tools.DeadBlobFinder
import org.sonatype.nexus.repository.tools.DeadBlobResult
import org.sonatype.nexus.repository.tools.MismatchedSHA1Exception

import com.google.common.base.Stopwatch
import groovy.transform.ToString

import static com.google.common.base.Preconditions.checkNotNull
import static org.sonatype.nexus.repository.tools.ResultState.ASSET_DELETED
import static org.sonatype.nexus.repository.tools.ResultState.DELETED
import static org.sonatype.nexus.repository.tools.ResultState.MISSING_BLOB_REF
import static org.sonatype.nexus.repository.tools.ResultState.SHA1_DISAGREEMENT
import static org.sonatype.nexus.repository.tools.ResultState.UNAVAILABLE_BLOB
import static org.sonatype.nexus.repository.tools.ResultState.UNKNOWN
import static org.sonatype.nexus.repository.tools.ResultState.UNREADABLE_BLOB

/**
 * Examines Asset metadata and confirms the sha1 of all referenced blobs. Reports on any instances where
 * Blob binary is missing or indicates a different sha1 than that stored in the DB.
 * @since 3.3
 */
@Priority(Integer.MAX_VALUE)
@ToString(includePackage = false)
@Named
class OrientDeadBlobFinder
    extends ComponentSupport
    implements DeadBlobFinder<Asset>
{
  final AssetEntityAdapter assetEntityAdapter

  @Inject
  OrientDeadBlobFinder(final AssetEntityAdapter assetEntityAdapter) {
    this.assetEntityAdapter = checkNotNull(assetEntityAdapter)
  }

  /**
   * Based on the db metadata, confirm that all Blobs exist and sha1 values match. Can optionally ignore any records
   * that don't have a blobRef, which is expected for NuGet search results.
   * @parem repository  The Repository to inspect
   * @param ignoreMissingBlobRefs (defaults to true)
   */
  List<DeadBlobResult> find(@NotNull final Repository repository, boolean ignoreMissingBlobRefs = true) {
    checkNotNull(repository)

    StorageTx tx = repository.facet(StorageFacet).txSupplier().get()
    try {
      tx.begin()
      List<DeadBlobResult> deadBlobCandidates = identifySuspects(tx, repository, ignoreMissingBlobRefs)
      return verifySuspects(tx, deadBlobCandidates, repository)
    }
    finally {
      tx.close()
    }
  }

  /**
   * Identify any potentially incorrect data by inspecting all Assets in the system. It is expected that
   * in a highly concurrent system some of the blob related data may already be stale after being loaded in
   * memory, so we collect those for later inspection.
   */
  private List<DeadBlobResult> identifySuspects(StorageTx tx, Repository repository,
                                                boolean ignoreMissingBlobRefs)
  {
    List<DeadBlobResult> deadBlobCandidates = []
    long blobsExamined = 0
    Stopwatch sw = Stopwatch.createStarted()
    tx.browseAssets(tx.findBucket(repository)).each { Asset asset ->
      blobsExamined++
      if (!asset.blobRef() && ignoreMissingBlobRefs) {
        log.trace("Set to ignore missing blobRef on ${asset}")
      }
      else {
        deadBlobCandidates << checkAsset(repository.name, tx, asset)
      }
    }
    deadBlobCandidates = deadBlobCandidates - null // trim out all null results
    log.debug(
        "Inspecting repository ${repository.name} took ${sw.elapsed(TimeUnit.MILLISECONDS)}ms for $blobsExamined " +
            "assets and identified ${deadBlobCandidates.size()} potentially incorrect Assets for followup assessment")
    return deadBlobCandidates
  }

  /**
   * Load each potentially incorrect Asset individually to see if it is truly out of sync with its referenced Blob.
   * If data still appears in an incorrect state, log all pertinent details, including whether or not the Asset
   * remains in the same state as when originally inspected.
   */
  private List<DeadBlobResult> verifySuspects(StorageTx tx, List<DeadBlobResult> deadBlobCandidates, Repository repository)
  {
    List<DeadBlobResult> deadBlobs = []
    if (!deadBlobCandidates.isEmpty()) {
      Stopwatch sw = Stopwatch.createStarted()
      deadBlobCandidates.each { DeadBlobResult candidateResult ->
        DeadBlobResult deadBlobResult =
            checkAsset(repository.name, tx, assetEntityAdapter.read(tx.db, candidateResult.asset.entityMetadata.id))
        if (deadBlobResult) {
          logResults(candidateResult, deadBlobResult)
          deadBlobs << deadBlobResult
        }
        else {
          log.debug(
              "Asset ${candidateResult.asset.name()} corrected from error state ${candidateResult.resultState} " +
                  "during inspection")
        }
      }
      deadBlobs = deadBlobs - null  // trim out all null results
      log.info("Followup inspection of repository ${repository.name} took ${sw.elapsed(TimeUnit.MILLISECONDS)}ms for " +
          "${deadBlobCandidates.size()} assets and identified ${deadBlobs.size()} incorrect Assets")
    }
    return deadBlobs
  }

  /**
   * Verify the Asset metadata against the attached Blob, categorizing any errors encountered.
   */
  private DeadBlobResult checkAsset(final String repositoryName, final StorageTx tx, final Asset asset) {
    if (!asset) {
      return new DeadBlobResult(repositoryName, null, ASSET_DELETED, 'Asset was deleted during inspection')
    }
    try {
      def blob = tx.requireBlob(asset.requireBlobRef())
      verifyBlob(blob, asset)
    }
    catch (IllegalStateException ise) {
      return new DeadBlobResult(repositoryName, asset, MISSING_BLOB_REF, ise.message)
    }
    catch (BlobStoreException bse) {
      if (bse.cause instanceof IOException) {
        return new DeadBlobResult(repositoryName, asset, UNREADABLE_BLOB, bse.message)
      }
      return new DeadBlobResult(repositoryName, asset, DELETED, bse.message) // check for specific message?
    }
    catch (MismatchedSHA1Exception pae) {
      return new DeadBlobResult(repositoryName, asset, SHA1_DISAGREEMENT, pae.message)
    }
    catch (BlobUnavilableException e) {
      return new DeadBlobResult(repositoryName, asset, UNAVAILABLE_BLOB, e.message ?: 'Blob inputstream unavailable')
    }
    catch (Throwable e) {
      return new DeadBlobResult(repositoryName, asset, UNKNOWN, e.message)
    }
    return null
  }

  /**
   * Verify that the Blob exists and is in agreement with the stored Asset metadata.
   */
  private void verifyBlob(Blob blob, Asset asset) {
    BlobMetrics metrics = blob.metrics
    if (metrics.sha1Hash != asset.getChecksum(HashAlgorithm.SHA1).toString()) {
      throw new MismatchedSHA1Exception()
    }
    InputStream blobstream
    try {
      blobstream = blob.inputStream
      if (metrics.contentSize > 0) {
        if (!blobstream.available()) {
          throw new BlobUnavilableException()
        }
      }
    }
    finally {
      if (blobstream) {
        try {
          blobstream.close()
        }
        catch (e) {
          log.error('Unable to close stream', e)
        }
      }
    }
  }

  /**
   * Log details about an incorrect result, including if state changed between inspections.
   */
  private void logResults(final DeadBlobResult firstResult, final DeadBlobResult secondResult) {
    log.info("Possible bad data found in Asset: ${secondResult.asset}")
    if (lastUpdated(firstResult) != lastUpdated(secondResult)) {
      log.info("Asset metadata was updated during inspection between ${lastUpdated(firstResult)} " +
          "and ${lastUpdated(secondResult)}")
    }
    if (firstResult.resultState != secondResult.resultState) {
      log.info("Error state changed from ${firstResult.resultState} to ${secondResult.resultState} during inspection")
    }
    if (blobUpdated(firstResult) != blobUpdated(secondResult)) {
      log.info("Asset blob was updated during inspection between ${blobUpdated(firstResult)} " +
          "and ${blobUpdated(secondResult)}")
    }
  }

  private static blobUpdated(DeadBlobResult deadBlobResult) {
    deadBlobResult.asset?.blobUpdated()
  }

  private static lastUpdated(DeadBlobResult deadBlobResult) {
    deadBlobResult.asset?.lastUpdated()
  }
}
