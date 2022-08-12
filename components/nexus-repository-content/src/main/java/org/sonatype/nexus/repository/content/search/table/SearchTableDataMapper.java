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
package org.sonatype.nexus.repository.content.search.table;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.store.InternalIds;
import org.sonatype.nexus.repository.search.normalize.VersionNormalizerService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.hash.HashAlgorithm.MD5;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA256;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA512;

/**
 * Utility class used to build {@link SearchTableData} object from the {@link Asset}
 */
@Named
@Singleton
public class SearchTableDataMapper
{
  private static final Logger log = LoggerFactory.getLogger(SearchTableDataMapper.class);

  private final Map<String, SearchCustomFieldContributor> searchCustomFieldContributors;

  private final VersionNormalizerService versionNormalizerService;

  @Inject
  public SearchTableDataMapper(final Map<String, SearchCustomFieldContributor> searchCustomFieldContributors,
                               final VersionNormalizerService versionNormalizerService) {
    this.searchCustomFieldContributors = checkNotNull(searchCustomFieldContributors);
    this.versionNormalizerService = checkNotNull(versionNormalizerService);
  }

  /**
   * Convert {@link Asset} into the {@link SearchTableData}
   *
   * @param asset      the asset to build from.
   * @param repository the repository which belongs to the asset.
   * @return the {@link SearchTableData} object.
   */
  public Optional<SearchTableData> convert(final Asset asset, final Repository repository)
  {
    checkNotNull(asset);
    checkNotNull(repository);

    Component component = asset.component().orElse(null);
    if (component == null) {
      log.debug("Unable to determine component for asset {}", asset.path());
      return Optional.empty();
    }
    AssetBlob blob = asset.blob().orElse(null);
    if (blob == null) {
      log.debug("Unable to determine blob for asset {}", asset.path());
      return Optional.empty();
    }

    Integer repositoryId = repository.facet(ContentFacet.class).contentRepositoryId();
    String repositoryName = repository.getName();
    String repositoryFormat = repository.getFormat().getValue();

    SearchTableData data = new SearchTableData();
    data.setRepositoryId(repositoryId);
    data.setComponentId(InternalIds.internalComponentId(component));
    data.setAssetId(InternalIds.internalAssetId(asset));
    data.setFormat(repositoryFormat);
    //data component
    data.setNamespace(component.namespace());
    data.setComponentName(component.name());
    data.setComponentKind(component.kind());
    data.setVersion(component.version());
    data.setNormalisedVersion(
        versionNormalizerService.getNormalizedVersionByFormat(component.version(), repository.getFormat()));
    data.setComponentCreated(component.created());
    data.setRepositoryName(repositoryName);
    //data asset
    data.setPath(asset.path());
    //data blob
    data.setContentType(blob.contentType());
    data.setMd5(blob.checksums().get(MD5.name()));
    data.setSha1(blob.checksums().get(SHA1.name()));
    data.setSha256(blob.checksums().get(SHA256.name()));
    data.setSha512(blob.checksums().get(SHA512.name()));

    //Custom format fields
    if (searchCustomFieldContributors.containsKey(repositoryFormat)) {
      searchCustomFieldContributors.get(repositoryFormat).populateSearchCustomFields(data, asset);
    }

    // uploader info
    data.setUploader(blob.createdBy().orElse(null));
    data.setUploaderIp(blob.createdByIp().orElse(null));

    return Optional.of(data);
  }
}
