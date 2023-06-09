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

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.search.SearchTableDataExtension;
import org.sonatype.nexus.repository.content.store.InternalIds;
import org.sonatype.nexus.repository.content.utils.PreReleaseEvaluator;
import org.sonatype.nexus.repository.search.normalize.VersionNormalizerService;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.sonatype.nexus.common.hash.HashAlgorithm.MD5;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA256;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA512;

/**
 * Producer of {@link SearchTableData} objects to be stored in the sql search table.
 */
@Named
@Singleton
public class SearchTableDataProducer
    extends ComponentSupport
{
  private static final String PATH_SPLIT_REGEX = "[/]+";

  private final Map<String, SearchCustomFieldContributor> searchCustomFieldContributors;

  private final VersionNormalizerService versionNormalizerService;

  private final Map<String, PreReleaseEvaluator> formatToReleaseEvaluators;

  private final Set<SearchTableDataExtension> searchTableDataExtensions;

  @Inject
  public SearchTableDataProducer(
      final Map<String, SearchCustomFieldContributor> searchCustomFieldContributors,
      final VersionNormalizerService versionNormalizerService,
      final Map<String, PreReleaseEvaluator> formatToReleaseEvaluators,
      final Set<SearchTableDataExtension> searchTableDataExtensions)
  {
    this.searchCustomFieldContributors = checkNotNull(searchCustomFieldContributors);
    this.versionNormalizerService = checkNotNull(versionNormalizerService);
    this.formatToReleaseEvaluators = checkNotNull(formatToReleaseEvaluators);
    this.searchTableDataExtensions = checkNotNull(searchTableDataExtensions);
  }

  public Optional<SearchTableData> createSearchTableData(final FluentComponent component, final Repository repository) {
    Integer repositoryId = repository.facet(ContentFacet.class).contentRepositoryId();
    String repositoryName = repository.getName();
    String repositoryFormat = repository.getFormat().getValue();

    SearchTableData data = new SearchTableData();
    data.setRepositoryId(repositoryId);
    data.setComponentId(InternalIds.internalComponentId(component));
    data.setFormat(repositoryFormat);
    data.setNamespace(component.namespace());
    data.setComponentName(component.name());
    data.addAliasComponentName(component.name());
    data.setComponentKind(component.kind());
    data.setVersion(component.version());
    data.setNormalisedVersion(
        versionNormalizerService.getNormalizedVersionByFormat(component.version(), repository.getFormat()));
    data.setRepositoryName(repositoryName);
    data.addKeywords(asList(component.namespace(), component.name(), component.version()));
    data.setEntityVersion(component.entityVersion());
    data.setAttributes(component.attributes());

    Collection<FluentAsset> assets = component.assets();
    if (assets.isEmpty()) {
      log.debug("Component {}:{}:{} has no assets", component.namespace(), component.name(), component.version());
      return Optional.empty();
    }

    assets.stream()
        .map(Asset::blob)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(AssetBlob::blobCreated)
        .max(OffsetDateTime::compareTo)
        .ifPresent(data::setLastModified);

    assets.forEach(asset -> addAssetData(data, component, asset, repository));
    addSearchExtensions(data, component);
    return Optional.of(data);
  }

  private void addAssetData(
      final SearchTableData searchTableData,
      final FluentComponent component,
      final Asset asset,
      final Repository repository)
  {
    Optional<AssetBlob> blob = asset.blob();
    String repositoryFormat = repository.getFormat().getValue();

    if (blob.isPresent()) {
      addBlobInfo(blob.get(), searchTableData);
    }
    else {
      log.debug("Unable to determine blob for asset {}", asset.path());
    }

    SearchCustomFieldContributor contributor = searchCustomFieldContributors.get(repositoryFormat);
    if (contributor != null) {
      if (contributor.isEnableSearchByPath(asset.path())) {
        splitAssetPathToKeywords(searchTableData, asset);
      }
      contributor.populateSearchCustomFields(searchTableData, asset);
    }
    else {
      splitAssetPathToKeywords(searchTableData, asset);
    }

    //prerelease evaluation false by default for all components
    PreReleaseEvaluator evaluator = formatToReleaseEvaluators.get(repositoryFormat);
    boolean preRelease = evaluator != null && evaluator.isPreRelease(component, singletonList(asset));
    searchTableData.setPrerelease(preRelease);
  }

  private static void addBlobInfo(final AssetBlob blob, final SearchTableData data) {
    data.addKeyword(blob.contentType());

    data.addMd5(blob.checksums().get(MD5.name()));
    data.addSha1(blob.checksums().get(SHA1.name()));
    data.addSha256(blob.checksums().get(SHA256.name()));
    data.addSha512(blob.checksums().get(SHA512.name()));

    blob.createdBy().ifPresent(data::addUploader);
    blob.createdByIp().ifPresent(data::addUploaderIp);
  }

  private static void splitAssetPathToKeywords(final SearchTableData searchTableData, final Asset asset) {
    String path = asset.path();
    searchTableData.addPath(path);
    searchTableData.addKeywords(asList(path.split(PATH_SPLIT_REGEX)));
    searchTableData.addKeywords(singletonList(path));
  }

  private void addSearchExtensions(final SearchTableData searchTableData, final FluentComponent component) {
    for (SearchTableDataExtension extension : searchTableDataExtensions) {
      extension.contribute(searchTableData, component);
    }
  }
}
