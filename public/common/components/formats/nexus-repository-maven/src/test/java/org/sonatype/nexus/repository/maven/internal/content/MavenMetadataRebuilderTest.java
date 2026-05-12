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
package org.sonatype.nexus.repository.maven.internal.content;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicBoolean;

import org.sonatype.nexus.common.failure.MultipleFailures;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.content.maven.MavenContentFacet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.FluentComponents;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPathParser;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.scheduling.CancelableHelper;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import static java.lang.Thread.sleep;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.slf4j.Logger.ROOT_LOGGER_NAME;

@RunWith(MockitoJUnitRunner.Silent.class)
public class MavenMetadataRebuilderTest

{
  @Mock
  private MavenContentFacet mavenContentFacet;

  @Mock
  private MavenPathParser mavenPathParser;

  @Mock
  private Repository repository;

  @Mock
  private Appender<ILoggingEvent> mockAppender;

  @Mock
  private FluentAssets assets;

  @Mock
  private FluentComponents components;

  @Before
  public void setup() {
    when(repository.facet(MavenContentFacet.class)).thenReturn(mavenContentFacet);
    when(repository.getFormat()).thenReturn(new Maven2Format());
    when(mavenContentFacet.getMavenPathParser()).thenReturn(mavenPathParser);
    when(mavenPathParser.parsePath(anyString())).thenReturn(mock(MavenPath.class));
    when(mavenContentFacet.assets()).thenReturn(assets);
    when(mavenContentFacet.components()).thenReturn(components);

    Logger logger = (Logger) LoggerFactory.getLogger(ROOT_LOGGER_NAME);
    logger.addAppender(mockAppender);
  }

  @After
  public void teardown() {
    Logger logger = (Logger) LoggerFactory.getLogger(ROOT_LOGGER_NAME);
    logger.detachAppender(mockAppender);
  }

  @Test
  public void rebuildIsCancelable() throws Exception {
    Component component = mock(Component.class);
    Asset asset = mock(Asset.class);
    doReturn(infiniteContinuation(component)).when(components).browse(anyInt(), anyString());
    doReturn(infiniteContinuation(asset)).when(assets).browse(anyInt(), anyString());

    final AtomicBoolean canceled = new AtomicBoolean(false);
    final List<Throwable> uncaught = new ArrayList<>();
    Thread taskThread = new Thread(() -> {
      CancelableHelper.set(canceled);

      new MavenMetadataRebuilder(20, 10).rebuild(repository, true, false, true, null, null, null);
    });
    taskThread.setUncaughtExceptionHandler((t, e) -> {
      if (e instanceof TaskInterruptedException) {
        return;
      }

      uncaught.add(e);
    });
    taskThread.start();

    sleep((long) (Math.random() * 1000)); // sleep for up to a second (emulate task running)
    canceled.set(true); // cancel the task
    taskThread.join(5000); // ensure task thread ends

    if (taskThread.isAlive()) {
      fail("Task did not cancel");
    }

    if (uncaught.size() > 0) {
      throw new MultipleFailureException(uncaught);
    }
  }

  @Test
  public void rebuildIsCancelable_CascadeDisabled() throws Exception {
    Component component = mock(Component.class);
    Asset asset = mock(Asset.class);
    doReturn(infiniteContinuation(component)).when(components).browse(anyInt(), anyString());
    doReturn(infiniteContinuation(asset)).when(assets).browse(anyInt(), anyString());

    final AtomicBoolean canceled = new AtomicBoolean(false);
    final List<Throwable> uncaught = new ArrayList<>();
    Thread taskThread = new Thread(() -> {
      // CancelableHelper.set(canceled);

      new MavenMetadataRebuilder(20, 10).rebuild(repository, true, false, false, "test_GroupId", "test_ArtifactId",
          null);
    });
    taskThread.setUncaughtExceptionHandler((t, e) -> {
      if (e instanceof TaskInterruptedException) {
        return;
      }

      uncaught.add(e);
    });
    taskThread.start();

    sleep((long) (Math.random() * 1000)); // sleep for up to a second (emulate task running)
    canceled.set(true); // cancel the task
    taskThread.join(5000); // ensure task thread ends

    if (taskThread.isAlive()) {
      fail("Task did not cancel");
    }

    if (uncaught.size() > 0) {
      throw new MultipleFailureException(uncaught);
    }
  }

