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
package org.sonatype.nexus.blobstore.restore.orient;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.Priority;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobAttributes;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobMetrics;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.logging.task.ProgressLogIntervalHelper;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.hash.HashCode;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonatype.nexus.blobstore.api.BlobAttributesConstants.HEADER_PREFIX;
import static org.sonatype.nexus.blobstore.api.BlobStore.BLOB_NAME_HEADER;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;

/**
 * Default {@link OrientIntegrityCheckStrategy} which checks name and SHA1 checksum
 *
 * @since 3.6.1
 */
@Priority(Integer.MAX_VALUE)
@Named(DefaultOrientIntegrityCheckStrategy.DEFAULT_NAME)
@Singleton
public class DefaultOrientIntegrityCheckStrategy
    extends ComponentSupport
    implements OrientIntegrityCheckStrategy
{
  public static final String NAME_MISMATCH = "Name does not match on asset! Metadata name: '{}', Blob name: '{}'";

  public static final String DEFAULT_NAME = "default";

  static final String BLOB_PROPERTIES_MISSING_FOR_ASSET = "Blob properties missing for asset '{}'.";

  static final String BLOB_DATA_MISSING_FOR_ASSET = "Blob data missing for asset '{}'.";

  static final String BLOB_PROPERTIES_MARKED_AS_DELETED = "Blob properties marked as deleted for asset '{}'. Will be removed on next compact.";

  static final String SHA1_MISMATCH = "SHA1 does not match on asset '{}'! Metadata SHA1: '{}', Blob SHA1: '{}'";

  static final String ASSET_SHA1_MISSING = "Asset is missing SHA1 hash code";

  static final String ASSET_NAME_MISSING = "Asset is missing name";

  static final String BLOB_NAME_MISSING = "Blob properties is missing name";

  static final String ERROR_ACCESSING_BLOB = "Error accessing blob for asset '{}'. Exception: {}";

  static final String ERROR_PROCESSING_ASSET = "Error processing asset '{}'";

  static final String ERROR_PROCESSING_ASSET_WITH_EX = ERROR_PROCESSING_ASSET + ". Exception: {}";

  static final String CANCEL_WARNING = "Cancelling blob integrity check";

  @Override
  public void check(
      final Repository repository,
      final BlobStore blobStore,
      final Supplier<Boolean> isCancelled,
      final Consumer<Asset> integrityCheckFailedHandler)
  {
    log.info("Checking integrity of assets in repository '{}' with blob store '{}'", repository.getName(),
        blobStore.getBlobStoreConfiguration().getName());
    long processed = 0;
    long failedCounter = 0;

    try (final ProgressLogIntervalHelper progressLogger = new ProgressLogIntervalHelper(log, 60)) {
      for (Asset asset : getAssets(repository)) {
        boolean failed = false;
        try {
          if (isCancelled.get()) {
            log.warn(CANCEL_WARNING);
            return;
          }

          log.debug("checking asset {}", asset);
          BlobRef blobRef = asset.requireBlobRef();
          BlobId blobId = blobRef.getBlobId();

          BlobAttributes blobAttributes = blobStore.getBlobAttributes(blobId);

          if (blobAttributes == null) {
            log.error(BLOB_PROPERTIES_MISSING_FOR_ASSET, asset.name());
            failed = true;
          }
          else if (blobAttributes.isDeleted()) {
            log.warn(BLOB_PROPERTIES_MARKED_AS_DELETED, asset.name());
            failed = true;
          }
          else if (!blobDataExists(blobStore.get(blobId))){
            log.error(BLOB_DATA_MISSING_FOR_ASSET, asset.name());
            failed = true;
          }
          else if (!checkAsset(blobAttributes, asset)) {
            failed = true;
          }
        }
        catch (IllegalStateException e) {
          // thrown by requireBlobRef
          log.error(ERROR_ACCESSING_BLOB, asset.toString(), e.getMessage(), log.isDebugEnabled() ? e : null);
          failed = true;
        }
        catch (IllegalArgumentException e) {
          // thrown by checkAsset inner methods
          log.error(ERROR_PROCESSING_ASSET_WITH_EX, asset.toString(), e.getMessage(), log.isDebugEnabled() ? e : null);
          failed = true;
        }
        catch (Exception e) {
          log.error(ERROR_PROCESSING_ASSET, asset.toString(), e);
          failed = true;
        }
        if (failed) {
          failedCounter++;
          if (integrityCheckFailedHandler != null) {
            integrityCheckFailedHandler.accept(asset);
          }
        }
        progressLogger
            .info("Elapsed time: {}, processed: {}, failed integrity check: {}", progressLogger.getElapsed(), ++processed, failedCounter);
      }
    }
  }

  /**
   * Check the asset for integrity. By default checks name and SHA1.
   *
   * @param blobAttributes the {@link BlobAttributes} from the {@link Blob}
   * @param asset          the {@link Asset}
   * @return true if asset integrity is intact, false otherwise
   */
  protected boolean checkAsset(final BlobAttributes blobAttributes, final Asset asset) {
    checkArgument(blobAttributes.getProperties() != null, "Blob attributes are missing properties");

    return checkSha1(blobAttributes, asset) && checkName(blobAttributes, asset);
  }

  /**
   * returns true if the checksum matches, false otherwise
   */
  private boolean checkSha1(final BlobAttributes blobAttributes, final Asset asset) {
    String assetSha1 = getAssetSha1(asset);
    String blobSha1 = getBlobSha1(blobAttributes);

    if (!Objects.equals(assetSha1, blobSha1)) {
      log.error(SHA1_MISMATCH, asset.name(), assetSha1, blobSha1);
      return false;
    }

    return true;
  }

  /**
   * Get the SHA1 from the {@link BlobAttributes}
   */
  protected String getBlobSha1(final BlobAttributes blobAttributes) {
    BlobMetrics metrics = blobAttributes.getMetrics();
    checkArgument(metrics != null, "Blob attributes are missing metrics");
    String blobSha1 = metrics.getSha1Hash();
    checkArgument(blobSha1 != null, "Blob metrics are missing SHA1 hash code");
    return blobSha1;
  }

  /**
   * Get the SHA1 from the {@link Asset}
   */
  protected String getAssetSha1(final Asset asset) {
    HashCode assetSha1HashCode = asset.getChecksum(SHA1);
    checkArgument(assetSha1HashCode != null, ASSET_SHA1_MISSING);
    return assetSha1HashCode.toString();
  }

  /**
   * returns true if the name matches, false otherwise
   */
  private boolean checkName(final BlobAttributes blobAttributes, final Asset asset) {
    String assetName = getAssetName(asset);
    String blobName = getBlobName(blobAttributes);

    checkArgument(blobName != null, BLOB_NAME_MISSING);
    checkArgument(assetName != null, ASSET_NAME_MISSING);

    if (blobName.startsWith("/") && !assetName.startsWith("/")) {
      blobName = blobName.substring(1);
    }

    if (!Objects.equals(assetName, blobName)) {
      log.error(NAME_MISMATCH, blobName, assetName);
      return false;
    }

    return true;
  }

  /**
   * returns true if the blobs data is accessible, false otherwise
   */
  protected boolean blobDataExists(final Blob blob) {
    try {
      blob.getInputStream().close();
      return true;
    }
    catch (Exception e) { // NOSONAR
      return false;
    }
  }

  /**
   * Get the name from the {@link BlobAttributes}
   */
  protected String getBlobName(final BlobAttributes blobAttributes) {
    return blobAttributes.getProperties().getProperty(HEADER_PREFIX + BLOB_NAME_HEADER);
  }

  /**
   * Get the name from the {@link Asset}
   */
  protected String getAssetName(final Asset asset) {
    return asset.name();
  }

  protected Iterable<Asset> getAssets(final Repository repository) {
    return Transactional.operation
        .withDb(repository.facet(StorageFacet.class).txSupplier())
        .call(() -> {
          final StorageTx tx = UnitOfWork.currentTx();
          return tx.browseAssets(tx.findBucket(repository));
        });
  }
}
