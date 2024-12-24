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
package org.sonatype.nexus.testsuite.testsupport.blobstore.restore;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.file.FileBlobStore;
import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.FluentComponentBuilder;
import org.sonatype.nexus.repository.content.fluent.FluentComponents;
import org.sonatype.nexus.repository.content.store.AssetBlobStore;
import org.sonatype.nexus.repository.content.store.AssetStore;
import org.sonatype.nexus.repository.content.store.ComponentStore;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.types.GroupType;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matcher;

import static java.util.Arrays.stream;
import static org.apache.commons.lang3.StringUtils.prependIfMissing;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.sonatype.nexus.blobstore.api.BlobRef.DATE_TIME_PATH_FORMATTER;
import static org.sonatype.nexus.blobstore.api.BlobStore.BLOB_NAME_HEADER;
import static org.sonatype.nexus.blobstore.api.BlobStore.REPO_NAME_HEADER;
import static org.sonatype.nexus.common.app.FeatureFlags.DATASTORE_ENABLED;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.DATA_STORE_NAME;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.STORAGE;

@FeatureFlag(name = DATASTORE_ENABLED)
@Singleton
@Named
public class DatastoreBlobstoreRestoreTestHelper
    extends BlobstoreRestoreTestHelperSupport
    implements BlobstoreRestoreTestHelper
{
  @Inject
  private Map<String, FormatStoreManager> storeManagers;

  @Inject
  private RepositoryManager manager;

  @Inject
  private BlobStoreManager blobstoreManager;

  @Inject
  private DataSessionSupplier sessionSupplier;

  @Override
  public void simulateComponentAndAssetMetadataLoss() {
    simulateAssetMetadataLoss();
    simulateComponentMetadataLoss();
  }

  private String getContentStore(final Repository repository) {
    NestedAttributesMap storageAttributes = repository.getConfiguration().attributes(STORAGE);
    return storageAttributes.get(DATA_STORE_NAME, String.class, DEFAULT_DATASTORE_NAME);
  }

  private AssetStore<?> getAssetStore(final Repository repo) {
    return storeManagers.get(repo.getFormat().getValue()).assetStore(getContentStore(repo));
  }

  private ComponentStore<?> getComponentStore(final Repository repo) {
    return storeManagers.get(repo.getFormat().getValue()).componentStore(getContentStore(repo));
  }

  @Override
  public Map<String, BlobId> getAssetToBlobIds(final Repository repo, final Predicate<String> pathFilter) {
    return getAssetStore(repo).browseAssets(getContentRepositoryId(repo), null, null, null, null, Integer.MAX_VALUE)
        .stream()
        .filter(asset -> pathFilter.test(asset.path()))
        .filter(asset -> asset.blob().isPresent())
        .collect(Collectors.toMap(Asset::path, DatastoreBlobstoreRestoreTestHelper::toBlobId));
  }

  @Override
  public void deleteAssetBlob(final Repository repository, final BlobStore blobStore, final BlobId blobId) {
    AssetBlobStore<?> assetBlobStore = getAssetBlobStore(repository);
    assetBlobStore.deleteAssetBlobWithAsset(createBlobRef(blobStore, repository, blobId));
  }

  @Override
  public boolean assetBlobExists(
      final Repository repository,
      final BlobStore blobStore,
      final BlobId blobId)
  {
    AssetBlobStore<?> assetBlobStore = getAssetBlobStore(repository);
    return assetBlobStore.readAssetBlob(createBlobRef(blobStore, repository, blobId)).isPresent();
  }

  private BlobRef createBlobRef(final BlobStore blobStore, final Repository repository, final BlobId blobId) {
    ContentFacetSupport contentFacetSupport = (ContentFacetSupport) repository.facet(ContentFacet.class);
    String blobStoreName = blobStore.getBlobStoreConfiguration().getName();
    return new BlobRef(
        contentFacetSupport.nodeName(),
        blobStoreName,
        blobId.asUniqueString(),
        blobId.getBlobCreatedRef());
  }

  private AssetBlobStore<?> getAssetBlobStore(final Repository repository) {
    return storeManagers.get(repository.getFormat().getValue()).assetBlobStore(getContentStore(repository));
  }

  private static BlobId toBlobId(final Asset asset) {
    return asset.blob()
        .map(AssetBlob::blobRef)
        .map(BlobRef::getBlobId)
        .orElse(null);
  }

  @Override
  public void simulateAssetMetadataLoss() {
    manager.browse().forEach(repo -> {
      repo.facet(ContentFacet.class)
          .assets()
          .browse(Integer.MAX_VALUE, null)
          .forEach(asset -> asset.delete());
    });
  }

  @Override
  public void simulateComponentMetadataLoss() {
    simulateAssetMetadataLoss();

    manager.browse().forEach(repo -> {
      int repoId = getContentRepositoryId(repo);
      getComponentStore(repo).deleteComponents(repoId);
    });
  }

  private int getContentRepositoryId(final Repository repo) {
    return repo.facet(ContentFacet.class).contentRepositoryId();
  }

  @Override
  public void assertAssetMatchesBlob(final Repository repo, final String... paths) {
    stream(paths)
        .map(path -> assets(repo).path(prependIfMissing(path, "/"))
            .find()
            .orElseThrow(() -> new AssertionError("Missing asset: " + path)))
        .forEach(a -> assetMatch(a, getBlobStore(repo)));
  }

  @Override
  public void assertAssetInRepository(final Repository repository, final String path) {
    Optional<FluentAsset> asset = findAsset(repository, path);
    assertThat("Missing asset: " + path, asset.isPresent(), is(true));
  }

  @Override
  public void assertAssetNotInRepository(final Repository repository, final String... paths) {
    for (String path : paths) {
      Optional<FluentAsset> asset = findAsset(repository, path);
      assertThat("Expected no asset for path: " + path, asset.isPresent(), is(false));
    }
  }

  @Override
  public void assertComponentInRepository(final Repository repository, final String name) {
    Optional<FluentComponent> component = findComponent(repository, name, "");
    assertThat("Missing component: " + name, component.isPresent(), is(true));
  }

  @Override
  public void assertComponentInRepository(final Repository repository, final String name, final String version) {
    Optional<FluentComponent> component = findComponent(repository, name, version);
    assertThat("Missing component " + name + ":" + version, component.isPresent(), is(true));
  }

  @Override
  public void assertComponentInRepository(
      final Repository repository,
      final String namespace,
      final String name,
      final String version)
  {
    Optional<FluentComponent> component = components(repository)
        .name(name)
        .version(version)
        .namespace(namespace)
        .find();
    assertThat("Missing component " + namespace + ":" + name + ":" + version, component.isPresent(), is(true));
  }

  @Override
  public void assertComponentNotInRepository(final Repository repository, final String name) {
    Optional<FluentComponent> component = findComponent(repository, name);
    assertThat("Expected no component for name: " + name, component.isPresent(), is(false));
  }

  @Override
  public void assertComponentNotInRepository(final Repository repository, final String name, final String version) {
    Optional<FluentComponent> component = findComponent(repository, name, version);
    assertThat("Expected no component for name, version: " + name + ":" + version, component.isPresent(), is(false));
  }

  @Override
  public void assertAssetAssociatedWithComponent(final Repository repository, final String name, final String path) {
    Optional<FluentComponent> component = findComponent(repository, name);
    assertThat("Missing component: " + name, component.isPresent(), is(true));
    Optional<FluentAsset> asset = component.get().asset(path).find();
    assertThat("Missing asset: " + path, asset.isPresent(), is(true));
  }

  @Override
  public void assertAssetAssociatedWithComponent(
      final Repository repository,
      @Nullable final String namespace,
      final String name,
      final String version,
      final String... paths)
  {
    FluentComponentBuilder componentBuilder = repository.facet(ContentFacet.class)
        .components()
        .name(name)
        .version(version);
    if (namespace != null) {
      componentBuilder = componentBuilder.namespace(namespace);
    }
    Optional<FluentComponent> component = componentBuilder.find();
    assertThat("Missing component " + namespace + ":" + name + ":" + version, component.isPresent(), is(true));

    for (String path : paths) {
      Optional<FluentAsset> asset = component.get().asset(prependIfMissing(path, "/")).find();
      assertThat("Missing asset: " + path, asset.isPresent(), is(true));
    }
  }

  @Override
  public void rewriteBlobNames() {
    manager.browse().forEach(repo -> {
      if (GroupType.NAME.equals(repo.getType().getValue())) {
        return;
      }
      ContentFacet content = repo.facet(ContentFacet.class);
      content.assets()
          .browse(Integer.MAX_VALUE, null)
          .stream()
          .map(Asset::blob)
          .filter(Optional::isPresent)
          .map(Optional::get)
          .map(AssetBlob::blobRef)
          .map(blobRef -> blobstoreManager.get(blobRef.getStore()).getBlobAttributes(blobRef.getBlobId()))
          .forEach(blobAttr -> {
            Map<String, String> headers = blobAttr.getHeaders();
            String name = headers.get(BLOB_NAME_HEADER);
            if (name != null && name.startsWith("/")) {
              headers.put(BLOB_NAME_HEADER, name.substring(1));
              try {
                blobAttr.store();
              }
              catch (IOException e) {
                throw new UncheckedIOException("Failed to rewrite blob: " + name, e);
              }
            }
            else {
              fail("Found missing name or unexpected name: " + name + " in repository: "
                  + headers.get(REPO_NAME_HEADER));
            }
          });
    });
  }

  @Override
  public boolean assertReconcilePlanExists(String type, String action) {
    List<BlobId> blobIds = getAssetBlobId();
    for (BlobId blobId : blobIds) {
      if (!assertReconcilePlanExistWithParameterForProvidedBlobIds(blobId.asUniqueString(), type, action)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void simulateFileLoss(String blobStorageName, String extension) {
    try {
      BlobStore blobstore = blobstoreManager.get(blobStorageName);
      String absoluteBlobDir = ((FileBlobStore) blobstore).getAbsoluteBlobDir().toString();
      List<BlobId> blobIds = getAssetBlobId();
      blobIds.forEach(blobId -> {
        String filePath = getFileAbsolutePathForBlobIdRef(blobId, extension, absoluteBlobDir);
        deleteFile(filePath);
      });
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean assertReconcilePlanExists(String type, String action, List<BlobId> blobIds) {
    for (BlobId blobId : blobIds) {
      if (!assertReconcilePlanExistWithParameterForProvidedBlobIds(blobId.asUniqueString(), type, action)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean assertPropertyFilesExist(String blobStorageName) {
    try {
      BlobStore blobstore = blobstoreManager.get(blobStorageName);
      String absoluteBlobDir = String.valueOf(((FileBlobStore) blobstore).getAbsoluteBlobDir());
      List<BlobId> blobIds = getAssetBlobId();
      for (BlobId blobId : blobIds) {
        String filePath = getFileAbsolutePathForBlobIdRef(blobId, ".properties", absoluteBlobDir);
        if (!isFileExist(filePath)) {
          return false;
        }
      }
      return true;
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public List<BlobId> getAssetBlobId() {
    List<BlobId> blobIds = new ArrayList<>();
    manager.browse().forEach(repo -> {
      repo.facet(ContentFacet.class)
          .assets()
          .browse(Integer.MAX_VALUE, null)
          .forEach(asset -> {
            BlobId blobId = toBlobId(asset);
            blobIds.add(blobId);
          });
    });
    return blobIds;
  }

  @Override
  public void truncateTables(final String format) {
    try (Connection connection = sessionSupplier.openConnection(DEFAULT_DATASTORE_NAME);
        PreparedStatement deletePlanDetails = connection.prepareStatement(
            "DELETE FROM reconcile_plan_details WHERE id IN (SELECT rpd.id FROM reconcile_plan_details rpd)");
        PreparedStatement deletePlans = connection.prepareStatement(
            "DELETE FROM reconcile_plan WHERE id IN (SELECT rp.id FROM reconcile_plan rp)");
        PreparedStatement deleteFormatAssets = connection.prepareStatement(deleteFormatAssetsSQL(format));
        PreparedStatement deleteFormatAssetBlobs = connection.prepareStatement(deleteFormatAssetBlobsSQL(format));
        PreparedStatement deleteFormatComponents = connection.prepareStatement(deleteFormatComponentsSQL(format))) {

      connection.setAutoCommit(false);

      deletePlanDetails.executeUpdate();
      deletePlans.executeUpdate();
      deleteFormatAssets.executeUpdate();
      deleteFormatAssetBlobs.executeUpdate();
      deleteFormatComponents.executeUpdate();

      connection.commit();
    }
    catch (SQLException e) {
      throw new RuntimeException("Error managing the connection: " + e.getMessage(), e);
    }
  }

  private String deleteFormatComponentsSQL(final String format) {
    return String.format("DELETE FROM %s_component WHERE component_id IN (SELECT rc.component_id FROM %s_component rc)",
        format, format);
  }

  private String deleteFormatAssetsSQL(final String format) {
    return String.format("DELETE FROM %s_asset WHERE asset_id IN (SELECT ra.asset_id FROM %s_asset ra)",
        format, format);
  }

  private String deleteFormatAssetBlobsSQL(final String format) {
    return String.format(
        "DELETE FROM %s_asset_blob WHERE asset_blob_id IN (SELECT rab.asset_blob_id FROM %s_asset_blob rab)",
        format, format);
  }

  private boolean isFileExist(String filePath) {
    File file = new File(filePath);
    return file.exists() && file.isFile();
  }

  private void deleteFile(String filePath) {
    File file = new File(filePath);
    if (file.exists() && file.isFile()) {
      if (!file.delete()) {
        throw new RuntimeException("Failed to delete file: " + filePath);
      }
    }
  }

  private String getFileAbsolutePathForBlobIdRef(BlobId blobId, String extension, String absolutePath) {
    OffsetDateTime blobCreationTime = blobId.getBlobCreatedRef();
    String datePath = blobCreationTime.format(DATE_TIME_PATH_FORMATTER);
    String pathInContentDirectory =
        datePath + "/" + Pattern.compile("[.\\\\:/]").matcher(blobId.asUniqueString()).replaceAll("-");
    return absolutePath + "/content/" + pathInContentDirectory + extension;
  }

  private boolean assertReconcilePlanExistWithParameterForProvidedBlobIds(String blobId, String type, String action) {
    try {
      PreparedStatement ps = null;
      try (Connection connection = sessionSupplier.openConnection(DEFAULT_DATASTORE_NAME)) {
        String sql = "SELECT rpd.* FROM reconcile_plan_details rpd \n" +
            "join reconcile_plan rp on rp.id = rpd.plan_id \n" +
            "WHERE rpd.blob_id = ? \n" +
            "order by rp.started desc limit 1;";

        ps = connection.prepareStatement(sql);
        ps.setString(1, blobId);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
          assertThat(rs.getString("type"), equalTo(type));
          assertThat(rs.getString("action"), equalTo(action));
          assertThat(rs.getString("state"), equalTo("EXECUTED"));
          return true;
        }
        else {
          return false;
        }
      }
      catch (SQLException e) {
        throw new RuntimeException(e);
      }
      finally {
        ps.close();
      }
    }
    catch (SQLException ex) {
      throw new RuntimeException("Script generation failed", ex);
    }
  }

  private FluentAssets assets(final Repository repo) {
    return repo.facet(ContentFacet.class).assets();
  }

  private Optional<FluentAsset> findAsset(final Repository repo, final String path) {
    return assets(repo).path(StringUtils.prependIfMissing(path, "/")).find();
  }

  private FluentComponents components(final Repository repo) {
    return repo.facet(ContentFacet.class).components();
  }

  private Optional<FluentComponent> findComponent(final Repository repo, final String name) {
    return components(repo).name(name).find();
  }

  private Optional<FluentComponent> findComponent(final Repository repo, final String name, final String version) {
    return components(repo).name(name).version(version).find();
  }

  private static BlobStore getBlobStore(final Repository repository) {
    return ((ContentFacetSupport) repository.facet(ContentFacet.class)).stores().blobStoreProvider.get();
  }

  private static void assetMatch(final FluentAsset asset, final BlobStore blobStore) {
    assertTrue(asset.blob().isPresent());
    AssetBlob assetBlob = asset.blob().orElseThrow(AssertionError::new);

    Blob blob = blobStore.get(assetBlob.blobRef().getBlobId());

    assertThat(blob, notNullValue());

    String blobNameHeader = blob.getHeaders().get(BlobStore.BLOB_NAME_HEADER);
    assertThat(asset.path(), equalIgnoringMissingSlash(blobNameHeader));
    assertThat(assetBlob.createdBy().orElse("MISSING_ASSET_BLOB"),
        equalTo(blob.getHeaders().get(BlobStore.CREATED_BY_HEADER)));
    assertThat(assetBlob.createdByIp().orElse("MISSING_CREATED_BY"),
        equalTo(blob.getHeaders().get(BlobStore.CREATED_BY_IP_HEADER)));
    assertThat(assetBlob.contentType(), equalTo(blob.getHeaders().get(BlobStore.CONTENT_TYPE_HEADER)));
    assertThat(assetBlob.checksums().get(SHA1.name()), equalTo(blob.getMetrics().getSha1Hash()));
    assertThat(assetBlob.blobSize(), equalTo(blob.getMetrics().getContentSize()));
  }

  private static Matcher<String> equalIgnoringMissingSlash(final String blobNameHeader) {
    if (blobNameHeader.startsWith("/")) {
      return equalTo(blobNameHeader);
    }
    return equalTo("/" + blobNameHeader);
  }
}
