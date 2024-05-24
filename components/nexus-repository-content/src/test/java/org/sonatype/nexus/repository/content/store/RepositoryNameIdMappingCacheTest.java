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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.db.DatabaseCheck;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryCreatedEvent;
import org.sonatype.nexus.repository.manager.RepositoryDeletedEvent;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasEntry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RepositoryNameIdMappingCacheTest
    extends TestSupport
{
  @Mock
  private FormatStoreManager formatStoreManager;

  @Mock
  private List<Format> formatNames;

  @Mock
  private ContentRepositoryStore<?> contentRepositoryStore;

  private RepositoryNameIdMappingCache underTest;

  @Mock
  private DatabaseCheck databaseCheck;

  @Before
  public void setup() {
    when(formatStoreManager.contentRepositoryStore(any())).thenReturn(contentRepositoryStore);
    when(databaseCheck.isPostgresql()).thenReturn(true);
    underTest = new RepositoryNameIdMappingCache(formatStoreManager, formatNames, databaseCheck);
  }

  @Test
  public void testGetRepositoryIds() {
    Map<Integer, String> repositoryIds = initializeCache();
    assertThat(repositoryIds.keySet(), contains(1, 2));
    assertThat(repositoryIds, hasEntry(1, "repo1"));
    assertThat(repositoryIds, hasEntry(2, "repo2"));
  }

  @Test
  public void testShouldAddNewRepositoryIdOnRepositoryCreatedEvent() {
    initializeCache();
    Map<String, Object> nameIdThree = getRepositoryNameId("repo3", 3);

    when(contentRepositoryStore.readContentRepositoryId(any(), any())).thenReturn(Optional.of(nameIdThree));

    underTest.on(new RepositoryCreatedEvent(repository("repo3")));

    Map<Integer, String> repositoryIds = underTest.getRepositoryNameIds(Arrays.asList("repo3"), "raw");
    assertThat(repositoryIds, hasEntry(3, "repo3"));
  }

  @Test
  public void testShouldRemoveRepositoryIdOnRepositoryDeletedEvent() {
    initializeCache();

    underTest.on(new RepositoryDeletedEvent(repository("repo1")));

    Map<Integer, String> repositoryIds = underTest.getRepositoryNameIds(Arrays.asList("repo1"), "raw");
    assertThat(repositoryIds, not(hasEntry(2, "repo2")));
  }

  private Map<Integer, String> initializeCache() {
    List<String> repositoryNames = Arrays.asList("repo1", "repo2");
    Map<String, Object> nameIdOne = getRepositoryNameId("repo1", 1);
    Map<String, Object> nameIdTwo = getRepositoryNameId("repo2", 2);
    List<Map<String, Object>> repositoryNameIds = Arrays.asList(nameIdOne, nameIdTwo);

    when(contentRepositoryStore.readAllContentRepositoryIds(any())).thenReturn(repositoryNameIds);

    return underTest.getRepositoryNameIds(repositoryNames, "raw");
  }

  private Map<String, Object> getRepositoryNameId(String name, int id) {
    Map<String, Object> nameId = new HashMap<>();
    nameId.put("name", name);
    nameId.put("repository_id", id);
    return nameId;
  }

  private Repository repository(String name) {
    Repository repository = mock(Repository.class);
    when(repository.getName()).thenReturn(name);
    when(repository.getName()).thenReturn(name);
    when(repository.getFormat()).thenReturn(mock(Format.class));
    return repository;
  }
}
