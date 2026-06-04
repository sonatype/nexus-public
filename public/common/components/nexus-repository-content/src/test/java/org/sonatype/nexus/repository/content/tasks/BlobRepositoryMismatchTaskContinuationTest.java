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
package org.sonatype.nexus.repository.content.tasks;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobAttributes;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.datastore.mybatis.ContinuationArrayList;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.MissingFacetException;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;
import org.sonatype.nexus.repository.content.fluent.FluentContinuation;
import org.sonatype.nexus.repository.content.fluent.internal.FluentAssetImpl;
import org.sonatype.nexus.repository.content.store.AssetData;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.spi.TaskResultStateStore;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Assertions;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.blobstore.api.BlobStore.REPO_NAME_HEADER;
import static org.sonatype.nexus.common.entity.Continuations.BROWSE_LIMIT;
import static org.sonatype.nexus.repository.content.tasks.BlobRepositoryMismatchTask.CONTINUATION_TOKEN_PREFIX;
import static org.sonatype.nexus.repository.content.tasks.BlobRepositoryMismatchTask.CURRENT_REPOSITORY_BLOB_COUNT_PREFIX;
import static org.sonatype.nexus.repository.RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.BLOB_STORE_NAME;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.STORAGE;

/**
 * Tests for BlobRepositoryMismatchTask continuation token and paging behavior.
 * Tests memory bounds, page-by-page iteration, and continuation token persistence.
 */
@ExtendWith(AuthenticationExtension.class)
public class BlobRepositoryMismatchTaskContinuationTest
{
  private static final int CONCURRENCY_LIMIT = 5;

  private RepositoryManager repositoryManager;

  private BlobStoreManager blobStoreManager;

  private BlobStore blobStore;

  private ContentFacetSupport contentFacet;

  private FluentAssets fluentAssets;

  private TaskResultStateStore taskResultStateStore;

  private BlobRepositoryMismatchTask underTest;

  @BeforeEach
  public void setup() {
    repositoryManager = mock(RepositoryManager.class);
    blobStoreManager = mock(BlobStoreManager.class);
    blobStore = mock(BlobStore.class);
    contentFacet = mock(ContentFacetSupport.class);
    fluentAssets = mock(FluentAssets.class);
    taskResultStateStore = mock(TaskResultStateStore.class);
    underTest = new BlobRepositoryMismatchTask(blobStoreManager, taskResultStateStore, CONCURRENCY_LIMIT, false);
    underTest.install(repositoryManager, new HostedType());
  }

  // ============== CONTINUATION TOKEN PERSISTS TESTS ==============

  @Test
  public void testContinuationTokenFromSavedConfiguration() throws Exception {
    String repoName = "test-repo";
    String savedToken = "persisted-token-123";

    mockBlobStore();
    Repository repository = mockRepository(repoName, new HostedType());
    when(repositoryManager.get(repoName)).thenReturn(repository);

    ContinuationArrayList<AssetData> page = new ContinuationArrayList<>();
    AssetData asset = mockAssetWithBlob("blob-from-token", repoName);
    page.add(asset);

    lenient().when(fluentAssets.browseEager(eq(1000), eq(savedToken)))
        .thenReturn(new FluentContinuation<>(page, a -> new FluentAssetImpl(contentFacet, a)));

    lenient().when(fluentAssets.browseEager(eq(1000), isNull()))
        .thenReturn(new FluentContinuation<>(new ContinuationArrayList<AssetData>(),
            a -> new FluentAssetImpl(contentFacet, a)));

    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(contentFacet.assets()).thenReturn(fluentAssets);

    TaskConfiguration task = new TaskConfiguration();
    task.setTypeId("typeId");
    task.setId("id");
    task.setString(REPOSITORY_NAME_FIELD_ID, repoName);
    task.setString(CONTINUATION_TOKEN_PREFIX + repoName, savedToken);
    underTest.configure(task);

    underTest.call();

    verify(fluentAssets).browseEager(eq(1000), eq(savedToken));
  }

  @Test
  public void testContinuationTokenClearsAfterComplete() throws Exception {
    String repoName = "test-repo";

    mockBlobStore();
    Repository repository = mockRepository(repoName, new HostedType());
    when(repositoryManager.get(repoName)).thenReturn(repository);

    ContinuationArrayList<AssetData> page = new ContinuationArrayList<>();
    AssetData asset = mockAssetWithBlob("blob-1", repoName);
    page.add(asset);

    lenient().when(fluentAssets.browseEager(anyInt(), any()))
        .thenReturn(new FluentContinuation<>(page, a -> new FluentAssetImpl(contentFacet, a)));

    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(contentFacet.assets()).thenReturn(fluentAssets);

    TaskConfiguration task = new TaskConfiguration();
    task.setTypeId("typeId");
    task.setId("id");
    task.setString(REPOSITORY_NAME_FIELD_ID, repoName);
    underTest.configure(task);

    underTest.call();

    // Task should complete without errors and clear continuation tokens
    verify(fluentAssets, never()).browseEager(eq(1000), eq("nonexistent-token"));

    // Verify continuation token is cleared
    assertThat(underTest.getConfiguration().getString(CONTINUATION_TOKEN_PREFIX + repoName), nullValue());
    // Verify processed count is cleared
    assertThat(underTest.getConfiguration().getLong(CURRENT_REPOSITORY_BLOB_COUNT_PREFIX + repoName, -1L), is(-1L));
  }

