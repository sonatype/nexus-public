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

import java.util.ArrayList;
import java.util.List;

import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.store.example.TestAssetDAO;
import org.sonatype.nexus.repository.content.store.example.TestComponentDAO;
import org.sonatype.nexus.repository.content.store.example.TestContentRepositoryDAO;

import org.apache.ibatis.exceptions.PersistenceException;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.hamcrest.collection.IsIterableContainingInOrder;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test {@link ComponentDAO}.
 */
public class ComponentDAOTest
    extends ExampleContentTestSupport
{
  private ContentRepositoryData contentRepository;

  private int repositoryId;

  @Before
  public void setupContent() {
    contentRepository = randomContentRepository();

    try (DataSession<?> session = sessionRule.openSession("content")) {
      ContentRepositoryDAO dao = session.access(TestContentRepositoryDAO.class);
      dao.createContentRepository(contentRepository);
      session.getTransaction().commit();
    }

    repositoryId = contentRepository.repositoryId;

    generateRandomNamespaces(100);
    generateRandomNames(100);
    generateRandomVersions(100);
    generateRandomPaths(100);
  }

  @Test
  public void testCrudOperations() throws InterruptedException {

    ComponentData component1 = randomComponent(repositoryId);
    ComponentData component2 = randomComponent(repositoryId);

    // cover with/without namespace and different versions
    component1.setNamespace("");
    component1.setVersion("1.1");
    component2.setNamespace("demo");
    component2.setVersion("1.2");

    String namespace1 = component1.namespace();
    String name1 = component1.name();
    String version1 = component1.version();

    String namespace2 = component2.namespace();
    String name2 = component2.name();
    String version2 = component2.version();

    Component tempResult;

    // CREATE

    try (DataSession<?> session = sessionRule.openSession("content")) {
      ComponentDAO dao = session.access(TestComponentDAO.class);

      assertThat(dao.browseComponents(repositoryId, 10, null), emptyIterable());

      dao.createComponent(component1);

      assertThat(dao.browseComponents(repositoryId, 10, null),
          contains(allOf(sameCoordinates(component1), sameAttributes(component1))));

      dao.createComponent(component2);

      assertThat(dao.browseComponents(repositoryId, 10, null),
          contains(allOf(sameCoordinates(component1), sameAttributes(component1)),
              allOf(sameCoordinates(component2), sameAttributes(component2))));

      session.getTransaction().commit();
    }

    // TRY CREATE AGAIN

    try (DataSession<?> session = sessionRule.openSession("content")) {
      ComponentDAO dao = session.access(TestComponentDAO.class);

      ComponentData duplicate = new ComponentData();
      duplicate.repositoryId = component1.repositoryId;
      duplicate.setNamespace(component1.namespace());
      duplicate.setName(component1.name());
      duplicate.setVersion(component1.version());
      duplicate.setAttributes(newAttributes("duplicate"));
      dao.createComponent(duplicate);

      session.getTransaction().commit();
      fail("Cannot create the same component twice");
    }
    catch (PersistenceException e) {
      logger.debug("Got expected exception", e);
    }

    // READ

    try (DataSession<?> session = sessionRule.openSession("content")) {
      ComponentDAO dao = session.access(TestComponentDAO.class);

      assertFalse(dao.readComponent(repositoryId, "test-namespace", "test-name", "test-version").isPresent());

      tempResult = dao.readComponent(repositoryId, namespace1, name1, version1).get();
      assertThat(tempResult, sameCoordinates(component1));
      assertThat(tempResult, sameAttributes(component1));

      tempResult = dao.readComponent(repositoryId, namespace2, name2, version2).get();
      assertThat(tempResult, sameCoordinates(component2));
      assertThat(tempResult, sameAttributes(component2));
    }

    // UPDATE

    Thread.sleep(2); // NOSONAR make sure any new last updated times will be different

    // must use a new session as CURRENT_TIMESTAMP (used for last_updated) is fixed once used inside a session

    try (DataSession<?> session = sessionRule.openSession("content")) {
      ComponentDAO dao = session.access(TestComponentDAO.class);

      tempResult = dao.readComponent(repositoryId, namespace1, name1, version1).get();

      DateTime oldCreated = tempResult.created();
      DateTime oldLastUpdated = tempResult.lastUpdated();

      component1.attributes("custom-section-1").set("custom-key-1", "more-test-values-1");
      dao.updateComponentAttributes(component1);

      tempResult = dao.readComponent(repositoryId, namespace1, name1, version1).get();
      assertThat(tempResult, sameCoordinates(component1));
      assertThat(tempResult, sameAttributes(component1));
      assertThat(tempResult.created(), is(oldCreated));
      assertTrue(tempResult.lastUpdated().isAfter(oldLastUpdated)); // should change as attributes have changed

      tempResult = dao.readComponent(repositoryId, namespace2, name2, version2).get();

      oldCreated = tempResult.created();
      oldLastUpdated = tempResult.lastUpdated();

      component2.componentId = null; // check a 'detached' entity with no internal id can be updated
      component2.attributes("custom-section-2").set("custom-key-2", "more-test-values-2");
      dao.updateComponentAttributes(component2);

      tempResult = dao.readComponent(repositoryId, namespace2, name2, version2).get();
      assertThat(tempResult, sameCoordinates(component2));
      assertThat(tempResult, sameAttributes(component2));
      assertThat(tempResult.created(), is(oldCreated));
      assertTrue(tempResult.lastUpdated().isAfter(oldLastUpdated)); // should change as attributes have changed

      session.getTransaction().commit();
    }

    // UPDATE AGAIN

    Thread.sleep(2); // NOSONAR make sure any new last updated times will be different

    // must use a new session as CURRENT_TIMESTAMP (used for last_updated) is fixed once used inside a session

    try (DataSession<?> session = sessionRule.openSession("content")) {
      ComponentDAO dao = session.access(TestComponentDAO.class);

      tempResult = dao.readComponent(repositoryId, namespace1, name1, version1).get();

      DateTime oldCreated = tempResult.created();
      DateTime oldLastUpdated = tempResult.lastUpdated();

      component1.attributes("custom-section-1").set("custom-key-1", "more-test-values-again");
      dao.updateComponentAttributes(component1);

      tempResult = dao.readComponent(repositoryId, namespace1, name1, version1).get();
      assertThat(tempResult, sameCoordinates(component1));
      assertThat(tempResult, sameAttributes(component1));
      assertThat(tempResult.created(), is(oldCreated));
      assertTrue(tempResult.lastUpdated().isAfter(oldLastUpdated)); // should change as attributes changed again

      tempResult = dao.readComponent(repositoryId, namespace2, name2, version2).get();

      oldCreated = tempResult.created();
      oldLastUpdated = tempResult.lastUpdated();

      dao.updateComponentAttributes(component2);

      tempResult = dao.readComponent(repositoryId, namespace2, name2, version2).get();
      assertThat(tempResult, sameCoordinates(component2));
      assertThat(tempResult, sameAttributes(component2));
      assertThat(tempResult.created(), is(oldCreated));
      assertThat(tempResult.lastUpdated(), is(oldLastUpdated)); // won't have changed as attributes haven't changed

      session.getTransaction().commit();
    }

    // DELETE

    try (DataSession<?> session = sessionRule.openSession("content")) {
      ComponentDAO dao = session.access(TestComponentDAO.class);

      assertTrue(dao.deleteComponent(component1));

      assertThat(dao.browseComponents(repositoryId, 10, null),
          contains(allOf(sameCoordinates(component2), sameAttributes(component2))));

      assertTrue(dao.deleteComponents(repositoryId, 0));

      assertThat(dao.browseComponents(repositoryId, 10, null), emptyIterable());

      assertFalse(dao.deleteCoordinate(repositoryId, "test-namespace", "test-name", "test-version"));
    }
  }

  @Test
  public void testBrowseComponentCoordinates() {

    // scatter components and assets
    generateRandomRepositories(10);
    generateRandomContent(10, 100);

    List<Component> browsedComponents = new ArrayList<>();

    try (DataSession<?> session = sessionRule.openSession("content")) {
      ComponentDAO dao = session.access(TestComponentDAO.class);

      // now gather them back by browsing
      generatedRepositories().forEach(r ->
          dao.browseNamespaces(r.repositoryId).forEach(ns ->
              dao.browseNames(r.repositoryId, ns).forEach(n ->
                  dao.browseVersions(r.repositoryId, ns, n).forEach(v ->
                      browsedComponents.add(
                          dao.readComponent(r.repositoryId, ns, n, v).get())
      ))));
    }

    // we should have the same components, but maybe in a different order
    // (use hamcrest class directly as javac picks the wrong static varargs method)
    assertThat(browsedComponents, new IsIterableContainingInAnyOrder<>(
        generatedComponents().stream()
        .map(ExampleContentTestSupport::sameCoordinates)
        .collect(toList())));
  }

  @Test
  public void testContinuationBrowsing() {

    generateRandomNamespaces(1000);
    generateRandomNames(1000);
    generateRandomVersions(1000);
    generateRandomPaths(10000);
    generateRandomRepositories(1);
    generateRandomContent(1000, 1000);

    repositoryId = generatedRepositories().get(0).repositoryId;

    try (DataSession<?> session = sessionRule.openSession("content")) {
      ComponentDAO dao = session.access(TestComponentDAO.class);

      int page = 0;

      Continuation<Component> components = dao.browseComponents(repositoryId, 10, null);
      while (!components.isEmpty()) {

        // verify we got the expected slice
        assertThat(components, new IsIterableContainingInOrder<>(
            generatedComponents()
                .subList(page * 10, (page + 1) * 10)
                .stream()
                .map(ExampleContentTestSupport::sameCoordinates)
                .collect(toList())));

        components = dao.browseComponents(repositoryId, 10, components.nextContinuationToken());

        page++;
      }

      assertThat(page, is(100));
    }
  }
  @Test
  public void testDeleteAllComponents() {

    // scatter components and assets
    generateRandomRepositories(1);
    generateRandomContent(100, 100);

    repositoryId = generatedRepositories().get(0).contentRepositoryId();

    try (DataSession<?> session = sessionRule.openSession("content")) {
      ComponentDAO dao = session.access(TestComponentDAO.class);

      assertThat(dao.browseComponents(repositoryId, 100, null).size(), is(100));

      // must delete assets before we start deleting their components
      session.access(TestAssetDAO.class).deleteAssets(repositoryId, 100);

      dao.deleteComponents(repositoryId, 20);

      assertThat(dao.browseComponents(repositoryId, 100, null).size(), is(80));

      dao.deleteComponents(repositoryId, 10);

      assertThat(dao.browseComponents(repositoryId, 100, null).size(), is(70));

      dao.deleteComponents(repositoryId, 0);

      assertThat(dao.browseComponents(repositoryId, 100, null).size(), is(0));

      dao.deleteComponents(repositoryId, -1);

      assertThat(dao.browseComponents(repositoryId, 100, null).size(), is(0));
    }
  }
}
