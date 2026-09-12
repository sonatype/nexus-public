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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.time.UTC;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.datastore.api.DuplicateKeyException;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.ComponentSet;
import org.sonatype.nexus.repository.content.store.example.TestAssetDAO;
import org.sonatype.nexus.repository.content.store.example.TestComponentDAO;
import org.sonatype.nexus.repository.content.store.example.TestContentRepositoryDAO;

import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.hamcrest.collection.IsIterableContainingInOrder;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.summingInt;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

/**
 * Support class for ComponentDao tests.
 */
public abstract class ComponentDAOTestSupport
    extends ExampleContentTestSupport
{
  private int repositoryId;

  private boolean entityVersionEnabled;

  public void setupContent(final boolean entityVersionEnabled) {
    this.entityVersionEnabled = entityVersionEnabled;

    ContentRepositoryData contentRepository = generateContentRepository();

    createContentRepository(contentRepository);

    repositoryId = contentRepository.repositoryId;

    generateNamespaces(100);
    generateNames(100);
    generateVersions(100);
    generatePaths(100);
  }

  protected void testCrudOperations() throws InterruptedException {

    ComponentData component1 = generateComponent(repositoryId, "namespace1", "name1", "1.0.0");
    ComponentData component2 = generateComponent(repositoryId, "namespace2", "name2", "2.0.0");
    ComponentData component3 = generateComponent(repositoryId, "namespace3", "name3", "3.0.0");

    // cover with/without namespace and different versions
    String akind = "kind1";
    String anotherKind = "kind2";
    component1.setNamespace("");
    component1.setVersion("1.1");
    component1.setNormalizedVersion("0000000001.0000000001");
    component1.setKind(akind);
    component2.setNamespace("demo");
    component2.setVersion("1.2");
    component2.setNormalizedVersion("0000000001.0000000002");
    component2.setKind(anotherKind);
    component3.setNamespace("another demo");
    component3.setVersion("1.0");
    component1.setNormalizedVersion("0000000001.0000000000");
    component3.setKind(anotherKind);

    String namespace1 = component1.namespace();
    String name1 = component1.name();
    String version1 = component1.version();

    String namespace2 = component2.namespace();
    String name2 = component2.name();
    String version2 = component2.version();

    Component tempResult;

    // CREATE

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      ComponentDAO dao = session.access(TestComponentDAO.class);

      assertThat(browseComponents(dao, repositoryId, null, 10, null), emptyIterable());

      dao.createComponent(component1, entityVersionEnabled);

      assertThat(browseComponents(dao, repositoryId, null, 10, null),
          contains(allOf(sameCoordinates(component1), sameKind(component1), sameAttributes(component1))));

      dao.createComponent(component2, entityVersionEnabled);
      dao.createComponent(component3, entityVersionEnabled);

      // browse all components
      Continuation<Component> components = browseComponents(dao, repositoryId, null, 10, null);
      assertThat(components, contains(
          allOf(sameCoordinates(component1), sameKind(component1), sameAttributes(component1)),
          allOf(sameCoordinates(component2), sameKind(component2), sameAttributes(component2)),
          allOf(sameCoordinates(component3), sameKind(component3), sameAttributes(component3))));

      assertEntityVersion(dao, component1, entityVersionEnabled ? 1 : null);
      assertEntityVersion(dao, component2, entityVersionEnabled ? 1 : null);
      assertEntityVersion(dao, component3, entityVersionEnabled ? 1 : null);

      // browse by kind
      assertThat(browseComponents(dao, repositoryId, anotherKind, 10, null),
          contains(allOf(sameCoordinates(component2), sameKind(component2), sameAttributes(component2)),
              allOf(sameCoordinates(component3), sameKind(component3), sameAttributes(component3))));

      session.getTransaction().commit();
    }

    // TRY CREATE AGAIN

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      ComponentDAO dao = session.access(TestComponentDAO.class);

      ComponentData duplicate = new ComponentData();
      duplicate.repositoryId = component1.repositoryId;
      duplicate.setNamespace(component1.namespace());
      duplicate.setName(component1.name());
      duplicate.setVersion(component1.version());
      duplicate.setNormalizedVersion(component1.normalizedVersion());
      duplicate.setKind(component1.kind());
      duplicate.setAttributes(component1.attributes());
      dao.createComponent(duplicate, entityVersionEnabled);

      session.getTransaction().commit();

      assertThat(browseComponents(dao, repositoryId, component1.kind(), 10, null).size(), is(1));
    }
    catch (DuplicateKeyException e) {
      logger.error("Got exception", e);
      fail("DuplicateKeyException exception shouldn't be thrown on duplicate create");
    }

    // READ

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      ComponentDAO dao = session.access(TestComponentDAO.class);

      assertFalse(dao.readCoordinate(repositoryId, "test-namespace", "test-name", "test-version").isPresent());

      tempResult = dao.readCoordinate(repositoryId, namespace1, name1, version1).get();
      assertThat(tempResult, sameCoordinates(component1));
      assertThat(tempResult, sameKind(component1));
      assertThat(tempResult, sameAttributes(component1));
      assertEntityVersion(tempResult, entityVersionEnabled ? 1 : null);

      tempResult = dao.readCoordinate(repositoryId, namespace2, name2, version2).get();
      assertThat(tempResult, sameCoordinates(component2));
      assertThat(tempResult, sameKind(component2));
      assertThat(tempResult, sameAttributes(component2));
      assertEntityVersion(tempResult, entityVersionEnabled ? 1 : null);

      tempResult =
          dao.readCoordinate(repositoryId, component3.namespace(), component3.name(), component3.version()).get();
      assertEntityVersion(tempResult, entityVersionEnabled ? 1 : null);
    }

    // UPDATE

    Thread.sleep(2); // NOSONAR make sure any new last updated times will be different

    // must use a new session as CURRENT_TIMESTAMP (used for last_updated) is fixed once used inside a session

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      ComponentDAO dao = session.access(TestComponentDAO.class);

      tempResult = dao.readCoordinate(repositoryId, namespace1, name1, version1).get();

      OffsetDateTime oldCreated = tempResult.created();
      OffsetDateTime oldLastUpdated = tempResult.lastUpdated();

      component1.attributes("custom-section-1").set("custom-key-1", "more-test-values-1");
      dao.updateComponentAttributes(component1, entityVersionEnabled);
      assertEntityVersion(dao, component1.componentId, entityVersionEnabled ? 2 : null);

      component1.setKind("new-kind-1");
      dao.updateComponentKind(component1, entityVersionEnabled);

      tempResult = dao.readCoordinate(repositoryId, namespace1, name1, version1).get();
      assertThat(tempResult, sameCoordinates(component1));
      assertThat(tempResult, sameKind(component1));
      assertThat(tempResult, sameAttributes(component1));
      assertEntityVersion(tempResult, entityVersionEnabled ? 3 : null);
      assertThat(tempResult.created(), is(oldCreated));
      assertTrue(tempResult.lastUpdated().isAfter(oldLastUpdated)); // should change as attributes have changed

      tempResult = dao.readCoordinate(repositoryId, namespace2, name2, version2).get();

      oldCreated = tempResult.created();
      oldLastUpdated = tempResult.lastUpdated();

      component2.componentId = null; // check a 'detached' entity with no internal id can be updated
      component2.attributes("custom-section-2").set("custom-key-2", "more-test-values-2");
      dao.updateComponentAttributes(component2, entityVersionEnabled);
      assertEntityVersion(dao.readCoordinate(repositoryId, namespace2, name2, version2).get(),
          entityVersionEnabled ? 2 : null);

      component2.setKind("new-kind-2");
      dao.updateComponentKind(component2, entityVersionEnabled);

      tempResult = dao.readCoordinate(repositoryId, namespace2, name2, version2).get();
      assertThat(tempResult, sameCoordinates(component2));
      assertThat(tempResult, sameKind(component2));
      assertThat(tempResult, sameAttributes(component2));
      assertEntityVersion(tempResult, entityVersionEnabled ? 3 : null);
      assertThat(tempResult.created(), is(oldCreated));
      assertTrue(tempResult.lastUpdated().isAfter(oldLastUpdated)); // should change as attributes have changed

      session.getTransaction().commit();
    }

    // UPDATE AGAIN

    Thread.sleep(2); // NOSONAR make sure any new last updated times will be different

    // must use a new session as CURRENT_TIMESTAMP (used for last_updated) is fixed once used inside a session

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      ComponentDAO dao = session.access(TestComponentDAO.class);

      tempResult = dao.readCoordinate(repositoryId, namespace1, name1, version1).get();

      OffsetDateTime oldCreated = tempResult.created();
      OffsetDateTime oldLastUpdated = tempResult.lastUpdated();

      component1.attributes("custom-section-1").set("custom-key-1", "more-test-values-again");
      dao.updateComponentAttributes(component1, entityVersionEnabled);

      tempResult = dao.readCoordinate(repositoryId, namespace1, name1, version1).get();
      assertThat(tempResult, sameCoordinates(component1));
      assertThat(tempResult, sameKind(component1));
      assertThat(tempResult, sameAttributes(component1));
      assertEntityVersion(tempResult, entityVersionEnabled ? 4 : null);
      assertThat(tempResult.created(), is(oldCreated));
      assertTrue(tempResult.lastUpdated().isAfter(oldLastUpdated)); // should change as attributes changed again

      tempResult = dao.readCoordinate(repositoryId, namespace2, name2, version2).get();

      oldCreated = tempResult.created();
      oldLastUpdated = tempResult.lastUpdated();

      dao.updateComponentAttributes(component2, entityVersionEnabled);

      tempResult = dao.readCoordinate(repositoryId, namespace2, name2, version2).get();
      assertThat(tempResult, sameCoordinates(component2));
      assertThat(tempResult, sameKind(component2));
      assertThat(tempResult, sameAttributes(component2));
      assertEntityVersion(tempResult, entityVersionEnabled ? 3 : null);

      assertThat(tempResult.created(), is(oldCreated));
      assertThat(tempResult.lastUpdated(), is(oldLastUpdated)); // won't have changed as attributes haven't changed

      session.getTransaction().commit();
    }

    // DELETE

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      ComponentDAO dao = session.access(TestComponentDAO.class);

      assertTrue(dao.deleteComponent(component1));

      Continuation<Component> components = browseComponents(dao, repositoryId, null, 10, null);
      assertThat(components,
          contains(allOf(sameCoordinates(component2), sameKind(component2), sameAttributes(component2)),
              allOf(sameCoordinates(component3), sameKind(component3), sameAttributes(component3))));

      assertEntityVersion(
          dao.readCoordinate(repositoryId, component2.namespace(), component2.name(), component2.version()).get(),
          entityVersionEnabled ? 3 : null);
      assertEntityVersion(
          dao.readCoordinate(repositoryId, component3.namespace(), component3.name(), component3.version()).get(),
          entityVersionEnabled ? 1 : null);

      assertThat(dao.deleteComponents(repositoryId, 0), greaterThan(0));

      assertThat(browseComponents(dao, repositoryId, null, 10, null), emptyIterable());

      ComponentData candidate = new ComponentData();
      candidate.setRepositoryId(repositoryId);
      candidate.setNamespace("test-namespace");
      candidate.setName("test-name");
      candidate.setVersion("test-version");
      assertFalse(dao.deleteComponent(candidate));
    }
  }

  protected void testBrowseComponentCoordinates() {

    // scatter components and assets
    generateRepositories(10);
    generateContent(10, 100, entityVersionEnabled);

    List<Component> browsedComponents = new ArrayList<>();

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      ComponentDAO dao = session.access(TestComponentDAO.class);

      assertThat(generatedRepositories().stream()
          .map(ContentRepositoryData::contentRepositoryId)
          .collect(summingInt(r -> countComponents(dao, r))), is(10));

      // now gather them back by browsing
      generatedRepositories().forEach(r -> dao.browseNamespaces(r.repositoryId)
          .forEach(ns -> dao.browseNames(r.repositoryId, ns)
              .forEach(n -> dao.browseVersions(r.repositoryId, ns, n)
                  .forEach(v -> browsedComponents.add(
                      dao.readCoordinate(r.repositoryId, ns, n, v).get())))));
    }

    // we should have the same components, but maybe in a different order
    // (use hamcrest class directly as javac picks the wrong static varargs method)
    assertThat(browsedComponents, new IsIterableContainingInAnyOrder<>(
        generatedComponents().stream()
            .map(ExampleContentTestSupport::sameCoordinates)
            .collect(toList())));
  }

  protected void testContinuationBrowsing() {

    generateNamespaces(1000);
    generateNames(1000);
    generateVersions(1000);
    generatePaths(10000);
    generateRepositories(1);
    generateContent(1000, 1000, entityVersionEnabled);

    repositoryId = generatedRepositories().get(0).repositoryId;

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      ComponentDAO dao = session.access(TestComponentDAO.class);

      assertThat(countComponents(dao, repositoryId), is(1000));

      int page = 0;

      Continuation<Component> components = browseComponents(dao, repositoryId, null, 10, null);
      while (!components.isEmpty()) {

        // verify we got the expected slice
        assertThat(components, new IsIterableContainingInOrder<>(
            generatedComponents()
                .subList(page * 10, (page + 1) * 10)
                .stream()
                .map(ExampleContentTestSupport::sameCoordinates)
                .collect(toList())));

        components = browseComponents(dao, repositoryId, null, 10, components.nextContinuationToken());
        assertSameEntityVersion(components, entityVersionEnabled ? 1 : null);

        page++;
      }

      assertThat(page, is(100));
    }
  }

  protected void testDeleteAllComponents() {

    // scatter components and assets
    generateRepositories(1);
    generateContent(100, 100, entityVersionEnabled);

    repositoryId = generatedRepositories().get(0).contentRepositoryId();

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      ComponentDAO dao = session.access(TestComponentDAO.class);

      assertThat(countComponents(dao, repositoryId), is(100));

      assertThat(browseComponents(dao, repositoryId, null, 100, null).size(), is(100));

      // must delete assets before we start deleting their components
      session.access(TestAssetDAO.class).deleteAssets(repositoryId, 100);

      dao.deleteComponents(repositoryId, 20);

      assertThat(browseComponents(dao, repositoryId, null, 100, null).size(), is(80));

      dao.deleteComponents(repositoryId, 10);

      assertThat(browseComponents(dao, repositoryId, null, 100, null).size(), is(70));

      dao.deleteComponents(repositoryId, 0);

      assertThat(browseComponents(dao, repositoryId, null, 100, null).size(), is(0));

      dao.deleteComponents(repositoryId, -1);

      assertThat(browseComponents(dao, repositoryId, null, 100, null).size(), is(0));
    }
  }

  protected void testPurgeOperation() {
    ComponentData component1 = generateComponent(repositoryId, "namespace1", "name1", "1.0.0");
    ComponentData component2 = generateComponent(repositoryId, "namespace2", "name2", "2.0.0");
    component2.setVersion(component1.version() + ".2"); // make sure versions are different

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      ComponentDAO dao = session.access(TestComponentDAO.class);
      dao.createComponent(component1, entityVersionEnabled);
      dao.createComponent(component2, entityVersionEnabled);
      session.getTransaction().commit();
    }

    AssetData asset1 = generateAsset(repositoryId, "/asset1/asset1.jar");
    AssetData asset2 = generateAsset(repositoryId, "/asset2/asset2.jar");
    asset2.setPath(asset1.path() + "/2"); // make sure paths are different

    asset1.setComponent(component1);
    asset1.setLastDownloaded(UTC.now().minusDays(2));
    asset2.setComponent(component2);
    asset2.setLastDownloaded(UTC.now().minusDays(4));

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetDAO dao = session.access(TestAssetDAO.class);
      dao.createAsset(asset1, entityVersionEnabled);
      dao.createAsset(asset2, entityVersionEnabled);
      session.getTransaction().commit();
    }

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      ComponentDAO componentDao = session.access(TestComponentDAO.class);
      AssetDAO assetDao = session.access(TestAssetDAO.class);

      Optional<Component> component = componentDao.readCoordinate(repositoryId,
          component1.namespace(), component1.name(), component1.version());
      assertTrue(component.isPresent());
      assertEntityVersion(component.get(), entityVersionEnabled ? 2 : null);

      component = componentDao.readCoordinate(repositoryId,
          component2.namespace(), component2.name(), component2.version());
      assertTrue(component.isPresent());
      assertEntityVersion(component.get(), entityVersionEnabled ? 2 : null);

      assertTrue(assetDao.readPath(repositoryId, asset1.path()).isPresent());
      assertTrue(assetDao.readPath(repositoryId, asset2.path()).isPresent());

      int[] componentIds = componentDao.selectNotRecentlyDownloaded(repositoryId, 3, 10);
      assertThat(componentIds, is(new int[]{2}));

      if ("H2".equals(session.sqlDialect())) {
        componentDao.purgeSelectedComponents(stream(componentIds).boxed().toArray(Integer[]::new));
      }
      else {
        componentDao.purgeSelectedComponents(componentIds);
      }

      component = componentDao.readCoordinate(repositoryId,
          component1.namespace(), component1.name(), component1.version());
      assertTrue(component.isPresent());
      assertEntityVersion(component.get(), entityVersionEnabled ? 2 : null);

      assertFalse(componentDao.readCoordinate(repositoryId,
          component2.namespace(), component2.name(), component2.version()).isPresent());

      assertTrue(assetDao.readPath(repositoryId, asset1.path()).isPresent());
      assertFalse(assetDao.readPath(repositoryId, asset2.path()).isPresent());
    }
  }

  protected void testRoundTrip() {
    ComponentData component1 = generateComponent(repositoryId, "namespace1", "name1", "1.0.0");
    ComponentData component2 = generateComponent(repositoryId, "namespace2", "name2", "2.0.0");
    component2.setVersion(component1.version() + ".2"); // make sure versions are different

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      ComponentDAO dao = session.access(TestComponentDAO.class);
      dao.createComponent(component1, entityVersionEnabled);
      dao.createComponent(component2, entityVersionEnabled);
      session.getTransaction().commit();
    }

    Component tempResult;

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      ComponentDAO dao = session.access(TestComponentDAO.class);

      tempResult = dao.readComponent(component1.componentId).get();
      assertThat(tempResult, sameCoordinates(component1));
      assertThat(tempResult, sameKind(component1));
      assertThat(tempResult, sameAttributes(component1));
      assertEntityVersion(tempResult, entityVersionEnabled ? 1 : null);

      tempResult = dao.readComponent(component2.componentId).get();
      assertThat(tempResult, sameCoordinates(component2));
      assertThat(tempResult, sameKind(component2));
      assertThat(tempResult, sameAttributes(component2));
      assertEntityVersion(tempResult, entityVersionEnabled ? 1 : null);
    }
  }

  protected void testBrowseComponentsInRepositories() {
    ContentRepositoryData anotherContentRepository = generateContentRepository();
    createContentRepository(anotherContentRepository);
    int anotherRepositoryId = anotherContentRepository.repositoryId;

    ComponentData component1 = generateComponent(repositoryId, "namespace1", "name1", "1.0.0");
    ComponentData component2 = generateComponent(anotherRepositoryId, "namespace2", "name2", "2.0.0");

    String akind = "kind1";
    String anotherKind = "kind2";
    component1.setNamespace("");
    component1.setVersion("1.1");
    component1.setKind(akind);
    component2.setNamespace("demo");
    component2.setVersion("1.2");
    component2.setKind(anotherKind);

    // CREATE

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      ComponentDAO dao = session.access(TestComponentDAO.class);

      assertThat(
          dao.browseComponentsInRepositories(Set.of(repositoryId, anotherRepositoryId), 10, null),
          emptyIterable());

      dao.createComponent(component1, entityVersionEnabled);

      Continuation<Component> components =
          dao.browseComponentsInRepositories(Set.of(repositoryId, anotherRepositoryId), 10, null);
      assertThat(components,
          contains(allOf(sameCoordinates(component1), sameKind(component1),
              sameAttributes(component1))));

      assertEntityVersion(dao, component1, entityVersionEnabled ? 1 : null);

      dao.createComponent(component2, entityVersionEnabled);

      // browse all components
      components = dao.browseComponentsInRepositories(Set.of(repositoryId, anotherRepositoryId), 10, null);
      assertThat(
          components,
          contains(allOf(sameCoordinates(component1), sameKind(component1), sameAttributes(component1)),
              allOf(sameCoordinates(component2), sameKind(component2), sameAttributes(component2))));
      assertSameEntityVersion(components, entityVersionEnabled ? 1 : null);

      session.getTransaction().commit();
    }
  }

  protected void testFilterClauseIsolation() {
    ContentRepositoryData anotherContentRepository = generateContentRepository();
    createContentRepository(anotherContentRepository);
    int anotherRepositoryId = anotherContentRepository.repositoryId;
    ComponentData component1 = generateComponent(repositoryId, "namespace1", "name1", "1.0.0");
    ComponentData component2 = generateComponent(anotherRepositoryId, "namespace2", "name2", "2.0.0");

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      ComponentDAO dao = session.access(TestComponentDAO.class);

      dao.createComponent(component1, entityVersionEnabled);
      dao.createComponent(component2, entityVersionEnabled);

      Continuation<Component> components =
          dao.browseComponents(repositoryId, 1000, null, null, "true or true", null);
      assertThat(components, hasSize(1));
      assertThat(dao.countComponents(repositoryId, null, "true or true", null), equalTo(1));

      assertSameEntityVersion(components, entityVersionEnabled ? 1 : null);
    }
  }

  protected void testContinuationSetBrowsing() {

    final int namespaceCount = 4;
    final int nameCount = 4;
    final int versionCount = 145;
    final int pageLimit = 13;

    generateNamespaces(namespaceCount);
    generateNames(nameCount);
    generateVersions(versionCount);
    generatePaths(500);
    generateRepositories(1);

    repositoryId = generatedRepositories().get(0).repositoryId;

    Set<ComponentSet> sets = new HashSet<>();
    Map<ComponentSet, List<ComponentData>> generatedComponents = new HashMap<>();

    List<ComponentData> components = new ArrayList<>();
    generatedNamespaces().forEach(namespace -> {
      generatedNames().forEach(name -> {

        ComponentSet set = createHashableComponentSetData(namespace, name);
        sets.add(set);
        List<ComponentData> setComponents = new ArrayList<>();
        generatedVersions().forEach(version -> {
          setComponents.add(generateComponent(repositoryId, namespace, name, version));
        });
        components.addAll(setComponents);
        generatedComponents.put(set, setComponents);
      });
      generateProvidedContent(components, entityVersionEnabled);
    });

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      ComponentDAO dao = session.access(TestComponentDAO.class);

      assertThat(countComponents(dao, repositoryId), is(namespaceCount * nameCount * versionCount));

      Continuation<ComponentSetData> browseSets = browseSets(dao, repositoryId, 0, null);
      assertEquals(sets.size(), browseSets.size());

      browseSets.stream().map(set -> createHashableComponentSetData(set.namespace, set.name)).forEach(set -> {
        List<Component> retrievedComponents = new ArrayList<>();
        Continuation<Component> setComponents =
            browseComponentsBySet(dao, repositoryId, set.namespace(), set.name(), pageLimit, null);
        retrievedComponents.addAll(setComponents);
        assertTrue(setComponents.size() > 0);
        while (!setComponents.isEmpty()) {
          // verify we got the expected slice

          setComponents = browseComponentsBySet(dao, repositoryId, set.namespace(), set.name(), pageLimit,
              setComponents.nextContinuationToken());
          retrievedComponents.addAll(setComponents);
          assertSameEntityVersion(setComponents, entityVersionEnabled ? 1 : null);
        }
        assertThat(retrievedComponents, new IsIterableContainingInAnyOrder<>(
            generatedComponents.get(set)
                .stream()
                .map(ExampleContentTestSupport::sameCoordinates)
                .collect(toList())));
      });

    }
  }

  protected void testNormalizationMethods() {
    ContentRepositoryData contentRepository = generateContentRepository();
    createContentRepository(contentRepository);
    ComponentData component1 =
        generateComponent(contentRepository.repositoryId, "namespace.one", "artifact-1", "1.0.0");
    ComponentData component2 =
        generateComponent(contentRepository.repositoryId, "namespace.two", "artifact-2", "2.0.0");

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      ComponentDAO dao = session.access(TestComponentDAO.class);

      dao.createComponent(component1, entityVersionEnabled);
      dao.createComponent(component2, entityVersionEnabled);

      // unnormalized should be empty/0 , since there is no unnormalized records
      Continuation<ComponentData> unnormalizedBrowse = dao.browseUnnormalized(10, null);
      assertThat(unnormalizedBrowse, hasSize(0));

      int count = dao.countUnnormalized();
      assertThat(count, is(0));

      // updating normalized_version
      component1.setNormalizedVersion("0000000001.0000000000.0000000000.a");
      component2.setNormalizedVersion("0000000001.0000000000.0000000001.b");

      dao.updateComponentNormalizedVersion(component1, entityVersionEnabled);
      dao.updateComponentNormalizedVersion(component2, entityVersionEnabled);

      assertNormalizedVersion(dao, component1, "0000000001.0000000000.0000000000.a");
      assertNormalizedVersion(dao, component2, "0000000001.0000000000.0000000001.b");
    }
  }

  private void assertNormalizedVersion(
      final ComponentDAO dao,
      final ComponentData component,
      final String expectedNormalizedVersion)
  {
    Optional<Component> maybeComponent =
        dao.readCoordinate(component.repositoryId, component.namespace(), component.name(), component.version());

    assertTrue(maybeComponent.isPresent());
    assertThat(maybeComponent.get().normalizedVersion(), is(expectedNormalizedVersion));
  }

  // This overrides the necessary methods to allow effective hashing for sets. Ideally this would be included
  // within the base class, but as none of the DAO objects currently implement hashcode and rely on object reference
  // for equality, doing so would then require overrides in all subclasses and potentially cause other problems.
  protected ComponentSetData createHashableComponentSetData(final String namespace, final String name) {
    ComponentSetData set = new ComponentSetData()
    {
      @Override
      public boolean equals(final Object o) {
        if (this == o) {
          return true;
        }
        if (o == null || getClass() != o.getClass()) {
          return false;
        }
        ComponentSetData that = (ComponentSetData) o;
        return Objects.equals(namespace, that.namespace) && Objects.equals(name, that.name);
      }

      @Override
      public int hashCode() {
        return Objects.hash(namespace, name);
      }
    };
    set.setNamespace(namespace);
    set.setName(name);
    return set;
  }

  protected void createContentRepository(final ContentRepositoryData contentRepository) {
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      ContentRepositoryDAO dao = session.access(TestContentRepositoryDAO.class);
      dao.createContentRepository(contentRepository);
      session.getTransaction().commit();
    }
  }

  static int countComponents(final ComponentDAO dao, final int repositoryId) {
    return dao.countComponents(repositoryId, null, null, null);
  }

  static Continuation<Component> browseComponents(
      final ComponentDAO dao,
      final int repositoryId,
      final String kind,
      final int limit,
      final String continuationToken)
  {
    return dao.browseComponents(repositoryId, limit, continuationToken, kind, null, null);
  }

  static Continuation<Component> browseComponentsBySet(
      final ComponentDAO dao,
      final int repositoryId,
      final String namespace,
      final String name,
      final int limit,
      final String continuationToken)
  {
    return dao.browseComponentsBySet(repositoryId, namespace, name, limit, continuationToken);
  }

  static Continuation<ComponentSetData> browseSets(
      final ComponentDAO dao,
      final int repositoryId,
      final int limit,
      final String continuationToken)
  {
    return dao.browseSets(repositoryId, limit, continuationToken);
  }

  /**
   * Test browseVersionsByRepoIds method which retrieves all versions of a component
   * across multiple repositories. This is particularly useful for group repositories
   * where we need to aggregate versions from all member repositories.
   */
  protected void testBrowseVersionsByRepoIds() {
    ContentRepositoryData contentRepository1 = generateContentRepository();
    ContentRepositoryData contentRepository2 = generateContentRepository();
    ContentRepositoryData contentRepository3 = generateContentRepository();

    createContentRepository(contentRepository1);
    createContentRepository(contentRepository2);
    createContentRepository(contentRepository3);

    int repo1Id = contentRepository1.repositoryId;
    int repo2Id = contentRepository2.repositoryId;
    int repo3Id = contentRepository3.repositoryId;

    String namespace = "org.example";
    String name = "my-component";

    // Create components with same namespace/name but different versions across repositories
    ComponentData component1Repo1 = generateComponent(repo1Id, namespace, name, "1.0.0");
    ComponentData component2Repo1 = generateComponent(repo1Id, namespace, name, "2.0.0");
    ComponentData component1Repo2 = generateComponent(repo2Id, namespace, name, "1.5.0");
    ComponentData component2Repo2 = generateComponent(repo2Id, namespace, name, "3.0.0");
    ComponentData component1Repo3 = generateComponent(repo3Id, namespace, name, "2.5.0");

    // Create a different component to verify filtering works correctly
    ComponentData differentComponent = generateComponent(repo1Id, "different.namespace", "different-name", "9.9.9");

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      ComponentDAO dao = session.access(TestComponentDAO.class);

      // Test: Empty result when no components exist
      Collection<String> versions = dao.browseVersionsByRepoIds(namespace, name, Set.of(repo1Id, repo2Id));
      assertThat(versions, emptyIterable());

      // Create components
      dao.createComponent(component1Repo1, entityVersionEnabled);
      dao.createComponent(component2Repo1, entityVersionEnabled);
      dao.createComponent(component1Repo2, entityVersionEnabled);
      dao.createComponent(component2Repo2, entityVersionEnabled);
      dao.createComponent(component1Repo3, entityVersionEnabled);
      dao.createComponent(differentComponent, entityVersionEnabled);

      session.getTransaction().commit();

      // Test: Browse versions from single repository
      versions = dao.browseVersionsByRepoIds(namespace, name, Set.of(repo1Id));
      assertThat(versions, hasSize(2));
      assertThat(versions, IsIterableContainingInAnyOrder.containsInAnyOrder("1.0.0", "2.0.0"));

      // Test: Browse versions from two repositories
      versions = dao.browseVersionsByRepoIds(namespace, name, Set.of(repo1Id, repo2Id));
      assertThat(versions, hasSize(4));
      assertThat(versions, IsIterableContainingInAnyOrder.containsInAnyOrder("1.0.0", "2.0.0", "1.5.0", "3.0.0"));

      // Test: Browse versions from all three repositories
      versions = dao.browseVersionsByRepoIds(namespace, name, Set.of(repo1Id, repo2Id, repo3Id));
      assertThat(versions, hasSize(5));
      assertThat(versions, IsIterableContainingInAnyOrder.containsInAnyOrder(
          "1.0.0", "2.0.0", "1.5.0", "3.0.0", "2.5.0"));

      // Test: Browse versions from repository that doesn't have the component
      versions = dao.browseVersionsByRepoIds(namespace, name, Set.of(repo3Id));
      assertThat(versions, hasSize(1));
      assertThat(versions, contains("2.5.0"));

      // Test: Non-existent component returns empty collection
      versions = dao.browseVersionsByRepoIds("non.existent", "component", Set.of(repo1Id, repo2Id));
      assertThat(versions, emptyIterable());

      // Test: Different component is not included in results
      versions = dao.browseVersionsByRepoIds(namespace, name, Set.of(repo1Id));
      assertThat(versions, hasSize(2));
      assertFalse(versions.contains(differentComponent.version()));
    }
  }

  /**
   * Test readCoordinateInRepoIds method which retrieves a component by its coordinate
   * (namespace, name, version) across multiple repositories. This is particularly useful
   * for group repositories where we need to find a specific component version from member repositories.
   */
  protected void testReadCoordinateInRepoIds() {
    ContentRepositoryData contentRepository1 = generateContentRepository();
    ContentRepositoryData contentRepository2 = generateContentRepository();
    ContentRepositoryData contentRepository3 = generateContentRepository();

    createContentRepository(contentRepository1);
    createContentRepository(contentRepository2);
    createContentRepository(contentRepository3);

    int repo1Id = contentRepository1.repositoryId;
    int repo2Id = contentRepository2.repositoryId;
    int repo3Id = contentRepository3.repositoryId;

    String namespace = "org.example";
    String name = "my-component";
    String version = "1.0.0";

    // Create same component in multiple repositories
    ComponentData componentRepo1 = generateComponent(repo1Id, namespace, name, version);
    ComponentData componentRepo2 = generateComponent(repo2Id, namespace, name, version);

    // Create a different component to verify filtering works correctly
    ComponentData differentComponent = generateComponent(repo3Id, "different.namespace", "different-name", "9.9.9");

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      ComponentDAO dao = session.access(TestComponentDAO.class);

      // Test: Empty result when no components exist
      Optional<Component> result = dao.readCoordinateInRepoIds(namespace, name, version, Set.of(repo1Id, repo2Id));
      assertFalse(result.isPresent());

      // Create components
      dao.createComponent(componentRepo1, entityVersionEnabled);
      dao.createComponent(componentRepo2, entityVersionEnabled);
      dao.createComponent(differentComponent, entityVersionEnabled);

      session.getTransaction().commit();

      // Test: Find component in single repository
      result = dao.readCoordinateInRepoIds(namespace, name, version, Set.of(repo1Id));
      assertTrue(result.isPresent());
      assertThat(result.get(), sameCoordinates(componentRepo1));

      // Test: Find component across multiple repositories (returns one)
      result = dao.readCoordinateInRepoIds(namespace, name, version, Set.of(repo1Id, repo2Id));
      assertTrue(result.isPresent());
      // Should match coordinates (either repo1 or repo2 component due to DISTINCT ON)
      assertEquals(namespace, result.get().namespace());
      assertEquals(name, result.get().name());
      assertEquals(version, result.get().version());

      // Test: Component not found in specified repo IDs
      result = dao.readCoordinateInRepoIds(namespace, name, version, Set.of(repo3Id));
      assertFalse(result.isPresent());

      // Test: Non-existent component returns empty
      result = dao.readCoordinateInRepoIds("non.existent", "component", "1.0.0", Set.of(repo1Id, repo2Id));
      assertFalse(result.isPresent());

      // Test: Different component is not matched
      result = dao.readCoordinateInRepoIds(
          differentComponent.namespace(),
          differentComponent.name(),
          differentComponent.version(),
          Set.of(repo3Id));
      assertTrue(result.isPresent());
      assertThat(result.get(), sameCoordinates(differentComponent));
    }
  }

  /**
   * Test browseComponentsEager which uses inline subquery instead of CTE.
   * This test ensures the query works correctly with H2 database.
   */
  protected void testBrowseComponentsEager() {
    ContentRepositoryData contentRepository1 = generateContentRepository();
    ContentRepositoryData contentRepository2 = generateContentRepository();

    createContentRepository(contentRepository1);
    createContentRepository(contentRepository2);

    int repo1Id = contentRepository1.repositoryId;
    int repo2Id = contentRepository2.repositoryId;

    ComponentData component1 = generateComponent(repo1Id, "namespace1", "name1", "1.0.0");
    ComponentData component2 = generateComponent(repo1Id, "namespace2", "name2", "2.0.0");
    ComponentData component3 = generateComponent(repo2Id, "namespace3", "name3", "3.0.0");
    ComponentData component4 = generateComponent(repo2Id, "namespace4", "name4", "4.0.0");

    component1.setKind("kind1");
    component2.setKind("kind2");
    component3.setKind("kind1");
    component4.setKind("kind2");

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      ComponentDAO componentDao = session.access(TestComponentDAO.class);
      TestAssetDAO assetDao = session.access(TestAssetDAO.class);

      // Create components and associated assets
      componentDao.createComponent(component1, entityVersionEnabled);
      componentDao.createComponent(component2, entityVersionEnabled);
      componentDao.createComponent(component3, entityVersionEnabled);
      componentDao.createComponent(component4, entityVersionEnabled);

      // Create some assets for each component to test eager loading
      AssetData asset1 = generateAsset(repo1Id, "/path1");
      asset1.componentId = component1.componentId;
      assetDao.createAsset(asset1, entityVersionEnabled);

      AssetData asset2 = generateAsset(repo1Id, "/path2");
      asset2.componentId = component2.componentId;
      assetDao.createAsset(asset2, entityVersionEnabled);

      AssetData asset3 = generateAsset(repo2Id, "/path3");
      asset3.componentId = component3.componentId;
      assetDao.createAsset(asset3, entityVersionEnabled);

      session.getTransaction().commit();

      // Test: Browse components from both repositories
      Continuation<ComponentData> result = componentDao.browseComponentsEager(
          Set.of(repo1Id, repo2Id), 10, null, null, null, null);

      assertThat(result, hasSize(4));
      List<ComponentData> components = new ArrayList<>();
      result.forEach(components::add);

      // Verify all components are returned
      assertThat(components, hasSize(4));

      // Test: Browse with kind filter
      result = componentDao.browseComponentsEager(
          Set.of(repo1Id, repo2Id), 10, null, "kind1", null, null);

      List<ComponentData> kind1Components = new ArrayList<>();
      result.forEach(kind1Components::add);
      assertThat(kind1Components, hasSize(2));

      // Test: Browse with limit
      result = componentDao.browseComponentsEager(
          Set.of(repo1Id, repo2Id), 2, null, null, null, null);

      List<ComponentData> limitedComponents = new ArrayList<>();
      result.forEach(limitedComponents::add);
      assertThat(limitedComponents, hasSize(2));

      // Test: Browse with continuation token
      String continuationToken = limitedComponents.get(1).componentId.toString();
      result = componentDao.browseComponentsEager(
          Set.of(repo1Id, repo2Id), 10, continuationToken, null, null, null);

      List<ComponentData> continuedComponents = new ArrayList<>();
      result.forEach(continuedComponents::add);
      assertThat(continuedComponents, hasSize(2));

      // Test: Browse single repository
      result = componentDao.browseComponentsEager(
          Set.of(repo1Id), 10, null, null, null, null);

      List<ComponentData> repo1Components = new ArrayList<>();
      result.forEach(repo1Components::add);
      assertThat(repo1Components, hasSize(2));

      // Verify assets are eagerly loaded
      repo1Components.forEach(component -> {
        if (component instanceof ComponentData) {
          ComponentData compData = (ComponentData) component;
          assertThat(compData.getAssets(), is(org.hamcrest.Matchers.notNullValue()));
        }
      });
    }
  }

  protected void testCreateComponentReturnsNullIdWhenRepositoryDeleted() {
    ContentRepositoryData contentRepository = generateContentRepository();
    createContentRepository(contentRepository);
    int deletedRepositoryId = contentRepository.repositoryId;

    // Delete the content repository to simulate a concurrent repository deletion
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      session.access(TestContentRepositoryDAO.class).deleteContentRepository(contentRepository);
      session.getTransaction().commit();
    }

    // On PostgreSQL the lock_repo CTE finds no row → WHERE EXISTS false → 0 rows inserted → componentId stays null
    ComponentData component = generateComponent(deletedRepositoryId, "test-ns", "deleted-repo-component", "1.0");
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      session.access(TestComponentDAO.class).createComponent(component, entityVersionEnabled);
      session.getTransaction().commit();
    }

    assertThat(component.componentId(), is(nullValue()));
  }

  private static void assertEntityVersion(
      final ComponentDAO dao,
      final ComponentData component,
      final Integer expectedEntityVersion)
  {
    Optional<Component> result =
        dao.readCoordinate(component.repositoryId, component.namespace(), component.name(), component.version());
    assertThat(result.isPresent(), is(true));
    assertThat(result.get().entityVersion(), is(expectedEntityVersion));
  }

  private static void assertEntityVersion(
      final ComponentDAO dao,
      final Integer componentId,
      final Integer expectedEntityVersion)
  {
    Optional<Component> component = dao.readComponent(componentId);
    assertThat(component.isPresent(), is(true));
    assertThat(component.get().entityVersion(), is(expectedEntityVersion));
  }

  private static void assertSameEntityVersion(
      final Collection<Component> components,
      final Integer expectedEntityVersion)
  {
    assertThat(components.stream()
        .map(Component::entityVersion)
        .allMatch(entityVersion -> Objects.equals(entityVersion, expectedEntityVersion)), is(true));
  }

  private static void assertEntityVersion(final Component component, final Integer expectedEntityVersion) {
    assertThat(component.entityVersion(), is(expectedEntityVersion));
  }
}
