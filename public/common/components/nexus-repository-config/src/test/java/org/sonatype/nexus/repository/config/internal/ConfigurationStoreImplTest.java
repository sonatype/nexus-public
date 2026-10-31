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
package org.sonatype.nexus.repository.config.internal;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationDAO;
import org.sonatype.nexus.testdb.DataSessionConfiguration;

import com.google.common.collect.ImmutableList;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sonatype.nexus.testdb.DatabaseTest;

import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class ConfigurationStoreImplTest
{
  @Nested
  class DatabaseTests
  {
    @DataSessionConfiguration(daos = ConfigurationDAO.class)
    private DataSessionSupplier sessionSupplier;

    private ConfigurationStoreImpl underTest;

    @BeforeEach
    void setup() {
      underTest = new ConfigurationStoreImpl(sessionSupplier);
      underTest.setDependencies(mock(EventManager.class));
    }

    @DatabaseTest
    void readByNamesShouldBeEmptyWhenRepositoriesIsEmpty() {
      Collection<Configuration> configurations = underTest.readByNames(emptySet());

      assertThat(configurations.isEmpty(), Is.is(true));
    }

    @DatabaseTest
    void countByBlobStoreCountsRepositoriesConfiguredForTheBlobStore() {
      underTest
          .create(configurationData("repo-a", "maven2-hosted", Map.of("storage", Map.of("blobStoreName", "bs-a"))));
      underTest.create(configurationData("repo-b", "npm-hosted", Map.of("storage", Map.of("blobStoreName", "bs-a"))));
      underTest.create(configurationData("repo-c", "raw-hosted", Map.of("storage", Map.of("blobStoreName", "bs-b"))));
      // a repository with no storage.blobStoreName (e.g. a group) must not be counted
      underTest
          .create(configurationData("repo-group", "maven2-group", Map.of("group", Map.of("memberNames", "repo-a"))));

      assertThat(underTest.countByBlobStore("bs-a"), Is.is(2));
      assertThat(underTest.countByBlobStore("bs-b"), Is.is(1));
      assertThat(underTest.countByBlobStore("bs-none"), Is.is(0));
    }

    @DatabaseTest
    void readByNameIsExactMatchNotLikeWildcard() {
      // "myXrepo" matches the LIKE pattern for "my_repo" ('_' is a single-char wildcard); exact-match readByName
      // must NOT return it, otherwise a read-through get() could materialize the wrong repository. See NEXUS-54229.
      underTest
          .create(configurationData("myXrepo", "maven2-hosted", Map.of("storage", Map.of("blobStoreName", "default"))));
      assertThat(underTest.readByName("my_repo").isPresent(), Is.is(false));

      underTest
          .create(configurationData("my_repo", "maven2-hosted", Map.of("storage", Map.of("blobStoreName", "default"))));
      assertThat(underTest.readByName("my_repo").isPresent(), Is.is(true));
    }

    @DatabaseTest
    void countByBlobStoreExcludesSoftDeletedRepositories() {
      underTest
          .create(configurationData("repo-a", "maven2-hosted", Map.of("storage", Map.of("blobStoreName", "bs-a"))));
      underTest.create(configurationData("repo-b", "npm-hosted", Map.of("storage", Map.of("blobStoreName", "bs-a"))));
      assertThat(underTest.countByBlobStore("bs-a"), Is.is(2));

      // A soft-deleted repository is being torn down and must no longer count as using its blob store.
      underTest.softDelete("repo-a", "admin");
      assertThat(underTest.countByBlobStore("bs-a"), Is.is(1));

      // Undoing the soft-delete restores it to the in-use count.
      underTest.undoSoftDelete("repo-a");
      assertThat(underTest.countByBlobStore("bs-a"), Is.is(2));
    }
  }

  @Nested
  class SoftDeleteFallbackTests
  {
    private ConfigurationStoreImpl underTest;

    @BeforeEach
    void setup() {
      underTest = new ConfigurationStoreImpl(mock(DataSessionSupplier.class));
    }

    @Test
    void existsFallsBackToUnfilteredCheckWhenDeletedOnColumnMissing() {
      ConfigurationStoreImpl spy = Mockito.spy(underTest);

      doThrow(new RuntimeException(
          new SQLException("ERROR: column \"deleted_on\" does not exist")))
              .when(spy)
              .doExists("myrepo");
      // Fail-closed no longer returns false; it runs an unfiltered existence check (NEXUS-51733).
      doReturn(true).when(spy).doExistsIgnoringSoftDelete("myrepo");

      boolean result = spy.exists("myrepo");

      assertThat(result, is(true));
    }

    @Test
    void existsRethrowsNonColumnErrors() {
      ConfigurationStoreImpl spy = Mockito.spy(underTest);

      doThrow(new RuntimeException("some other database error"))
          .when(spy)
          .doExists("myrepo");

      assertThrows(RuntimeException.class, () -> spy.exists("myrepo"));
    }

    @Test
    void readByRecipeFallsBackToRecipeScopedIgnoringSoftDeleteWhenDeletedOnColumnMissing() {
      ConfigurationStoreImpl spy = Mockito.spy(underTest);
      List<Configuration> recipeConfigs = ImmutableList.of(new ConfigurationData());

      doThrow(new RuntimeException(
          new SQLException("ERROR: column \"deleted_on\" does not exist")))
              .when(spy)
              .doReadByRecipe("maven2-hosted");
      // Keeps the recipe predicate (no blind listAll) via the ignoring-soft-delete variant (NEXUS-51733).
      doReturn(recipeConfigs).when(spy).doReadByRecipeIgnoringSoftDelete("maven2-hosted");

      java.util.Collection<Configuration> result = spy.readByRecipe("maven2-hosted");

      assertThat(result, is(recipeConfigs));
    }

    @Test
    void readByNamesFallsBackToNameScopedIgnoringSoftDeleteWhenDeletedOnColumnMissing() {
      ConfigurationStoreImpl spy = Mockito.spy(underTest);
      List<Configuration> nameConfigs = ImmutableList.of(new ConfigurationData());

      doThrow(new RuntimeException(
          new SQLException("ERROR: column \"deleted_on\" does not exist")))
              .when(spy)
              .doReadByNames(java.util.Set.of("repo1"));
      // Keeps the name predicate (no blind listAll) via the ignoring-soft-delete variant (NEXUS-51733).
      doReturn(nameConfigs).when(spy).doReadByNamesIgnoringSoftDelete(java.util.Set.of("repo1"));

      java.util.Collection<Configuration> result = spy.readByNames(java.util.Set.of("repo1"));

      assertThat(result, is(nameConfigs));
    }

    @Test
    void listFallsBackWhenDeletedOnColumnMissing() {
      ConfigurationStoreImpl spy = Mockito.spy(underTest);
      List<Configuration> expectedConfigs = ImmutableList.of(new ConfigurationData());

      doThrow(new RuntimeException(
          new SQLException("ERROR: column \"deleted_on\" does not exist")))
              .when(spy)
              .listActive();
      doReturn(expectedConfigs).when(spy).listAll();

      List<Configuration> result = spy.list();

      assertThat(result, hasSize(1));
      assertThat(result, is(expectedConfigs));
    }

    @Test
    void listRethrowsNonColumnErrors() {
      ConfigurationStoreImpl spy = Mockito.spy(underTest);

      doThrow(new RuntimeException("some other database error"))
          .when(spy)
          .listActive();

      assertThrows(RuntimeException.class, () -> spy.list());
    }

    @Test
    void listUsesActiveWhenColumnExists() {
      ConfigurationStoreImpl spy = Mockito.spy(underTest);
      List<Configuration> activeConfigs = ImmutableList.of(new ConfigurationData());

      doReturn(activeConfigs).when(spy).listActive();

      List<Configuration> result = spy.list();

      assertThat(result, is(activeConfigs));
      Mockito.verify(spy, Mockito.never()).listAll();
    }

    @Test
    void listHandlesNestedPSQLException() {
      ConfigurationStoreImpl spy = Mockito.spy(underTest);
      List<Configuration> fallbackConfigs = ImmutableList.of(new ConfigurationData());

      RuntimeException nested = new RuntimeException("outer",
          new RuntimeException("middle",
              new SQLException("ERROR: column \"deleted_on\" does not exist")));
      doThrow(nested).when(spy).listActive();
      doReturn(fallbackConfigs).when(spy).listAll();

      List<Configuration> result = spy.list();

      assertThat(result, hasSize(1));
      assertThat(result, is(fallbackConfigs));
    }

    @Test
    void countByBlobStoreFallsBackToIgnoringSoftDeleteWhenDeletedOnColumnMissing() {
      ConfigurationStoreImpl spy = Mockito.spy(underTest);

      doThrow(new RuntimeException(
          new SQLException("ERROR: column \"deleted_on\" does not exist")))
              .when(spy)
              .doCountByBlobStore("bs-a");
      doReturn(3).when(spy).doCountByBlobStoreIgnoringSoftDelete("bs-a");

      assertThat(spy.countByBlobStore("bs-a"), is(3));
    }

    @Test
    void createFallsBackToRefreshIgnoringSoftDeleteWhenDeletedOnColumnMissing() {
      ConfigurationStoreImpl spy = Mockito.spy(underTest);
      ConfigurationData config = new ConfigurationData();
      doNothing().when(spy).persistCreate(config);
      // The read-back's soft-delete-filtered read hits the not-yet-migrated column during a rolling upgrade.
      doThrow(new RuntimeException(new SQLException("ERROR: column \"deleted_on\" does not exist")))
          .when(spy)
          .doRefreshPersistedAttributes(config);
      doNothing().when(spy).doRefreshPersistedAttributesIgnoringSoftDelete(config);

      spy.create(config);

      // The write is not rolled back and the read-back retries without the soft-delete filter (NEXUS-51733).
      Mockito.verify(spy).persistCreate(config);
      Mockito.verify(spy).doRefreshPersistedAttributesIgnoringSoftDelete(config);
    }

    @Test
    void createRethrowsNonColumnErrorsFromRefresh() {
      ConfigurationStoreImpl spy = Mockito.spy(underTest);
      ConfigurationData config = new ConfigurationData();
      doNothing().when(spy).persistCreate(config);
      doThrow(new RuntimeException("some other database error")).when(spy).doRefreshPersistedAttributes(config);

      assertThrows(RuntimeException.class, () -> spy.create(config));

      Mockito.verify(spy, Mockito.never()).doRefreshPersistedAttributesIgnoringSoftDelete(config);
    }

    @Test
    void createDoesNotFallBackWhenRefreshSucceeds() {
      ConfigurationStoreImpl spy = Mockito.spy(underTest);
      ConfigurationData config = new ConfigurationData();
      doNothing().when(spy).persistCreate(config);
      doNothing().when(spy).doRefreshPersistedAttributes(config);

      spy.create(config);

      Mockito.verify(spy).doRefreshPersistedAttributes(config);
      Mockito.verify(spy, Mockito.never()).doRefreshPersistedAttributesIgnoringSoftDelete(config);
    }

    @Test
    void updateFallsBackToRefreshIgnoringSoftDeleteWhenDeletedOnColumnMissing() {
      ConfigurationStoreImpl spy = Mockito.spy(underTest);
      ConfigurationData config = new ConfigurationData();
      doNothing().when(spy).persistUpdate(config);
      doThrow(new RuntimeException(new SQLException("ERROR: column \"deleted_on\" does not exist")))
          .when(spy)
          .doRefreshPersistedAttributes(config);
      doNothing().when(spy).doRefreshPersistedAttributesIgnoringSoftDelete(config);

      spy.update(config);

      Mockito.verify(spy).persistUpdate(config);
      Mockito.verify(spy).doRefreshPersistedAttributesIgnoringSoftDelete(config);
    }

    @Test
    void updateRethrowsNonColumnErrorsFromRefresh() {
      ConfigurationStoreImpl spy = Mockito.spy(underTest);
      ConfigurationData config = new ConfigurationData();
      doNothing().when(spy).persistUpdate(config);
      doThrow(new RuntimeException("some other database error")).when(spy).doRefreshPersistedAttributes(config);

      assertThrows(RuntimeException.class, () -> spy.update(config));

      Mockito.verify(spy, Mockito.never()).doRefreshPersistedAttributesIgnoringSoftDelete(config);
    }
  }

  private static ConfigurationData configurationData(
      final String name,
      final String recipeName,
      final Map<String, Map<String, Object>> attributes)
  {
    ConfigurationData configuration = new ConfigurationData();
    configuration.setName(name);
    configuration.setRecipeName(recipeName);
    configuration.setOnline(true);
    configuration.setAttributes(attributes);
    return configuration;
  }
}