  @Test
  public void rebuild_GA_Flow() throws Exception {
    int bufferSize = 20;
    int maxThreads = 1;
    final String group1 = "group1";
    final String artifact1 = "artifact1";
    final String version1 = "1.0-SNAPSHOT";
    List<String> baseVersions = Collections.singletonList(version1);

    Content content = mock(Content.class);
    FluentComponent component = mock(FluentComponent.class);
    Continuation<FluentComponent> fluentComponents = new ContinuationArrayList<>();
    fluentComponents.add(component);
    when(mavenContentFacet.get(nullable(MavenPath.class))).thenReturn(Optional.of(content));
    when(mavenContentFacet.getBaseVersions(group1, artifact1)).thenReturn(baseVersions);
    when(mavenContentFacet.findComponentsForBaseVersion(anyInt(), eq(null), eq(group1), eq(artifact1), eq(version1)))
        .thenReturn(fluentComponents);

    MavenMetadataRebuilder mavenMetadataRebuilder = new MavenMetadataRebuilder(bufferSize, maxThreads);

    DatastoreMetadataUpdater metadataUpdaterSpy = Mockito.spy(new DatastoreMetadataUpdater(true, repository));
    MetadataRebuildWorker worker = new MetadataRebuildWorker(repository, true, group1, artifact1, null, bufferSize);
    worker.setMetadataUpdater(metadataUpdaterSpy);
    MetadataRebuildWorker workerSpy = Mockito.spy(worker);

    doNothing().when(workerSpy).rebuildGroupMetadata(group1);
    doNothing().when(metadataUpdaterSpy).write(any(), any());

    mavenMetadataRebuilder.rebuildWithWorker(workerSpy, false, true, group1, artifact1, null);

    Thread.sleep(1_000L);

    verify(workerSpy, times(1)).rebuildGA(group1, artifact1);
    verify(workerSpy, times(1)).rebuildBaseVersionsAndChecksums(group1, artifact1, baseVersions, false);
    verify(workerSpy, times(1)).rebuildVersionsMetadata(group1, artifact1, baseVersions);
    verify(workerSpy, times(1)).rebuildArtifactMetadata(repository, group1, artifact1);

    MultipleFailures failures = worker.getFailures();
    assertThat(failures.size(), is(0));
  }

  @Test
  public void rebuild_GA_Flow_not_SNAPSHOT() throws Exception {
    int bufferSize = 20;
    int maxThreads = 1;
    final String group1 = "group1";
    final String artifact1 = "artifact1";
    final String version1 = "1.0";
    List<String> baseVersions = Collections.singletonList(version1);

    Content content = mock(Content.class);
    when(mavenContentFacet.get(nullable(MavenPath.class))).thenReturn(Optional.of(content));
    when(mavenContentFacet.getBaseVersions(group1, artifact1)).thenReturn(baseVersions);

    MavenMetadataRebuilder mavenMetadataRebuilder = new MavenMetadataRebuilder(bufferSize, maxThreads);

    DatastoreMetadataUpdater metadataUpdaterSpy = Mockito.spy(new DatastoreMetadataUpdater(true, repository));
    MetadataRebuildWorker worker = new MetadataRebuildWorker(repository, true, group1, artifact1, null, bufferSize);
    worker.setMetadataUpdater(metadataUpdaterSpy);
    MetadataRebuildWorker workerSpy = Mockito.spy(worker);

    doNothing().when(workerSpy).rebuildGroupMetadata(group1);
    doNothing().when(metadataUpdaterSpy).write(any(), any());

    mavenMetadataRebuilder.rebuildWithWorker(workerSpy, false, true, group1, artifact1, null);

    Thread.sleep(1_000L);

    verify(workerSpy, times(1)).rebuildGA(group1, artifact1);
    verify(workerSpy, times(1)).rebuildBaseVersionsAndChecksums(group1, artifact1, baseVersions, false);
    verify(workerSpy, times(1)).rebuildVersionsMetadata(group1, artifact1, baseVersions);
    verify(workerSpy, times(1)).rebuildArtifactMetadata(repository, group1, artifact1);

    MultipleFailures failures = worker.getFailures();
    assertThat(failures.size(), is(0));
  }

