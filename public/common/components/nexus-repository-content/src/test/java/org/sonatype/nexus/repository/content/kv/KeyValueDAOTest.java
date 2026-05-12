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

import java.time.OffsetDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.entity.EntityUUID;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.repository.content.store.ContentRepositoryDAO;
import org.sonatype.nexus.repository.content.store.ContentRepositoryData;
import org.sonatype.nexus.repository.content.store.example.TestContentRepositoryDAO;
import org.sonatype.nexus.testdb.DataSessionConfiguration;

import org.sonatype.nexus.testdb.TestDataSessionSupplier;

import org.assertj.db.type.Table;
import org.junit.jupiter.api.BeforeEach;
import org.sonatype.nexus.testdb.DatabaseTest;

import static org.assertj.db.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.sonatype.nexus.datastore.mybatis.CombUUID.combUUID;

class KeyValueDAOTest
{
  private static final String CATEGORY = "test";

  private static final String CATEGORY_2 = "category-2";

  @DataSessionConfiguration(daos = {TestContentRepositoryDAO.class, TestKeyValueDAO.class})
  TestDataSessionSupplier sessionRule;

  private Integer contentRepositoryId;

  private Integer otherRepositoryId;

  @BeforeEach
  void setup() {
    contentRepositoryId = createContentRepository(randomContentRepository());
    otherRepositoryId = createContentRepository(randomContentRepository());
  }

  @DatabaseTest
  void testSet() {
    withDAO(dao -> dao.set(contentRepositoryId, CATEGORY, "foo", "bar"));

    assertThat(table()).hasNumberOfRows(1)
        .row(0)
        .value("category")
        .isEqualTo(CATEGORY)
        .value("key")
        .isEqualTo("foo")
        .value("value")
        .isEqualTo("bar");
  }

  @DatabaseTest
  void testGet() {
    Optional<String> result = callDAO(dao -> dao.get(contentRepositoryId, CATEGORY, "foo"));

    assertFalse(result.isPresent());

    insert(CATEGORY, "foo", "bar");

    result = callDAO(dao -> dao.get(contentRepositoryId, CATEGORY, "foo"));

    assertTrue(result.isPresent());
    assertEquals("bar", result.get());
  }

  @DatabaseTest
  void testBrowse() {
    Function<String, Continuation<KeyValue>> browse =
        continuationToken -> callDAO(dao -> dao.browse(contentRepositoryId, CATEGORY, 1, continuationToken));

    // different category
    insert("test2", "bar2", "foo2");
    // different repository
    insert(otherRepositoryId, CATEGORY, "bar3", "foo3");

    Continuation<KeyValue> values = browse.apply(null);
    assertEquals(0, values.size());
    assertTrue(values.isEmpty());

    // matching targets
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

  @DatabaseTest
  void testBrowseCategories() {
    List<String> categories = callDAO(dao -> dao.browseCategories(contentRepositoryId));
    assertThat(categories, empty());

    insert(CATEGORY, "key1", "value1");
    insert(CATEGORY, "key2", "value2");
    insert(CATEGORY_2, "key3", "value3");
    insert(otherRepositoryId, "other-repo-category", "key4", "value4");

    categories = callDAO(dao -> dao.browseCategories(contentRepositoryId));
    assertThat(categories, containsInAnyOrder(CATEGORY, CATEGORY_2));
  }

  @DatabaseTest
  void testFindCategories() {
    List<String> categories = callDAO(dao -> dao.findCategories(contentRepositoryId, "some-key"));
    assertThat(categories, empty());

    insert(CATEGORY, "key1", "value1");
    insert(CATEGORY_2, "key1", "value3");
    insert(otherRepositoryId, "other-repo-category", "key4", "value4");

    categories = callDAO(dao -> dao.findCategories(contentRepositoryId, "key1"));
    assertThat(categories, containsInAnyOrder(CATEGORY, CATEGORY_2));
  }

  @DatabaseTest
  void testCount() {
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

    // matching target
    insert(CATEGORY, "foo", "bar");

    count = callDAO(dao -> dao.count(contentRepositoryId, CATEGORY));
    assertEquals(1, count);
  }

  @DatabaseTest
  void testRemove() {
    // matching target
    insert(CATEGORY, "foo", "bar");
    // different key
    insert(CATEGORY, "bar", "foo");
    // different category
    insert("test2", "foo", "foo");

    withDAO(dao -> dao.remove(contentRepositoryId, CATEGORY, "foo"));

    assertThat(table()).hasNumberOfRows(2)
        .row(0)
        .value("category")
        .isEqualTo(CATEGORY)
        .value("key")
        .isEqualTo("bar")
        .row(1)
        .value("category")
        .isEqualTo("test2");
  }

  @DatabaseTest
  void testRemoveAll() {
    // matching targets
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
        .row(0)
        .value("category")
        .isEqualTo("test2")
        .row(1)
        .value("repository_id")
        .isEqualTo(otherRepositoryId);
  }

  @DatabaseTest
  void testRemoveRepository() {
    // matching targets
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
        .value("repository_id")
        .isEqualTo(otherRepositoryId);
  }

  @DatabaseTest
  void testSetCreatedDate() {
    int ourRepositoryId = 1;
    createEntries(ourRepositoryId, 101, "category-to-update");
    createEntries(ourRepositoryId, 101, "category-not-to-update");

    // Some other repository that should not be touched
    createEntries(ourRepositoryId + 10, 101, "category-to-update");

    int count = sessionRule.withDAO(TestKeyValueDAO.class,
        dao -> dao.setCreatedDate(ourRepositoryId, "category-to-update", OffsetDateTime.MIN));

    assertThat(count, is(101));
  }

  private void createEntries(final int repositoryId, final int count, final String category) {
    sessionRule.callDAO(TestKeyValueDAO.class, dao -> {
      IntStream.range(0, count).forEach(id -> dao.set(repositoryId, category, "key-%d".formatted(id), "value"));
    });
  }

  private void insert(final String category, final String key, final String value) {
    withDAO(dao -> dao.set(contentRepositoryId, category, key, value));
  }

  private void insert(final int repositoryId, final String category, final String key, final String value) {
    withDAO(dao -> dao.set(repositoryId, category, key, value));
  }

  private Table table() {
    return new Table(sessionRule.getDataSource().get(), "test_key_value");
  }

  private void withDAO(final Consumer<KeyValueDAO> consumer) {
    try (DataSession<?> session = sessionRule.openSession()) {
      consumer.accept(session.access(TestKeyValueDAO.class));
      session.getTransaction().commit();
    }
  }

  private <R> R callDAO(final Function<KeyValueDAO, R> consumer) {
    try (DataSession<?> session = sessionRule.openSession()) {
      R result = consumer.apply(session.access(TestKeyValueDAO.class));

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
    try (DataSession<?> session = sessionRule.openSession()) {
      ContentRepositoryDAO dao = session.access(TestContentRepositoryDAO.class);
      dao.createContentRepository(contentRepository);
      session.getTransaction().commit();
    }
    return contentRepository.contentRepositoryId();
  }
}
