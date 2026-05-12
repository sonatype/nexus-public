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
package org.sonatype.nexus.repository.search.sql.store.upgrade.task;

import java.util.List;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.EntityUUID;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.search.sql.store.SearchRepositoryData;
import org.sonatype.nexus.repository.search.sql.store.SearchStore;
import org.sonatype.nexus.testcommon.extensions.LoggingExtension;
import org.sonatype.nexus.testcommon.extensions.LoggingExtension.CaptureLogsFor;
import org.sonatype.nexus.testcommon.extensions.LoggingExtension.TestLogAccessor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.event.Level;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.testcommon.matchers.NexusMatchers.formattedMessage;
import static org.sonatype.nexus.testcommon.matchers.NexusMatchers.logLevel;

@ExtendWith(MockitoExtension.class)
@ExtendWith(LoggingExtension.class)
class CleanOrphanedSearchRecordsTaskTest
{
  @Mock
  private SearchStore searchStore;

  @Mock
  private RepositoryManager repositoryManager;

  @CaptureLogsFor(value = CleanOrphanedSearchRecordsTask.class, level = Level.DEBUG)
  TestLogAccessor logs;

  @Test
  void shouldCleanupOrphanedRecords_whenRepositoryDoesNotExist() throws Exception {
    // Given: A search record for a repository that doesn't exist
    SearchRepositoryData searchData = new SearchRepositoryData("npm-registry", 100, "npm");
    when(searchStore.getSearchRepositories()).thenReturn(List.of(searchData));
    when(repositoryManager.get("npm-registry")).thenReturn(null);

    CleanOrphanedSearchRecordsTask task = new CleanOrphanedSearchRecordsTask(
        searchStore,
        repositoryManager);

    // When: Task executes
    task.execute();

    // Then: Orphaned search records are deleted
    verify(searchStore).deleteAllForRepository(eq(100), eq("npm"));
    verify(searchStore).deleteAllSearchAssets(eq(100), eq("npm"));

    // Verify debug log was written with correct details
    assertThat(logs.logs(), hasItem(allOf(
        logLevel(Level.DEBUG),
        formattedMessage(
            containsString(
                "Removing orphaned search records for stale repository: npm-registry - format npm (id=100)")))));
  }

  @Test
  void shouldCleanupOrphanedRecords_whenFormatDoesNotMatch() throws Exception {
    // Given: Repository exists but with different format
    SearchRepositoryData searchData = new SearchRepositoryData("nuget-hosted", 250, "nuget");
    when(searchStore.getSearchRepositories()).thenReturn(List.of(searchData));

    EntityId configRepoId = new EntityUUID();
    Repository repository = createRepository("nuget-hosted", "docker", configRepoId);
    when(repositoryManager.get("nuget-hosted")).thenReturn(repository);

    CleanOrphanedSearchRecordsTask task = new CleanOrphanedSearchRecordsTask(
        searchStore,
        repositoryManager);

    // When: Task executes
    task.execute();

    // Then: Orphaned search records are deleted due to format mismatch
    verify(searchStore).deleteAllForRepository(eq(250), eq("nuget"));
    verify(searchStore).deleteAllSearchAssets(eq(250), eq("nuget"));

    // Verify debug log was written with correct details
    assertThat(logs.logs(), hasItem(allOf(
        logLevel(Level.DEBUG),
        formattedMessage(containsString(
            "Removing orphaned search records for stale repository: nuget-hosted - format nuget (id=250)")))));
  }

  @Test
  void shouldCleanupOrphanedRecords_whenContentRepositoryIdMismatch() throws Exception {
    // Given: Content repository exists but with different ID
    SearchRepositoryData searchData = new SearchRepositoryData("pypi-releases", 500, "pypi");
    when(searchStore.getSearchRepositories()).thenReturn(List.of(searchData));

    EntityId configRepoId = new EntityUUID();
    Repository repository = createRepository("pypi-releases", "pypi", configRepoId);
    when(repositoryManager.get("pypi-releases")).thenReturn(repository);

    // Content repository exists but with mismatched ID (999 instead of 500)
    ContentFacet contentFacet = mock();
    when(contentFacet.contentRepositoryId()).thenReturn(999);
    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);

    CleanOrphanedSearchRecordsTask task = new CleanOrphanedSearchRecordsTask(
        searchStore,
        repositoryManager);

    // When: Task executes
    task.execute();

    // Then: Orphaned search records are deleted due to ID mismatch
    verify(searchStore).deleteAllForRepository(eq(500), eq("pypi"));
    verify(searchStore).deleteAllSearchAssets(eq(500), eq("pypi"));

    // Verify debug log was written with correct details
    assertThat(logs.logs(), hasItem(allOf(
        logLevel(Level.DEBUG),
        formattedMessage(containsString(
            "Removing orphaned search records for stale repository: pypi-releases - format pypi (id=500)")))));
  }

  @Test
  void shouldNotCleanup_whenRepositoryExistsAndMatches() throws Exception {
    // Given: Repository exists, format matches, and content repository ID matches
    SearchRepositoryData searchData = new SearchRepositoryData("maven-central", 1, "maven");
    when(searchStore.getSearchRepositories()).thenReturn(List.of(searchData));

    EntityId configRepoId = new EntityUUID();
    Repository repository = createRepository("maven-central", "maven", configRepoId);
    when(repositoryManager.get("maven-central")).thenReturn(repository);

    ContentFacet contentFacet = mock();
    when(contentFacet.contentRepositoryId()).thenReturn(1);

    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);

    CleanOrphanedSearchRecordsTask task = new CleanOrphanedSearchRecordsTask(
        searchStore,
        repositoryManager);

    // When: Task executes
    task.execute();

    // Then: No cleanup should occur since repository is valid
    verify(searchStore, never()).deleteAllForRepository(anyInt(), anyString());
    verify(searchStore, never()).deleteAllSearchAssets(anyInt(), anyString());

    // Verify no debug logs (no orphaned repos found)
    assertThat(logs.logs(), not(hasItem(logLevel(Level.DEBUG))));
  }

  /**
   * Helper method to create a Repository mock with format and configuration.
   */
  private Repository createRepository(final String name, final String format, final EntityId configRepoId) {
    Repository repository = mock(Repository.class);
    lenient().when(repository.getName()).thenReturn(name);

    Format formatMock = mock(Format.class);
    lenient().when(formatMock.getValue()).thenReturn(format);
    lenient().when(repository.getFormat()).thenReturn(formatMock);

    Configuration config = mock(Configuration.class);
    lenient().when(config.getRepositoryId()).thenReturn(configRepoId);
    lenient().when(repository.getConfiguration()).thenReturn(config);

    return repository;
  }
}
