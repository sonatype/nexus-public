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
package org.sonatype.nexus.repository.content.store;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.repository.capability.GlobalRepositorySettings;
import org.sonatype.nexus.repository.content.AssetInfo;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.facet.ContentFacetFinder;
import org.sonatype.nexus.repository.content.store.example.TestAssetBlobDAO;
import org.sonatype.nexus.repository.content.store.example.TestAssetDAO;
import org.sonatype.nexus.repository.content.store.example.TestComponentDAO;
import org.sonatype.nexus.repository.content.store.example.TestContentRepositoryDAO;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.collect.ImmutableList;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertTrue;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;
import static org.sonatype.nexus.repository.content.store.VersionedAssetDAOTest.browseAssets;
import static org.sonatype.nexus.repository.content.store.VersionedAssetDAOTest.countAssets;

@ExtendWith(MockitoExtension.class)
public abstract class AssetStoreTestSupport
    extends ExampleContentTestSupport
{
  @Mock
  protected ContentFacetFinder contentFacetFinder;

  @Mock
  protected EventManager eventManager;

  protected int repositoryId;

  protected AssetStore<TestAssetDAO> underTest;

  protected AssetBlobStore<TestAssetBlobDAO> assetBlobStore;

  protected ComponentStore<TestComponentDAO> componentStore;

  protected GlobalRepositorySettings globalRepositorySettings = new GlobalRepositorySettings();

  protected boolean entityVersioningEnabled;

  protected void initialiseStores(final boolean entityVersioningEnabled) {
    this.entityVersioningEnabled = entityVersioningEnabled;
    ContentRepositoryData contentRepository = generateContentRepository();
    createContentRepository(contentRepository);
    repositoryId = contentRepository.repositoryId;

    generateNamespaces(4);
    generateNames(4);
    generateVersions(4);

    underTest = new AssetStore<>(sessionRule, entityVersioningEnabled, "test", TestAssetDAO.class);
    underTest.setGlobalRepositorySettings(globalRepositorySettings);
    componentStore = new ComponentStore<>(sessionRule, entityVersioningEnabled, "test", TestComponentDAO.class);
    underTest.setDependencies(contentFacetFinder, eventManager);
    componentStore.setDependencies(contentFacetFinder, eventManager);
    assetBlobStore = new AssetBlobStore<>(sessionRule, "test", TestAssetBlobDAO.class);
  }

  private void createContentRepository(final ContentRepositoryData contentRepository) {
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      ContentRepositoryDAO dao = session.access(TestContentRepositoryDAO.class);
      dao.createContentRepository(contentRepository);
      session.getTransaction().commit();
    }
  }

  protected void testDeleteAsset() {
    AssetData asset1 = generateAsset(repositoryId, "/path/1");
    AssetData asset2 = generateAsset(repositoryId, "/path/2");
    AssetData asset3 = generateAsset(repositoryId, "/path/3");
    AssetData asset4 = generateAsset(repositoryId, "/path/4");
    AssetData asset5 = generateAsset(repositoryId, "/path/5");

    ComponentData component1 = generateComponent(repositoryId, "namespace1", "name1", "1.0.0");
    component1.setComponentId(1);
    component1.setNamespace(component1.namespace() + "1");
    component1.setName(component1.name() + "1");
    ComponentData component2 = generateComponent(repositoryId, "namespace2", "name2", "2.0.0");
    component2.setNamespace(component2.namespace() + "2");
    component2.setName(component2.name() + "2");
    component2.setComponentId(2);

    asset1.setComponent(component1);
    asset2.setComponent(component1);
    asset3.setComponent(component2);
    asset4.setComponent(component2);
    asset5.setComponent(component2);

    inTx(() -> {
      TestAssetDAO assetDAO = underTest.dao();

      assertThat(browseAssets(assetDAO, repositoryId, null, 10, null), emptyIterable());

      createComponents(component1, component2);
      Stream.of(asset1, asset2, asset3, asset4, asset5)
          .forEach(underTest::createAsset);

      assertThat(browseAssets(assetDAO, repositoryId, null, 10, null), contains(
          allOf(samePath(asset1), sameKind(asset1), sameAttributes(asset1)),
          allOf(samePath(asset2), sameKind(asset2), sameAttributes(asset2)),
          allOf(samePath(asset3), sameKind(asset3), sameAttributes(asset3)),
          allOf(samePath(asset4), sameKind(asset4), sameAttributes(asset4)),
          allOf(samePath(asset5), sameKind(asset5), sameAttributes(asset5))));

      assertEntityVersion(component1.componentId, entityVersioningEnabled ? 3 : null);
      assertEntityVersion(component2.componentId, entityVersioningEnabled ? 4 : null);

      assertTrue(underTest.deleteAsset(asset1));
      assertThat(countAssets(assetDAO, repositoryId), Matchers.is(4));
      assertEntityVersion(component1.componentId, entityVersioningEnabled ? 4 : null);
      assertEntityVersion(component2.componentId, entityVersioningEnabled ? 4 : null);

      assertTrue(underTest.deleteAsset(asset2));

      assertThat(countAssets(assetDAO, repositoryId), Matchers.is(3));
      assertEntityVersion(component1.componentId, entityVersioningEnabled ? 5 : null);
      assertEntityVersion(component2.componentId, entityVersioningEnabled ? 4 : null);

      assertThat(browseAssets(assetDAO, repositoryId, null, 10, null), contains(
          allOf(samePath(asset3), sameKind(asset3), sameAttributes(asset3)),
          allOf(samePath(asset4), sameKind(asset4), sameAttributes(asset4)),
          allOf(samePath(asset5), sameKind(asset5), sameAttributes(asset5))));

      assertTrue(underTest.deleteAsset(asset3));

      assertThat(countAssets(assetDAO, repositoryId), Matchers.is(2));
      assertEntityVersion(component1.componentId, entityVersioningEnabled ? 5 : null);
      assertEntityVersion(component2.componentId, entityVersioningEnabled ? 5 : null);

      assertThat(browseAssets(assetDAO, repositoryId, null, 10, null), contains(
          allOf(samePath(asset4), sameKind(asset4), sameAttributes(asset4)),
          allOf(samePath(asset5), sameKind(asset5), sameAttributes(asset5))));

      assertTrue(underTest.deleteAsset(asset4));

      assertThat(countAssets(assetDAO, repositoryId), Matchers.is(1));
      assertEntityVersion(component1.componentId, entityVersioningEnabled ? 5 : null);
      assertEntityVersion(component2.componentId, entityVersioningEnabled ? 6 : null);

      assertThat(browseAssets(assetDAO, repositoryId, null, 10, null), contains(
          allOf(samePath(asset5), sameKind(asset5), sameAttributes(asset5))));

      assertTrue(underTest.deleteAsset(asset5));

      assertThat(countAssets(assetDAO, repositoryId), Matchers.is(0));
      assertEntityVersion(component1.componentId, entityVersioningEnabled ? 5 : null);
      assertEntityVersion(component2.componentId, entityVersioningEnabled ? 7 : null);

      assertThat(browseAssets(assetDAO, repositoryId, null, 10, null), emptyIterable());
    });
  }

  protected void testBrowseUpdatedAssetsDifferentDates() {
    OffsetDateTime time = OffsetDateTime.now();
    AssetData asset1 = generateAsset(repositoryId, "/asset1/asset1.jar");
    AssetData asset2 = generateAsset(repositoryId, "/asset2/asset2.jar");
    AssetData asset3 = generateAsset(repositoryId, "/asset3/asset3.jar");
    AssetData asset4 = generateAsset(repositoryId, "/asset4/asset4.jar");
    AssetData asset5 = generateAsset(repositoryId, "/asset5/asset5.jar");
    AssetData asset6 = generateAsset(repositoryId, "/asset6/asset6.jar");

    AssetBlobData assetBlob1 = generateAssetBlob();
    AssetBlobData assetBlob2 = generateAssetBlob();
    AssetBlobData assetBlob3 = generateAssetBlob();
    AssetBlobData assetBlob4 = generateAssetBlob();
    AssetBlobData assetBlob5 = generateAssetBlob();
    AssetBlobData assetBlob6 = generateAssetBlob();

    assetBlob1.setAddedToRepository(time);
    assetBlob2.setAddedToRepository(time.plusSeconds(1));
    assetBlob3.setAddedToRepository(time.plusSeconds(2));
    assetBlob4.setAddedToRepository(time.plusSeconds(3));
    assetBlob5.setAddedToRepository(time.plusSeconds(4));
    assetBlob6.setAddedToRepository(time.plusSeconds(5));

    inTx(() -> {
      Collection<AssetInfo> assets = underTest.findUpdatedAssets(repositoryId, null, ImmutableList.of(), null, null, 2);
      assertThat(assets, is(empty()));

      assetBlobStore.createAssetBlob(assetBlob1);
      asset1.setAssetBlob(assetBlob1);
      underTest.createAsset(asset1);
      assets = underTest.findUpdatedAssets(repositoryId, null, ImmutableList.of(), null, null, 2);
      assertThat(assets.size(), is(1));

      assetBlobStore.createAssetBlob(assetBlob2);
      asset2.setAssetBlob(assetBlob2);
      underTest.createAsset(asset2);

      assetBlobStore.createAssetBlob(assetBlob3);
      asset3.setAssetBlob(assetBlob3);
      underTest.createAsset(asset3);

      assets = underTest.findUpdatedAssets(repositoryId, null, ImmutableList.of(), null, null, 2);
      assertThat(assets.size(), is(2));

      assetBlobStore.createAssetBlob(assetBlob4);
      asset4.setAssetBlob(assetBlob4);
      underTest.createAsset(asset4);

      assetBlobStore.createAssetBlob(assetBlob5);
      asset5.setAssetBlob(assetBlob5);
      underTest.createAsset(asset5);

      assetBlobStore.createAssetBlob(assetBlob6);
      asset6.setAssetBlob(assetBlob6);
      underTest.createAsset(asset6);

      assets = underTest.findUpdatedAssets(repositoryId, null, ImmutableList.of("asset5"), null, null, 100);
      assertThat(assets.size(), is(1));

      assets = underTest.findUpdatedAssets(repositoryId, null, ImmutableList.of("asset4", "asset5"), null, null, 100);
      assertThat(assets.size(), is(2));

      assets = underTest.findUpdatedAssets(repositoryId, null, ImmutableList.of("/asset.?/a.*.jar"), null, null, 100);
      assertThat(assets.size(), is(6));
    });
  }

  protected void testBrowseUpdatedAssetsIdenticalDates() {
    OffsetDateTime time1 = OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS);
    AssetData asset1 = generateAsset(repositoryId, "/asset1/asset1.jar");
    AssetData asset2 = generateAsset(repositoryId, "/asset2/asset2.jar");
    AssetData asset3 = generateAsset(repositoryId, "/asset3/asset3.jar");
    AssetData asset4 = generateAsset(repositoryId, "/asset4/asset4.jar");

    AssetBlobData assetBlob1 = generateAssetBlob();
    AssetBlobData assetBlob2 = generateAssetBlob();
    AssetBlobData assetBlob3 = generateAssetBlob();
    AssetBlobData assetBlob4 = generateAssetBlob();

    // times are considered the same if they are at the same millisecond
    OffsetDateTime time2 = time1.plusNanos(100000);
    OffsetDateTime time3 = time2.plusNanos(100000);
    OffsetDateTime time4 = time3.plusNanos(100000);

    assetBlob1.setAddedToRepository(time1);
    assetBlob2.setAddedToRepository(time2);
    assetBlob3.setAddedToRepository(time3);
    assetBlob4.setAddedToRepository(time4);

    inTx(() -> {
      assetBlobStore.createAssetBlob(assetBlob1);
      asset1.setAssetBlob(assetBlob1);
      underTest.createAsset(asset1);
      assetBlobStore.createAssetBlob(assetBlob2);
      asset2.setAssetBlob(assetBlob2);
      underTest.createAsset(asset2);
      Collection<AssetInfo> assets = underTest.findUpdatedAssets(repositoryId, null, ImmutableList.of(), null, null, 2);
      assertThat(assets.size(), is(2));

      assetBlobStore.createAssetBlob(assetBlob3);
      asset3.setAssetBlob(assetBlob3);
      underTest.createAsset(asset3);
      assets = underTest.findUpdatedAssets(repositoryId, null, ImmutableList.of(), null, null, 2);
      assertThat(assets.size(), is(3));

      assetBlobStore.createAssetBlob(assetBlob4);
      asset4.setAssetBlob(assetBlob4);
      underTest.createAsset(asset4);
      assets = underTest.findUpdatedAssets(repositoryId, null, ImmutableList.of(), null, null, 2);
      assertThat(assets.size(), is(4));
    });
  }

  protected void testDeleteAssetsByPaths() {
    ComponentData component1 = generateComponent(repositoryId, "namespace1", "name1", "1.0.0");
    ComponentData component2 = generateComponent(repositoryId, "namespace2", "name2", "2.0.0");
    component2.setVersion(component1.version() + ".2"); // make sure versions are different

    inTx(() -> {

      try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
        ComponentDAO dao = session.access(TestComponentDAO.class);
        dao.createComponent(component1, entityVersioningEnabled);
        dao.createComponent(component2, entityVersioningEnabled);
        session.getTransaction().commit();
      }

      AssetData asset1 = generateAsset(repositoryId, "/asset1/asset1.jar");
      AssetData asset2 = generateAsset(repositoryId, "/asset2/asset2.jar");
      AssetData asset3 = generateAsset(repositoryId, "/asset3/asset3.jar");

      asset1.setComponent(component1);
      asset2.setComponent(component2);
      asset3.setComponent(component2);

      try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
        AssetDAO dao = session.access(TestAssetDAO.class);
        dao.createAsset(asset1, entityVersioningEnabled);
        dao.createAsset(asset2, entityVersioningEnabled);
        dao.createAsset(asset3, entityVersioningEnabled);
        session.getTransaction().commit();
      }

      try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
        ComponentDAO componentDao = session.access(TestComponentDAO.class);
        AssetDAO assetDao = session.access(TestAssetDAO.class);

        assertTrue(assetDao.readPath(repositoryId, asset1.path()).isPresent());
        assertTrue(assetDao.readPath(repositoryId, asset2.path()).isPresent());
        assertTrue(assetDao.readPath(repositoryId, asset3.path()).isPresent());

        Optional<Component> component = componentDao.readCoordinate(repositoryId,
            component1.namespace(), component1.name(), component1.version());
        assertTrue(component.isPresent());
        assertEntityVersion(component.get(), entityVersioningEnabled ? 2 : null);

        component = componentDao.readCoordinate(repositoryId,
            component2.namespace(), component2.name(), component2.version());
        assertTrue(component.isPresent());
        assertEntityVersion(component.get(), entityVersioningEnabled ? 3 : null);

        int deletedCount = underTest.deleteAssetsByPaths(repositoryId, asList(asset2.path(), asset3.path()));
        assertThat(deletedCount, is(2));
      }
    });

    inTx(() -> {
      try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
        ComponentDAO componentDao = session.access(TestComponentDAO.class);
        Optional<Component> component = componentDao.readCoordinate(repositoryId,
            component2.namespace(), component2.name(), component2.version());
        assertTrue(component.isPresent());
        assertEntityVersion(component.get(), entityVersioningEnabled ? 4 : null);

        component = componentDao.readCoordinate(repositoryId,
            component1.namespace(), component1.name(), component1.version());
        assertTrue(component.isPresent());
        assertEntityVersion(component.get(), entityVersioningEnabled ? 2 : null);
      }
    });
  }

  protected void createComponents(final ComponentData... components) {
    stream(components).forEach(componentStore::createComponent);
  }

  protected void inTx(final Runnable action) {
    UnitOfWork.begin(() -> sessionRule.openSession(DEFAULT_DATASTORE_NAME));
    try {
      Transactional.operation.run(action::run);
    }
    finally {
      UnitOfWork.end();
    }
  }

  private void assertEntityVersion(final Integer componentId, final Integer entityVersion) {
    Optional<Component> dbComponent = componentStore.dao().readComponent(componentId);
    assertThat(dbComponent.isPresent(), Matchers.is(true));
    assertThat(dbComponent.get().entityVersion(), is(entityVersion));
  }

  private static void assertEntityVersion(final Component component, final Integer expectedEntityVersion) {
    assertThat(component.entityVersion(), Matchers.is(expectedEntityVersion));
  }

  protected void testMarkAsDownloadedReturnsTrueWhenRowUpdated() {
    // GIVEN — fresh asset, never downloaded
    AssetData asset = generateAsset(repositoryId, "/store-throttle-1");
    globalRepositorySettings.setLastDownloadedInterval(Duration.ofHours(12));

    inTx(() -> {
      TestAssetDAO dao = underTest.dao();
      dao.createAsset(asset, entityVersioningEnabled);

      // WHEN
      boolean updated = underTest.markAsDownloaded(asset);

      // THEN
      assertThat(updated, is(true));
    });
  }

  protected void testMarkAsDownloadedReturnsFalseWhenThrottled() {
    // GIVEN — asset just downloaded
    AssetData asset = generateAsset(repositoryId, "/store-throttle-2");
    globalRepositorySettings.setLastDownloadedInterval(Duration.ofHours(12));

    inTx(() -> {
      TestAssetDAO dao = underTest.dao();
      dao.createAsset(asset, entityVersioningEnabled);

      underTest.markAsDownloaded(asset); // first call — sets last_downloaded

      // WHEN — second call within the interval
      boolean updated = underTest.markAsDownloaded(asset);

      // THEN
      assertThat(updated, is(false));
    });
  }

  protected void testConcurrentMarkAsDownloadedOnlyOneSucceeds() throws Exception {
    // GIVEN — fresh asset, never downloaded; 12h interval
    AssetData asset = generateAsset(repositoryId, "/concurrency-test");
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      TestAssetDAO dao = session.access(TestAssetDAO.class);
      dao.createAsset(asset, entityVersioningEnabled);
      session.getTransaction().commit();
    }
    globalRepositorySettings.setLastDownloadedInterval(Duration.ofHours(12));

    // WHEN — 10 threads race to mark the same asset
    int threads = 10;
    ExecutorService executor = Executors.newFixedThreadPool(threads);
    CountDownLatch ready = new CountDownLatch(threads);
    CountDownLatch start = new CountDownLatch(1);
    AtomicInteger successCount = new AtomicInteger();
    List<Future<?>> futures = new ArrayList<>();

    for (int i = 0; i < threads; i++) {
      futures.add(executor.submit(() -> {
        ready.countDown();
        start.await();
        UnitOfWork.begin(() -> sessionRule.openSession(DEFAULT_DATASTORE_NAME));
        try {
          Transactional.operation.run(() -> {
            if (underTest.markAsDownloaded(asset)) {
              successCount.incrementAndGet();
            }
          });
        }
        finally {
          UnitOfWork.end();
        }
        return null;
      }));
    }

    ready.await();
    start.countDown();
    for (Future<?> f : futures) {
      f.get(30, SECONDS);
    }
    executor.shutdown();

    // THEN — exactly one winner
    assertThat(successCount.get(), is(1));
  }
}