  @Test
  public void deleteMetadata_deletesArtifactLevelMetadataSuccessfully() {
    // Setup: artifact a:b:1.0.0 with metadata at /a/b/maven-metadata.xml
    List<String[]> gavs = Arrays.<String[]>asList(
        new String[]{"a", "b", "1.0.0"});

    // No components exist at namespace=a, name=b (safe to delete)
    when(mavenContentFacet.getBaseVersions("a", "b")).thenReturn(Collections.emptyList());
    when(mavenContentFacet.delete(any(List.class))).thenReturn(true);

    MavenMetadataRebuilder rebuilder = new MavenMetadataRebuilder(1000, 1);
    Set<String> deletedPaths = rebuilder.deleteMetadata(repository, gavs);

    // Verify metadata and checksums were deleted
    assertThat(deletedPaths.size(), is(5)); // maven-metadata.xml + .md5 + .sha1 + .sha256 + .sha512
    verify(mavenContentFacet, times(1)).delete(any(List.class));
  }

  @Test
  public void deleteMetadata_deletesGroupLevelMetadataWhenNoPathCollision() {
    // Setup: group-level metadata for groupId=a.b at /a/b/maven-metadata.xml
    List<String[]> gavs = Arrays.<String[]>asList(
        new String[]{"a.b", null, null} // Group-level delete
    );

    // No artifact with namespace=a, name=b exists (safe to delete)
    when(mavenContentFacet.getBaseVersions("a", "b")).thenReturn(Collections.emptyList());
    when(mavenContentFacet.delete(any(List.class))).thenReturn(true);

    MavenMetadataRebuilder rebuilder = new MavenMetadataRebuilder(1000, 1);
    Set<String> deletedPaths = rebuilder.deleteMetadata(repository, gavs);

    // Verify metadata was deleted
    assertThat(deletedPaths.size(), is(5)); // maven-metadata.xml + .md5 + .sha1 + .sha256 + .sha512
    verify(mavenContentFacet, times(1)).delete(any(List.class));
  }

  @Test
  public void deleteMetadata_skipsGroupLevelDeleteWhenPathCollisionDetected() {
    // NEXUS-47092: Setup path collision scenario
    // - Group metadata for groupId=a.b at /a/b/maven-metadata.xml
    // - Artifact a:b:1.0.0 also has metadata at /a/b/maven-metadata.xml
    List<String[]> gavs = Arrays.<String[]>asList(
        new String[]{"a.b", null, null} // Group-level delete attempt
    );

    // Artifact with namespace=a, name=b exists (path collision!)
    when(mavenContentFacet.getBaseVersions("a", "b"))
        .thenReturn(Collections.singletonList("1.0.0"));

    MavenMetadataRebuilder rebuilder = new MavenMetadataRebuilder(1000, 1);
    Set<String> deletedPaths = rebuilder.deleteMetadata(repository, gavs);

    // Verify metadata was NOT deleted due to path collision
    assertThat(deletedPaths.size(), is(0));
    verify(mavenContentFacet, times(0)).delete(any(List.class));
  }

  @Test
  public void deleteMetadata_handlesMultipleGavsWithMixedPathCollisions() {
    // NEXUS-47092: Mixed scenario with collision and non-collision
    List<String[]> gavs = Arrays.<String[]>asList(
        new String[]{"a.b", null, null}, // Group-level: has collision
        new String[]{"c.d", null, null}, // Group-level: no collision
        new String[]{"e", "f", "1.0.0"} // Artifact-level: always safe
    );

    // a.b has path collision with artifact a:b
    when(mavenContentFacet.getBaseVersions("a", "b"))
        .thenReturn(Collections.singletonList("1.0.0"));

    // c.d has no collision
    when(mavenContentFacet.getBaseVersions("c", "d"))
        .thenReturn(Collections.emptyList());

    // e.f is artifact-level (no collision check needed)
    when(mavenContentFacet.getBaseVersions("e", "f"))
        .thenReturn(Collections.emptyList());

    when(mavenContentFacet.delete(any(List.class))).thenReturn(true);

    MavenMetadataRebuilder rebuilder = new MavenMetadataRebuilder(1000, 1);
    Set<String> deletedPaths = rebuilder.deleteMetadata(repository, gavs);

    // Verify only c.d and e:f:1.0.0 were deleted (a.b was skipped)=assertThat(deletedPaths.size(), is(10)); // 2
    // metadata files * 5 (xml + md5 + sha1 + sha256 + sha512)
    verify(mavenContentFacet, times(2)).delete(any(List.class));
  }

