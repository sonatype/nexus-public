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
package org.sonatype.nexus.repository.content.kv;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import org.assertj.db.type.AssertDbConnectionFactory;
import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.entity.EntityUUID;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.repository.content.store.ContentRepositoryDAO;
import org.sonatype.nexus.repository.content.store.ContentRepositoryData;
import org.sonatype.nexus.repository.content.store.example.TestContentRepositoryDAO;
import org.sonatype.nexus.testdb.DataSessionRule;

import org.assertj.db.type.Table;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.db.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;
import static org.sonatype.nexus.datastore.mybatis.CombUUID.combUUID;

public class KeyValueDAOTest
    extends TestSupport
{
  private static final String CATEGORY = "test";

  private static final String CATEGORY_2 = "category-2";

  @Rule
  public DataSessionRule sessionRule = new DataSessionRule()
      .access(TestContentRepositoryDAO.class)
      .access(TestKeyValueDAO.class);

  private Integer contentRepositoryId;

  private Integer otherRepositoryId;

  @Before
  public void setup() {
    contentRepositoryId = createContentRepository(randomContentRepository());
    otherRepositoryId = createContentRepository(randomContentRepository());
  }

  @Test
  public void testSet() {
    withDAO(dao -> dao.set(contentRepositoryId, CATEGORY, "foo", "bar"));

    assertThat(table()).hasNumberOfRows(1)
        .row(0)
        .value("category").isEqualTo(CATEGORY)
        .value("key").isEqualTo("foo")
        .value("value").isEqualTo("bar");
  }

  @Test
  public void testGet() {
    Optional<String> result = callDAO(dao -> dao.get(contentRepositoryId, CATEGORY, "foo"));

    assertFalse(result.isPresent());

    insert(CATEGORY, "foo", "bar");

    result = callDAO(dao -> dao.get(contentRepositoryId, CATEGORY, "foo"));

    assertTrue(result.isPresent());
    assertEquals("bar", result.get());
  }

  @Test
  public void testBrowse() {
    Function<String, Continuation<KeyValue>> browse =
        continuationToken -> callDAO(dao -> dao.browse(contentRepositoryId, CATEGORY, 1, continuationToken));

    // different category
    insert("test2", "bar2", "foo2");
    // different repository
    insert(otherRepositoryId, CATEGORY, "bar3", "foo3");

    Continuation<KeyValue> values = browse.apply(null);
    assertEquals(0, values.size());
    assertTrue(values.isEmpty());

    //matching targets
    insert(CATEGORY, "foo", "bar");
    insert(CATEGORY, "bar", "foo");

    values = browse.apply(null);
    assertEquals(1, values.size());
    assertNotNull(values.nextContinuationToken());

    List<String> completeResults = new LinkedList<>();

    // Collect results for later
    values.stream().map(KeyValue::getValue).forEach(completeResults::add);

    values = browse.apply(values.nextContinuationToken());
    assertEquals(1, values.size());
    assertNotNull(values.nextContinuationToken());

    // Collect results for later
    values.stream().map(KeyValue::getValue).forEach(completeResults::add);

    values = browse.apply(values.nextContinuationToken());
    assertEquals(0, values.size());

    assertThat(completeResults, contains("foo", "bar"));
  }

  @Test
  public void testBrowseCategories() {
    List<String> categories = callDAO(dao -> dao.browseCategories(contentRepositoryId));
    assertThat(categories, empty());

    insert(CATEGORY, "key1", "value1");
    insert(CATEGORY, "key2", "value2");
    insert(CATEGORY_2, "key3", "value3");
    insert(otherRepositoryId, "other-repo-category", "key4", "value4");

    categories = callDAO(dao -> dao.browseCategories(contentRepositoryId));
    assertThat(categories, containsInAnyOrder(CATEGORY, CATEGORY_2));
  }

  @Test
  public void testFindCategories() {
    List<String> categories = callDAO(dao -> dao.findCategories(contentRepositoryId, "some-key"));
    assertThat(categories, empty());

    insert(CATEGORY, "key1", "value1");
    insert(CATEGORY_2, "key1", "value3");
    insert(otherRepositoryId, "other-repo-category", "key4", "value4");

    categories = callDAO(dao -> dao.findCategories(contentRepositoryId, "key1"));
    assertThat(categories, containsInAnyOrder(CATEGORY, CATEGORY_2));
  }

  @Test
  public void testCount() {
    int count = callDAO(dao -> dao.count(contentRepositoryId, CATEGORY));
    assertEquals(0, count);

    // different category
    insert("test2", "bar2", "foo2");

    count = callDAO(dao -> dao.count(contentRepositoryId, CATEGORY));
    assertEquals(0, count);

    // different repository
    insert(otherRepositoryId, CATEGORY, "bar3", "foo3");

    count = callDAO(dao -> dao.count(contentRepositoryId, CATEGORY));
    assertEquals(0, count);

    //matching target
    insert(CATEGORY, "foo", "bar");

    count = callDAO(dao -> dao.count(contentRepositoryId, CATEGORY));
    assertEquals(1, count);
  }

  @Test
  public void testRemove() {
    //matching target
    insert(CATEGORY, "foo", "bar");
    // different key
    insert(CATEGORY, "bar", "foo");
    // different category
    insert("test2", "foo", "foo");

    withDAO(dao -> dao.remove(contentRepositoryId, CATEGORY, "foo"));

    assertThat(table()).hasNumberOfRows(2)
        .row(0)
        .value("category").isEqualTo(CATEGORY)
        .value("key").isEqualTo("bar")
        .row(1)
        .value("category").isEqualTo("test2");
  }

  @Test
  public void testRemoveAll() {
    //matching targets
    insert(CATEGORY, "foo", "bar");
    insert(CATEGORY, "bar", "foo");
    // different category
    insert("test2", "bar2", "foo3");
    // different repository
    insert(otherRepositoryId, CATEGORY, "bar3", "foo3");

    int removed = callDAO(dao -> dao.removeAll(contentRepositoryId, CATEGORY, 1));
    assertEquals(1, removed);

    removed = callDAO(dao -> dao.removeAll(contentRepositoryId, CATEGORY, 1));
    assertEquals(1, removed);

    removed = callDAO(dao -> dao.removeAll(contentRepositoryId, CATEGORY, 1));
    assertEquals(0, removed);

    assertThat(table()).hasNumberOfRows(2)
        .row(0).value("category").isEqualTo("test2")
        .row(1).value("repository_id").isEqualTo(otherRepositoryId);
  }

  @Test
  public void testRemoveRepository() {
    //matching targets
    insert(CATEGORY, "foo", "bar");
    insert(CATEGORY, "bar", "foo");
    // different repository
    insert(otherRepositoryId, CATEGORY, "foo3", "bar3");

    int removed = callDAO(dao -> dao.removeRepository(contentRepositoryId, 1));
    assertEquals(1, removed);

    removed = callDAO(dao -> dao.removeRepository(contentRepositoryId, 1));
    assertEquals(1, removed);

    removed = callDAO(dao -> dao.removeRepository(contentRepositoryId, 1));
    assertEquals(0, removed);

    assertThat(table()).hasNumberOfRows(1)
        .row(0)
        .value("repository_id").isEqualTo(otherRepositoryId);
  }

  private void insert(final String category, final String key, final String value) {
    withDAO(dao -> dao.set(contentRepositoryId, category, key, value));
  }

  private void insert(final int repositoryId, final String category, final String key, final String value) {
    withDAO(dao -> dao.set(repositoryId, category, key, value));
  }

  private Table table() {
    return AssertDbConnectionFactory
            .of(sessionRule.getDataStore(DEFAULT_DATASTORE_NAME).get().getDataSource()).create()
            .table("test_key_value").build();
  }

  private void withDAO(final Consumer<KeyValueDAO> consumer) {
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      consumer.accept(session.access(TestKeyValueDAO.class));
      session.getTransaction().commit();
    }
  }

  private <R> R callDAO(final Function<KeyValueDAO, R> consumer) {
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      R result =  consumer.apply(session.access(TestKeyValueDAO.class));

      session.getTransaction().commit();
      return result;
    }
  }

  protected static ContentRepositoryData randomContentRepository() {
    ContentRepositoryData repository = new ContentRepositoryData();
    repository.setConfigRepositoryId(new EntityUUID(combUUID()));
    repository.setAttributes(new NestedAttributesMap());
    return repository;
  }

  private Integer createContentRepository(final ContentRepositoryData contentRepository) {
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      ContentRepositoryDAO dao = session.access(TestContentRepositoryDAO.class);
      dao.createContentRepository(contentRepository);
      session.getTransaction().commit();
    }
    return contentRepository.contentRepositoryId();
  }
}
