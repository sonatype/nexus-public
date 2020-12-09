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

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.repository.attributes.AttributesFacet;
import org.sonatype.nexus.repository.content.AttributeChange;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.npm.NpmContentFacet;
import org.sonatype.nexus.repository.npm.internal.NpmFormatAttributesExtractor;
import org.sonatype.nexus.repository.npm.internal.NpmPackageParser;
import org.sonatype.nexus.repository.npm.internal.search.v1.NpmSearchFacet;
import org.sonatype.nexus.repository.search.index.SearchIndexFacet;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.scheduling.Cancelable;

import com.google.common.base.Throwables;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.npm.internal.NpmAttributes.AssetKind.TARBALL;

/**
 * Task that reindexes npm proxy and hosted repositories by opening each tarball and extracting the contents of the
 * {@code package.json} as format attributes. This task is necessary to "upgrade" existing npm repositories to contain
 * the search-indexed format attributes necessary for npm v1 search.
 *
 * @since 3.7
 */
@Named
public class DatastoreReindexNpmRepositoryTask
    extends RepositoryTaskSupport
    implements Cancelable, ReindexNpmRepositoryTask
{
  private final NpmPackageParser npmPackageParser;

  @Inject
  public DatastoreReindexNpmRepositoryTask(final NpmPackageParser npmPackageParser) {
    this.npmPackageParser = checkNotNull(npmPackageParser);
  }

  @Override
  protected void execute(final Repository repository) {
    // the search index must be rebuilt first in order to absorb the changes to the elasticsearch mapping
    SearchIndexFacet searchIndexFacet = repository.facet(SearchIndexFacet.class);
    searchIndexFacet.rebuildIndex();

    NpmContentFacet npmContentFacet = repository.facet(NpmContentFacet.class);
    // format attributes must be extracted from each asset as we may not have done so previously (for existing npm
    // repositories with content before we actually bothered to extract and save any asset-specific format metadata)
    Continuation<FluentAsset> assets = npmContentFacet.assets().byKind(TARBALL.toString()).browse(1000, null);

    while (!assets.isEmpty() && !isCanceled()) {
      try {
        updateAssets(repository, assets);
        assets = npmContentFacet.assets().byKind(TARBALL.toString()).browse(1000, assets.nextContinuationToken());
      }
      catch (Exception e) {
        Throwables.propagateIfPossible(e, RuntimeException.class);
        throw new RuntimeException(e);
      }
    }

    // once processed (as best we could) the repository should no longer be flagged (if it ever was)
    repository.facet(AttributesFacet.class)
        .modifyAttributes((final NestedAttributesMap attributes) -> attributes.remove(NPM_V1_SEARCH_UNSUPPORTED));
  }

  /**
   * Updates a batch of assets, opening each asset that represents a tarball and repopulating its format-specific
   * metadata from the tarball itself. The ID for the last asset examined is returned for use in finding the next page.
   */
  private void updateAssets(final Repository repository, final Continuation<FluentAsset> assets) {
    assets.forEach(asset -> maybeUpdateAsset(repository, asset));
  }

  /**
   * Processes an asset, potentially updating the asset if the asset happens to be a tarball and the format attributes
   * can be extracted from the asset's package.json.
   */
  private void maybeUpdateAsset(final Repository repository, final FluentAsset asset) {
    Content content = asset.download();
    if (content == null) {
      return;
    }
    try (InputStream in = content.openInputStream()) {
      Map<String, Object> formatAttributes = npmPackageParser.parsePackageJson(() -> in);
      if (formatAttributes.isEmpty()) {
        log.warn(
            "No format attributes found in package.json for npm asset {} in repository {}, will not be searchable",
            asset.path(), repository.getName());
      }
      else {
        NpmFormatAttributesExtractor formatAttributesExtractor = new NpmFormatAttributesExtractor(formatAttributes);

        NestedAttributesMap attributes = new NestedAttributesMap(null, new HashMap<>());
        formatAttributesExtractor.copyFormatAttributes(attributes);
        attributes.entries().forEach(entry -> asset.attributes(AttributeChange.SET, entry.getKey(), entry.getValue()));
      }
    }
    catch (Exception e) {
      log.error("Error occurred while reindexing npm asset {} in repository {}, will not be searchable", asset.path(),
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
