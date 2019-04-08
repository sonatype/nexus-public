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
package org.sonatype.nexus.repository.npm.internal.tasks;

import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.repository.npm.internal.NpmAttributes.AssetKind;
import org.sonatype.nexus.repository.npm.internal.NpmFormatAttributesExtractor;
import org.sonatype.nexus.repository.npm.internal.NpmPackageParser;
import org.sonatype.nexus.repository.npm.internal.search.v1.NpmSearchFacet;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.repository.attributes.AttributesFacet;
import org.sonatype.nexus.repository.search.SearchFacet;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetEntityAdapter;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalStoreMetadata;
import org.sonatype.nexus.scheduling.Cancelable;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.npm.internal.NpmAttributes.AssetKind.TARBALL;
import static java.util.Collections.singletonList;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;

/**
 * Task that reindexes npm proxy and hosted repositories by opening each tarball and extracting the contents of the
 * {@code package.json} as format attributes. This task is necessary to "upgrade" existing npm repositories to contain
 * the search-indexed format attributes necessary for npm v1 search.
 *
 * @since 3.7
 */
@Named
public class ReindexNpmRepositoryTask
    extends RepositoryTaskSupport
    implements Cancelable
{
  public static final String NPM_V1_SEARCH_UNSUPPORTED = "npm_v1_search_unsupported";

  private static final String ASSETS_WHERE = "@rid > :rid";

  private static final String ASSETS_SUFFIX = "ORDER BY @rid LIMIT :limit";

  private static final int BATCH_SIZE = 100;

  private static final String BEGINNING_ID = "#-1:-1";

  private final NpmPackageParser npmPackageParser;

  private final AssetEntityAdapter assetEntityAdapter;

  @Inject
  public ReindexNpmRepositoryTask(final NpmPackageParser npmPackageParser,
                                  final AssetEntityAdapter assetEntityAdapter) {
    this.npmPackageParser = checkNotNull(npmPackageParser);
    this.assetEntityAdapter = checkNotNull(assetEntityAdapter);
  }

  @Override
  protected void execute(final Repository repository) {
    // the search index must be rebuilt first in order to absorb the changes to the elasticsearch mapping
    SearchFacet searchFacet = repository.facet(SearchFacet.class);
    searchFacet.rebuildIndex();

    // format attributes must be extracted from each asset as we may not have done so previously (for existing npm
    // repositories with content before we actually bothered to extract and save any asset-specific format metadata)
    String lastId = BEGINNING_ID;
    while (lastId != null && !isCanceled()) {
      try {
        lastId = processBatch(repository, lastId);
      }
      catch (Exception e) {
        Throwables.propagateIfPossible(e, RuntimeException.class);
        throw new RuntimeException(e);
      }
    }

    // once processed (as best we could) the repository should no longer be flagged (if it ever was)
    repository.facet(AttributesFacet.class)
        .modifyAttributes((NestedAttributesMap attributes) -> attributes.remove(NPM_V1_SEARCH_UNSUPPORTED));
  }

  /**
   * Processes a batch of records starting after the provided RID of the last asset in the previous batch.
   */
  @Nullable
  private String processBatch(final Repository repository, final String lastId) throws Exception {
    return TransactionalStoreMetadata.operation
        .withDb(repository.facet(StorageFacet.class).txSupplier())
        .throwing(Exception.class)
        .call(() -> {
          Iterable<Asset> assets = readAssets(repository, lastId);
          return updateAssets(repository, assets);
        });
  }

  /**
   * Reads the next batch of tarball assets to process, starting after the RID of the last asset in the previous batch.
   */
  private Iterable<Asset> readAssets(final Repository repository, final String lastId) {
    StorageTx storageTx = UnitOfWork.currentTx();
    Map<String, Object> parameters = ImmutableMap.of("rid", lastId, "limit", BATCH_SIZE);
    return storageTx.findAssets(ASSETS_WHERE, parameters, singletonList(repository), ASSETS_SUFFIX);
  }

  /**
   * Updates a batch of assets, opening each asset that represents a tarball and repopulating its format-specific
   * metadata from the tarball itself. The ID for the last asset examined is returned for use in finding the next page.
   */
  @Nullable
  private String updateAssets(final Repository repository, final Iterable<Asset> assets) {
    String lastId = null;
    for (Asset asset : assets) {
      lastId = assetEntityAdapter.recordIdentity(asset).toString();
      maybeUpdateAsset(repository, asset);
    }
    return lastId;
  }

  /**
   * Processes an asset, potentially updating the asset if the asset happens to be a tarball and the format attributes
   * can be extracted from the asset's package.json.
   */
  private void maybeUpdateAsset(final Repository repository, final Asset asset) {
    try {
      AssetKind assetKind = AssetKind.valueOf(asset.formatAttributes().get(P_ASSET_KIND, String.class));
      if (assetKind != TARBALL) {
        return;
      }
      StorageTx storageTx = UnitOfWork.currentTx();
      Blob blob = storageTx.requireBlob(asset.blobRef());
      Map<String, Object> formatAttributes = npmPackageParser.parsePackageJson(blob::getInputStream);
      if (formatAttributes.isEmpty()) {
        log.warn(
            "No format attributes found in package.json for npm asset {} in repository {}, will not be searchable",
            asset.name(), repository.getName());
      }
      else {
        NpmFormatAttributesExtractor formatAttributesExtractor = new NpmFormatAttributesExtractor(formatAttributes);
        formatAttributesExtractor.copyFormatAttributes(asset);
        storageTx.saveAsset(asset);
      }
    }
    catch (Exception e) {
      log.error("Error occurred while reindexing npm asset {} in repository {}, will not be searchable", asset.name(),
          repository.getName(), e);
    }
  }

  @Override
  protected boolean appliesTo(final Repository repository) {
    return repository.optionalFacet(NpmSearchFacet.class).isPresent();
  }

  @Override
  public String getMessage() {
    return "Reindexing npm format attributes of " + getRepositoryField();
  }
}
