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

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.EntityHelper;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.DatabaseInstanceNames;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.ComponentMaintenance;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import org.joda.time.DateTime;

import static com.google.common.collect.Lists.newArrayList;
import static java.time.LocalDate.now;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.endsWith;
import static org.apache.commons.lang3.StringUtils.indexOf;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.substring;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;

@FeatureFlag(name = "nexus.orient.store.content")
@Named
@Singleton
@Priority(Integer.MAX_VALUE)
public class OrientComponentAssetTestHelper
    implements ComponentAssetTestHelper
{
  private static final String DELETE_COMPONENT_SQL =
      "delete from component where group = ? and name = ? and version = ?";

  private static final DateTimeFormatter YEAR_MONTH_DAY_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

  private static final String SNAPSHOT_VERSION_SUFFIX = "-SNAPSHOT";

  @Inject
  @Named(DatabaseInstanceNames.COMPONENT)
  public Provider<DatabaseInstance> databaseInstanceProvider;

  @Inject
  RepositoryManager repositoryManager;

  @Override
  public DateTime getBlobCreatedTime(final Repository repository, final String path) {
    return findAssetByName(repository, path).map(Asset::blobCreated).orElse(null);
  }

  @Override
  public DateTime getAssetCreatedTime(final Repository repository, final String path) {
    //not sure why it is the same
    return getBlobCreatedTime(repository, path);
  }

  @Override
  public DateTime getUpdatedTime(final Repository repository, final String path) {
    return findAssetByName(repository, path).map(Asset::blobUpdated).orElse(null);
  }

  @Override
  public DateTime getLastDownloadedTime(final Repository repository, final String path) {
    return findAssetByName(repository, path).map(Asset::lastDownloaded).orElse(null);
  }

  @Override
  public void removeAsset(final Repository repository, final String path) {
    EntityId nanoAssetId = EntityHelper.id(findAssetByNameNotNull(repository, path));
    ComponentMaintenance maintenanceFacet = repository.facet(ComponentMaintenance.class);
    maintenanceFacet.deleteAsset(nanoAssetId);
  }

  @Override
  public void deleteComponent(
      final Repository repository,
      final String namespace,
      final String name,
      final String version)
  {
    try (StorageTx tx = repository.facet(StorageFacet.class).txSupplier().get()) {
      tx.begin();
      tx.getDb().command(new OCommandSQL(DELETE_COMPONENT_SQL)).execute(namespace, name, version);
      tx.commit();
    }
  }

  @Override
  public List<String> findAssetPaths(final String repositoryName) {
    String sql = "SELECT * FROM asset WHERE bucket.repository_name = ?";
    try (ODatabaseDocumentTx tx = databaseInstanceProvider.get().acquire()) {
      tx.begin();
      List<ODocument> results = tx.command(new OCommandSQL(sql)).execute(repositoryName);
      return results.stream().map(doc -> doc.field("name", String.class).toString()).collect(toList());
    }
  }

  @Override
  public String contentTypeFor(final String repositoryName, final String path) {
    return findAssetByNameNotNull(repositoryManager.get(repositoryName), path).contentType();
  }

  @Override
  public int countComponents(final Repository repository) {
    return findComponents(repository).size();
  }

  private List<Component> findComponents(final Repository repository) {
    try (StorageTx tx = repository.facet(StorageFacet.class).txSupplier().get()) {
      tx.begin();
      return newArrayList(tx.browseComponents(tx.findBucket(repository)));
    }
  }

  private static Asset findAssetByNameNotNull(final Repository repository, final String name) {
    return findAssetByName(repository, name).orElseThrow(() -> new AssetNotFoundException(repository, name));
  }

  private static Optional<Asset> findAssetByName(final Repository repository, final String name) {
    try (StorageTx tx = repository.facet(StorageFacet.class).txSupplier().get()) {
      tx.begin();
      return Optional.ofNullable(tx.findAssetWithProperty(P_NAME, name, tx.findBucket(repository)));
    }
  }

  @Override
  public NestedAttributesMap attributes(final Repository repository, final String path) {
    return findAssetByNameNotNull(repository, path).attributes();
  }

  @Override
  public boolean assetExists(final Repository repository, final String path) {
    return findAssetByName(repository, path).isPresent();
  }

  @Override
  public boolean componentExists(final Repository repository, final String name, final String version) {
    if (version.endsWith(SNAPSHOT_VERSION_SUFFIX)) {
      Optional<Component> component = findSnapshotComponent(repository, name, version);
      return component.isPresent();
    }
    else {
      return findComponents(repository).stream()
          .filter(c -> name.equals(c.name()))
          .anyMatch(c -> version.equals(c.version()));
    }
  }

  @Override
  public boolean componentExists(
      final Repository repository,
      final String namespace,
      final String name,
      final String version)
  {
    if (endsWith(version, SNAPSHOT_VERSION_SUFFIX)) {
      Optional<Component> component = findSnapshotComponent(repository, name, version);
      return component.isPresent();
    }
    else {
      return findComponent(repository, namespace, name, version).isPresent();
    }
  }

  private Optional<Component> findSnapshotComponent(
      final Repository repository,
      final String name,
      final String version)
  {
    String gav = substring(version, 0, indexOf(version, SNAPSHOT_VERSION_SUFFIX));
    String versionWithDate = String.format("%s-%s", gav, now().format(YEAR_MONTH_DAY_FORMAT));
    return findComponents(repository)
        .stream()
        .filter(c -> name.equals(c.name()))
        .filter(comp -> startsWith(comp.version(), versionWithDate))
        .findAny();
  }

  @Override
  public boolean assetWithComponentExists(
      final Repository repository,
      final String path,
      final String group,
      final String name)
  {
    return findAssetByName(repository, path)
        .map(Asset::componentId)
        .flatMap(componentId -> {
          try (StorageTx tx = repository.facet(StorageFacet.class).txSupplier().get()) {
            tx.begin();
            return Optional.ofNullable(tx.findComponent(componentId));
          }
        })
        .filter(component -> Objects.equals(component.group(), group))
        .filter(component -> Objects.equals(component.name(), name))
        .isPresent();
  }

  @Override
  public boolean assetWithComponentExists(
      final Repository repository,
      final String path,
      final String group,
      final String name,
      final String version)
  {
    return findAssetByName(repository, path)
        .map(Asset::componentId)
        .flatMap(componentId -> {
          try (StorageTx tx = repository.facet(StorageFacet.class).txSupplier().get()) {
            tx.begin();
            return Optional.ofNullable(tx.findComponent(componentId));
          }
        })
        .filter(component -> group == null || Objects.equals(component.group(), group))
        .filter(component -> Objects.equals(component.name(), name))
        .filter(component -> Objects.equals(component.version(), version))
        .isPresent();
  }

  @Override
  public NestedAttributesMap componentAttributes(
      final Repository repository,
      final String namespace,
      final String name,
      final String version)
  {
    return findComponent(repository, namespace, name, version).map(Component::attributes)
        .orElseThrow(() -> new ComponentNotFoundException(repository, namespace, name, version));
  }

  private Optional<Component> findComponent(
      final Repository repository,
      final String namespace,
      final String name,
      final String version)
  {
    return findComponents(repository).stream()
        .filter(c -> Objects.equals(namespace, c.group()))
        .filter(c -> name.equals(c.name()))
        .filter(c -> version.equals(c.version())).findAny();
  }

  @Override
  public boolean assetWithoutComponentExists(final Repository repository, final String path) {
    return !findAssetByName(repository, path).map(Asset::componentId).isPresent();
  }
}