  @Test
  public void testMultiRepoTokenCleanup() throws Exception {
    // This test verifies the fix for multi-repo mode: when task runs against "*" (all repos),
    // each individual repository's continuation token should be cleaned up properly when
    // that repository's processing completes.
    String targetRepo = "my-repo";
    String allRepos = "*";

    mockBlobStore();
    Repository repository = mockRepository(targetRepo, new HostedType());

    // Stub repositoryManager.browse() to return a list with the repository
    // This prevents NPE when RepositoryParallelTaskSupport calls .spliterator() on the Iterable
    when(repositoryManager.browse()).thenReturn(Collections.singletonList(repository));

    // Set an existing continuation token to verify cleanup
    TaskConfiguration task = new TaskConfiguration();
    task.setTypeId("typeId");
    task.setId("id");
    task.setString(REPOSITORY_NAME_FIELD_ID, allRepos);
    task.setString(CONTINUATION_TOKEN_PREFIX + targetRepo, "existing-token");
    underTest.configure(task);

    ContinuationArrayList<AssetData> page = new ContinuationArrayList<>();
    AssetData asset = mockAssetWithBlob("blob-1", targetRepo);
    page.add(asset);

    lenient().when(fluentAssets.browseEager(anyInt(), any()))
        .thenReturn(new FluentContinuation<>(page, a -> new FluentAssetImpl(contentFacet, a)));

    lenient().when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    lenient().when(contentFacet.assets()).thenReturn(fluentAssets);

    underTest.call();

    // The target repo's continuation token should be cleared (not "*")
    assertThat(underTest.getConfiguration().getString(CONTINUATION_TOKEN_PREFIX + targetRepo), nullValue());
    assertThat(underTest.getConfiguration().getLong(CURRENT_REPOSITORY_BLOB_COUNT_PREFIX + targetRepo, -1L), is(-1L));
  }

