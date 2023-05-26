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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.common.collect.ImmutableNestedAttributesMap;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.time.DateHelper;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.event.asset.AssetDownloadedEvent;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.maintenance.ContentMaintenanceFacet;
import org.sonatype.nexus.repository.content.store.ContentStoreEvent;
import org.sonatype.nexus.repository.content.store.InternalIds;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.repository.view.Content;

import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;

import static com.google.common.base.Strings.nullToEmpty;
import static java.lang.Boolean.TRUE;
import static java.time.LocalDate.now;
import static org.apache.commons.lang.StringUtils.stripStart;
import static org.apache.commons.lang3.StringUtils.endsWith;
import static org.apache.commons.lang3.StringUtils.indexOf;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.substring;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.sonatype.nexus.blobstore.api.BlobStoreManager.DEFAULT_BLOBSTORE_NAME;
import static org.sonatype.nexus.common.entity.Continuations.iterableOf;
import static org.sonatype.nexus.common.entity.Continuations.streamOf;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;
import static org.sonatype.nexus.repository.cache.CacheInfo.CACHE_TOKEN;
import static org.sonatype.nexus.repository.cache.CacheInfo.LAST_VERIFIED;
import static org.sonatype.nexus.repository.content.AttributeOperation.OVERLAY;

@Named
@Singleton
public class DatastoreComponentAssetTestHelper
    extends ComponentSupport
    implements ComponentAssetTestHelper
{
  private static final String SNAPSHOT_VERSION_SUFFIX = "-SNAPSHOT";

  private static final String UPDATE_TIME_ERROR_MESSAGE = "Failed to set download time: ";

  private static final DateTimeFormatter YEAR_MONTH_DAY_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

  @Inject
  private RepositoryManager repositoryManager;

  @Inject
  private DataSessionSupplier sessionSupplier;

  @Inject
  private EventManager eventManager;

  @Inject
  private BlobStoreManager blobStoreManager;

  @Override
  public DateTime getAssetCreatedTime(final Repository repository, final String path) {
    return DateHelper.toDateTime(findAssetByPathNotNull(repository, path).created());
  }

  @Override
  public DateTime getBlobUpdatedTime(final Repository repository, final String path) {
    // AssetBlobs are immutable so the created time is the equivalent of the old updated time.
    return findAssetByPathNotNull(repository, path)
        .blob()
        .map(AssetBlob::blobCreated)
        .map(DateHelper::toDateTime)
        .orElse(null);
  }

  @Override
  public DateTime getLastDownloadedTime(final Repository repository, final String path) {
    return findAssetByPathNotNull(repository, path)
        .lastDownloaded()
        .map(DateHelper::toDateTime)
        .orElse(null);
  }

  @Override
  public String getCreatedBy(final Repository repository, final String path) {
    return findAssetByPath(repository, path)
        .flatMap(fluentAsset -> fluentAsset.blob()
            .flatMap(AssetBlob::createdBy))
        .orElse(null);
  }

  @Override
  public String getCreatedByIP(final Repository repository, final String path) {
    return findAssetByPath(repository, path)
        .flatMap(fluentAsset -> fluentAsset.blob()
            .flatMap(AssetBlob::createdByIp))
        .orElse(null);
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

  @Override
  public void deleteComponent(final Repository repository, final String name, final String version)
  {
    removeComponent(repository, null, name, version);
  }

  @Override
  public void removeComponent(final Repository repository, final String namespace, final String name, final String version)
  {
    Component component = findComponent(repository, namespace, name, version);
    repository.facet(ContentMaintenanceFacet.class).deleteComponent(component);
  }

  private static FluentComponent findComponent(
      final Repository repository,
      @Nullable final String namespace,
      final String name,
      final String version)
  {
    return repository.facet(ContentFacet.class).components()
        .name(name)
        .namespace(nullToEmpty(namespace))
        .version(version)
        .find()
        .orElseThrow(() -> new ComponentNotFoundException(repository, namespace, name, version));
  }

  private static FluentComponent findComponent(
      final Repository repository,
      final String namespace,
      final String name)
  {
    return repository.facet(ContentFacet.class).components()
        .name(name)
        .namespace(nullToEmpty(namespace))
        .find()
        .orElseThrow(() -> new ComponentNotFoundException(repository, namespace, name, null));
  }

  private Asset findAssetByPathNotNull(final Repository repository, final String path) {
    return findAssetByPath(repository, path).orElseThrow(() -> new AssetNotFoundException(repository, path));
  }

  private Optional<FluentAsset> findAssetByPath(final Repository repository, final String path) {
    return repository.facet(ContentFacet.class).assets()
        .path(adjustedPath(path))
        .find();
  }

  @Override
  public List<String> findAssetPaths(final String repositoryName) {
    return streamOf(repositoryManager.get(repositoryName).facet(ContentFacet.class).assets()::browse)
        .map(Asset::path)
        .collect(Collectors.toList());
  }

  @Override
  public String assetKind(final Repository repository, final String path) {
    return findAssetByPathNotNull(repository, path).kind();
  }

  @Override
  public String contentTypeFor(final Repository repository, final String path) {
    Optional<AssetBlob> blob = findAssetByPathNotNull(repository, path).blob();
    return blob.map(AssetBlob::contentType).orElseThrow(() -> new BlobNotFoundException(repository, path));
  }

  @Override
  public void removeAsset(final Repository repository, final String path) {
    ContentMaintenanceFacet contentMaintenanceFacet = repository.facet(ContentMaintenanceFacet.class);
    findAssetByPath(repository, path).ifPresent(contentMaintenanceFacet::deleteAsset);
  }

  @Override
  public int countAssets(final Repository repository) {
    return repository.facet(ContentFacet.class).assets().count();
  }

  @Override
  public int countComponents(final Repository repository) {
    return repository.facet(ContentFacet.class).components().count();
  }

  @Override
  public NestedAttributesMap attributes(final Repository repository, final String path) {
    return findAssetByPathNotNull(repository, adjustedPath(path)).attributes();
  }

  @Override
  public Map<String, String> checksums(final Repository repository, final String path) {
    Optional<AssetBlob> blob = findAssetByPathNotNull(repository, path).blob();
    return blob.isPresent() ? blob.get().checksums() : Collections.emptyMap();
  }

  @Override
  public boolean assetExists(final Repository repository, final String path) {
    return findAssetByPath(repository, path).isPresent();
  }

  @Override
  public boolean assetExists(final Repository repository, final String componentName, final String formatExtension) {
    Optional<FluentComponent> component = browseComponents(repository).stream()
        .filter(c -> componentName.equals(c.name()))
        .findFirst();

    assertThat(component.isPresent(), is(TRUE));
    return component.get().assets().stream().anyMatch(asset -> asset.path().endsWith(formatExtension));
  }

  @Override
  public boolean componentExists(final Repository repository, final String name) {
    List<FluentComponent> components = browseComponents(repository);
    return components.stream().anyMatch(comp -> comp.name().equals(name));
  }

  @Override
  public boolean checkComponentExist(final Repository repository, final Predicate<String> nameMatcher)
  {
    return browseComponents(repository).stream().anyMatch(c -> nameMatcher.test(c.name()));
  }

  @Override
  public boolean componentExists(final Repository repository, final String name, final String version) {
    if (endsWith(version, SNAPSHOT_VERSION_SUFFIX)) {
      return findSnapshotComponent(repository, name, version).isPresent();
    }
    else {
      List<FluentComponent> components = browseComponents(repository);
      return components.stream().anyMatch(comp -> comp.name().equals(name) && comp.version().equals(version));
    }
  }

  private Optional<FluentComponent> findSnapshotComponent(
      final Repository repository,
      final String name,
      final String version)
  {
    List<FluentComponent> components = browseComponents(repository);
    String gav = substring(version, 0, indexOf(version, SNAPSHOT_VERSION_SUFFIX));
    String versionWithDate = String.format("%s-%s", gav, now().format(YEAR_MONTH_DAY_FORMAT));
    return components.stream()
        .filter(comp -> comp.name().equals(name))
        .filter(comp -> startsWith(comp.version(), versionWithDate))
        .findAny();
  }

  private List<FluentComponent> browseComponents(final Repository repository) {
    List<FluentComponent> componentsFound = new ArrayList<>();
    ContentFacet facet = repository.facet(ContentFacet.class);
    Continuation<FluentComponent> components = facet.components().browse(10, null);
    while (!components.isEmpty()) {
      componentsFound.addAll(components);
      components = facet.components().browse(10, components.nextContinuationToken());
    }
    return componentsFound;
  }

  @Override
  public boolean componentExists(
      final Repository repository,
      final String namespace,
      final String name,
      final String version)
  {
    if (endsWith(version, SNAPSHOT_VERSION_SUFFIX)) {
      return findSnapshotComponent(repository, name, version).isPresent();
    }
    else {
      List<FluentComponent> components = browseComponents(repository);
      return components.stream().anyMatch(comp -> comp.namespace().equals(namespace) && comp.name().equals(name) && comp.version().equals(version));
    }
  }

  @Override
  public boolean componentExistsWithAssetPathMatching(
      final Repository repository,
      final Predicate<String> pathMatcher)
  {
    return streamOf(repository.facet(ContentFacet.class).assets()::browse)
        .filter(asset -> asset.component().isPresent())
        .map(FluentAsset::path)
        .map(this::adjustedPath)
        .anyMatch(pathMatcher);
  }

  @Override
  public boolean assetWithComponentExists(
      final Repository repository,
      final String path,
      final String namespace,
      final String name)
  {
    Collection<FluentAsset> assets = findComponent(repository, namespace, this.adjustedPath(name)).assets();

    return assets.stream()
        .map(FluentAsset::path)
        .anyMatch(adjustedPath(path)::equals);
  }

  @Override
  public boolean assetWithComponentExists(
      final Repository repository,
      final String path,
      final String namespace,
      final String name,
      final String version)
  {
    Collection<FluentAsset> assets = findComponent(repository, namespace, name, version).assets();

    return assets.stream()
        .map(FluentAsset::path)
        .anyMatch(adjustedPath(path)::equals);
  }

  @Override
  public boolean assetWithoutComponentExists(final Repository repository, final String path) {
    return !findAssetByPathNotNull(repository, path).component().isPresent();
  }

  @Override
  public NestedAttributesMap componentAttributes(
      final Repository repository,
      @Nullable final String namespace,
      final String name,
      final String version)
  {
    return findComponent(repository, namespace, name, version).attributes();
  }

  @Override
  public NestedAttributesMap componentAttributes(
      final Repository repository,
      final String namespace,
      final String name)
  {
    return findComponent(repository, namespace, adjustedPath(name)).attributes();
  }

  @Override
  public NestedAttributesMap snapshotComponentAttributes(
      final Repository repository,
      final String name,
      final String version)
  {
    return findSnapshotComponent(repository, name, version).map(Component::attributes)
        .orElseThrow(() -> new ComponentNotFoundException(repository, "", name, version));
  }

  @Override
  public String adjustedPath(final String path) {
    return "/" + stripStart(path, "/");
  }

  @Override
  public void setLastDownloadedTime(final Repository repository, final int minusSeconds) {
    int repositoryId = ((ContentFacetSupport) repository.facet(ContentFacet.class)).contentRepositoryId();

    Timestamp time = Timestamp.from(Instant.now().minusSeconds(minusSeconds));

    String sql = "UPDATE " + repository.getFormat().getValue() + "_asset "
        + "SET last_downloaded = ? WHERE repository_id = ?";

    update(sql, stmt -> {
      stmt.setTimestamp(1, time);
      stmt.setInt(2, repositoryId);
    });

    streamOf(repository.facet(ContentFacet.class).assets()::browse)
        .forEach(asset -> sendEvent(repository, asset));
  }

  @Override
  public void setLastDownloadedTime(final Repository repository, final int minusSeconds, final String regex) {
    int repositoryId = ((ContentFacetSupport) repository.facet(ContentFacet.class)).contentRepositoryId();

    Timestamp time = Timestamp.from(Instant.now().minusSeconds(minusSeconds));

    String sql = "UPDATE " + repository.getFormat().getValue() + "_asset "
        + "SET last_downloaded = ? WHERE repository_id = ? AND path ~ ?";

    update(sql, stmt -> {
      stmt.setTimestamp(1, time);
      stmt.setInt(2, repositoryId);
      stmt.setString(3, regex);
    });

    streamOf(repository.facet(ContentFacet.class).assets()::browse)
        .forEach(asset -> sendEvent(repository, asset));
  }

  @Override
  public void setLastDownloadedTime(final Repository repository, final String path, final Date date) {
    int repositoryId = repository.facet(ContentFacet.class).contentRepositoryId();

    String sql = "UPDATE " + repository.getFormat().getValue() + "_asset "
        + "SET last_downloaded = ? WHERE repository_id = ? AND path = ?";

    update(sql, stmt -> {
      setDate(date, (timestamp, calendar) -> stmt.setTimestamp(1, timestamp, calendar));
      stmt.setInt(2, repositoryId);
      stmt.setString(3, adjustedPath(path));
    });
  }

  @Override
  public void setComponentLastUpdatedTime(final Repository repository, final Date date) {
    setLastUpdatedTime(repository, date, "component");
  }

  @Override
  public void setAssetCreatedTime(final Repository repository, final String path, final Date date) {
    int repositoryId = repository.facet(ContentFacet.class).contentRepositoryId();
    String format = repository.getFormat().getValue();

    update("UPDATE " + format + "_asset SET created = ? WHERE repository_id = ?  AND path = ?", stmt -> {
      setDate(date, (timestamp, calendar) -> stmt.setTimestamp(1, timestamp, calendar));
      stmt.setInt(2, repositoryId);
      stmt.setString(3, adjustedPath(path));
    });
  }

  @Override
  public void setAssetLastUpdatedTime(final Repository repository, final Date date) {
    setLastUpdatedTime(repository, date, "asset");
  }

  private void setLastUpdatedTime(final Repository repository, final Date date, final String table) {
    int repositoryId = ((ContentFacetSupport) repository.facet(ContentFacet.class)).contentRepositoryId();

    String sql = "UPDATE " + repository.getFormat().getValue() + "_" + table +
        " SET last_updated = ? WHERE repository_id = ?";

    update(sql, stmt -> {
      setDate(date, (timestamp, calendar) -> stmt.setTimestamp(1, timestamp, calendar));
      stmt.setInt(2, repositoryId);
    });
  }

  @Override
  public void setAssetLastUpdatedTime(final Repository repository, final String path, final Date date) {
    setLastUpdatedTime(repository, path, date, "asset");
  }

  /**
   * @deprecated last_updated indicates when the DB record was last touched, calling this is almost certainly wrong.
   */
  @Deprecated
  private void setLastUpdatedTime(final Repository repository, final String path, final Date date, final String table) {
    int repositoryId = repository.facet(ContentFacet.class).contentRepositoryId();

    String sql = "UPDATE " + repository.getFormat().getValue() + "_" + table +
        " SET last_updated = ? WHERE repository_id = ? AND path = ?";

    update(sql, stmt -> {
      setDate(date, (timestamp, calendar) -> stmt.setTimestamp(1, timestamp, calendar));
      stmt.setInt(2, repositoryId);
      stmt.setString(3, path);
    });
  }

  @Override
  public void setBlobUpdatedTime(final Repository repository, final String pathRegex, final Date date) {
    int repositoryId = repository.facet(ContentFacet.class).contentRepositoryId();

    String sql = "UPDATE " + repository.getFormat().getValue() + "_asset_blob ab" +
            " SET blob_created = ?" +
            " WHERE EXISTS (SELECT * FROM " + repository.getFormat().getValue() + "_asset a" +
            " WHERE a.asset_blob_id = ab.asset_blob_id AND a.repository_id = ? AND a.path ~ ?)";

    update(sql, stmt -> {
      setDate(date, (timestamp, calendar) -> stmt.setTimestamp(1, timestamp, calendar));
      stmt.setInt(2, repositoryId);
      stmt.setString(3, pathRegex);
    });
  }

  @Override
  public void setAssetContentLastModified(final Repository repository, final String path, final Date date) {
    if (repository.getType() instanceof ProxyType) {
      Asset asset = findAssetByPathNotNull(repository, path);

      repository.facet(ContentFacet.class).assets().with(asset).attributes(OVERLAY, "content",
          Collections.singletonMap(Content.CONTENT_LAST_MODIFIED, new DateTime(date)));
    }
    else {
      // Note behaviour difference with Orient
      log.info("SQL does not support setting last_modified for non-proxy repositories: {} path:", repository.getName(),
          path);
    }
  }

  @Override
  public void setLastDownloadedTimeNull(final Repository repository) {
    int repositoryId = repository.facet(ContentFacet.class).contentRepositoryId();

    String sql = "UPDATE " + repository.getFormat().getValue() + "_asset "
        + "SET last_downloaded = ? WHERE repository_id = ?";

    update(sql, stmt -> {
      stmt.setNull(1, Types.TIMESTAMP);
      stmt.setInt(2, repositoryId);
    });
    streamOf(repository.facet(ContentFacet.class).assets()::browse)
        .forEach(asset -> sendEvent(repository, asset));
  }

  @Override
  public void setLastDownloadedTime(
      final Repository repository,
      final int minusSeconds,
      final Predicate<String> pathMatcher)
  {
    int repositoryId = ((ContentFacetSupport) repository.facet(ContentFacet.class)).contentRepositoryId();

    List<String> pathes = streamOf(repository.facet(ContentFacet.class).assets()::browse)
        .map(FluentAsset::path)
        .filter(pathMatcher)
        .collect(Collectors.toList());

    Timestamp time = Timestamp.from(LocalDateTime.now().minusSeconds(minusSeconds).toInstant(ZoneOffset.UTC));

    try (Connection connection = sessionSupplier.openConnection(DEFAULT_DATASTORE_NAME);
         PreparedStatement stmt = connection.prepareStatement("UPDATE " + repository.getFormat().getValue() + "_asset "
            + "SET last_downloaded = ? "
            + "WHERE repository_id = ? AND path = ?")) {

      for (String path : pathes) {
        stmt.setTimestamp(1, time);
        stmt.setInt(2, repositoryId);
        stmt.setString(3, path);

        stmt.execute();
        if(stmt.getWarnings() != null) {
          throw new RuntimeException(UPDATE_TIME_ERROR_MESSAGE + stmt.getWarnings());
        }
      }
    }
    catch (SQLException e) {
      throw new RuntimeException(e);
    }

    pathes.stream().map(path -> findAssetByPath(repository, path))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .forEach(asset -> sendEvent(repository, asset));
  }

  @Override
  public Optional<InputStream> read(final Repository repository, final String path) {
    return findAssetByPath(repository, path)
        .map(FluentAsset::download)
        .map(content -> {
          try {
            return content.openInputStream();
          }
          catch (IOException e) {
            throw new UncheckedIOException(e);
          }
        });
  }

  private void sendEvent(final Repository repository, final FluentAsset asset) {
    try {
      asset.component().isPresent(); // prime it
      AssetDownloadedEvent event = new AssetDownloadedEvent(asset);
      Method method = ContentStoreEvent.class.getDeclaredMethod("setRepositorySupplier", Supplier.class);
      method.setAccessible(true);
      method.invoke(event, (Supplier<Optional<Repository>>) () -> Optional.of(repository));
      eventManager.post(event);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void deleteAssetBlob(final Repository repository, final String assetPath) {
    BlobStore blobStore = blobStoreManager.get(DEFAULT_BLOBSTORE_NAME);
    findAssetByPath(repository, assetPath)
        .flatMap(Asset::blob)
        .map(AssetBlob::blobRef)
        .map(BlobRef::getBlobId)
        .ifPresent(blobId -> blobStore.delete(blobId, "test merge recovery"));
  }

  @Override
  public Optional<Blob> getBlob(final Repository repository, final String assetPath) {
    return findAssetByPath(repository, assetPath)
        .flatMap(Asset::blob)
        .flatMap(assetBlob -> repository.facet(ContentFacet.class).blobs().blob(assetBlob.blobRef()));
  }

  @Override
  public BlobRef getBlobRefOfAsset(final Repository repository, final String path) {
    Optional<FluentAsset> optionalAsset = findAssetByPath(repository, path);
    return optionalAsset.flatMap(FluentAsset::blob).map(AssetBlob::blobRef).orElse(null);
  }

  @Override
  public EntityId getComponentId(final Repository repository, final String assetPath) {
    return findAssetByPathNotNull(repository, assetPath)
        .component()
        .map(InternalIds::internalComponentId)
        .map(InternalIds::toExternalId)
        .orElse(null);
  }

  @Override
  public NestedAttributesMap getAttributes(final Repository repository) {
    NestedAttributesMap attr = repository.facet(ContentFacet.class).attributes();
    return new ImmutableNestedAttributesMap(attr.getParent(), attr.getKey(), attr.backing());
  }

  @Override
  public void modifyAttributes(final Repository repository, final String child1, final String child2, final int value) {
    repository.facet(ContentFacet.class)
        .attributes(OVERLAY, child1, ImmutableMap.of(child2, ImmutableMap.of(Integer.class, value)));
  }

  @Override
  public void deleteAllComponents(final Repository repository) {
    iterableOf(repository.facet(ContentFacet.class).components()::browse)
        .forEach(component -> repository.facet(ContentMaintenanceFacet.class).deleteComponent(component));
  }

  @Override
  public CacheInfo getCacheInfo(final Repository repository, final String path) {
    return findAssetByPath(repository, path)
        .map(FluentAsset::attributes)
        .map(attr -> attr.child(CacheInfo.CACHE))
        .map(map -> new CacheInfo(new DateTime(map.get(LAST_VERIFIED)), map.get(CACHE_TOKEN, String.class)))
        .orElse(null);
  }

  private void update(final String sql, final ThrowingConsumer<PreparedStatement> consumer) {
    try (Connection connection = sessionSupplier.openConnection(DEFAULT_DATASTORE_NAME);
        PreparedStatement stmt = connection.prepareStatement(sql)) {

     consumer.accept(stmt);

     stmt.execute();

     if(stmt.getWarnings() != null) {
       throw new RuntimeException(UPDATE_TIME_ERROR_MESSAGE + stmt.getWarnings());
     }
   }
   catch (SQLException e) {
     throw new RuntimeException(e);
   }
  }

  public void setDate(final Date date, final ThrowingBiConsumer<java.sql.Timestamp, Calendar> consumer) throws SQLException {
    Calendar cal = Calendar.getInstance();
    cal.setTime(date);
    consumer.accept(Timestamp.from(date.toInstant()), cal);
  }

  @FunctionalInterface
  public interface ThrowingConsumer<T>
  {
    void accept(T t) throws SQLException;
  }

  @FunctionalInterface
  public interface ThrowingBiConsumer<T, R>
  {
    void accept(T t, R r) throws SQLException;
  }
}
