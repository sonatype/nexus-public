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

import java.util.Collections;
import java.util.List;

import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.FluentComponents;
import org.sonatype.nexus.repository.content.fluent.FluentQuery;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.search.sql.index.SqlSearchIndexService;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.testcommon.extensions.LoggingExtension;
import org.sonatype.nexus.testcommon.extensions.LoggingExtension.CaptureLogsFor;
import org.sonatype.nexus.testcommon.extensions.LoggingExtension.TestLogAccessor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.event.Level;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.search.sql.store.upgrade.task.ReIndexSearchFilterByPatternTask.FILTER_CONDITION_FIELD_ID;
import static org.sonatype.nexus.testcommon.matchers.NexusMatchers.formattedMessage;
import static org.sonatype.nexus.testcommon.matchers.NexusMatchers.logLevel;

@ExtendWith(MockitoExtension.class)
@ExtendWith(LoggingExtension.class)
class ReIndexSearchFilterByPatternTaskTest
{
  private static final int BATCH_SIZE = 500;

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private SqlSearchIndexService sqlSearchIndexService;

  @CaptureLogsFor(value = ReIndexSearchFilterByPatternTask.class, level = Level.INFO)
  TestLogAccessor logs;

  private ReIndexSearchFilterByPatternTask underTest;

  @BeforeEach
  void setUp() {
    underTest = new ReIndexSearchFilterByPatternTask(repositoryManager, sqlSearchIndexService, BATCH_SIZE);
  }

  private void configureTask(String filterCondition) {
    TaskConfiguration taskConfiguration = new TaskConfiguration();
    taskConfiguration.setId("test-task-id");
    taskConfiguration.setTypeId(ReIndexSearchFilterByPatternTask.TYPE_ID);
    taskConfiguration.setName("Test Reindex Task");
    if (filterCondition != null) {
      taskConfiguration.setString(FILTER_CONDITION_FIELD_ID, filterCondition);
    }
    underTest.configure(taskConfiguration);
  }

  @Test
  void shouldSkipWhenFilterConditionIsNull() throws Exception {
    configureTask(null);

    Object result = underTest.execute();

    assertThat(result, is(nullValue()));
    verify(repositoryManager, never()).browse();
    verify(sqlSearchIndexService, never()).indexBatch(any(), any());

    assertThat(logs.logs(), hasItem(allOf(
        logLevel(Level.WARN),
        formattedMessage(containsString("No filter condition provided, skipping re-index task")))));
  }

  @Test
  void shouldProcessRepositoriesWithMatchingComponents() throws Exception {
    String filterCondition = "namespace LIKE '%\\_%' OR version LIKE '%\\_%'";
    configureTask(filterCondition);

    Repository repo1 = createRepositoryWithComponents("maven-central", 2);
    Repository repo2 = createRepositoryWithComponents("npm-proxy", 1);

    when(repositoryManager.browse()).thenReturn(List.of(repo1, repo2));

    Object result = underTest.execute();

    assertThat(result, is(equalTo(3L)));
    verify(sqlSearchIndexService, times(3)).indexBatch(any(), any());

    assertThat(logs.logs(), hasItem(allOf(
        logLevel(Level.INFO),
        formattedMessage(containsString("Starting re-index task with filter condition: " + filterCondition)))));

    assertThat(logs.logs(), hasItem(allOf(
        logLevel(Level.INFO),
        formattedMessage(containsString("Completed re-indexing 2 components for repository maven-central")))));

    assertThat(logs.logs(), hasItem(allOf(
        logLevel(Level.INFO),
        formattedMessage(containsString("Completed re-indexing 1 components for repository npm-proxy")))));

    assertThat(logs.logs(), hasItem(allOf(
        logLevel(Level.INFO),
        formattedMessage(containsString("Completed re-index task. Total components processed: 3")))));
  }

  @Test
  void shouldSkipRepositoriesWithNoMatchingComponents() throws Exception {
    String filterCondition = "namespace LIKE '%\\_%'";
    configureTask(filterCondition);

    Repository repo = createRepositoryWithComponents("maven-central", 0);
    when(repositoryManager.browse()).thenReturn(List.of(repo));

    Object result = underTest.execute();

    assertThat(result, is(equalTo(0L)));
    verify(sqlSearchIndexService, never()).indexBatch(any(), any());

    assertThat(logs.logs(), hasItem(allOf(
        logLevel(Level.INFO),
        formattedMessage(containsString("No components matching filter in repository maven-central")))));
  }

