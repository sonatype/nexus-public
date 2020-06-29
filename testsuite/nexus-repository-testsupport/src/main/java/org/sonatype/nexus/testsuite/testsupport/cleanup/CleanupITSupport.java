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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.sonatype.nexus.cleanup.storage.CleanupPolicy;
import org.sonatype.nexus.cleanup.storage.CleanupPolicyStorage;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.security.subject.FakeAlmightySubject;
import org.sonatype.nexus.testsuite.testsupport.RepositoryITSupport;
import org.sonatype.nexus.testsuite.testsupport.utility.SearchTestHelper;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.shiro.util.ThreadContext;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.junit.Before;
import org.junit.experimental.categories.Category;

import static com.google.common.collect.Iterables.size;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.rangeQuery;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.joda.time.DateTime.now;
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

  protected static final int TWO_SECONDS = 2;

  private static final BoolQueryBuilder SEARCH_ALL = boolQuery().must(matchAllQuery());

  @Inject
  private SearchTestHelper searchTestHelper;

  @Inject
  private CleanupPolicyStorage cleanupPolicyStorage;

  @Before
  public void setupSearchSecurity() {
    ThreadContext.bind(FakeAlmightySubject.forUserId("disabled-security"));
  }

  protected void setPolicyToBeLastBlobUpdatedInSeconds(final Repository repository,
                                                       final int lastBlobUpdatedSeconds)
      throws Exception
  {
    setPolicyToBeLastBlobUpdatedInSeconds(testName.getMethodName(), repository, lastBlobUpdatedSeconds);
  }

  protected void setPolicyToBeLastBlobUpdatedInSeconds(final String policyName,
                                                       final Repository repository,
                                                       final int lastBlobUpdatedSeconds)
      throws Exception
  {
    createOrUpdatePolicyWithCriteria(policyName,
        repository.getFormat().getValue(),
        ImmutableMap.of(LAST_BLOB_UPDATED_KEY, Integer.toString(lastBlobUpdatedSeconds)));
    addPolicyToRepository(policyName, repository);
  }

  protected void setPolicyToBeLastDownloadedInSeconds(final Repository repository, final int lastDownloadedSeconds)
      throws Exception
  {
    setPolicyToBeLastDownloadedInSeconds(testName.getMethodName(), repository, lastDownloadedSeconds);
  }

  protected void setPolicyToBeLastDownloadedInSeconds(final String policyName,
                                                      final Repository repository,
                                                      final int lastDownloadedSeconds)
      throws Exception
  {
    createOrUpdatePolicyWithCriteria(policyName,
        repository.getFormat().getValue(),
        ImmutableMap.of(LAST_DOWNLOADED_KEY, Integer.toString(lastDownloadedSeconds)));
    addPolicyToRepository(policyName, repository);
  }

  protected void setPolicyToBePrerelease(final Repository repository, final boolean prerelease)
      throws Exception
  {
    createOrUpdatePolicyWithCriteria(repository.getFormat().getValue(),
        ImmutableMap.of(IS_PRERELEASE_KEY, Boolean.toString(prerelease)));
    addPolicyToRepository(testName.getMethodName(), repository);
  }

  protected void setPolicyToBeRegex(final Repository repository, final String regex)
      throws Exception
  {
    createOrUpdatePolicyWithCriteria(repository.getFormat().getValue(), ImmutableMap.of(REGEX_KEY, regex));
    addPolicyToRepository(testName.getMethodName(), repository);
  }

  protected void setPolicyToBeMixed(final Repository repository,
                                    final int lastBlobUpdatedSeconds,
                                    final int lastDownloadedSeconds,
                                    final boolean prerelease)
      throws Exception
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

  protected void createOrUpdatePolicyWithCriteria(final String policyName,
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

    Map<String, Map<String, Object>> attributes = repository.getConfiguration().getAttributes();
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

    repositoryManager.update(repository.getConfiguration());
  }

  protected void setLastDownloadedTimes(final String repositoryName, final int minusSeconds) {
    updateAssets(repositoryName, asset -> asset.lastDownloaded(now().minusSeconds(minusSeconds)));
  }

  protected void updateAssets(final String repositoryName, final Consumer<Asset> updater) {
    Repository repository = repositoryManager.get(repositoryName);
    try (StorageTx tx = repository.facet(StorageFacet.class).txSupplier().get()) {
      tx.begin();

      Iterable<Asset> assets = tx.browseAssets(tx.findBucket(repository));

      for (Asset asset : assets) {
        updater.accept(asset);
        tx.saveAsset(asset);
      }

      tx.commit();
    }
  }

  protected void runCleanupTask() throws Exception {
    searchTestHelper.waitForSearch();

    TaskInfo task = findCleanupTask().get();

    // taskScheduler may have beat us to it; only call runNow if we are in WAITING
    waitFor(() -> task.getCurrentState().getState() == WAITING);

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

  protected List<Component> findComponents(final String repositoryName) {
    Repository repository = repositoryManager.get(repositoryName);
    try (StorageTx tx = repository.facet(StorageFacet.class).txSupplier().get()) {
      tx.begin();

      List<Component> components = new ArrayList<>();

      tx.findComponents(Query.builder().where("1").eq("1").build(), ImmutableList.of(repository))
          .iterator().forEachRemaining(components::add);

      return components;
    }
  }

  protected long countComponents(final String repositoryName) {
    Repository repository = repositoryManager.get(repositoryName);
    try (StorageTx tx = repository.facet(StorageFacet.class).txSupplier().get()) {
      tx.begin();

      return tx.countComponents(Query.builder().where("1").eq("1").build(), ImmutableList.of(repository));
    }
  }

  protected long countAssets(final String repositoryName) {
    Repository repository = repositoryManager.get(repositoryName);
    try (StorageTx tx = repository.facet(StorageFacet.class).txSupplier().get()) {
      tx.begin();

      return tx.countAssets(Query.builder().where("1").eq("1").build(), ImmutableList.of(repository));
    }
  }

  protected void assertLastBlobUpdatedComponentsCleanedUp(final Repository repository,
                                                          final long startingCount,
                                                          final Supplier<Integer> artifactUploader,
                                                          final long expectedCountAfterCleanup) throws Exception
  {
    setPolicyToBeLastBlobUpdatedInSeconds(repository, TWO_SECONDS);
    runCleanupTask();

    assertThat(countComponents(testName.getMethodName()), is(equalTo(startingCount)));

    //Guarantee that the TWO_SECOND lastBlobUpdated time has passed
    Thread.sleep(3000L);

    int numberUploaded = artifactUploader.get();
    long totalComponents = startingCount + numberUploaded;

    assertThat(countComponents(testName.getMethodName()), is(equalTo(totalComponents)));

    waitFor(() -> size(searchTestHelper.queryService().browse(SEARCH_ALL)) == totalComponents);

    runCleanupTask();

    assertThat(countComponents(testName.getMethodName()), is(equalTo(expectedCountAfterCleanup)));
  }

  protected void assertLastDownloadedComponentsCleanedUp(final Repository repository,
                                                         final long startingCount,
                                                         final Supplier<Integer> artifactUploader,
                                                         final long expectedCountAfterCleanup) throws Exception
  {
    setPolicyToBeLastDownloadedInSeconds(repository, FIFTY_SECONDS);
    runCleanupTask();

    assertThat(countComponents(testName.getMethodName()), is(equalTo(startingCount)));

    setLastDownloadedTimes(testName.getMethodName(), ONE_HUNDRED_SECONDS);

    int numberUploaded = artifactUploader.get();
    long totalComponents = startingCount + numberUploaded;

    assertThat(countComponents(testName.getMethodName()), is(equalTo(totalComponents)));

    waitFor(() -> size(searchTestHelper.queryService().browse(SEARCH_ALL)) == totalComponents);

    runCleanupTask();

    assertThat(countComponents(testName.getMethodName()), is(equalTo(expectedCountAfterCleanup)));
  }

  protected void assertLastBlobUpdatedAndLastDownloadedComponentsCleanUp(
      final Repository repository,
      final long startingCount,
      final Supplier<Integer> lastDownloadedArtifactUploader,
      final Supplier<Integer> componentsToKeepArtifactUploader,
      final String[] versionsOfComponentsToKeep)
      throws Exception
  {
    setPolicyToBeLastBlobUpdatedInSeconds("policyLastBlobUpdated", repository, TWO_SECONDS);
    setPolicyToBeLastDownloadedInSeconds("policyLastDownloaded", repository, FIFTY_SECONDS);

    runCleanupTask();

    assertThat(countComponents(testName.getMethodName()), is(equalTo(startingCount)));

    // Target first three by last blob update, a guarantee that the TWO_SECOND lastBlobUpdated time has passed
    Thread.sleep(3000L);

    // Deploy new ones to be targeted by last downloaded date/time
    int numberUploaded = lastDownloadedArtifactUploader.get();

    // Target all, but most specifically, the latest added components by updating their downloaded times
    setLastDownloadedTimes(testName.getMethodName(), ONE_HUNDRED_SECONDS);

    // Deploy new ones to proof that neither policies effected the latest added component
    int numberUploadedVersionsToKeep = componentsToKeepArtifactUploader.get();
    long totalComponents = startingCount + numberUploaded + numberUploadedVersionsToKeep;

    assertThat(countComponents(testName.getMethodName()), is(equalTo(totalComponents)));

    waitFor(() -> size(searchTestHelper.queryService().browse(SEARCH_ALL)) == totalComponents);

    runCleanupTask();

    List<Component> components = findComponents(testName.getMethodName());
    assertThat(components.size(), is(equalTo(numberUploadedVersionsToKeep)));

    for (String versionKept : versionsOfComponentsToKeep) {
      assertThat(anyComponentMatchVersion(components, versionKept), is(true));
    }
  }

  protected void assertCleanupByPrerelease(final Repository repository,
                                           final long countAfterReleaseCleanup,
                                           final long countAfterPrereleaseCleanup) throws Exception
  {
    assertCleanupByPrerelease(repository, countAfterReleaseCleanup, 0L, countAfterPrereleaseCleanup);
  }

  protected void assertCleanupByPrerelease(final Repository repository,
                                           final long countAfterReleaseCleanup,
                                           final long assetCountAfterReleaseCleanup,
                                           final long countAfterPrereleaseCleanup) throws Exception
  {
    long count = countComponents(repository.getName());
    waitFor(() -> size(searchTestHelper.queryService().browse(
        unrestricted(SEARCH_ALL).inRepositories(repository))) == count);

    setPolicyToBePrerelease(repository, true);
    runCleanupTask();

    waitFor(() -> countComponents(testName.getMethodName()) == countAfterPrereleaseCleanup);

    setPolicyToBePrerelease(repository, false);
    runCleanupTask();

    waitFor(() -> countComponents(testName.getMethodName()) == countAfterReleaseCleanup);

    waitFor(() -> countAssets(testName.getMethodName()) == assetCountAfterReleaseCleanup);
  }

  protected void assertCleanupByRegex(final Repository repository,
                                      final long startingCount,
                                      final String expression,
                                      final Supplier<Integer> artifactUploader,
                                      final long expectedCountAfterCleanup) throws Exception
  {
    assertThat(countComponents(testName.getMethodName()), is(equalTo(startingCount)));

    int numberUploaded = artifactUploader.get();
    long totalComponents = startingCount + numberUploaded;

    assertThat(countComponents(testName.getMethodName()), is(equalTo(totalComponents)));

    setPolicyToBeRegex(repository, expression);

    waitFor(() -> size(searchTestHelper.queryService().browse(SEARCH_ALL)) == totalComponents);

    runCleanupTask();

    assertThat(countComponents(testName.getMethodName()), is(equalTo(expectedCountAfterCleanup)));
  }

  protected void assertCleanupByMixedPolicy(final Repository repository,
                                            final long startingCount,
                                            final long countAfterCleanup) throws Exception
  {
    setPolicyToBeMixed(repository, TWO_SECONDS, FIFTY_SECONDS, true);

    runCleanupTask();

    assertThat(countComponents(testName.getMethodName()), is(equalTo(startingCount)));

    //Guarantee that the TWO_SECOND lastBlobUpdated time has passed
    Thread.sleep(TWO_SECONDS * 1000);

    runCleanupTask();

    assertThat(countComponents(testName.getMethodName()), is(equalTo(startingCount)));

    setLastDownloadedTimes(testName.getMethodName(), FIFTY_SECONDS);

    waitForMixedSearch();

    runCleanupTask();

    assertThat(countComponents(testName.getMethodName()), is(equalTo(countAfterCleanup)));
  }

  private void waitForMixedSearch() throws Exception {
    BoolQueryBuilder query = boolQuery().must(matchAllQuery());

    query.filter(
        rangeQuery(LAST_DOWNLOADED_KEY)
            .lte("now-" + FIFTY_SECONDS + "s")
    ).filter(
        rangeQuery(LAST_BLOB_UPDATED_KEY)
            .lte("now-" + TWO_SECONDS + "s")
    ).must(matchQuery(IS_PRERELEASE_KEY, true));

    waitFor(() -> size(searchTestHelper.queryService().browse(unrestricted(query))) > 0);
  }

  public static String randomName() {
    return RandomStringUtils.random(5, "abcdefghijklmnopqrstuvwxyz".toCharArray());
  }

  protected boolean anyComponentMatchVersion(final List<Component> components, final String version) {
    // check that the version (or name as certain formats don't have a meaning full version, like docker) matches.
    return components.stream().anyMatch(component -> componentMatchesByVersion(component, version));
  }

  protected boolean componentMatchesByVersion(final Component component, final String version) {
    return version.equals(component.version()) || version.equals(component.name());
  }
}
