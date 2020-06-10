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
package org.sonatype.nexus.testsuite.helpers;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.time.DateHelper;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.facet.ContentMaintenanceFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.manager.RepositoryManager;

import org.joda.time.DateTime;

@Named
@Singleton
public class DatastoreComponentAssetTestHelper
    implements ComponentAssetTestHelper
{
  @Inject
  private RepositoryManager repositoryManager;

  @Override
  public DateTime getCreatedTime(final Repository repository, final String path) {
    return findAssetByPathNotNull(repository, path).blob().map(AssetBlob::blobCreated)
        .map(DateHelper::toDateTime).orElse(null);
  }

  @Override
  public DateTime getUpdatedTime(final Repository repository, final String path) {
    // AssetBlobs are immutable so the created time is the equivalent of the old updated time.
    return getCreatedTime(repository, path);
  }

  @Override
  public DateTime getLastDownloadedTime(final Repository repository, final String path) {
    return findAssetByPathNotNull(repository, path).lastDownloaded().map(DateHelper::toDateTime).orElse(null);
  }

  @Override
  public void deleteComponent(
      final Repository repository,
      final String namespace,
      final String name,
      final String version)
  {
    Component component = findComponent(repository, namespace, name, version);
    repository.facet(ContentMaintenanceFacet.class).deleteComponent(component);
  }

  private static Component findComponent(
      final Repository repository,
      final String namespace,
      final String name,
      final String version)
  {
    return repository.facet(ContentFacet.class).components().name(name).namespace(namespace).version(version).find()
        .orElseThrow(() -> new ComponentNotFoundException(repository, namespace, name, version));
  }

  private static Asset findAssetByPathNotNull(final Repository repository, final String path) {
    return findAssetByPath(repository, path).orElseThrow(() -> new AssetNotFoundException(repository, path));
  }

  private static Optional<FluentAsset> findAssetByPath(final Repository repository, final String path) {
    return repository.facet(ContentFacet.class).assets().path('/' + path).find();
  }

  @Override
  public List<String> findAssetPaths(final String repositoryName) {
    return repositoryManager.get(repositoryName).facet(ContentFacet.class).assets().browse(Integer.MAX_VALUE, null)
        .stream().map(Asset::path).collect(Collectors.toList());
  }

  @Override
  public String contentTypeFor(final String repositoryName, final String path) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeAsset(final Repository repository, final String path) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int countComponents(final Repository repository) {
    return repository.facet(ContentFacet.class).components().browse(Integer.MAX_VALUE, null).size();
  }

  @Override
  public NestedAttributesMap attributes(final Repository repository, final String path) {
    return findAssetByPath(repository, path).map(FluentAsset::attributes).orElse(null);
  }

  @Override
  public boolean assetExists(final Repository repository, final String path) {
    return findAssetByPath(repository, path).isPresent();
  }

  @Override
  public boolean componentExists(final Repository repository, final String name, final String version) {
    Optional<FluentComponent> component =
        repository.facet(ContentFacet.class).components().name(name).version(version).find();
    return component.isPresent();
  }

  @Override
  public boolean componentExists(final Repository repository, final String namespace, final String name, final String version) {
    Optional<FluentComponent> component = repository.facet(ContentFacet.class).components().name(name).version(version)
        .find().filter(c -> Objects.equals(c.namespace(), namespace));

    return component.isPresent();
  }
}