  @Test
  void shouldHandleRepositoryWithoutContentFacet() throws Exception {
    String filterCondition = "namespace LIKE '%\\_%'";
    configureTask(filterCondition);

    Repository repo = mock(Repository.class);
    when(repo.getName()).thenReturn("proxy-repo");
    when(repo.facet(ContentFacet.class)).thenReturn(null);

    when(repositoryManager.browse()).thenReturn(List.of(repo));

    Object result = underTest.execute();

    assertThat(result, is(equalTo(0L)));
    verify(sqlSearchIndexService, never()).indexBatch(any(), any());

    assertThat(logs.logs(), hasItem(allOf(
        logLevel(Level.ERROR),
        formattedMessage(containsString("Error processing repository proxy-repo")))));
  }

  @Test
  void shouldProcessMultipleBatches() throws Exception {
    String filterCondition = "namespace LIKE '%\\_%'";
    configureTask(filterCondition);

    Repository repo = createRepositoryWithComponents("maven-central", 3);
    when(repositoryManager.browse()).thenReturn(List.of(repo));

    Object result = underTest.execute();

    assertThat(result, is(equalTo(3L)));
    verify(sqlSearchIndexService, times(3)).indexBatch(any(), eq(repo));
  }

  /**
   * Helper method to create a Repository mock with FluentComponents that return matching components.
   */
  private Repository createRepositoryWithComponents(
      final String name,
      final int componentCount)
  {
    Repository repository = mock(Repository.class);
    lenient().when(repository.getName()).thenReturn(name);

    Format format = mock(Format.class);
    lenient().when(format.getValue()).thenReturn("maven2");
    lenient().when(repository.getFormat()).thenReturn(format);

    ContentFacet contentFacet = mock(ContentFacet.class);
    FluentComponents fluentComponents = mock(FluentComponents.class);
    FluentQuery<FluentComponent> fluentQuery = mock(FluentQuery.class);

    lenient().when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    lenient().when(contentFacet.components()).thenReturn(fluentComponents);
    lenient().when(fluentComponents.byFilter(anyString(), anyMap())).thenReturn(fluentQuery);

    Continuation<FluentComponent> emptyContinuation = createContinuation(0, null);

    switch (componentCount) {
      case 1 -> {
        Continuation<FluentComponent> continuation = createContinuation(1, null);
        lenient().when(fluentQuery.browse(eq(BATCH_SIZE), isNull()))
            .thenReturn(continuation)
            .thenReturn(emptyContinuation);
      }
      case 2 -> {
        Continuation<FluentComponent> continuation1 = createContinuation(1, "token1");

        Continuation<FluentComponent> continuation2 = createContinuation(1, null);

        lenient().when(fluentQuery.browse(eq(BATCH_SIZE), isNull()))
            .thenReturn(continuation1)
            .thenReturn(emptyContinuation);
        lenient().when(fluentQuery.browse(eq(BATCH_SIZE), eq("token1")))
            .thenReturn(continuation2);
      }
      case 3 -> {
        Continuation<FluentComponent> continuation3 = createContinuation(1, "token1");
        Continuation<FluentComponent> continuation4 = createContinuation(1, "token2");
        Continuation<FluentComponent> continuation5 = createContinuation(1, null);

        lenient().when(fluentQuery.browse(eq(BATCH_SIZE), isNull()))
            .thenReturn(continuation3)
            .thenReturn(emptyContinuation);
        lenient().when(fluentQuery.browse(eq(BATCH_SIZE), eq("token1"))).thenReturn(continuation4);
        lenient().when(fluentQuery.browse(eq(BATCH_SIZE), eq("token2")))
            .thenReturn(continuation5);
      }
      default -> lenient().when(fluentQuery.browse(eq(BATCH_SIZE), isNull())).thenReturn(emptyContinuation);
    }

    return repository;
  }

  /**
   * Helper method to create a Continuation mock.
   */
  private Continuation<FluentComponent> createContinuation(final int size, final String nextToken) {
    Continuation<FluentComponent> continuation = mock(Continuation.class);
    List<FluentComponent> components = size > 0
        ? List.of(mock(FluentComponent.class))
        : Collections.emptyList();

    lenient().when(continuation.isEmpty()).thenReturn(size == 0);
    lenient().when(continuation.size()).thenReturn(size);
    lenient().when(continuation.nextContinuationToken()).thenReturn(nextToken);
    lenient().when(continuation.iterator()).thenReturn(components.iterator());

    return continuation;
  }
}
