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

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;
import org.sonatype.nexus.repository.content.store.AssetStore;
import org.sonatype.nexus.repository.content.store.ComponentStore;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskScheduler;

import static java.util.Arrays.stream;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.StringUtils.prependIfMissing;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.datastore.api.DataStoreManager.CONTENT_DATASTORE_NAME;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.DATA_STORE_NAME;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.STORAGE;
import static org.sonatype.nexus.scheduling.TaskState.OK;

@FeatureFlag(name = "nexus.datastore.enabled")
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

  @Override
  public void simulateComponentAndAssetMetadataLoss() {
    simulateAssetMetadataLoss();
    simulateComponentMetadataLoss();
  }

  private String getContentStore(final Repository repository) {
    NestedAttributesMap storageAttributes = repository.getConfiguration().attributes(STORAGE);
    return storageAttributes.get(DATA_STORE_NAME, String.class, CONTENT_DATASTORE_NAME);
  }

  private AssetStore<?> getAssetStore(final Repository repo) {
    return storeManagers.get(repo.getFormat().getValue()).assetStore(getContentStore(repo));
  }

  private ComponentStore<?> getComponentStore(final Repository repo) {
    return storeManagers.get(repo.getFormat().getValue()).componentStore(getContentStore(repo));
  }

  @Override
  public void simulateAssetMetadataLoss() {
    manager.browse().forEach(repo -> getAssetStore(repo).deleteAssets(getContentRepositoryId(repo)));
  }

  @Override
  public void simulateComponentMetadataLoss() {
    manager.browse().forEach(repo -> getComponentStore(repo).deleteComponents(getContentRepositoryId(repo)));
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
    runRestoreMetadataTaskWithTimeout(60, false);
  }

  private FluentAssets assets(final Repository repo) {
    return repo.facet(ContentFacet.class).assets();
  }

  @Override
  public void assertAssetMatchesBlob(final Repository repo, final String... paths) {
    stream(paths)
        .map(path -> assets(repo).path(prependIfMissing(path, "/")).find())
        .forEach(a -> assetMatch(a, getBlobStore(repo)));
  }

  @Override
  public void assertAssetMatchesBlob(final Repository repo, final String path) {
    assetMatch(assets(repo).path(prependIfMissing(path, "/")).find(), getBlobStore(repo));
  }

  private static BlobStore getBlobStore(final Repository repository) {
    return ((ContentFacetSupport) repository.facet(ContentFacet.class)).stores().blobStore;
  }

  private static void assetMatch(final Optional<FluentAsset> asset, final BlobStore blobStore) {
    assertTrue(asset.isPresent() && asset.get().blob().isPresent());
    AssetBlob assetBlob = asset.orElseThrow(AssertionError::new).blob().orElseThrow(AssertionError::new);

    Blob blob = blobStore.get(assetBlob.blobRef().getBlobId());

    assertThat(blob, notNullValue());

    assertThat(asset.map(FluentAsset::path).orElse("MISSING_FLUENT_ASSET"), equalTo(blob.getHeaders().get(BlobStore.BLOB_NAME_HEADER)));
    assertThat(assetBlob.createdBy().orElse("MISSING_ASSET_BLOB"), equalTo(blob.getHeaders().get(BlobStore.CREATED_BY_HEADER)));
    assertThat(assetBlob.createdByIp().orElse("MISSING_CREATED_BY"), equalTo(blob.getHeaders().get(BlobStore.CREATED_BY_IP_HEADER)));
    assertThat(assetBlob.contentType(), equalTo(blob.getHeaders().get(BlobStore.CONTENT_TYPE_HEADER)));
    assertThat(assetBlob.checksums().get(SHA1.name()), equalTo(blob.getMetrics().getSha1Hash()));
    assertThat(assetBlob.blobSize(), equalTo(blob.getMetrics().getContentSize()));
  }
}
