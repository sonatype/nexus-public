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
package org.sonatype.nexus.testsuite.testsupport.cleanup;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.sonatype.nexus.cleanup.storage.CleanupPolicy;
import org.sonatype.nexus.cleanup.storage.CleanupPolicyStorage;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.security.subject.FakeAlmightySubject;
import org.sonatype.nexus.testsuite.testsupport.RepositoryITSupport;
import org.sonatype.nexus.testsuite.testsupport.utility.SearchTestHelper;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.shiro.util.ThreadContext;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.junit.Before;
import org.junit.experimental.categories.Category;

import static com.google.common.collect.Iterables.size;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.awaitility.Awaitility.await;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.existsQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.rangeQuery;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.IS_PRERELEASE_KEY;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.LAST_BLOB_UPDATED_KEY;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.LAST_DOWNLOADED_KEY;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.REGEX_KEY;
import static org.sonatype.nexus.repository.search.query.RepositoryQueryBuilder.unrestricted;
import static org.sonatype.nexus.scheduling.TaskState.WAITING;

/**
 * Support for cleanup ITs
 */
@Category(CleanupTestGroup.class)
public class CleanupITSupport
    extends RepositoryITSupport
{
  protected static final String TASK_ID = "repository.cleanup";

  protected static final String CLEANUP_MODE = "delete";

  protected static final String POLICY_NAME_KEY = "policyName";

  protected static final String CLEANUP_KEY = "cleanup";

  protected static final int ONE_HUNDRED_SECONDS = 100;

  protected static final int FIFTY_SECONDS = 50;

  protected static final int THREE_SECONDS = 3;

  private static final BoolQueryBuilder SEARCH_ALL = boolQuery().must(matchAllQuery());

  private static final BoolQueryBuilder LAST_DOWNLOAD_SET = createLastDownloadQuery(boolQuery(), "1");

  @Inject
  private SearchTestHelper searchTestHelper;

  @Inject
  private CleanupPolicyStorage cleanupPolicyStorage;

  @Before
  public void setupSearchSecurity() {
    ThreadContext.bind(FakeAlmightySubject.forUserId("disabled-security"));
  }

  protected void setPolicyToBeLastBlobUpdatedInSeconds(
      final Repository repository,
      final int lastBlobUpdatedSeconds) throws Exception
  {
    setPolicyToBeLastBlobUpdatedInSeconds(testName.getMethodName(), repository, lastBlobUpdatedSeconds);
  }

  protected void setPolicyToBeLastBlobUpdatedInSeconds(
      final String policyName,
      final Repository repository,
      final int lastBlobUpdatedSeconds) throws Exception
  {
    createOrUpdatePolicyWithCriteria(policyName, repository.getFormat().getValue(),
        ImmutableMap.of(LAST_BLOB_UPDATED_KEY, Integer.toString(lastBlobUpdatedSeconds)));
    addPolicyToRepository(policyName, repository);
  }

  protected void setPolicyToBeLastDownloadedInSeconds(
      final Repository repository,
      final int lastDownloadedSeconds) throws Exception
  {
    setPolicyToBeLastDownloadedInSeconds(testName.getMethodName(), repository, lastDownloadedSeconds);
  }

  protected void setPolicyToBeLastDownloadedInSeconds(
      final String policyName,
      final Repository repository,
      final int lastDownloadedSeconds) throws Exception
  {
    createOrUpdatePolicyWithCriteria(policyName, repository.getFormat().getValue(),
        ImmutableMap.of(LAST_DOWNLOADED_KEY, Integer.toString(lastDownloadedSeconds)));
    addPolicyToRepository(policyName, repository);
  }

  protected void setPolicyToBePrerelease(final Repository repository, final boolean prerelease) throws Exception {
    createOrUpdatePolicyWithCriteria(repository.getFormat().getValue(),
        ImmutableMap.of(IS_PRERELEASE_KEY, Boolean.toString(prerelease)));
    addPolicyToRepository(testName.getMethodName(), repository);
  }

  protected void setPolicyToBeRegex(final Repository repository, final String regex) throws Exception {
    String updatedRegex = regex;
    if (isNewDb() && regex.charAt(0) >= 'a' && regex.charAt(0) <= 'z') {
      updatedRegex = '/' + regex;
    }
    createOrUpdatePolicyWithCriteria(repository.getFormat().getValue(), ImmutableMap.of(REGEX_KEY, updatedRegex));
    addPolicyToRepository(testName.getMethodName(), repository);
  }

  protected void setPolicyToBeMixed(
      final Repository repository,
      final int lastBlobUpdatedSeconds,
      final int lastDownloadedSeconds,
      final boolean prerelease) throws Exception
  {
    createOrUpdatePolicyWithCriteria(
        repository.getFormat().getValue(),
        ImmutableMap.of(LAST_BLOB_UPDATED_KEY, Integer.toString(lastBlobUpdatedSeconds),
        LAST_DOWNLOADED_KEY, Integer.toString(lastDownloadedSeconds),
        IS_PRERELEASE_KEY, Boolean.toString(prerelease)));

    addPolicyToRepository(testName.getMethodName(), repository);
  }

  protected void createOrUpdatePolicyWithCriteria(final String format, final Map<String, String> criteria) {
    createOrUpdatePolicyWithCriteria(testName.getMethodName(), format, criteria);
  }

  protected void createOrUpdatePolicyWithCriteria(
      final String policyName,
      final String format,
      final Map<String, String> criteria)
  {

    CleanupPolicy existingPolicy = cleanupPolicyStorage.get(testName.getMethodName());

    if (existingPolicy != null) {
      existingPolicy.setCriteria(criteria);
      cleanupPolicyStorage.update(existingPolicy);
    }
    else {
      CleanupPolicy policy = cleanupPolicyStorage.newCleanupPolicy();
      policy.setName(policyName);
      policy.setFormat(format);
      policy.setMode(CLEANUP_MODE);
      policy.setNotes("This is a policy for testing");
      policy.setCriteria(criteria);

      cleanupPolicyStorage.add(policy);
    }
  }

  protected void addPolicyToRepository(final String policyName, final Repository repository) throws Exception {
    Configuration configuration = repository.getConfiguration().copy();
    Map<String, Map<String, Object>> attributes = configuration.getAttributes();
    Map<String, Object> cleanup = attributes.get(CLEANUP_KEY);

    if (nonNull(cleanup)) {
      @SuppressWarnings("unchecked")
      Set<String> policies = (Set<String>) cleanup.get(POLICY_NAME_KEY);

      if (isNull(policies)) {
        policies = newLinkedHashSet();
        policies.add(policyName);
        policies = newLinkedHashSet(policies);
      }
      else if (!policies.contains(policyName)) {
        policies = newLinkedHashSet(policies);
        policies.add(policyName);
      }

      attributes.put(CLEANUP_KEY, ImmutableMap.of(POLICY_NAME_KEY, policies));
    }
    else {
      Set<String> policies = newLinkedHashSet();
      policies.add(policyName);
      attributes.put(CLEANUP_KEY, ImmutableMap.of(POLICY_NAME_KEY, policies));
    }

    repositoryManager.update(configuration.copy());
  }

  protected void setLastDownloadedTimes(final String repositoryName, final int minusSeconds) {
    componentAssetTestHelper.setLastDownloadedTime(repositoryManager.get(repositoryName), minusSeconds);
  }

  protected void runCleanupTask() throws Exception {
    searchTestHelper.waitForSearch();

    TaskInfo task = findCleanupTask().get();

    // taskScheduler may have beat us to it; only call runNow if we are in WAITING
    await().untilAsserted(() -> assertThat(task.getCurrentState().getState(), is(WAITING)));

    // run the cleanup task and wait for the underlying future to return to ensure completion
    task.runNow();
    task.getCurrentState().getFuture().get();
  }

  protected Optional<TaskInfo> findCleanupTask() {
    return taskScheduler.listsTasks()
        .stream()
        .filter(info -> info.getTypeId().equals(TASK_ID))
        .findFirst();
  }

  protected int countComponents(final String repositoryName) {
    Repository repository = repositoryManager.get(repositoryName);
    return componentAssetTestHelper.countComponents(repository);
  }

  protected int countAssets(final String repositoryName) {
    Repository repository = repositoryManager.get(repositoryName);
    return componentAssetTestHelper.countAssets(repository);
  }

  protected void assertLastBlobUpdatedComponentsCleanedUp(
      final Repository repository,
      final int startingCount,
      final Supplier<Integer> artifactUploader,
      final int expectedCountAfterCleanup) throws Exception
  {
    setPolicyToBeLastBlobUpdatedInSeconds(repository, THREE_SECONDS);
    runCleanupTask();

    assertThat(countComponents(testName.getMethodName()), is(startingCount));

    //Guarantee that the TWO_SECOND lastBlobUpdated time has passed
    Thread.sleep(4000L);

    int numberUploaded = artifactUploader.get();
    int totalComponents = startingCount + numberUploaded;

    assertThat(countComponents(testName.getMethodName()), is(totalComponents));

    await()
        .untilAsserted(() -> assertThat(size(searchTestHelper.queryService().browse(SEARCH_ALL)), is(totalComponents)));

    runCleanupTask();

    assertThat(countComponents(testName.getMethodName()), is(expectedCountAfterCleanup));
  }

  protected void assertLastDownloadedComponentsCleanedUp(
      final Repository repository,
      final int startingCount,
      final Supplier<Integer> artifactUploader,
      final int expectedCountAfterCleanup) throws Exception
  {
    setPolicyToBeLastDownloadedInSeconds(repository, FIFTY_SECONDS);
    runCleanupTask();

    assertThat(countComponents(testName.getMethodName()), is(startingCount));

    setLastDownloadedTimes(testName.getMethodName(), ONE_HUNDRED_SECONDS);
    await().untilAsserted(
        () -> assertThat(size(searchTestHelper.queryService().browse(LAST_DOWNLOAD_SET)), is(startingCount)));

    int numberUploaded = artifactUploader.get();
    int totalComponents = startingCount + numberUploaded;

    assertThat(countComponents(testName.getMethodName()), is(totalComponents));

    await()
        .untilAsserted(() -> assertThat(size(searchTestHelper.queryService().browse(SEARCH_ALL)), is(totalComponents)));

    runCleanupTask();

    assertThat(countComponents(testName.getMethodName()), is(expectedCountAfterCleanup));
  }

  protected void assertLastBlobUpdatedAndLastDownloadedComponentsCleanUp(
      final Repository repository,
      final int startingCount,
      final Supplier<Integer> lastDownloadedArtifactUploader,
      final Supplier<Integer> componentsToKeepArtifactUploader,
      final String... versionsOfComponentsToKeep) throws Exception
  {
    setPolicyToBeLastBlobUpdatedInSeconds("policyLastBlobUpdated", repository, THREE_SECONDS);
    setPolicyToBeLastDownloadedInSeconds("policyLastDownloaded", repository, FIFTY_SECONDS);

    runCleanupTask();

    assertThat(countComponents(testName.getMethodName()), is(startingCount));

    // Target first three by last blob update, a guarantee that the TWO_SECOND lastBlobUpdated time has passed
    Thread.sleep(4000L);

    // Deploy new ones to be targeted by last downloaded date/time
    int numberUploaded = lastDownloadedArtifactUploader.get();

    // Target all, but most specifically, the latest added components by updating their downloaded times
    setLastDownloadedTimes(testName.getMethodName(), ONE_HUNDRED_SECONDS);
    await().untilAsserted(() -> assertThat(size(searchTestHelper.queryService().browse(LAST_DOWNLOAD_SET)),
        is(startingCount + numberUploaded)));

    // Deploy new ones to proof that neither policies effected the latest added component
    int numberUploadedVersionsToKeep = componentsToKeepArtifactUploader.get();
    int totalComponents = startingCount + numberUploaded + numberUploadedVersionsToKeep;

    assertThat(countComponents(testName.getMethodName()), is(totalComponents));

    await()
        .untilAsserted(() -> assertThat(size(searchTestHelper.queryService().browse(SEARCH_ALL)), is(totalComponents)));

    runCleanupTask();

    assertThat(countComponents(repository.getName()), is(numberUploadedVersionsToKeep));

    for (String versionKept : versionsOfComponentsToKeep) {
      assertTrue(versionKept, componentAssetTestHelper.componentExistsWithAssetPathMatching(repository,
          componentMatchesByVersion(versionKept)));
    }
  }

  protected void assertCleanupByPrerelease(
      final Repository repository,
      final int countAfterReleaseCleanup,
      final int countAfterPrereleaseCleanup) throws Exception
  {
    assertCleanupByPrerelease(repository, countAfterReleaseCleanup, 0, countAfterPrereleaseCleanup);
  }

  protected void assertCleanupByPrerelease(
      final Repository repository,
      final int countAfterReleaseCleanup,
      final int assetCountAfterReleaseCleanup,
      final int countAfterPrereleaseCleanup) throws Exception
  {
    int count = countComponents(repository.getName());
    await().untilAsserted(() -> assertThat(
        size(searchTestHelper.queryService().browse(unrestricted(SEARCH_ALL).inRepositories(repository))), is(count)));

    setPolicyToBePrerelease(repository, true);
    runCleanupTask();

    await().untilAsserted(() -> assertThat(countComponents(testName.getMethodName()), is(countAfterPrereleaseCleanup)));

    setPolicyToBePrerelease(repository, false);
    runCleanupTask();

    await().untilAsserted(() -> assertThat(countComponents(testName.getMethodName()), is(countAfterReleaseCleanup)));

    await()
        .untilAsserted(() -> assertThat(countAssets(testName.getMethodName()), is(assetCountAfterReleaseCleanup)));
  }

  protected Predicate<String> componentMatchesByVersion(final String version) {
    return path -> path.contains(version);
  }

  protected void assertCleanupByRegex(
      final Repository repository,
      final int startingCount,
      final String expression,
      final Supplier<Integer> artifactUploader,
      final int expectedCountAfterCleanup) throws Exception
  {
    await()
        .untilAsserted(() -> assertThat(size(searchTestHelper.queryService().browse(SEARCH_ALL)), is(startingCount)));

    int numberUploaded = artifactUploader.get();
    int totalComponents = startingCount + numberUploaded;

    assertThat(countComponents(testName.getMethodName()), is(totalComponents));

    setPolicyToBeRegex(repository, expression);

    await()
        .untilAsserted(() -> assertThat(size(searchTestHelper.queryService().browse(SEARCH_ALL)), is(totalComponents)));

    runCleanupTask();

    assertThat(countComponents(testName.getMethodName()), is(expectedCountAfterCleanup));
  }

  protected void assertCleanupByMixedPolicy(
      final Repository repository,
      final int startingCount,
      final int countAfterCleanup) throws Exception
  {
    setPolicyToBeMixed(repository, THREE_SECONDS, FIFTY_SECONDS, true);

    runCleanupTask();

    assertThat(countComponents(testName.getMethodName()), is(startingCount));

    //Guarantee that the TWO_SECOND lastBlobUpdated time has passed
    Thread.sleep(4000);

    runCleanupTask();

    assertThat(countComponents(testName.getMethodName()), is(startingCount));

    setLastDownloadedTimes(testName.getMethodName(), FIFTY_SECONDS);
    await().untilAsserted(
        () -> assertThat(size(searchTestHelper.queryService().browse(LAST_DOWNLOAD_SET)), is(startingCount)));

    waitForMixedSearch();

    runCleanupTask();

    assertThat(countComponents(testName.getMethodName()), is(countAfterCleanup));
  }

  private void waitForMixedSearch() {
    BoolQueryBuilder query = boolQuery().must(matchAllQuery());

    query.filter(rangeQuery(LAST_DOWNLOADED_KEY).lte("now-" + FIFTY_SECONDS + "s"))
        .filter(rangeQuery(LAST_BLOB_UPDATED_KEY).lte("now-" + THREE_SECONDS + "s"))
        .must(matchQuery(IS_PRERELEASE_KEY, true));

    await().untilAsserted(
        () -> assertThat(size(searchTestHelper.queryService().browse(unrestricted(query))), greaterThan(0)));
  }

  public static String randomName() {
    return RandomStringUtils.random(5, "abcdefghijklmnopqrstuvwxyz".toCharArray());
  }

  private static BoolQueryBuilder createLastDownloadQuery(final BoolQueryBuilder query, final String value) {
    String NOW_MINUS_DAYS = "now-%ss";
    BoolQueryBuilder neverDownloadDownloadBuilder = QueryBuilders.boolQuery();
    neverDownloadDownloadBuilder.mustNot(existsQuery(LAST_BLOB_UPDATED_KEY));
    neverDownloadDownloadBuilder.filter(rangeQuery(LAST_BLOB_UPDATED_KEY).lte(format(NOW_MINUS_DAYS, value)));

    RangeQueryBuilder lastDownloadRangeBuilder = rangeQuery(LAST_BLOB_UPDATED_KEY).lte(format(NOW_MINUS_DAYS, value));

    BoolQueryBuilder lastDownloadShouldBuilder = QueryBuilders.boolQuery();
    lastDownloadShouldBuilder.must(lastDownloadRangeBuilder);

    BoolQueryBuilder filterBuilder = QueryBuilders.boolQuery();
    filterBuilder.should(lastDownloadShouldBuilder);
    filterBuilder.should(neverDownloadDownloadBuilder);

    query.filter(filterBuilder);

    return query;
  }
}
