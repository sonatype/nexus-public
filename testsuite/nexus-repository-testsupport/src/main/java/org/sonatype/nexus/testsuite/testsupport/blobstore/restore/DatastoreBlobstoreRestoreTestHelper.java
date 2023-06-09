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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
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
import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
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
import org.sonatype.nexus.repository.content.store.AssetStore;
import org.sonatype.nexus.repository.content.store.ComponentStore;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskScheduler;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matcher;

import static java.util.Arrays.stream;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.StringUtils.prependIfMissing;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.sonatype.nexus.blobstore.api.BlobStore.BLOB_NAME_HEADER;
import static org.sonatype.nexus.blobstore.api.BlobStore.REPO_NAME_HEADER;
import static org.sonatype.nexus.common.app.FeatureFlags.DATASTORE_ENABLED;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.DATA_STORE_NAME;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.STORAGE;
import static org.sonatype.nexus.scheduling.TaskState.OK;

@FeatureFlag(name = DATASTORE_ENABLED)
@Singleton
@Named
public class DatastoreBlobstoreRestoreTestHelper
    implements BlobstoreRestoreTestHelper
{
  @Inject
  private TaskScheduler taskScheduler;

  @Inject
  private Map<String, FormatStoreManager> storeManagers;

  @Inject
  private RepositoryManager manager;

  @Inject
  private BlobStoreManager blobstoreManager;

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

  private static BlobId toBlobId(final Asset asset) {
    return asset.blob()
        .map(AssetBlob::blobRef)
        .map(BlobRef::getBlobId)
        .orElse(null);
  }

  @Override
  public void simulateAssetMetadataLoss() {
    manager.browse().forEach(repo -> {
      repo.facet(ContentFacet.class).assets().browse(Integer.MAX_VALUE, null)
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
  public void runRestoreMetadataTaskWithTimeout(final long timeout, final boolean dryRun) {
    TaskConfiguration config = taskScheduler.createTaskConfigurationInstance(TYPE_ID);
    config.setEnabled(true);
    config.setName("restore");
    config.setString(BLOB_STORE_NAME_FIELD_ID, "default");
    config.setBoolean(DRY_RUN, dryRun);
    config.setBoolean(RESTORE_BLOBS, true);
    config.setBoolean(UNDELETE_BLOBS, false);
    config.setBoolean(INTEGRITY_CHECK, false);
    TaskInfo taskInfo = taskScheduler.submit(config);
    await().atMost(timeout, SECONDS).until(() ->
        taskInfo.getLastRunState() != null && taskInfo.getLastRunState().getEndState().equals(OK));
  }

  @Override
  public void runRestoreMetadataTask() {
    runRestoreMetadataTask(false);
  }

  @Override
  public void runRestoreMetadataTask(final boolean isDryRun) {
    runRestoreMetadataTaskWithTimeout(60, isDryRun);
  }

  @Override
  public void assertAssetMatchesBlob(final Repository repo, final String... paths) {
    stream(paths)
        .map(path -> assets(repo).path(prependIfMissing(path, "/")).find()
            .orElseThrow(() -> new AssertionError("Missing asset: " + path)))
        .forEach(a -> assetMatch(a, getBlobStore(repo)));
  }

  @Override
  public void assertAssetInRepository(final Repository repository, final String path) {
    Optional<FluentAsset> asset = findAsset(repository, path);
    assertThat(asset.isPresent(), is(true));
  }

  @Override
  public void assertAssetNotInRepository(final Repository repository, final String... paths) {
    for (String path : paths) {
      Optional<FluentAsset> asset = findAsset(repository, path);
      assertThat(asset.isPresent(), is(false));
    }
  }

  @Override
  public void assertComponentInRepository(final Repository repository, final String name) {
    Optional<FluentComponent> component = findComponent(repository, name, "");
    assertThat(component.isPresent(), is(true));
  }

  @Override
  public void assertComponentInRepository(final Repository repository, final String name, final String version) {
    Optional<FluentComponent> component = findComponent(repository, name, version);
    assertThat(component.isPresent(), is(true));
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
    assertThat(component.isPresent(), is(true));
  }

  @Override
  public void assertComponentNotInRepository(final Repository repository, final String name) {
    Optional<FluentComponent> component = findComponent(repository, name);
    assertThat(component.isPresent(), is(false));
  }

  @Override
  public void assertComponentNotInRepository(final Repository repository, final String name, final String version) {
    Optional<FluentComponent> component = findComponent(repository, name, version);
    assertThat(component.isPresent(), is(false));
  }

  @Override
  public void assertAssetAssociatedWithComponent(final Repository repository, final String name, final String path) {
    Optional<FluentComponent> component = findComponent(repository, name);
    assertThat(component.isPresent(), is(true));
    Optional<FluentAsset> asset = component.get().asset(path).find();
    assertThat(asset.isPresent(), is(true));
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
    assertThat(component.isPresent(), is(true));

    for (String path : paths) {
      Optional<FluentAsset> asset = component.get().asset(prependIfMissing(path, "/")).find();
      assertThat(asset.isPresent(), is(true));
    }
  }

  @Override
  public void rewriteBlobNames() {
    manager.browse().forEach(repo -> {
      if (GroupType.NAME.equals(repo.getType().getValue())) {
        return;
      }
      ContentFacet content = repo.facet(ContentFacet.class);
      content.assets().browse(Integer.MAX_VALUE, null).stream()
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
              fail("Found missing name or unexpected name: " + name + " in repository: " + headers.get(REPO_NAME_HEADER));
            }
          });
    });
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