  @Test
  public void deleteMetadata_allowsDeleteWhenPathHasLessThanTwoSegments() {
    // Edge case: single-segment group ID (e.g., "a")
    List<String[]> gavs = Arrays.<String[]>asList(
        new String[]{"a", null, null} // Group-level with single segment
    );

    when(mavenContentFacet.delete(any(List.class))).thenReturn(true);

    MavenMetadataRebuilder rebuilder = new MavenMetadataRebuilder(1000, 1);
    Set<String> deletedPaths = rebuilder.deleteMetadata(repository, gavs);

    // Should delete since less than 2 segments means no collision possible
    assertThat(deletedPaths.size(), is(5)); // maven-metadata.xml + .md5 + .sha1 + .sha256 + .sha512
    verify(mavenContentFacet, times(1)).delete(any(List.class));
  }

  @Test
  public void deleteMetadata_handlesDeepPathCollision() {
    // NEXUS-47092: Deep path collision
    // - Group metadata for groupId=a.b.c.d at /a/b/c/d/maven-metadata.xml
    // - Artifact a.b.c:d:1.0.0 also has metadata at /a/b/c/d/maven-metadata.xml
    List<String[]> gavs = Arrays.<String[]>asList(
        new String[]{"a.b.c.d", null, null} // Group-level delete
    );

    // Artifact with namespace=a.b.c, name=d exists (path collision!)
    when(mavenContentFacet.getBaseVersions("a.b.c", "d"))
        .thenReturn(Collections.singletonList("1.0.0"));

    MavenMetadataRebuilder rebuilder = new MavenMetadataRebuilder(1000, 1);
    Set<String> deletedPaths = rebuilder.deleteMetadata(repository, gavs);

    // Verify metadata was NOT deleted due to path collision
    assertThat(deletedPaths.size(), is(0));
    verify(mavenContentFacet, times(0)).delete(any(List.class));
  }

  @Test
  public void deleteMetadata_allowsDeleteWhenExceptionOccursDuringCollisionCheck() {
    // Fail-safe behavior: if collision check throws exception, allow deletion
    List<String[]> gavs = Arrays.<String[]>asList(
        new String[]{"a.b", null, null});

    // Simulate database error during collision check
    when(mavenContentFacet.getBaseVersions("a", "b"))
        .thenThrow(new RuntimeException("Database error"));
    when(mavenContentFacet.delete(any(List.class))).thenReturn(true);

    MavenMetadataRebuilder rebuilder = new MavenMetadataRebuilder(1000, 1);
    Set<String> deletedPaths = rebuilder.deleteMetadata(repository, gavs);

    // Verify metadata was deleted (fail-safe: when in doubt, allow deletion)
    assertThat(deletedPaths.size(), is(5)); // maven-metadata.xml + .md5 + .sha1 + .sha256 + .sha512
    verify(mavenContentFacet, times(1)).delete(any(List.class));
  }

  @Test
  public void deleteMetadata_handlesEmptyGavList() {
    // Edge case: empty list
    List<String[]> gavs = Collections.emptyList();

    MavenMetadataRebuilder rebuilder = new MavenMetadataRebuilder(1000, 1);
    Set<String> deletedPaths = rebuilder.deleteMetadata(repository, gavs);

    // Verify no deletions occurred
    assertThat(deletedPaths.size(), is(0));
    verify(mavenContentFacet, times(0)).delete(any(List.class));
  }

  @Test
  public void deleteMetadata_handlesDeleteFailure() {
    // Setup: artifact metadata delete fails
    List<String[]> gavs = Arrays.<String[]>asList(
        new String[]{"a", "b", "1.0.0"});

    when(mavenContentFacet.getBaseVersions("a", "b")).thenReturn(Collections.emptyList());
    when(mavenContentFacet.delete(any(List.class))).thenReturn(false); // Delete fails

    MavenMetadataRebuilder rebuilder = new MavenMetadataRebuilder(1000, 1);
    Set<String> deletedPaths = rebuilder.deleteMetadata(repository, gavs);

    // Verify no paths were added to deletedPaths when delete fails
    assertThat(deletedPaths.size(), is(0));
    verify(mavenContentFacet, times(1)).delete(any(List.class));
  }

  private Continuation infiniteContinuation(final Object returnItem) {
    Continuation continuation = mock(Continuation.class);
    Iterator iterator = mock(Iterator.class);
    Spliterator spliterator = mock(Spliterator.class);

    when(continuation.spliterator()).thenReturn(spliterator);
    when(continuation.iterator()).thenReturn(iterator);
    when(iterator.hasNext()).thenReturn(true);
    when(iterator.next()).thenReturn(returnItem);

    return continuation;
  }

  private static class ContinuationArrayList<E>
      extends ArrayList<E>
      implements Continuation<E>
  {
    @Override
    public String nextContinuationToken() {
      return null;
    }
  }
}