  @Test
  public void testSpecificRepositoryFilterWithContinuation() throws Exception {
    String targetRepo = "target-repo";
    String otherRepo = "other-repo";

    mockBlobStore();

    // Create repositories - mockRepository already sets up repositoryManager.get()
    Repository target = mockRepository(targetRepo, new HostedType());
    Repository other = mockRepository(otherRepo, new HostedType());

    when(target.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(contentFacet.assets()).thenReturn(fluentAssets);

    AssetData asset = mockAssetWithBlob("blob-1", targetRepo);
    ContinuationArrayList<AssetData> page = new ContinuationArrayList<>();
    page.add(asset);

    lenient().when(fluentAssets.browseEager(anyInt(), any()))
        .thenReturn(new FluentContinuation<>(page, a -> new FluentAssetImpl(contentFacet, a)));

    TaskConfiguration task = new TaskConfiguration();
    task.setTypeId("typeId");
    task.setId("id");
    task.setString(REPOSITORY_NAME_FIELD_ID, targetRepo);
    underTest.configure(task);
    underTest.call();

    verify(repositoryManager).get(targetRepo);
    verify(repositoryManager, never()).get(otherRepo);
  }

  // ============== TEST 2.2: CONTINUATION TOKEN SAVED AFTER EACH PAGE ==============

  @Test
  public void testStaleOrphanedTokensCleanedUpOnRepoSwitch() throws Exception {
    // This test verifies that stale continuation tokens are cleaned up when the task
    // is reconfigured to run against a different repository.
    String repoA = "repo-a";
    String repoB = "repo-b";

    // First configure task for repo A with a continuation token (simulating a run in progress)
    TaskConfiguration task = new TaskConfiguration();
    task.setTypeId("typeId");
    task.setId("id");
    task.setString(REPOSITORY_NAME_FIELD_ID, repoA);
    // Set a continuation token and processed count for repo A
    task.setString(CONTINUATION_TOKEN_PREFIX + repoA, "stale-token-for-repo-a");
    task.setString(".currentRepositoryBlobCount." + repoA, "42");
    underTest.configure(task);

    // Now reconfigure task for repo B (different repository)
    // This should trigger cleanup of repo A's stale tokens
    TaskConfiguration task2 = new TaskConfiguration();
    task2.setTypeId("typeId");
    task2.setId("id");
    task2.setString(REPOSITORY_NAME_FIELD_ID, repoB);
    underTest.configure(task2);

    // Verify that the stale token for repo A has been cleaned up
    assertThat("Stale token for repo A should be cleaned up when switching to repo B",
        underTest.getConfiguration().getString(CONTINUATION_TOKEN_PREFIX + repoA),
        nullValue());

    // Verify that the processed count for repo A has been cleaned up
    assertThat("Processed count for repo A should be cleaned up when switching to repo B",
        underTest.getConfiguration().getLong(CURRENT_REPOSITORY_BLOB_COUNT_PREFIX + repoA, -1L),
        is(-1L));
  }

  @Test
  public void testContinuationTokenSavedAfterEachPage() throws Exception {
    String repoName = "large-repo";
    String firstToken = "page-1-token";
    String secondToken = "page-2-token";

    mockBlobStore();
    Repository repository = mockRepository(repoName, new HostedType());
    when(repositoryManager.get(repoName)).thenReturn(repository);

    // Create a single mock AssetData that can be reused for all pages
    // This reduces mock creation overhead while still testing multi-page logic
    AssetData mockAsset = mockAssetWithBlob("blob", repoName);

    // First page returns BROWSE_LIMIT assets with a continuation token
    ContinuationArrayList<AssetData> page1 = new ContinuationArrayList<>();
    for (int i = 0; i < BROWSE_LIMIT; i++) {
      page1.add(mockAsset);
    }
    ContinuationArrayList<AssetData> page2 = new ContinuationArrayList<>();
    for (int i = 0; i < BROWSE_LIMIT; i++) {
      page2.add(mockAsset);
    }
    ContinuationArrayList<AssetData> page3 = new ContinuationArrayList<>();
    for (int i = 0; i < 500; i++) {
      page3.add(mockAsset);
    }

    // Mock browseEager with a counter to track which page is being requested
    int[] pageCounter = {0};
    lenient().when(fluentAssets.browseEager(anyInt(), any()))
        .thenAnswer(invocation -> {
          int currentPage = pageCounter[0]++;
          Continuation<AssetData> mockContinuation;
          if (currentPage == 0) {
            // First page (no token)
            mockContinuation = mock();
            lenient().when(mockContinuation.nextContinuationToken()).thenReturn(firstToken);
            lenient().when(mockContinuation.iterator()).thenAnswer(i -> page1.iterator());
            lenient().when(mockContinuation.size()).thenReturn(BROWSE_LIMIT);
          }
          else if (currentPage == 1) {
            // Second page
            mockContinuation = mock();
            lenient().when(mockContinuation.nextContinuationToken()).thenReturn(secondToken);
            lenient().when(mockContinuation.iterator()).thenAnswer(i -> page2.iterator());
            lenient().when(mockContinuation.size()).thenReturn(BROWSE_LIMIT);
          }
          else if (currentPage == 2) {
            // Third page (last page, no more tokens)
            mockContinuation = mock();
            lenient().when(mockContinuation.nextContinuationToken()).thenReturn(null);
            lenient().when(mockContinuation.iterator()).thenAnswer(i -> page3.iterator());
            lenient().when(mockContinuation.size()).thenReturn(500);
          }
          else {
            // After all pages - empty continuation
            mockContinuation = mock();
            lenient().when(mockContinuation.isEmpty()).thenReturn(true);
            lenient().when(mockContinuation.iterator())
                .thenAnswer(i -> new ContinuationArrayList<AssetData>().iterator());
          }
          return new FluentContinuation<>(mockContinuation, a -> new FluentAssetImpl(contentFacet, a));
        });

    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(contentFacet.assets()).thenReturn(fluentAssets);

    TaskConfiguration task = new TaskConfiguration();
    task.setTypeId("typeId");
    task.setId("id");
    task.setString(REPOSITORY_NAME_FIELD_ID, repoName);
    underTest.configure(task);

    underTest.call();

    // Verify browseEager was called with null for first page, then tokens for subsequent pages
    verify(fluentAssets).browseEager(eq(BROWSE_LIMIT), isNull());
    verify(fluentAssets).browseEager(eq(BROWSE_LIMIT), eq(firstToken));
    verify(fluentAssets).browseEager(eq(BROWSE_LIMIT), eq(secondToken));

    // Verify updateJobDataMap was called for:
    // - firstToken saved after page 1 completes
    // - secondToken saved after page 2 completes
    // - null saved after page 3 completes (final page)
    // = 3 total saves (finished() clears tokens, but that's done via setString(key, null))
    verify(taskResultStateStore, times(3)).updateJobDataMap(any());
  }

  @Test
  public void testContinuationTokenClearedAfterCompletion() throws Exception {
    String repoName = "test-repo";

    mockBlobStore();
    Repository repository = mockRepository(repoName, new HostedType());
    when(repositoryManager.get(repoName)).thenReturn(repository);

    // Empty page to simulate no assets to process
    ContinuationArrayList<AssetData> emptyPage = new ContinuationArrayList<>();

    lenient().when(fluentAssets.browseEager(anyInt(), any()))
        .thenReturn(new FluentContinuation<>(emptyPage, a -> new FluentAssetImpl(contentFacet, a)));

    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(contentFacet.assets()).thenReturn(fluentAssets);

    // First configure task with a continuation token (simulating a run in progress)
    TaskConfiguration task = new TaskConfiguration();
    task.setTypeId("typeId");
    task.setId("id");
    task.setString(REPOSITORY_NAME_FIELD_ID, repoName);
    task.setString(CONTINUATION_TOKEN_PREFIX + repoName, "some-token");
    underTest.configure(task);

    underTest.call();

    // After completion with empty first page, the task should:
    // 1. Process the empty page (no assets to fix)
    // 2. Clear continuation tokens (done by finishedProgress())
    // 3. Call updateJobDataMap for both continuation processing and completion
    verify(taskResultStateStore, times(2)).updateJobDataMap(any());

    // Verify continuation token is cleared
    assertThat(underTest.getConfiguration().getString(CONTINUATION_TOKEN_PREFIX + repoName), nullValue());
    // Verify processed count is cleared
    assertThat(underTest.getConfiguration().getLong(CURRENT_REPOSITORY_BLOB_COUNT_PREFIX + repoName, -1L), is(-1L));
  }

  @Test
  public void testContinuationTokenCallbackWithNullTokenOnLastPage() throws Exception {
    String repoName = "test-repo";

    mockBlobStore();
    Repository repository = mockRepository(repoName, new HostedType());
    when(repositoryManager.get(repoName)).thenReturn(repository);

    // Page with no continuation token (last page)
    ContinuationArrayList<AssetData> page = new ContinuationArrayList<>();
    AssetData asset = mockAssetWithBlob("blob-1", repoName);
    page.add(asset);

    Continuation<AssetData> mockContinuation = mock();
    lenient().when(mockContinuation.nextContinuationToken()).thenReturn(null);
    lenient().when(mockContinuation.iterator()).thenAnswer(invocation -> page.iterator());
    lenient().when(mockContinuation.size()).thenReturn(1);
    lenient().when(fluentAssets.browseEager(anyInt(), any()))
        .thenReturn(new FluentContinuation<>(mockContinuation, a -> new FluentAssetImpl(contentFacet, a)));

    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(contentFacet.assets()).thenReturn(fluentAssets);

    TaskConfiguration task = new TaskConfiguration();
    task.setTypeId("typeId");
    task.setId("id");
    task.setString(REPOSITORY_NAME_FIELD_ID, repoName);
    underTest.configure(task);

    underTest.call();

    // When token is null (last page), the callback still executes but doesn't save progress
    // because of the if (nextToken != null) check in the task
    // Only 1 call for the final completion message
    verify(taskResultStateStore, times(1)).updateJobDataMap(any());
  }

  /**
   * Tests that callback exceptions are handled gracefully and don't prevent task completion.
   * The exception occurs during the continuation callback (after the first page is fully processed),
   * not during browse itself. The first page has a matching REPO_NAME_HEADER, so no fix happens
   * and the result is 0 fixes (coincidental, not caused by the exception).
   */
  @Test
  public void testCallbackExceptionHandled() throws Exception {
    String repoName = "test-repo";

    mockBlobStore();
    Repository repository = mockRepository(repoName, new HostedType());
    when(repositoryManager.get(repoName)).thenReturn(repository);

    // First page with continuation token - asset has matching REPO_NAME_HEADER so no fix needed
    ContinuationArrayList<AssetData> page = new ContinuationArrayList<>();
    AssetData asset = mockAssetWithBlob("blob-1", repoName);
    page.add(asset);

    Continuation<AssetData> mockContinuation = mock();
    lenient().when(mockContinuation.nextContinuationToken()).thenReturn("next-token");
    lenient().when(mockContinuation.iterator()).thenAnswer(invocation -> page.iterator());
    lenient().when(mockContinuation.size()).thenReturn(1);

    // Mock to throw exception on second call (when callback tries to fetch next page)
    final int[] callCount = {0};
    lenient().when(fluentAssets.browseEager(anyInt(), any()))
        .thenAnswer(invocation -> {
          callCount[0]++;
          if (callCount[0] > 1) {
            // Throw exception when callback tries to fetch next page
            throw new RuntimeException("Continuation callback triggered exception");
          }
          return new FluentContinuation<>(mockContinuation, a -> new FluentAssetImpl(contentFacet, a));
        });

    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(contentFacet.assets()).thenReturn(fluentAssets);

    TaskConfiguration task = new TaskConfiguration();
    task.setTypeId("typeId");
    task.setId("id");
    task.setString(REPOSITORY_NAME_FIELD_ID, repoName);
    underTest.configure(task);

    // Task should handle callback exception gracefully and complete
    underTest.call();

    // Task completed despite callback exception
    verify(taskResultStateStore, times(1)).updateJobDataMap(any());

    // Task completes with 0 fixes (asset has matching header, so no fix needed)
    // Note: 0 fixes is due to matching REPO_NAME_HEADER, not the exception
    assertThat(((Long) underTest.result()), equalTo(0L));
  }

  @Test
  public void testContentFacetExceptionHandled() throws Exception {
    String repoName = "test-repo";

    mockBlobStore();
    Repository repository = mockRepository(repoName, new HostedType());
    when(repositoryManager.get(repoName)).thenReturn(repository);

    // Mock facet() to throw MissingFacetException - when ContentFacet is not available,
    // the task should return an empty stream and complete with 0 results
    doThrow(new MissingFacetException(repository, ContentFacet.class)).when(repository).facet(ContentFacet.class);

    TaskConfiguration task = new TaskConfiguration();
    task.setTypeId("typeId");
    task.setId("id");
    task.setString(REPOSITORY_NAME_FIELD_ID, repoName);
    underTest.configure(task);

    // Task should handle ContentFacet exception gracefully by completing with 0 fixes
    underTest.call();

    assertThat(((Long) underTest.result()), equalTo(0L));
  }

  @Test
  public void testContinuationTokenPersistenceCallbackExceptionHandled() throws Exception {
    String repoName = "test-repo";

    mockBlobStore();
    Repository repository = mockRepository(repoName, new HostedType());
    when(repositoryManager.get(repoName)).thenReturn(repository);

    // First page with continuation token
    ContinuationArrayList<AssetData> page = new ContinuationArrayList<>();
    AssetData asset = mockAssetWithBlob("blob-1", repoName);
    page.add(asset);

    Continuation<AssetData> mockContinuation = mock();
    lenient().when(mockContinuation.nextContinuationToken()).thenReturn("next-token");
    lenient().when(mockContinuation.iterator()).thenAnswer(invocation -> page.iterator());
    lenient().when(mockContinuation.size()).thenReturn(1);

    lenient().when(fluentAssets.browseEager(anyInt(), any()))
        .thenReturn(new FluentContinuation<>(mockContinuation, a -> new FluentAssetImpl(contentFacet, a)));

    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(contentFacet.assets()).thenReturn(fluentAssets);

    // Mock updateJobDataMap to throw an exception - this simulates a failure in the callback
    // when persisting the continuation token. The task should handle this gracefully and continue.
    lenient().doThrow(new RuntimeException("Failed to persist continuation token"))
        .when(taskResultStateStore)
        .updateJobDataMap(any());

    TaskConfiguration task = new TaskConfiguration();
    task.setTypeId("typeId");
    task.setId("id");
    task.setString(REPOSITORY_NAME_FIELD_ID, repoName);
    underTest.configure(task);

    // Task should handle callback exception gracefully and continue processing
    underTest.call();

    // The callback exception should be caught and logged, but the task should complete
    // Task completes with 0 fixes because the exception occurred before any assets were processed
    assertThat(((Long) underTest.result()), equalTo(0L));
  }

  /**
   * Tests that the task handles a valid but stale continuation token gracefully when resuming.
   * The token "invalid-token-format" is valid by validation rules (only alphanumeric, hyphens, underscores),
   * but it represents a stale token from a previous run that may no longer be valid.
   * The task should handle this gracefully by returning an empty page and clearing the token.
   */
  @Test
  public void testResumeWithStaleContinuationToken() throws Exception {
    String repoName = "test-repo";
    String staleToken = "invalid-token-format";

    mockBlobStore();
    Repository repository = mockRepository(repoName, new HostedType());
    when(repositoryManager.get(repoName)).thenReturn(repository);

    // The browse function returns empty for stale/unrecognized tokens.
    // Test that the task handles this gracefully and clears the stale token.
    ContinuationArrayList<AssetData> emptyPage = new ContinuationArrayList<>();
    when(fluentAssets.browseEager(anyInt(), eq(staleToken)))
        .thenReturn(new FluentContinuation<>(emptyPage, a -> new FluentAssetImpl(contentFacet, a)));

    // Set up the ContentFacet to return the repository
    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(contentFacet.assets()).thenReturn(fluentAssets);

    TaskConfiguration task = new TaskConfiguration();
    task.setTypeId("typeId");
    task.setId("id");
    task.setString(REPOSITORY_NAME_FIELD_ID, repoName);
    task.setString(CONTINUATION_TOKEN_PREFIX + repoName, staleToken);
    underTest.configure(task);

    // Task should handle stale token gracefully by completing without error
    underTest.call();

    // After processing (even if empty), tokens should be cleared
    assertThat(underTest.getConfiguration().getString(CONTINUATION_TOKEN_PREFIX + repoName), nullValue());
  }

  /**
   * Tests that tokens returned from browse function are validated.
   * When the browse function returns a token with invalid characters (like '/'),
   * the Continuations iterator validates it before calling browseEager again.
   * The invalid token triggers completion callback which clears the continuation token.
   */
  @Test
  public void testBrowseFunctionReturnsInvalidToken() throws Exception {
    String repoName = "test-repo";
    String invalidTokenFromBrowse = "invalid/token"; // Contains '/' which should be rejected

    mockBlobStore();
    Repository repository = mockRepository(repoName, new HostedType());
    when(repositoryManager.get(repoName)).thenReturn(repository);

    // First page succeeds with a valid token
    ContinuationArrayList<AssetData> page = new ContinuationArrayList<>();
    AssetData asset = mockAssetWithBlob("blob-1", repoName);
    page.add(asset);

    // First page returned continuation has invalid token
    Continuation<AssetData> firstPageContinuation = mock();
    lenient().when(firstPageContinuation.nextContinuationToken()).thenReturn(invalidTokenFromBrowse);
    lenient().when(firstPageContinuation.iterator()).thenAnswer(invocation -> page.iterator());
    lenient().when(firstPageContinuation.size()).thenReturn(1);

    // browseEager is only called once for the first page; the invalid token
    // from firstPageContinuation is validated in Continuations.iteratorOf()
    // before browseEager is called again for the next page
    when(fluentAssets.browseEager(anyInt(), any()))
        .thenReturn(new FluentContinuation<>(firstPageContinuation, a -> new FluentAssetImpl(contentFacet, a)));

    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(contentFacet.assets()).thenReturn(fluentAssets);

    TaskConfiguration task = new TaskConfiguration();
    task.setTypeId("typeId");
    task.setId("id");
    task.setString(REPOSITORY_NAME_FIELD_ID, repoName);
    underTest.configure(task);

    // Task should handle invalid token from browse function gracefully
    underTest.call();

    // browseEager should only be called once (for the first page)
    // The invalid token is rejected in Continuations before a second browseEager call
    verify(fluentAssets, times(1)).browseEager(anyInt(), any());

    // The invalid token triggers completion callback which clears the continuation token
    assertThat(underTest.getConfiguration().getString(CONTINUATION_TOKEN_PREFIX + repoName), nullValue());
  }

  /**
   * Tests handling of a token that is at the maximum allowed length (boundary testing).
   * Tokens exceeding the maximum length should be rejected.
   */
  @Test
  public void testMaximumLengthContinuationToken() throws Exception {
    String repoName = "test-repo";
    // Create a token that is 1024 characters long (maximum allowed)
    StringBuilder maxTokenBuilder = new StringBuilder();
    for (int i = 0; i < 1024; i++) {
      maxTokenBuilder.append('a');
    }
    String maxToken = maxTokenBuilder.toString();

    mockBlobStore();
    Repository repository = mockRepository(repoName, new HostedType());
    when(repositoryManager.get(repoName)).thenReturn(repository);

    // Even with a token at maximum length, the browse function should be called
    ContinuationArrayList<AssetData> emptyPage = new ContinuationArrayList<>();
    lenient().when(fluentAssets.browseEager(anyInt(), eq(maxToken)))
        .thenReturn(new FluentContinuation<>(emptyPage, a -> new FluentAssetImpl(contentFacet, a)));

    // Set up the ContentFacet to return the repository
    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(contentFacet.assets()).thenReturn(fluentAssets);

    TaskConfiguration task = new TaskConfiguration();
    task.setTypeId("typeId");
    task.setId("id");
    task.setString(REPOSITORY_NAME_FIELD_ID, repoName);
    task.setString(CONTINUATION_TOKEN_PREFIX + repoName, maxToken);
    underTest.configure(task);

    // Task should handle maximum length token without crashing
    underTest.call();

    // After processing, tokens should be cleared
    assertThat(underTest.getConfiguration().getString(CONTINUATION_TOKEN_PREFIX + repoName), nullValue());
  }

  /**
   * Tests that tokens exceeding the maximum length are rejected.
   */
  @Test
  public void testTokenTooLongIsRejected() throws Exception {
    String repoName = "test-repo";
    // Create a token that is 2048 characters (exceeds the 1024 limit)
    StringBuilder tooLongTokenBuilder = new StringBuilder();
    for (int i = 0; i < 2048; i++) {
      tooLongTokenBuilder.append('a');
    }
    String tooLongToken = tooLongTokenBuilder.toString();

    mockBlobStore();
    Repository repository = mockRepository(repoName, new HostedType());
    when(repositoryManager.get(repoName)).thenReturn(repository);

    TaskConfiguration task = new TaskConfiguration();
    task.setTypeId("typeId");
    task.setId("id");
    task.setString(REPOSITORY_NAME_FIELD_ID, repoName);
    task.setString(CONTINUATION_TOKEN_PREFIX + repoName, tooLongToken);
    underTest.configure(task);

    // Task should fail with IllegalArgumentException for too-long token
    IllegalArgumentException exception =
        Assertions.assertThrows(IllegalArgumentException.class, () -> underTest.call());
    assertThat(exception.getMessage(), containsString("Continuation token too long"));
  }

  /**
   * Tests that totalBlobCount returns -1 when ContentFacet is absent, and the DCL pattern
   * does not repeatedly query the facet on subsequent calls (Long.MIN_VALUE sentinel case).
   */
  @Test
  public void testTotalBlobCountWhenContentFacetAbsent() {
    Repository repository = mock(Repository.class);
    when(repository.optionalFacet(ContentFacet.class)).thenReturn(Optional.empty());

    BlobRepositoryMismatchTask.RepositoryData repoData =
        new BlobRepositoryMismatchTask.RepositoryData(repository, 0L);

    // First call should return -1 (no ContentFacet)
    assertThat(repoData.totalBlobCount(), is(-1L));

    // Second call should also return -1 without re-querying the facet (DCL pattern)
    assertThat(repoData.totalBlobCount(), is(-1L));

    // Verify optionalFacet was called only once (DCL working correctly)
    verify(repository, times(1)).optionalFacet(ContentFacet.class);
  }

  /**
   * Tests that totalBlobCount returns the actual count when ContentFacet is present.
   */
  @Test
  public void testTotalBlobCountWithContentFacet() {
    Repository repository = mock(Repository.class);
    ContentFacet contentFacet = mock(ContentFacet.class);
    FluentAssets fluentAssets = mock(FluentAssets.class);

    when(repository.optionalFacet(ContentFacet.class)).thenReturn(Optional.of(contentFacet));
    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(fluentAssets.count()).thenReturn(500);

    BlobRepositoryMismatchTask.RepositoryData repoData =
        new BlobRepositoryMismatchTask.RepositoryData(repository, 0L);

    assertThat(repoData.totalBlobCount(), is(500L));
    // Second call should use cached value
    assertThat(repoData.totalBlobCount(), is(500L));
    // Facet should only be queried once due to caching
    verify(contentFacet, times(1)).assets();
  }

  /**
   * Tests that totalBlobCount returns -1 when ContentFacet returns 0 assets.
   */
  @Test
  public void testTotalBlobCountZeroAssets() {
    Repository repository = mock(Repository.class);
    ContentFacet contentFacet = mock(ContentFacet.class);
    FluentAssets fluentAssets = mock(FluentAssets.class);

    when(repository.optionalFacet(ContentFacet.class)).thenReturn(Optional.of(contentFacet));
    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(fluentAssets.count()).thenReturn(0);

    BlobRepositoryMismatchTask.RepositoryData repoData =
        new BlobRepositoryMismatchTask.RepositoryData(repository, 0L);

    // When count is 0, totalBlobCount returns 0 (not -1)
    assertThat(repoData.totalBlobCount(), is(0L));
  }

  /**
   * Tests processedBlobCount and incrementProcessedBlobCount.
   */
  @Test
  public void testProcessedBlobCount() {
    Repository repository = mock(Repository.class);

    BlobRepositoryMismatchTask.RepositoryData repoData =
        new BlobRepositoryMismatchTask.RepositoryData(repository, 100L);

    assertThat(repoData.processedBlobCount(), is(100L));

    repoData.incrementProcessedBlobCount(50L);
    assertThat(repoData.processedBlobCount(), is(150L));

    repoData.incrementProcessedBlobCount(25L);
    assertThat(repoData.processedBlobCount(), is(175L));
  }

  /**
   * Tests fixedBlobCount and incrementFixedBlobCount.
   */
  @Test
  public void testFixedBlobCount() {
    Repository repository = mock(Repository.class);

    BlobRepositoryMismatchTask.RepositoryData repoData =
        new BlobRepositoryMismatchTask.RepositoryData(repository, 0L);

    assertThat(repoData.fixedBlobCount(), is(0L));

    repoData.incrementFixedBlobCount();
    assertThat(repoData.fixedBlobCount(), is(1L));

    repoData.incrementFixedBlobCount();
    repoData.incrementFixedBlobCount();
    assertThat(repoData.fixedBlobCount(), is(3L));
  }

  /**
   * Tests percentageComplete calculation.
   */
  @Test
  public void testPercentageComplete() {
    Repository repository = mock(Repository.class);
    ContentFacet contentFacet = mock(ContentFacet.class);
    FluentAssets fluentAssets = mock(FluentAssets.class);

    when(repository.optionalFacet(ContentFacet.class)).thenReturn(Optional.of(contentFacet));
    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(fluentAssets.count()).thenReturn(200);

    BlobRepositoryMismatchTask.RepositoryData repoData =
        new BlobRepositoryMismatchTask.RepositoryData(repository, 50L);

    // 50/200 = 25%
    assertThat(repoData.percentageComplete(), is(25));

    repoData.incrementProcessedBlobCount(50L);
    // 100/200 = 50%
    assertThat(repoData.percentageComplete(), is(50));
  }

  /**
   * Tests percentageComplete returns 0 when totalBlobCount is -1 (no ContentFacet).
   */
  @Test
  public void testPercentageCompleteWhenNoContentFacet() {
    Repository repository = mock(Repository.class);
    when(repository.optionalFacet(ContentFacet.class)).thenReturn(Optional.empty());

    BlobRepositoryMismatchTask.RepositoryData repoData =
        new BlobRepositoryMismatchTask.RepositoryData(repository, 100L);

    // Should return 0% when total is -1
    assertThat(repoData.percentageComplete(), is(0));
  }

  /**
   * Tests percentageComplete returns 0 when totalBlobCount is 0.
   */
  @Test
  public void testPercentageCompleteZeroTotal() {
    Repository repository = mock(Repository.class);
    ContentFacet contentFacet = mock(ContentFacet.class);
    FluentAssets fluentAssets = mock(FluentAssets.class);

    when(repository.optionalFacet(ContentFacet.class)).thenReturn(Optional.of(contentFacet));
    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(fluentAssets.count()).thenReturn(0);

    BlobRepositoryMismatchTask.RepositoryData repoData =
        new BlobRepositoryMismatchTask.RepositoryData(repository, 0L);

    assertThat(repoData.percentageComplete(), is(0));
  }

  /**
   * Tests repository() and name() methods.
   */
  @Test
  public void testRepositoryDataAccessors() {
    Repository repository = mock(Repository.class);
    when(repository.getName()).thenReturn("test-repo");

    BlobRepositoryMismatchTask.RepositoryData repoData =
        new BlobRepositoryMismatchTask.RepositoryData(repository, 0L);

    assertThat(repoData.repository(), is(repository));
    assertThat(repoData.name(), is("test-repo"));
  }

  // ============== HELPER METHODS ==============

  private void mockBlobStore() {
    String name = "my-blobstore";
    lenient().when(blobStoreManager.get(name)).thenReturn(blobStore);

    BlobStoreConfiguration config = mock(BlobStoreConfiguration.class);
    lenient().when(blobStore.getBlobStoreConfiguration()).thenReturn(config);
    lenient().when(config.getType()).thenReturn("File");
    lenient().when(config.getName()).thenReturn(name);
  }

  private void ensureBlobStoreMocked() {
    if (blobStore.getBlobStoreConfiguration() == null) {
      mockBlobStore();
    }
  }

  private Repository mockRepository(final String name, final Type type) {
    ensureBlobStoreMocked();
    Repository repository = mock(Repository.class);
    Configuration config = mock(Configuration.class);
    lenient().when(repository.getConfiguration()).thenReturn(config);

    Map<String, Map<String, Object>> attributes =
        Map.of(STORAGE, Map.of(BLOB_STORE_NAME, blobStore.getBlobStoreConfiguration().getName()));
    lenient().when(config.getAttributes()).thenReturn(attributes);

    lenient().when(repositoryManager.get(name)).thenReturn(repository);
    lenient().when(repository.getName()).thenReturn(name);
    lenient().when(repository.getType()).thenReturn(type);

    return repository;
  }

  private AssetData mockAssetWithBlob(final String blobIdString, final String repoName) {
    AssetData asset = mock(AssetData.class);
    AssetBlob assetBlob = mock(AssetBlob.class);
    BlobRef ref = mock(BlobRef.class);
    BlobId blobId = new BlobId(blobIdString, null);
    Blob blob = mock(Blob.class);

    lenient().when(asset.hasBlob()).thenReturn(true);
    lenient().when(asset.blob()).thenReturn(Optional.of(assetBlob));
    lenient().when(ref.getBlobId()).thenReturn(blobId);
    lenient().when(assetBlob.blobRef()).thenReturn(ref);
    lenient().when(ref.getStore()).thenReturn("my-blobstore");

    lenient().when(blobStoreManager.get("my-blobstore")).thenReturn(blobStore);
    lenient().when(blobStore.get(eq(blobId))).thenReturn(blob);
    lenient().when(blob.getHeaders()).thenReturn(Map.of(REPO_NAME_HEADER, repoName));

    lenient().when(blobStore.createBlobAttributesInstance(eq(blobId), any(), any()))
        .thenAnswer(invocation -> {
          Map<String, String> newHeaders = invocation.getArgument(1);
          BlobAttributes blobAttributes = mock(BlobAttributes.class);
          lenient().when(blobAttributes.getHeaders()).thenReturn(newHeaders);
          return blobAttributes;
        });

    lenient().doAnswer(invocation -> {
      Map<String, String> newHeaders = invocation.getArgument(1);
      lenient().when(blob.getHeaders()).thenReturn(newHeaders);
      return null;
    }).when(blobStore).setBlobAttributes(eq(blobId), any());

    lenient().when(asset.nextContinuationToken()).thenReturn(null);

    return asset;
  }
}
