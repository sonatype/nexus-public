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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.time.UTC;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.datastore.api.DuplicateKeyException;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.AssetInfo;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.store.example.TestAssetBlobDAO;
import org.sonatype.nexus.repository.content.store.example.TestAssetDAO;
import org.sonatype.nexus.repository.content.store.example.TestAssetData;
import org.sonatype.nexus.repository.content.store.example.TestComponentDAO;
import org.sonatype.nexus.repository.content.store.example.TestContentRepositoryDAO;

import com.google.common.collect.ImmutableList;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.hamcrest.collection.IsIterableContainingInOrder;

import static com.google.common.collect.ImmutableSet.of;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.summingInt;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;
import static org.sonatype.nexus.repository.content.store.ComponentDAOTestSupport.browseComponents;
import static org.sonatype.nexus.repository.content.store.ComponentDAOTestSupport.countComponents;

/**
 * Support class for AssetDao tests.
 */
public class AssetDAOTestSupport
    extends ExampleContentTestSupport
{
  protected int repositoryId;

  private boolean entityVersionEnabled;

  protected void initialiseContent(boolean entityVersionEnabled) {
    this.entityVersionEnabled = entityVersionEnabled;
    ContentRepositoryData contentRepository = randomContentRepository();

    createContentRepository(contentRepository);

    repositoryId = contentRepository.repositoryId;

    generateRandomNamespaces(100);
    generateRandomNames(100);
    generateRandomVersions(100);
    generateRandomPaths(100);
  }

  public void testCrudOperations() throws InterruptedException {

    String aKind = "a kind";
    String anotherKind = "another kind";
    AssetData asset1 = randomAsset(repositoryId);
    AssetData asset2 = randomAsset(repositoryId);
    AssetData asset3 = randomAsset(repositoryId, aKind);
    AssetData asset4 = randomAsset(repositoryId, anotherKind);
    AssetData asset5 = randomAsset(repositoryId, anotherKind);

    ComponentData component1 = randomComponent(repositoryId);
    component1.setComponentId(1);
    ComponentData component2 = randomComponent(repositoryId);
    component2.setComponentId(2);

    asset1.setComponent(component1);
    asset2.setComponent(component1);
    asset3.setComponent(component1);
    asset4.setComponent(component2);
    asset5.setComponent(component2);

    // make sure paths are different
    asset2.setPath(asset1.path() + "/2");
    asset3.setPath(asset1.path() + "/3");
    asset4.setPath(asset1.path() + "/4");
    asset5.setPath(asset1.path() + "/5");

    String path1 = asset1.path();
    String path2 = asset2.path();

    Asset tempResult;

    // CREATE

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetDAO dao = session.access(TestAssetDAO.class);
      ComponentDAO componentDAO = session.access(TestComponentDAO.class);

      assertThat(browseAssets(dao, repositoryId, null, 10, null), emptyIterable());

      createComponents(componentDAO, entityVersionEnabled, component1, component2);
      dao.createAsset(asset1, entityVersionEnabled);

      assertThat(browseAssets(dao, repositoryId, null, 10, null), contains(
          allOf(samePath(asset1), sameKind(asset1), sameAttributes(asset1))));

      dao.createAsset(asset2, entityVersionEnabled);
      dao.createAsset(asset3, entityVersionEnabled);
      dao.createAsset(asset4, entityVersionEnabled);
      dao.createAsset(asset5, entityVersionEnabled);

      assertEntityVersion(component1.componentId, componentDAO, entityVersionEnabled ? 4 : null);
      assertEntityVersion(component2.componentId, componentDAO, entityVersionEnabled ? 3 : null);

      // browse all assets
      assertThat(browseAssets(dao, repositoryId, null, 10, null), contains(
          allOf(samePath(asset1), sameKind(asset1), sameAttributes(asset1)),
          allOf(samePath(asset2), sameKind(asset2), sameAttributes(asset2)),
          allOf(samePath(asset3), sameKind(asset3), sameAttributes(asset3)),
          allOf(samePath(asset4), sameKind(asset4), sameAttributes(asset4)),
          allOf(samePath(asset5), sameKind(asset5), sameAttributes(asset5))));

      // browse by kind
      assertThat(browseAssets(dao, repositoryId, aKind, 10, null), contains(
          allOf(samePath(asset3), sameKind(asset3), sameAttributes(asset3))));

      assertThat(browseAssets(dao, repositoryId, anotherKind, 10, null), contains(
          allOf(samePath(asset4), sameKind(asset4), sameAttributes(asset4)),
          allOf(samePath(asset5), sameKind(asset5), sameAttributes(asset5))));

      session.getTransaction().commit();
    }

    // TRY CREATE AGAIN

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetDAO dao = session.access(TestAssetDAO.class);

      AssetData duplicate = new AssetData();
      duplicate.repositoryId = asset1.repositoryId;
      duplicate.setPath(asset1.path());
      duplicate.setKind(asset1.kind());
      duplicate.setAttributes(newAttributes("duplicate"));
      duplicate.setLastUpdated(OffsetDateTime.now());
      dao.createAsset(duplicate, entityVersionEnabled);

      session.getTransaction().commit();
      fail("Cannot create the same component twice");
    }
    catch (DuplicateKeyException e) {
      logger.debug("Got expected exception", e);
    }

    // READ

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetDAO dao = session.access(TestAssetDAO.class);

      assertFalse(dao.readPath(repositoryId, "test-path").isPresent());

      tempResult = dao.readPath(repositoryId, path1).get();
      assertThat(tempResult, samePath(asset1));
      assertThat(tempResult, sameKind(asset1));
      assertThat(tempResult, sameAttributes(asset1));

      tempResult = dao.readPath(repositoryId, path2).get();
      assertThat(tempResult, samePath(asset2));
      assertThat(tempResult, sameKind(asset2));
      assertThat(tempResult, sameAttributes(asset2));
    }

    // UPDATE

    Thread.sleep(2); // NOSONAR make sure any new last updated times will be different

    // must use a new session as CURRENT_TIMESTAMP (used for last_updated) is fixed once used inside a session

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetDAO dao = session.access(TestAssetDAO.class);
      ComponentDAO componentDAO = session.access(TestComponentDAO.class);

      tempResult = dao.readPath(repositoryId, path1).get();

      OffsetDateTime oldCreated = tempResult.created();
      OffsetDateTime oldLastUpdated = tempResult.lastUpdated();

      asset1.attributes("custom-section-1").set("custom-key-1", "more-test-values-1");
      dao.updateAssetAttributes(asset1, entityVersionEnabled);
      asset1.setKind("new-kind-1");
      dao.updateAssetKind(asset1, entityVersionEnabled);

      tempResult = dao.readPath(repositoryId, path1).get();
      assertThat(tempResult, samePath(asset1));
      assertThat(tempResult, sameKind(asset1));
      assertThat(tempResult, sameAttributes(asset1));
      assertThat(tempResult.created(), is(oldCreated));
      assertTrue(tempResult.lastUpdated().isAfter(oldLastUpdated)); // should change as attributes have changed
      assertEntityVersion(component1.componentId, componentDAO, entityVersionEnabled ? 6 : null);

      tempResult = dao.readPath(repositoryId, path2).get();

      oldCreated = tempResult.created();
      oldLastUpdated = tempResult.lastUpdated();

      asset2.assetId = null; // check a 'detached' entity with no internal id can be updated
      asset2.attributes("custom-section-2").set("custom-key-2", "more-test-values-2");
      dao.updateAssetAttributes(asset2, entityVersionEnabled);
      asset2.setKind("new-kind-2");
      dao.updateAssetKind(asset2, entityVersionEnabled);

      tempResult = dao.readPath(repositoryId, path2).get();
      assertThat(tempResult, samePath(asset2));
      assertThat(tempResult, sameKind(asset2));
      assertThat(tempResult, sameAttributes(asset2));
      assertThat(tempResult.created(), is(oldCreated));
      assertTrue(tempResult.lastUpdated().isAfter(oldLastUpdated)); // should change as attributes have changed
      assertEntityVersion(component1.componentId, componentDAO, entityVersionEnabled ? 8 : null);

      session.getTransaction().commit();
    }

    // UPDATE AGAIN

    Thread.sleep(2); // NOSONAR make sure any new last updated times will be different

    // must use a new session as CURRENT_TIMESTAMP (used for last_updated) is fixed once used inside a session

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetDAO dao = session.access(TestAssetDAO.class);
      ComponentDAO componentDAO = session.access(TestComponentDAO.class);

      tempResult = dao.readPath(repositoryId, path1).get();

      OffsetDateTime oldCreated = tempResult.created();
      OffsetDateTime oldLastUpdated = tempResult.lastUpdated();

      asset1.attributes("custom-section-1").set("custom-key-1", "more-test-values-again");
      dao.updateAssetAttributes(asset1, entityVersionEnabled);

      tempResult = dao.readPath(repositoryId, path1).get();
      assertThat(tempResult, samePath(asset1));
      assertThat(tempResult, sameKind(asset1));
      assertThat(tempResult, sameAttributes(asset1));
      assertThat(tempResult.created(), is(oldCreated));
      assertTrue(tempResult.lastUpdated().isAfter(oldLastUpdated)); // should change as attributes changed again
      assertEntityVersion(component1.componentId, componentDAO, entityVersionEnabled ? 9 : null);

      tempResult = dao.readPath(repositoryId, path2).get();

      oldCreated = tempResult.created();
      oldLastUpdated = tempResult.lastUpdated();

      dao.updateAssetAttributes(asset2, entityVersionEnabled);

      tempResult = dao.readPath(repositoryId, path2).get();
      assertThat(tempResult, samePath(asset2));
      assertThat(tempResult, sameKind(asset2));
      assertThat(tempResult, sameAttributes(asset2));
      assertThat(tempResult.created(), is(oldCreated));
      assertThat(tempResult.lastUpdated(), is(oldLastUpdated)); // won't have changed as attributes haven't changed
      assertEntityVersion(component1.componentId, componentDAO, entityVersionEnabled ? 9 : null); // version shouldn't
                                                                                                  // change

      session.getTransaction().commit();
    }

    // DELETE

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetDAO dao = session.access(TestAssetDAO.class);
      ComponentDAO componentDAO = session.access(TestComponentDAO.class);

      assertTrue(dao.deleteAsset(asset1));

      assertThat(browseAssets(dao, repositoryId, null, 10, null), contains(
          allOf(samePath(asset2), sameKind(asset2), sameAttributes(asset2)),
          allOf(samePath(asset3), sameKind(asset3), sameAttributes(asset3)),
          allOf(samePath(asset4), sameKind(asset4), sameAttributes(asset4)),
          allOf(samePath(asset5), sameKind(asset5), sameAttributes(asset5))));

      assertTrue(dao.deleteAssets(repositoryId, 0));

      assertThat(browseAssets(dao, repositoryId, null, 10, null), emptyIterable());
      assertEntityVersion(component1.componentId, componentDAO, entityVersionEnabled ? 9 : null);
      assertEntityVersion(component2.componentId, componentDAO, entityVersionEnabled ? 3 : null);

      AssetData candidate = new AssetData();
      candidate.setRepositoryId(repositoryId);
      candidate.setPath("/test-path");
      assertFalse(dao.deleteAsset(candidate));
    }
  }

  public void testLastDownloaded() throws InterruptedException {

    AssetData asset = randomAsset(repositoryId);
    String path = asset.path();
    Asset tempResult;

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetDAO dao = session.access(TestAssetDAO.class);
      dao.createAsset(asset, false);
      session.getTransaction().commit();
    }

    // INITIAL DOWNLOAD

    Thread.sleep(2); // NOSONAR

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetDAO dao = session.access(TestAssetDAO.class);

      tempResult = dao.readPath(repositoryId, path).get();

      OffsetDateTime oldCreated = tempResult.created();
      OffsetDateTime oldLastUpdated = tempResult.lastUpdated();
      assertFalse(tempResult.lastDownloaded().isPresent());

      dao.markAsDownloaded(asset);

      tempResult = dao.readPath(repositoryId, path).get();
      assertTrue(tempResult.lastDownloaded().isPresent());
      assertTrue(tempResult.lastDownloaded().get().isAfter(oldLastUpdated));
      assertThat(tempResult.created(), is(oldCreated));
      assertTrue(tempResult.lastUpdated().isAfter(oldLastUpdated));

      session.getTransaction().commit();
    }

    // SOME LATER DOWNLOAD

    Thread.sleep(2); // NOSONAR

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetDAO dao = session.access(TestAssetDAO.class);

      tempResult = dao.readPath(repositoryId, path).get();

      OffsetDateTime oldCreated = tempResult.created();
      OffsetDateTime oldLastUpdated = tempResult.lastUpdated();
      OffsetDateTime oldLastDownloaded = tempResult.lastDownloaded().get();

      dao.markAsDownloaded(asset);

      tempResult = dao.readPath(repositoryId, path).get();
      assertTrue(tempResult.lastDownloaded().isPresent());
      assertTrue(tempResult.lastDownloaded().get().isAfter(oldLastDownloaded));
      assertThat(tempResult.created(), is(oldCreated));
      assertTrue(tempResult.lastUpdated().isAfter(oldLastUpdated));

      session.getTransaction().commit();
    }
  }

  public void testAttachingBlobs() throws InterruptedException {

    AssetBlobData assetBlob1 = randomAssetBlob();
    AssetBlobData assetBlob2 = randomAssetBlob();
    AssetData asset = randomAsset(repositoryId);
    String path = asset.path();
    Asset tempResult;

    ComponentData componentData = randomComponent(repositoryId);
    componentData.setComponentId(1);

    asset.setComponent(componentData);

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetBlobDAO dao = session.access(TestAssetBlobDAO.class);
      ComponentDAO componentDAO = session.access(TestComponentDAO.class);

      dao.createAssetBlob(assetBlob1);
      dao.createAssetBlob(assetBlob2);
      componentDAO.createComponent(componentData, entityVersionEnabled);
      session.access(TestAssetDAO.class).createAsset(asset, entityVersionEnabled);
      session.getTransaction().commit();

      assertThat(dao.browseUnusedAssetBlobs(10, 60, null),
          contains(sameBlob(assetBlob1), sameBlob(assetBlob2)));

      assertEntityVersion(componentData.componentId, componentDAO, entityVersionEnabled ? 2 : null);
    }

    // ATTACH BLOB

    Thread.sleep(2); // NOSONAR

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetDAO dao = session.access(TestAssetDAO.class);

      tempResult = dao.readPath(repositoryId, path).get();

      OffsetDateTime oldCreated = tempResult.created();
      OffsetDateTime oldLastUpdated = tempResult.lastUpdated();
      assertFalse(tempResult.blob().isPresent());

      asset.setAssetBlob(assetBlob1);
      dao.updateAssetBlobLink(asset, entityVersionEnabled);

      tempResult = dao.readPath(repositoryId, path).get();
      assertTrue(tempResult.blob().isPresent());
      assertThat(tempResult.blob().get(), sameBlob(assetBlob1));
      assertThat(tempResult, sameBlob(asset));
      assertThat(tempResult.created(), is(oldCreated));
      assertTrue(tempResult.lastUpdated().isAfter(oldLastUpdated));
      assertEntityVersion(componentData.componentId, session.access(TestComponentDAO.class),
          entityVersionEnabled ? 3 : null);

      session.getTransaction().commit();

      assertThat(session.access(TestAssetBlobDAO.class).browseUnusedAssetBlobs(10, 60, null),
          contains(sameBlob(assetBlob2)));
    }

    // REPLACE BLOB

    Thread.sleep(2); // NOSONAR

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetDAO dao = session.access(TestAssetDAO.class);

      tempResult = dao.readPath(repositoryId, path).get();

      OffsetDateTime oldCreated = tempResult.created();
      OffsetDateTime oldLastUpdated = tempResult.lastUpdated();
      assertThat(tempResult.blob().get(), sameBlob(assetBlob1));

      asset.setAssetBlob(assetBlob2);
      dao.updateAssetBlobLink(asset, entityVersionEnabled);

      tempResult = dao.readPath(repositoryId, path).get();
      assertTrue(tempResult.blob().isPresent());
      assertThat(tempResult.blob().get(), sameBlob(assetBlob2));
      assertThat(tempResult, sameBlob(asset));

      assertThat(tempResult.created().truncatedTo(ChronoUnit.SECONDS), is(oldCreated.truncatedTo(ChronoUnit.SECONDS)));

      assertTrue(tempResult.lastUpdated().isAfter(oldLastUpdated));
      assertEntityVersion(componentData.componentId, session.access(TestComponentDAO.class),
          entityVersionEnabled ? 4 : null);

      session.getTransaction().commit();

      assertThat(session.access(TestAssetBlobDAO.class).browseUnusedAssetBlobs(10, 60, null),
          contains(sameBlob(assetBlob1)));
    }

    // REPLACING WITH SAME BLOB DOESN'T UPDATE

    Thread.sleep(2); // NOSONAR

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetDAO dao = session.access(TestAssetDAO.class);

      tempResult = dao.readPath(repositoryId, path).get();

      OffsetDateTime oldCreated = tempResult.created();
      OffsetDateTime oldLastUpdated = tempResult.lastUpdated();
      assertThat(tempResult.blob().get(), sameBlob(assetBlob2));

      asset.setAssetBlob(assetBlob2);
      dao.updateAssetBlobLink(asset, entityVersionEnabled);

      tempResult = dao.readPath(repositoryId, path).get();
      assertTrue(tempResult.blob().isPresent());
      assertThat(tempResult.blob().get(), sameBlob(assetBlob2));
      assertThat(tempResult, sameBlob(asset));

      assertThat(tempResult.created().truncatedTo(ChronoUnit.SECONDS), is(oldCreated.truncatedTo(ChronoUnit.SECONDS)));
      assertThat(tempResult.lastUpdated().truncatedTo(ChronoUnit.SECONDS),
          is(oldLastUpdated.truncatedTo(ChronoUnit.SECONDS)));

      assertEntityVersion(componentData.componentId, session.access(TestComponentDAO.class),
          entityVersionEnabled ? 4 : null);

      session.getTransaction().commit();

      assertThat(session.access(TestAssetBlobDAO.class).browseUnusedAssetBlobs(10, 60, null),
          contains(sameBlob(assetBlob1)));
    }

    // DETACH BLOB

    Thread.sleep(2); // NOSONAR

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetDAO dao = session.access(TestAssetDAO.class);

      tempResult = dao.readPath(repositoryId, path).get();

      OffsetDateTime oldCreated = tempResult.created();
      OffsetDateTime oldLastUpdated = tempResult.lastUpdated();
      assertThat(tempResult.blob().get(), sameBlob(assetBlob2));

      asset.setAssetBlob(null);
      dao.updateAssetBlobLink(asset, entityVersionEnabled);

      tempResult = dao.readPath(repositoryId, path).get();
      assertFalse(tempResult.blob().isPresent());
      assertThat(tempResult.created().truncatedTo(ChronoUnit.SECONDS), is(oldCreated.truncatedTo(ChronoUnit.SECONDS)));
      assertTrue(tempResult.lastUpdated().isAfter(oldLastUpdated));
      assertEntityVersion(componentData.componentId, session.access(TestComponentDAO.class),
          entityVersionEnabled ? 5 : null);

      session.getTransaction().commit();

      assertThat(session.access(TestAssetBlobDAO.class).browseUnusedAssetBlobs(10, 60, null),
          contains(sameBlob(assetBlob1), sameBlob(assetBlob2)));
    }

    // DETACHING BLOB AGAIN DOESN'T UPDATE

    Thread.sleep(2); // NOSONAR

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetDAO dao = session.access(TestAssetDAO.class);

      tempResult = dao.readPath(repositoryId, path).get();

      OffsetDateTime oldCreated = tempResult.created();
      OffsetDateTime oldLastUpdated = tempResult.lastUpdated();
      assertFalse(tempResult.blob().isPresent());

      asset.setAssetBlob(null);
      dao.updateAssetBlobLink(asset, entityVersionEnabled);

      tempResult = dao.readPath(repositoryId, path).get();
      assertFalse(tempResult.blob().isPresent());

      assertThat(tempResult.created().truncatedTo(ChronoUnit.SECONDS), is(oldCreated.truncatedTo(ChronoUnit.SECONDS)));
      assertThat(tempResult.lastUpdated().truncatedTo(ChronoUnit.SECONDS),
          is(oldLastUpdated.truncatedTo(ChronoUnit.SECONDS)));

      assertEntityVersion(componentData.componentId, session.access(TestComponentDAO.class),
          entityVersionEnabled ? 5 : null);

      session.getTransaction().commit();

      assertThat(session.access(TestAssetBlobDAO.class).browseUnusedAssetBlobs(10, 60, null),
          contains(sameBlob(assetBlob1), sameBlob(assetBlob2)));
    }
  }

  public void testBrowseComponentAssets() {

    // scatter components and assets
    generateRandomRepositories(10);
    generateRandomContent(10, 100, entityVersionEnabled);

    List<Asset> browsedAssets = new ArrayList<>();

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetDAO assetDao = session.access(TestAssetDAO.class);
      ComponentDAO componentDAO = session.access(TestComponentDAO.class);

      assertThat(generatedRepositories().stream()
          .map(ContentRepositoryData::contentRepositoryId)
          .collect(summingInt(r -> countAssets(assetDao, r))), is(100));

      assertThat(generatedRepositories().stream()
          .map(ContentRepositoryData::contentRepositoryId)
          .collect(summingInt(r -> countComponents(componentDAO, r))), is(10));

      // now gather them back by browsing
      generatedRepositories().forEach(r -> browseComponents(componentDAO, r.repositoryId, null, 10, null).stream()
          .map(ComponentData.class::cast)
          .map(assetDao::browseComponentAssets)
          .forEach(browsedAssets::addAll));
    }

    // we should have the same assets, but maybe in a different order
    // (use hamcrest class directly as javac picks the wrong static varargs method)
    assertThat(browsedAssets, new IsIterableContainingInAnyOrder<>(
        generatedAssets().stream()
            // ignore generated assets without components
            .filter(asset -> asset.component().isPresent())
            .map(ExampleContentTestSupport::samePath)
            .collect(toList())));

    // check assets under a 'detached' entity with no internal id can still be browsed
    ComponentData component = (ComponentData) generatedComponents().get(0);
    component.componentId = null;
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      assertTrue(session.access(TestAssetDAO.class)
          .browseComponentAssets(component)
          .stream()
          .map(Asset::component)
          .map(Optional::get)
          .allMatch(sameCoordinates(component)::matches));
    }
  }

  public void testContinuationBrowsing() {

    generateRandomNamespaces(1);
    generateRandomNames(1);
    generateRandomVersions(1);
    generateRandomPaths(10000);
    generateRandomRepositories(1);
    generateRandomContent(1, 1000, entityVersionEnabled);

    repositoryId = generatedRepositories().get(0).repositoryId;

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetDAO dao = session.access(TestAssetDAO.class);

      assertThat(countAssets(dao, repositoryId), is(1000));

      int page = 0;

      Continuation<Asset> assets = browseAssets(dao, repositoryId, null, 10, null);
      while (!assets.isEmpty()) {

        // verify we got the expected slice
        assertThat(assets, new IsIterableContainingInOrder<>(
            generatedAssets()
                .subList(page * 10, (page + 1) * 10)
                .stream()
                .map(ExampleContentTestSupport::samePath)
                .collect(toList())));

        assets = browseAssets(dao, repositoryId, null, 10, assets.nextContinuationToken());

        page++;
      }

      assertThat(page, is(100));
    }
  }

  public void testFlaggedBrowsing() {

    TestAssetData asset1 = randomAsset(repositoryId);
    TestAssetData asset2 = randomAsset(repositoryId);
    asset2.setPath(asset1.path() + "/2"); // make sure paths are different

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      TestAssetDAO dao = session.access(TestAssetDAO.class);

      // our bespoke schema will be applied automatically via 'extendSchema'...

      dao.createAsset(asset1, false);
      dao.createAsset(asset2, false);

      assertThat(dao.browseFlaggedAssets(repositoryId, 10, null), emptyIterable());

      asset2.setTestFlag(true);
      dao.updateAssetFlag(asset2);

      assertThat(dao.browseFlaggedAssets(repositoryId, 10, null),
          contains(allOf(samePath(asset2), sameAttributes(asset2))));

      asset1.setTestFlag(true);
      dao.updateAssetFlag(asset1);

      asset2.setTestFlag(false);
      dao.updateAssetFlag(asset2);

      assertThat(dao.browseFlaggedAssets(repositoryId, 10, null),
          contains(allOf(samePath(asset1), sameAttributes(asset1))));
    }
  }

  public void testReadPathTest() {
    TestAssetData asset1 = randomAsset(repositoryId);
    TestAssetData asset2 = randomAsset(repositoryId);
    asset2.setPath(asset1.path() + "/2"); // make sure paths are different

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      TestAssetDAO dao = session.access(TestAssetDAO.class);

      // our bespoke schema will be applied automatically via 'extendSchema'...

      dao.createAsset(asset1, false);
      dao.createAsset(asset2, false);

      asset2.setTestFlag(true);
      dao.updateAssetFlag(asset2);

      TestAssetData test1 = dao.readPathTest(repositoryId, asset1.path()).orElse(null);
      TestAssetData test2 = dao.readPathTest(repositoryId, asset2.path()).orElse(null);
      assertThat(test1, notNullValue());
      assertThat(test1.getTestFlag(), equalTo(false));
      assertThat(test2, notNullValue());
      assertThat(test2.getTestFlag(), equalTo(true));

      Continuation<Asset> continuation = dao.browseFlaggedAssets(repositoryId, 10, null);
      assertThat(continuation.size(), equalTo(1));
      TestAssetData test3 = continuation.stream()
          .filter(obj -> obj instanceof TestAssetData)
          .map(obj -> (TestAssetData) obj)
          .findFirst()
          .orElseThrow(() -> new IllegalStateException("Expect asset not found"));
      assertThat(test3.getTestFlag(), equalTo(true));
    }
  }

  public void testDeleteAllAssets() {

    // scatter components and assets
    generateRandomRepositories(1);
    generateRandomContent(100, 100, entityVersionEnabled);

    repositoryId = generatedRepositories().get(0).contentRepositoryId();

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetDAO dao = session.access(TestAssetDAO.class);

      assertThat(countAssets(dao, repositoryId), is(100));

      assertThat(browseAssets(dao, repositoryId, null, 100, null).size(), is(100));

      dao.deleteAssets(repositoryId, 20);

      assertThat(browseAssets(dao, repositoryId, null, 100, null).size(), is(80));

      dao.deleteAssets(repositoryId, 10);

      assertThat(browseAssets(dao, repositoryId, null, 100, null).size(), is(70));

      dao.deleteAssets(repositoryId, 0);

      assertThat(browseAssets(dao, repositoryId, null, 100, null).size(), is(0));

      dao.deleteAssets(repositoryId, -1);

      assertThat(browseAssets(dao, repositoryId, null, 100, null).size(), is(0));
    }
  }

  public void testReadPaths() {

    AssetData asset1 = randomAsset(repositoryId);
    AssetData asset2 = randomAsset(repositoryId);
    AssetData asset3 = randomAsset(repositoryId);
    AssetData asset4 = randomAsset(repositoryId);
    AssetData asset5 = randomAsset(repositoryId);

    // make sure paths are different
    asset2.setPath(asset1.path() + "/2");
    asset3.setPath(asset1.path() + "/3");
    asset4.setPath(asset1.path() + "/4");
    asset5.setPath(asset1.path() + "/5");

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetDAO dao = session.access(TestAssetDAO.class);

      dao.createAsset(asset1, false);
      dao.createAsset(asset2, false);
      dao.createAsset(asset3, false);
      dao.createAsset(asset4, false);
      dao.createAsset(asset5, false);

      assertThat(countAssets(dao, repositoryId), is(5));

      Collection<Asset> assets =
          dao.readPathsFromRepository(repositoryId, asList(asset1.path(), asset2.path(), asset3.path()));

      assertThat(assets.size(), is(3));

      assets = dao.readPathsFromRepository(repositoryId, asList(asset4.path(), asset5.path()));

      assertThat(assets.size(), is(2));
    }
  }

  public void testPurgeOperation() {
    AssetData asset1 = randomAsset(repositoryId);
    AssetData asset2 = randomAsset(repositoryId);
    asset2.setPath(asset1.path() + "/2"); // make sure paths are different

    asset1.setLastDownloaded(UTC.now().minusDays(2));
    asset2.setLastDownloaded(UTC.now().minusDays(4));

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetDAO dao = session.access(TestAssetDAO.class);
      dao.createAsset(asset1, false);
      dao.createAsset(asset2, false);
      session.getTransaction().commit();
    }

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetDAO dao = session.access(TestAssetDAO.class);

      assertTrue(dao.readPath(repositoryId, asset1.path()).isPresent());
      assertTrue(dao.readPath(repositoryId, asset2.path()).isPresent());

      int[] assetIds = dao.selectNotRecentlyDownloaded(repositoryId, 3, 10);
      assertThat(assetIds, is(new int[]{2}));

      if ("H2".equals(session.sqlDialect())) {
        dao.purgeSelectedAssets(stream(assetIds).boxed().toArray(Integer[]::new));
      }
      else {
        dao.purgeSelectedAssets(assetIds, false);
      }

      assertTrue(dao.readPath(repositoryId, asset1.path()).isPresent());
      assertFalse(dao.readPath(repositoryId, asset2.path()).isPresent());
    }
  }

  public void testRoundTrip() {
    AssetData asset1 = randomAsset(repositoryId);
    AssetData asset2 = randomAsset(repositoryId);
    asset2.setPath(asset1.path() + "/2"); // make sure paths are different

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetDAO dao = session.access(TestAssetDAO.class);
      dao.createAsset(asset1, false);
      dao.createAsset(asset2, false);
      session.getTransaction().commit();
    }

    Asset tempResult;

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetDAO dao = session.access(TestAssetDAO.class);

      tempResult = dao.readAsset(asset1.assetId).get();
      assertThat(tempResult, samePath(asset1));
      assertThat(tempResult, sameKind(asset1));
      assertThat(tempResult, sameAttributes(asset1));

      tempResult = dao.readAsset(asset2.assetId).get();
      assertThat(tempResult, samePath(asset2));
      assertThat(tempResult, sameKind(asset2));
      assertThat(tempResult, sameAttributes(asset2));
    }
  }

  public void testBrowseAssetsInRepositories() {
    ContentRepositoryData anotherContentRepository = randomContentRepository();
    createContentRepository(anotherContentRepository);
    int anotherRepositoryId = anotherContentRepository.repositoryId;
    AssetData asset1 = randomAsset(repositoryId);
    AssetData asset2 = randomAsset(repositoryId);
    AssetData asset3 = randomAsset(anotherRepositoryId);
    AssetData asset4 = randomAsset(anotherRepositoryId);

    // make sure paths are different
    asset2.setPath(asset1.path() + "/2");
    asset3.setPath(asset1.path() + "/3");
    asset4.setPath(asset1.path() + "/4");

    // CREATE

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetDAO dao = session.access(TestAssetDAO.class);

      assertThat(
          dao.browseAssetsInRepositories(newHashSet(repositoryId, anotherRepositoryId), null,
              null, null, emptyMap(), 10),
          emptyIterable());

      dao.createAsset(asset1, false);

      assertThat(
          dao.browseAssetsInRepositories(newHashSet(repositoryId, anotherRepositoryId), null,
              null, null, emptyMap(), 10),
          contains(allOf(samePath(asset1), sameAttributes(asset1))));

      dao.createAsset(asset2, false);
      dao.createAsset(asset3, false);
      dao.createAsset(asset4, false);

      // browse all assets
      assertThat(
          dao.browseAssetsInRepositories(newHashSet(repositoryId, anotherRepositoryId), null,
              null, null, emptyMap(), 10),
          contains(allOf(samePath(asset1), sameAttributes(asset1)), allOf(samePath(asset2), sameAttributes(asset2)),
              allOf(samePath(asset3), sameAttributes(asset3)), allOf(samePath(asset4), sameAttributes(asset4))));

      session.getTransaction().commit();
    }
  }

  public void testBrowseEagerAssetsInRepository() {
    generateConfiguration();
    EntityId entityId = generatedConfigurations().get(0).getRepositoryId();
    generateSingleRepository(UUID.fromString(entityId.getValue()));
    repositoryId = generatedRepositories().get(0).repositoryId;

    // create 5 components with assets and blobs
    generateContent(5, entityVersionEnabled);

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetDAO dao = session.access(TestAssetDAO.class);

      Continuation<Asset> assets = dao.browseEagerAssetsInRepository(repositoryId, null, 10);
      assertThat(assets.size(), is(5));
      // each asset should contain a blob and should belong to a component
      for (Asset asset : assets) {
        assertThat(asset.component().isPresent(), is(true));
        assertThat(asset.blob().isPresent(), is(true));
      }
    }
  }

  public void testSetLastDownloaded() {
    AssetData asset1 = randomAsset(repositoryId);
    ComponentData componentData = randomComponent(repositoryId);
    componentData.setComponentId(1);
    asset1.setComponent(componentData);

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetDAO dao = session.access(TestAssetDAO.class);
      ComponentDAO componentDAO = session.access(TestComponentDAO.class);

      createComponents(componentDAO, entityVersionEnabled, componentData);
      asset1.assetId = dao.createAsset(asset1, entityVersionEnabled);
      OffsetDateTime dateTime = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);
      dao.lastDownloaded(asset1.assetId, dateTime);

      Optional<OffsetDateTime> actual = dao.readAsset(asset1.assetId).get().lastDownloaded();
      assertThat(actual.map(t -> t.truncatedTo(ChronoUnit.SECONDS)).orElse(null),
          is(dateTime.truncatedTo(ChronoUnit.SECONDS)));

      assertEntityVersion(componentData.componentId, session.access(TestComponentDAO.class),
          entityVersionEnabled ? 2 : null);
    }
  }

  public void testLastUpdated() {
    AssetData asset1 = randomAsset(repositoryId);
    ComponentData componentData = randomComponent(repositoryId);
    componentData.setComponentId(1);
    asset1.setComponent(componentData);

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetDAO dao = session.access(TestAssetDAO.class);
      ComponentDAO componentDAO = session.access(TestComponentDAO.class);

      createComponents(componentDAO, entityVersionEnabled, componentData);
      asset1.assetId = dao.createAsset(asset1, entityVersionEnabled);

      OffsetDateTime dateTime = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);
      dao.lastUpdated(asset1.assetId, dateTime);

      OffsetDateTime actual = dao.readAsset(asset1.assetId).get().lastUpdated();
      assertThat(actual.truncatedTo(ChronoUnit.SECONDS), is(dateTime.truncatedTo(ChronoUnit.SECONDS)));

      assertEntityVersion(componentData.componentId, session.access(TestComponentDAO.class),
          entityVersionEnabled ? 2 : null);
    }
  }

  public void testFilterClauseIsolation() {
    ContentRepositoryData anotherContentRepository = randomContentRepository();
    createContentRepository(anotherContentRepository);
    int anotherRepositoryId = anotherContentRepository.repositoryId;
    AssetData asset1 = randomAsset(repositoryId);
    AssetData asset2 = randomAsset(anotherRepositoryId);

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetDAO dao = session.access(TestAssetDAO.class);

      dao.createAsset(asset1, false);
      dao.createAsset(asset2, false);

      assertThat(dao.browseAssets(repositoryId, 1000, null, null, "true or true", null), hasSize(1));
      assertThat(dao.countAssets(repositoryId, null, "true or true", null), equalTo(1));
      assertThat(dao.browseAssetsInRepositories(of(repositoryId), null, null, "true or true", null, 1000), hasSize(1));
    }
  }

  public void testFindByBlobRef() throws InterruptedException {
    AssetBlobData assetBlob = randomAssetBlob();
    AssetData asset1 = randomAsset(repositoryId);
    AssetData asset2 = randomAsset(repositoryId);
    String path = asset2.path();
    Asset tempResult;

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetBlobDAO dao = session.access(TestAssetBlobDAO.class);
      dao.createAssetBlob(assetBlob);
      session.access(TestAssetDAO.class).createAsset(asset2, false);
      session.getTransaction().commit();
    }
    // ATTACH BLOB

    Thread.sleep(2); // NOSONAR

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetDAO dao = session.access(TestAssetDAO.class);
      tempResult = dao.readPath(repositoryId, path).get();
      asset2.setAssetBlob(assetBlob);
      dao.updateAssetBlobLink(asset2, false);
      session.getTransaction().commit();
    }

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetDAO dao = session.access(TestAssetDAO.class);
      tempResult = dao.findByBlobRef(repositoryId, assetBlob.blobRef()).get();
      assertThat(tempResult.path(), is(path));
    }
  }

  public void testFindByComponentIds() {
    generateConfiguration();
    EntityId repositoryId = generatedConfigurations().get(0).getRepositoryId();
    generateSingleRepository(UUID.fromString(repositoryId.getValue()));
    generateContent(2, entityVersionEnabled);

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      TestAssetDAO dao = session.access(TestAssetDAO.class);
      Collection<AssetInfo> assets = dao.findByComponentIds(Collections.singleton(1), null, Collections.emptyMap());

      Optional<AssetInfo> assetOpt = assets.stream().findFirst();
      assertThat(assetOpt.isPresent(), is(true));
      AssetInfo asset = assetOpt.get();

      Asset generatedAsset = generatedAssets().get(0);
      AssetBlob generatedAssetBlob = generatedAssetBlobs().get(0);

      assertThat(asset.path(), is(generatedAsset.path()));
      assertThat(asset.contentType(), is(generatedAssetBlob.contentType()));
      assertThat(asset.lastUpdated(), notNullValue());
      assertThat(asset.checksums(), notNullValue());
    }
  }

  public void testFindAddedToRepository() {
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetDAO dao = session.access(TestAssetDAO.class);
      AssetBlobDAO blobDao = session.access(TestAssetBlobDAO.class);
      OffsetDateTime baseTime = OffsetDateTime.of(2022, 4, 24, 15, 18, 22,
          111111000, ZoneOffset.UTC);
      Collection<AssetInfo> found = dao.findAddedToRepositoryWithinRange(repositoryId, baseTime,
          baseTime.plus(1, ChronoUnit.MILLIS), ImmutableList.of(), null, null, 100);
      assertThat(found, emptyIterable());
      found = dao.findGreaterThanOrEqualToAddedToRepository(repositoryId, baseTime, ImmutableList.of(), null, null, 10);
      assertThat(found, emptyIterable());

      AssetData asset1 = generateAsset(repositoryId, "/asset1/asset1.jar");
      AssetData asset2 = generateAsset(repositoryId, "/asset2/asset2.jar");
      AssetData asset3 = generateAsset(repositoryId, "/asset3/asset3.jar");
      AssetData asset4 = generateAsset(repositoryId, "/asset4/asset4.jar");
      AssetData asset5 = generateAsset(repositoryId, "/asset5/asset5.jar");
      AssetData asset6 = generateAsset(repositoryId, "/asset6/asset6.jar");

      AssetBlobData assetBlob1 = randomAssetBlob();
      AssetBlobData assetBlob2 = randomAssetBlob();
      AssetBlobData assetBlob3 = randomAssetBlob();
      AssetBlobData assetBlob4 = randomAssetBlob();
      AssetBlobData assetBlob5 = randomAssetBlob();
      AssetBlobData assetBlob6 = randomAssetBlob();

      assetBlob1.setAddedToRepository(baseTime);
      assetBlob2.setAddedToRepository(baseTime);
      assetBlob3.setAddedToRepository(baseTime.plusSeconds(1));
      assetBlob4.setAddedToRepository(baseTime.plusSeconds(2));
      assetBlob5.setAddedToRepository(baseTime.minusSeconds(1));
      assetBlob6.setAddedToRepository(baseTime.minusSeconds(3));

      createAssetBlobs(blobDao, assetBlob1, assetBlob2, assetBlob3, assetBlob4, assetBlob5, assetBlob6);
      asset1.setAssetBlob(assetBlob1);
      asset2.setAssetBlob(assetBlob2);
      asset3.setAssetBlob(assetBlob3);
      asset4.setAssetBlob(assetBlob4);
      asset5.setAssetBlob(assetBlob5);
      asset6.setAssetBlob(assetBlob6);

      createAssets(dao, entityVersionEnabled, asset1, asset2, asset3, asset4, asset5, asset6);

      found = dao.findAddedToRepositoryWithinRange(repositoryId, baseTime, baseTime.plus(1, ChronoUnit.MILLIS),
          ImmutableList.of(), null, null, 100);
      assertThat(found.size(), is(2));

      found =
          dao.findGreaterThanOrEqualToAddedToRepository(repositoryId, baseTime, ImmutableList.of(), null, null, 100);
      assertThat(found.size(), is(4));

      found = dao.findGreaterThanOrEqualToAddedToRepository(repositoryId, baseTime, ImmutableList.of(), null, null, 1);
      assertThat(found.size(), is(1));

      found = dao.findGreaterThanOrEqualToAddedToRepository(repositoryId, baseTime.minusDays(1),
          ImmutableList.of(".*/asset1/.*"), null, null, 100);
      assertThat(found.size(), is(1));

      found = dao.findGreaterThanOrEqualToAddedToRepository(repositoryId, baseTime.minusDays(1),
          ImmutableList.of(".*/asset3/.*", ".*/asset5/.*"), null, null, 100);
      assertThat(found.size(), is(2));

      found = dao.findGreaterThanOrEqualToAddedToRepository(repositoryId, baseTime.minusDays(1),
          ImmutableList.of(".*/asset.?/a.*\\.jar.*"), null, null, 100);
      assertThat(found.size(), is(6));
    }
  }

  public void testFindAddedToRepositoryTruncatesToMilliseconds() {
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetDAO dao = session.access(TestAssetDAO.class);
      AssetBlobDAO blobDao = session.access(TestAssetBlobDAO.class);
      OffsetDateTime baseTime = OffsetDateTime.of(2022, 4, 24, 15, 18, 22,
          111111000, ZoneOffset.UTC);
      Collection<AssetInfo> found =
          dao.findAddedToRepositoryWithinRange(repositoryId, baseTime, baseTime.plus(1, ChronoUnit.MILLIS),
              ImmutableList.of(), null, null, 100);
      assertThat(found, emptyIterable());
      found = dao.findGreaterThanOrEqualToAddedToRepository(repositoryId, baseTime, ImmutableList.of(), null, null, 10);
      assertThat(found, emptyIterable());

      AssetData asset1 = generateAsset(repositoryId, "/asset1/asset1.jar");
      AssetData asset2 = generateAsset(repositoryId, "/asset2/asset2.jar");
      AssetData asset3 = generateAsset(repositoryId, "/asset3/asset3.jar");
      AssetData asset4 = generateAsset(repositoryId, "/asset4/asset4.jar");

      AssetBlobData assetBlob1 = randomAssetBlob();
      AssetBlobData assetBlob2 = randomAssetBlob();
      AssetBlobData assetBlob3 = randomAssetBlob();
      AssetBlobData assetBlob4 = randomAssetBlob();

      assetBlob1.setAddedToRepository(baseTime);
      assetBlob2.setAddedToRepository(baseTime);
      // Microsecond level time can be stored in both h2 and postgres. However, the last updated queries should truncate
      // the time and treat these as identical to baseTime
      assetBlob3.setAddedToRepository(baseTime.plusNanos(5000));
      assetBlob4.setAddedToRepository(baseTime.plusNanos(8000));

      createAssetBlobs(blobDao, assetBlob1, assetBlob2, assetBlob3, assetBlob4);

      asset1.setAssetBlob(assetBlob1);
      asset2.setAssetBlob(assetBlob2);
      asset3.setAssetBlob(assetBlob3);
      asset4.setAssetBlob(assetBlob4);

      createAssets(dao, entityVersionEnabled, asset1, asset2, asset3, asset4);

      found = dao.findAddedToRepositoryWithinRange(repositoryId, baseTime, baseTime.plus(1, ChronoUnit.MILLIS),
          ImmutableList.of(), null, null, 100);
      assertThat(found.size(), is(4));

      found = dao.findGreaterThanOrEqualToAddedToRepository(repositoryId,
          baseTime.plus(1, ChronoUnit.MILLIS).truncatedTo(ChronoUnit.MILLIS), ImmutableList.of(), null, null, 100);
      assertThat(found.size(), is(0));
    }
  }

  public void testDeleteByPaths() {

    AssetData asset1 = randomAsset(repositoryId);
    AssetData asset2 = randomAsset(repositoryId);
    AssetData asset3 = randomAsset(repositoryId);
    AssetData asset4 = randomAsset(repositoryId);
    AssetData asset5 = randomAsset(repositoryId);

    ComponentData component1 = randomComponent(repositoryId);
    component1.setComponentId(1);
    ComponentData component2 = randomComponent(repositoryId);
    component2.setComponentId(2);

    asset1.setComponent(component1);
    asset2.setComponent(component1);
    asset3.setComponent(component1);
    asset4.setComponent(component2);
    asset5.setComponent(component2);

    // make sure paths are different
    asset2.setPath(asset1.path() + "/2");
    asset3.setPath(asset1.path() + "/3");
    asset4.setPath(asset1.path() + "/4");
    asset5.setPath(asset1.path() + "/5");

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      TestAssetDAO dao = session.access(TestAssetDAO.class);
      ComponentDAO componentDAO = session.access(TestComponentDAO.class);

      createComponents(componentDAO, entityVersionEnabled, component1, component2);

      dao.createAsset(asset1, entityVersionEnabled);
      dao.createAsset(asset2, entityVersionEnabled);
      dao.createAsset(asset3, entityVersionEnabled);
      dao.createAsset(asset4, entityVersionEnabled);
      dao.createAsset(asset5, entityVersionEnabled);

      assertThat(countAssets(dao, repositoryId), is(5));
      assertThat(dao.readPathsFromRepository(repositoryId,
          asList(asset1.path(), asset2.path(), asset3.path(), asset4.path(), asset5.path())).size(),
          is(5));

      assertEntityVersion(component1.componentId, session.access(TestComponentDAO.class),
          entityVersionEnabled ? 4 : null);
      assertEntityVersion(component2.componentId, session.access(TestComponentDAO.class),
          entityVersionEnabled ? 3 : null);

      dao.deleteAssetsByPaths(repositoryId, asList(asset1.path(), asset2.path()), entityVersionEnabled);

      assertEntityVersion(component1.componentId, session.access(TestComponentDAO.class),
          entityVersionEnabled ? 5 : null);
      assertEntityVersion(component2.componentId, session.access(TestComponentDAO.class),
          entityVersionEnabled ? 3 : null);

      dao.deleteAssetsByPaths(repositoryId, singletonList(asset4.path()), entityVersionEnabled);

      assertEntityVersion(component1.componentId, session.access(TestComponentDAO.class),
          entityVersionEnabled ? 5 : null);
      assertEntityVersion(component2.componentId, session.access(TestComponentDAO.class),
          entityVersionEnabled ? 4 : null);

      dao.deleteAssetsByPaths(repositoryId, asList(asset3.path(), asset5.path()), entityVersionEnabled);

      assertEntityVersion(component1.componentId, session.access(TestComponentDAO.class),
          entityVersionEnabled ? 6 : null);
      assertEntityVersion(component2.componentId, session.access(TestComponentDAO.class),
          entityVersionEnabled ? 5 : null);

      // assets don't exist - should still be the same
      dao.deleteAssetsByPaths(repositoryId, asList(asset1.path(), asset2.path()), entityVersionEnabled);
      assertEntityVersion(component1.componentId, session.access(TestComponentDAO.class),
          entityVersionEnabled ? 6 : null);
      assertEntityVersion(component2.componentId, session.access(TestComponentDAO.class),
          entityVersionEnabled ? 5 : null);
    }
  }

  public void testAssetRecordsExist() {
    AssetBlobData assetBlob = randomAssetBlob();
    AssetBlobData assetBlobWithoutComponent = randomAssetBlob();

    AssetData asset = randomAsset(repositoryId);
    AssetData assetWithoutComponent = randomAsset(repositoryId);

    ComponentData componentData = randomComponent(repositoryId);
    componentData.setComponentId(1);

    asset.setComponent(componentData);

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetDAO assetDao = session.access(TestAssetDAO.class);
      AssetBlobDAO assetBlobDao = session.access(TestAssetBlobDAO.class);
      ComponentDAO componentDAO = session.access(TestComponentDAO.class);

      assetBlobDao.createAssetBlob(assetBlob);
      assetBlobDao.createAssetBlob(assetBlobWithoutComponent);
      componentDAO.createComponent(componentData, entityVersionEnabled);
      asset.setAssetBlob(assetBlob);
      assetWithoutComponent.setAssetBlob(assetBlobWithoutComponent);
      assetDao.createAsset(asset, entityVersionEnabled);
      assetDao.createAsset(assetWithoutComponent, entityVersionEnabled);
      session.getTransaction().commit();

      // existing asset + blob + component
      assertThat(assetDao.assetRecordsExist(assetBlob.blobRef(), null, null), is(true));

      // random blob is not exists
      BlobRef blobRef = new BlobRef("default", UUID.randomUUID().toString());
      assertThat(assetDao.assetRecordsExist(blobRef, null, null), is(false));

      // record still exists without a component
      assertThat(assetDao.assetRecordsExist(assetBlobWithoutComponent.blobRef(), null, null), is(true));
    }
  }

  protected void createContentRepository(final ContentRepositoryData contentRepository) {
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      ContentRepositoryDAO dao = session.access(TestContentRepositoryDAO.class);
      dao.createContentRepository(contentRepository);
      session.getTransaction().commit();
    }
  }

  protected void createComponents(
      final ComponentDAO componentDAO,
      final boolean entityVersionEnabled,
      ComponentData... components)
  {
    stream(components).forEach(component -> componentDAO.createComponent(component, entityVersionEnabled));
  }

  static int countAssets(final AssetDAO dao, final int repositoryId) {
    return dao.countAssets(repositoryId, null, null, null);
  }

  static Continuation<Asset> browseAssets(
      final AssetDAO dao,
      final int repositoryId,
      final String kind,
      final int limit,
      final String continuationToken)
  {
    return dao.browseAssets(repositoryId, limit, continuationToken, kind, null, null);
  }

  protected void createAssets(
      final AssetDAO dao,
      final boolean updateComponentEntityVersion,
      final AssetData... assets)
  {
    for (AssetData asset : assets) {
      dao.createAsset(asset, updateComponentEntityVersion);
    }
  }

  protected void createAssetBlobs(final AssetBlobDAO dao, final AssetBlobData... assetBlobs) {
    for (AssetBlobData assetBlob : assetBlobs) {
      dao.createAssetBlob(assetBlob);
    }
  }

  private void assertEntityVersion(
      final int componentId,
      final ComponentDAO componentDAO,
      final Integer expectedEntityVersion)
  {
    Optional<Component> component = componentDAO.readComponent(componentId);
    assertThat(component.isPresent(), is(true));
    assertThat(component.get().entityVersion(), is(expectedEntityVersion));
  }
}
