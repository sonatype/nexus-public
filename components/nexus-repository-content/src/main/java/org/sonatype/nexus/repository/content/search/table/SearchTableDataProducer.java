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
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.RepositoryContent;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.store.InternalIds;
import org.sonatype.nexus.repository.content.utils.PreReleaseEvaluator;
import org.sonatype.nexus.repository.search.normalize.VersionNormalizerService;

import com.google.common.collect.Iterables;
import org.apache.commons.lang3.StringUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;
import static org.apache.commons.lang3.StringUtils.substring;
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

  private static final char PATH_SEPARATOR = '/';

  private final Map<String, SearchCustomFieldContributor> searchCustomFieldContributors;

  private final VersionNormalizerService versionNormalizerService;

  private final Map<String, PreReleaseEvaluator> formatToReleaseEvaluators;

  @Inject
  public SearchTableDataProducer(
      final Map<String, SearchCustomFieldContributor> searchCustomFieldContributors,
      final VersionNormalizerService versionNormalizerService,
      final Map<String, PreReleaseEvaluator> formatToReleaseEvaluators)
  {
    this.searchCustomFieldContributors = checkNotNull(searchCustomFieldContributors);
    this.versionNormalizerService = checkNotNull(versionNormalizerService);
    this.formatToReleaseEvaluators = checkNotNull(formatToReleaseEvaluators);
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
    data.setComponentCreated(component.created());
    data.setRepositoryName(repositoryName);
    data.addKeywords(asList(component.namespace(), component.name(), component.version()));

    Collection<FluentAsset> assets = component.assets();
    if (assets.isEmpty()) {
      log.debug("Component {}:{}:{} has no assets", component.namespace(), component.name(), component.version());
      return Optional.empty();
    }

    assets.forEach(asset -> addAssetData(data, component, asset, repository));
    data.setLastEventTime(getLatestUpdatedTime(component, assets));
    return Optional.of(data);
  }

  public static OffsetDateTime getLatestUpdatedTime(
      final FluentComponent component,
      final Collection<FluentAsset> assets)
  {
    Set<OffsetDateTime> updatedTimes = new TreeSet<>();
    updatedTimes.add(component.lastUpdated());

    Optional<FluentAsset> latestAssetUpdated = assets.stream().max(comparing(RepositoryContent::lastUpdated));
    latestAssetUpdated.map(RepositoryContent::lastUpdated).ifPresent(updatedTimes::add);

    return Iterables.getLast(updatedTimes);
  }

  private void addAssetData(
      final SearchTableData searchTableData,
      final FluentComponent component,
      final Asset asset,
      final Repository repository)
  {
    Optional<AssetBlob> blob = asset.blob();
    String repositoryFormat = repository.getFormat().getValue();

    splitAssetPathToKeywords(searchTableData, asset);

    if (blob.isPresent()) {
      addBlobInfo(blob.get(), searchTableData);
    }
    else {
      log.debug("Unable to determine blob for asset {}", asset.path());
    }

    if (searchCustomFieldContributors.containsKey(repositoryFormat)) {
      searchCustomFieldContributors.get(repositoryFormat).populateSearchCustomFields(searchTableData, asset);
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
    searchTableData.addKeywords(singletonList(path));
    searchTableData.addKeywords(asList(path.split(PATH_SPLIT_REGEX)));
    Optional.of(substring(path, path.lastIndexOf(PATH_SEPARATOR)))
        .filter(StringUtils::isNotBlank)
        .map(Collections::singletonList)
        .ifPresent(searchTableData::addKeywords);
  }
}
