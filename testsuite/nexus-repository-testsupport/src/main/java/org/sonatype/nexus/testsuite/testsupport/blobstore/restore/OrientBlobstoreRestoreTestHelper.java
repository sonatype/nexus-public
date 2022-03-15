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
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.Nullable;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.DatabaseInstanceNames;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetEntityAdapter;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskScheduler;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.sql.OCommandSQL;

import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.sonatype.nexus.blobstore.api.BlobStore.BLOB_NAME_HEADER;
import static org.sonatype.nexus.common.app.FeatureFlags.ORIENT_ENABLED;
import static org.sonatype.nexus.repository.storage.ComponentEntityAdapter.P_GROUP;
import static org.sonatype.nexus.repository.storage.ComponentEntityAdapter.P_VERSION;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;
import static org.sonatype.nexus.scheduling.TaskState.OK;

/**
 * Helper class containing common functionality needed in ITs testing the restoration of component metadata from blobs.
 * Assumes a unit of work has already been started.
 */
@FeatureFlag(name = ORIENT_ENABLED)
@Priority(Integer.MAX_VALUE)
@Singleton
@Named
public class OrientBlobstoreRestoreTestHelper
    implements BlobstoreRestoreTestHelper
{
  private static final String DELETE_ASSETS_SQL = "DELETE FROM asset";

  private static final String DELETE_COMPONENTS_SQL = "DELETE FROM component";

  @Inject
  @Named(DatabaseInstanceNames.COMPONENT)
  private Provider<DatabaseInstance> componentDb;

  @Inject
  private TaskScheduler taskScheduler;

  @Inject
  private RepositoryManager manager;

  @Inject
  private BlobStoreManager blobstoreManager;

  @Override
  public void simulateComponentAndAssetMetadataLoss() {
    executeSqlStatements(DELETE_ASSETS_SQL, DELETE_COMPONENTS_SQL);
  }

  @Override
  public void simulateAssetMetadataLoss() {
    executeSqlStatements(DELETE_ASSETS_SQL);
  }

  @Override
  public void simulateComponentMetadataLoss() {
    executeSqlStatements(DELETE_COMPONENTS_SQL);
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
    runRestoreMetadataTaskWithTimeout(10, isDryRun);
  }

  @Override
  public void assertComponentNotInRepository(final Repository repository, final String name) {
    Component component = findComponent(repository, name);
    assertThat(component, nullValue());
  }

  @Override
  public void assertComponentNotInRepository(final Repository repository, final String name, final String version) {
    Query query = getQuery(name, version);
    Component component = findComponent(repository, query);
    assertThat(component, nullValue());
  }

  @Override
  public void assertComponentInRepository(final Repository repository, final String name) {
    Component component = findComponent(repository, name);
    assertThat(component, notNullValue());
  }

  @Override
  public void assertComponentInRepository(final Repository repository, final String name, final String version) {
    Query query = getQuery(name, version);
    Component component = findComponent(repository, query);
    assertThat(component, notNullValue());
  }

  @Override
  public void assertComponentInRepository(
      final Repository repository,
      final String group,
      final String name,
      final String version)
  {
    Query query = getQuery(group, name, version);
    Component component = findComponent(repository, query);
    assertThat(component, notNullValue());
  }

  @Override
  public void assertAssetNotInRepository(final Repository repository, final String... names) {
    for (String name : names) {
      Asset asset = findAsset(repository, name);
      assertThat(asset, nullValue());
    }
  }

  @Override
  public void assertAssetInRepository(final Repository repository, final String name) {
    Asset asset = findAsset(repository, name);
    assertThat(asset, notNullValue());
  }


  public void assertComponentWithGAVInRepository(final Repository repository,
                                                 final String group,
                                                 final String name,
                                                 final String version)
  {
    Query query = getQuery(group, name, version);
    Component component = findComponent(repository, query);
    assertThat(component, notNullValue());
  }

  public void assertComponentWithGAVNotInRepository(final Repository repository,
                                                    final String group,
                                                    final String name,
                                                    final String version)
  {
    Query query = getQuery(group, name, version);
    Component component = findComponent(repository, query);
    assertThat(component, nullValue());
  }

  @Override
  public void assertAssetAssociatedWithComponent(final Repository repository,
                                                 @Nullable final String group,
                                                 final String name,
                                                 final String version,
                                                 final String... paths)
  {
    Query query = group != null ? getQuery(group, name, version) : getQuery(name, version);
    Component component = findComponent(repository, query);
    assertThat(component, notNullValue());

    for (String path : paths) {
      assertForComponentId(repository, component, path);
    }
  }

  @Override
  public void assertAssetAssociatedWithComponent(final Repository repository, final String name, final String path) {
    Component component = findComponent(repository, name);
    assertThat(component, notNullValue());
    assertForComponentId(repository, component, path);
  }

  private void assertForComponentId(final Repository repository, final Component component, final String path) {
    EntityId compId = component.getEntityMetadata().getId();
    assertThat(compId, notNullValue());

    try (StorageTx tx = getStorageTx(repository)) {
      tx.begin();

      Asset asset = tx.findAssetWithProperty(AssetEntityAdapter.P_NAME, path, tx.findBucket(repository));
      assertThat(asset, notNullValue());

      final EntityId assertEntityId = asset.componentId();
      assertThat(assertEntityId, notNullValue());
      assertEquals(assertEntityId, compId);
    }
  }

  @Override
  public void assertAssetMatchesBlob(final Repository repository, final String... names) {
    for (String name : names) {
      try (StorageTx tx = getStorageTx(repository)) {
        tx.begin();
        Asset asset = tx.findAssetWithProperty(AssetEntityAdapter.P_NAME, name, tx.findBucket(repository));
        Blob blob = tx.requireBlob(asset.blobRef());

        assertThat(repository.getName(), equalTo(blob.getHeaders().get(BlobStore.REPO_NAME_HEADER)));
        assertThat(asset.name(), either(equalTo(blob.getHeaders().get(BlobStore.BLOB_NAME_HEADER)))
            .or(equalTo(blob.getHeaders().get(BlobStore.BLOB_NAME_HEADER).substring(1))));
        assertThat(asset.createdBy(), equalTo(blob.getHeaders().get(BlobStore.CREATED_BY_HEADER)));
        assertThat(asset.createdByIp(), equalTo(blob.getHeaders().get(BlobStore.CREATED_BY_IP_HEADER)));
        assertThat(asset.contentType(), equalTo(blob.getHeaders().get(BlobStore.CONTENT_TYPE_HEADER)));
        assertThat(asset.attributes().child("checksum").get("sha1"), equalTo(blob.getMetrics().getSha1Hash()));
        assertThat(asset.size(), equalTo(blob.getMetrics().getContentSize()));
      }
    }
  }

  @Override
  public void rewriteBlobNames() {
    manager.browse().forEach(repo -> {
      try (StorageTx tx = repo.facet(StorageFacet.class).txSupplier().get()) {
        tx.begin();
        StreamSupport.stream(tx.browseAssets(tx.findBucket(repo)).spliterator(), false)
            .map(Asset::blobRef)
            .map(blobRef -> blobstoreManager.get(blobRef.getStore()).getBlobAttributes(blobRef.getBlobId()))
            .forEach(blobAttr -> {
              Map<String, String> headers = blobAttr.getHeaders();
              String name = headers.get(BLOB_NAME_HEADER);
              if (name != null && !name.startsWith("/")) {
                headers.put(BLOB_NAME_HEADER, '/' + name);
                try {
                  blobAttr.store();
                }
                catch (IOException e) {
                  throw new UncheckedIOException("Failed to rewrite blob: " + name, e);
                }
              }
              else {
                fail("Found missing name or unexpected name: " + name);
              }
            });
      }
    });
  }

  private void executeSqlStatements(final String... sqlStatements) {
    try (ODatabaseDocumentTx db = componentDb.get().connect()) {
      for (String sqlStatement : sqlStatements) {
        db.command(new OCommandSQL(sqlStatement)).execute();
      }
    }
  }

  private static Component findComponent(final Repository repository, final String name) {
    try (StorageTx tx = getStorageTx(repository)) {
      tx.begin();
      return tx.findComponentWithProperty(P_NAME, name, tx.findBucket(repository));
    }
  }

  private static Component findComponent(final Repository repository, final Query query)
  {
    try (StorageTx tx = getStorageTx(repository)) {
      tx.begin();
      Iterator<Component> components = tx.findComponents(query, singletonList(repository)).iterator();
      return components.hasNext() ? components.next() : null;
    }
  }

  private static Asset findAsset(final Repository repository, final String name) {
    try (StorageTx tx = getStorageTx(repository)) {
      tx.begin();
      return tx.findAssetWithProperty(P_NAME, name, tx.findBucket(repository));
    }
  }

  private static StorageTx getStorageTx(final Repository repository) {
    return repository.facet(StorageFacet.class).txSupplier().get();
  }

  private static Query getQuery(final String name, final String version) {
    return Query.builder().where(P_NAME).eq(name).and(P_VERSION).eq(version).build();
  }

  private static Query getQuery(final String group, final String name, final String version) {
    return Query.builder().where(P_GROUP).eq(group).and(P_NAME).eq(name).and(P_VERSION).eq(version).build();
  }

  @Override
  public Map<String, BlobId> getAssetToBlobIds(final Repository repo, final Predicate<String> pathFilter) {
    try (StorageTx tx = repo.facet(StorageFacet.class).txSupplier().get()) {
      tx.begin();
      return StreamSupport.stream(tx.browseAssets(tx.findBucket(repo)).spliterator(), false)
          .filter(asset -> Objects.nonNull(asset.blobRef()))
          .filter(asset -> pathFilter.test(asset.name()))
          .collect(Collectors.toMap(Asset::name, OrientBlobstoreRestoreTestHelper::toBlobId));
    }
  }

  private static BlobId toBlobId(final Asset asset) {
    return Optional.ofNullable(asset.blobRef())
        .map(BlobRef::getBlobId)
        .orElse(null);
  }
}
