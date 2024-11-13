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

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.inject.Inject;

import org.joda.time.DateTime;
import org.sonatype.nexus.cleanup.storage.CleanupPolicy;
import org.sonatype.nexus.cleanup.storage.CleanupPolicyStorage;
import org.sonatype.nexus.common.log.LogManager;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.security.subject.FakeAlmightySubject;
import org.sonatype.nexus.testsuite.testsupport.RepositoryITSupport;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.shiro.util.ThreadContext;
import org.junit.Before;
import org.junit.experimental.categories.Category;

import static com.google.common.collect.Sets.newLinkedHashSet;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;
import static org.sonatype.nexus.cleanup.config.CleanupPolicyConstants.IS_PRERELEASE_KEY;
import static org.sonatype.nexus.cleanup.config.CleanupPolicyConstants.LAST_BLOB_UPDATED_KEY;
import static org.sonatype.nexus.cleanup.config.CleanupPolicyConstants.LAST_DOWNLOADED_KEY;
import static org.sonatype.nexus.cleanup.config.CleanupPolicyConstants.REGEX_KEY;
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

  public static final String RETAIN_SORT_KEY = "sortBy";

  public static final String RETAIN_COUNT_KEY = "retain";

  protected static final int ONE_HUNDRED_SECONDS = 100;

  protected static final int FIFTY_SECONDS = 50;

  protected static final int THREE_SECONDS = 3;

  @Inject
  private CleanupTestHelper cleanupTestHelper;

  @Inject
  private CleanupPolicyStorage cleanupPolicyStorage;

  @Inject
  protected LogManager logManager;

  @Before
  public void setupSearchSecurity() {
    ThreadContext.bind(FakeAlmightySubject.forUserId("disabled-security"));
  }

  protected void setEmptyPolicy(
          final Repository repository) throws Exception
  {
    createOrUpdatePolicyWithCriteria(testName.getMethodName(), repository.getFormat().getValue(), ImmutableMap.of());
    addPolicyToRepository(testName.getMethodName(), repository);
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

  // This method needs to include an additional criteria (regex) as just prerelease should not delete anything
  protected void setPolicyToBePrereleaseWithAllAssets(final Repository repository, final boolean prerelease) throws Exception {
    createOrUpdatePolicyWithCriteria(repository.getFormat().getValue(),
        ImmutableMap.of(IS_PRERELEASE_KEY, Boolean.toString(prerelease), REGEX_KEY, ".*"));
    addPolicyToRepository(testName.getMethodName(), repository);
  }

  protected void setPolicyToBeRegex(final Repository repository, final String regex) throws Exception {
    String updatedRegex = regex;
    if (regex.charAt(0) >= 'a' && regex.charAt(0) <= 'z') {
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

  protected void setPolicyToBeRetainByVersion(
          final String policyName,
          final Repository repository,
          final int versionCountToKeep) throws Exception
  {
    createOrUpdatePolicyWithCriteria(policyName, repository.getFormat().getValue(),
            ImmutableMap.of(RETAIN_SORT_KEY, "version", RETAIN_COUNT_KEY, Integer.toString(versionCountToKeep),
            REGEX_KEY, ".*"));
    addPolicyToRepository(policyName, repository);
  }

  protected void setPolicyToBeRetainByVersionAndRegex(
          final String policyName,
          final Repository repository,
          final int versionCountToKeep,
          final String regex) throws Exception
  {
    createOrUpdatePolicyWithCriteria(policyName, repository.getFormat().getValue(),
            ImmutableMap.of(RETAIN_SORT_KEY, "version", RETAIN_COUNT_KEY, Integer.toString(versionCountToKeep),
                    REGEX_KEY, regex));
    addPolicyToRepository(policyName, repository);
  }

  protected void setPolicyToBeRetainByDate(
          final String policyName,
          final Repository repository,
          final int countToKeep) throws Exception
  {
    createOrUpdatePolicyWithCriteria(policyName, repository.getFormat().getValue(),
            ImmutableMap.of(RETAIN_SORT_KEY, "date", RETAIN_COUNT_KEY, Integer.toString(countToKeep),
                    REGEX_KEY, ".*"));
    addPolicyToRepository(policyName, repository);
  }

  protected void setPolicyToBeRetainByDateAndRegex(
          final String policyName,
          final Repository repository,
          final int countToKeep,
          final String regex) throws Exception
  {
    createOrUpdatePolicyWithCriteria(policyName, repository.getFormat().getValue(),
            ImmutableMap.of(RETAIN_SORT_KEY, "date", RETAIN_COUNT_KEY, Integer.toString(countToKeep),
                    REGEX_KEY, regex));
    addPolicyToRepository(policyName, repository);
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
      Collection<String> policies = (Collection<String>) cleanup.get(POLICY_NAME_KEY);

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

  protected void setLastDownloadedTimes(final String repositoryName, final int minusSeconds, final String regex) {
    componentAssetTestHelper.setLastDownloadedTime(repositoryManager.get(repositoryName), minusSeconds, regex);
  }

  protected void setAssetBlobUpdatedTime(final Repository repository, final Date date, final String pathRegex) {
    componentAssetTestHelper.setBlobUpdatedTime(repository, pathRegex, date);
  }


  protected void runCleanupTask() throws Exception {
    cleanupTestHelper.waitForIndex();

    TaskInfo task = findCleanupTask().orElseThrow(() -> new IllegalStateException("Cleanup task not found"));

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

  protected void assertNothingRemovedWhenNoCriteria(
          final Repository repository,
          final int startingCount,
          final int expectedCountAfterCleanup) throws Exception
  {
    setEmptyPolicy(repository);
    assertThat(countComponents(testName.getMethodName()), is(startingCount));
    cleanupTestHelper.waitForComponentsIndexed(startingCount);

    runCleanupTask();

    assertThat(countComponents(testName.getMethodName()), is(expectedCountAfterCleanup));
  }

  protected void assertLastBlobUpdatedComponentsCleanedUp(
      final Repository repository,
      final int startingCount,
      final IntSupplier artifactUploader,
      final int expectedCountAfterCleanup) throws Exception
  {
    setPolicyToBeLastBlobUpdatedInSeconds(repository, FIFTY_SECONDS);
    runCleanupTask();

    assertThat(countComponents(repository.getName()), is(startingCount));
    cleanupTestHelper.waitForComponentsIndexed(startingCount);

    // Guarantee that the lastBlobUpdated time has passed
    setAssetBlobUpdatedTime(repository, DateTime.now().minusSeconds(300).toDate(), ".*");

    int numberUploaded = artifactUploader.getAsInt();
    int totalComponents = startingCount + numberUploaded;

    assertThat(countComponents(repository.getName()), is(totalComponents));
    cleanupTestHelper.waitForComponentsIndexed(totalComponents);

    runCleanupTask();

    assertThat(countComponents(testName.getMethodName()), is(expectedCountAfterCleanup));
  }

  protected void assertPartialLastBlobUpdatedComponentsNotCleanedUp(
      final Repository repository,
      final int startingCount) throws Exception
  {
    setPolicyToBeLastBlobUpdatedInSeconds(repository, THREE_SECONDS);
    runCleanupTask();

    assertThat(countComponents(testName.getMethodName()), is(startingCount));
    cleanupTestHelper.waitForComponentsIndexed(startingCount);

    // Guarantee that the lastBlobUpdated time has passed
    cleanupTestHelper.awaitLastBlobUpdatedTimePassed(THREE_SECONDS);

    setAssetBlobUpdatedTime(repository, new Date(), ".*-sources.jar");

    runCleanupTask();

    assertThat(countComponents(testName.getMethodName()), is(startingCount));
  }

  protected void assertLastDownloadedComponentsCleanedUp(
      final Repository repository,
      final int startingCount,
      final IntSupplier artifactUploader,
      final int expectedCountAfterCleanup) throws Exception
  {
    setPolicyToBeLastDownloadedInSeconds(repository, FIFTY_SECONDS);
    runCleanupTask();

    assertThat(countComponents(testName.getMethodName()), is(startingCount));

    setLastDownloadedTimes(testName.getMethodName(), ONE_HUNDRED_SECONDS);
    cleanupTestHelper.waitForComponentsIndexed(startingCount);

    int numberUploaded = artifactUploader.getAsInt();
    int totalComponents = startingCount + numberUploaded;

    assertThat(countComponents(testName.getMethodName()), is(totalComponents));

    cleanupTestHelper.waitForComponentsIndexed(totalComponents);

    runCleanupTask();

    assertThat(countComponents(testName.getMethodName()), is(expectedCountAfterCleanup));
  }

  /* Asserts that is some assets under a component match the lastDownloaded criteria but others do not, then the
     component should not be deleted. Assets whose path matches the regex will be marked as downloaded more recently
     than the criteria. */
  protected void assertPartialLastDownloadedComponentsNotCleanedUp(
      final Repository repository,
      final String regex,
      final int startingCount) throws Exception
  {
    setPolicyToBeLastDownloadedInSeconds(repository, FIFTY_SECONDS);
    runCleanupTask();

    assertThat(countComponents(testName.getMethodName()), is(startingCount));

    setLastDownloadedTimes(testName.getMethodName(), ONE_HUNDRED_SECONDS);
    setLastDownloadedTimes(testName.getMethodName(), THREE_SECONDS, regex);
    cleanupTestHelper.waitForLastDownloadSet(startingCount);

    runCleanupTask();

    assertThat(countComponents(testName.getMethodName()), is(startingCount));
  }

  protected void assertLastBlobUpdatedAndLastDownloadedComponentsCleanUp(
      final Repository repository,
      final int startingCount,
      final IntSupplier lastDownloadedArtifactUploader,
      final IntSupplier componentsToKeepArtifactUploader,
      final String... versionsOfComponentsToKeep) throws Exception
  {
    setPolicyToBeLastBlobUpdatedInSeconds("policyLastBlobUpdated", repository, THREE_SECONDS);
    setPolicyToBeLastDownloadedInSeconds("policyLastDownloaded", repository, FIFTY_SECONDS);

    runCleanupTask();

    assertThat(countComponents(testName.getMethodName()), is(startingCount));

    // Target first three by last blob update, a guarantee that the THREE_SECONDS lastBlobUpdated time has passed
    cleanupTestHelper.awaitLastBlobUpdatedTimePassed(THREE_SECONDS);

    // Deploy new ones to be targeted by last downloaded date/time
    int numberUploaded = lastDownloadedArtifactUploader.getAsInt();

    // Target all, but most specifically, the latest added components by updating their downloaded times
    setLastDownloadedTimes(testName.getMethodName(), ONE_HUNDRED_SECONDS);
    cleanupTestHelper.waitForLastDownloadSet(startingCount + numberUploaded);

    // Deploy new ones to proof that neither policies effected the latest added component
    int numberUploadedVersionsToKeep = componentsToKeepArtifactUploader.getAsInt();
    int totalComponents = startingCount + numberUploaded + numberUploadedVersionsToKeep;

    assertThat(countComponents(testName.getMethodName()), is(totalComponents));

    cleanupTestHelper.waitForComponentsIndexed(totalComponents);

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

    cleanupTestHelper.waitForComponentsIndexed(count);

    setPolicyToBePrereleaseWithAllAssets(repository, true);
    runCleanupTask();

    await().untilAsserted(() -> assertThat(countComponents(testName.getMethodName()), is(countAfterPrereleaseCleanup)));

    setPolicyToBePrereleaseWithAllAssets(repository, false);
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
      final IntSupplier artifactUploader,
      final int expectedCountAfterCleanup) throws Exception
  {
    cleanupTestHelper.waitForComponentsIndexed(startingCount);

    int numberUploaded = artifactUploader.getAsInt();
    int totalComponents = startingCount + numberUploaded;

    assertThat(countComponents(testName.getMethodName()), is(totalComponents));

    setPolicyToBeRegex(repository, expression);

    cleanupTestHelper.waitForComponentsIndexed(totalComponents);

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

    assertThat(countComponents(repository.getName()), is(startingCount));

    // Guarantee that the THREE_SECONDS lastBlobUpdated time has passed
    setAssetBlobUpdatedTime(repository, DateTime.now().minusSeconds(10).toDate(), ".*");

    runCleanupTask();

    // We should still have all components, as both conditions need to be satisfied
    assertThat(countComponents(repository.getName()), is(startingCount));

    setLastDownloadedTimes(repository.getName(), FIFTY_SECONDS * 2);
    cleanupTestHelper.waitForLastDownloadSet(startingCount);

    cleanupTestHelper.waitForMixedSearch();

    runCleanupTask();

    assertThat(countComponents(repository.getName()), is(countAfterCleanup));
  }

  protected void assertRetainByVersionComponentsCleanUp(
          final Repository repository,
          final IntSupplier artifactUploader,
          final Map<String, String[]> endNameVersionsMap) throws Exception
  {
    int startingCount = artifactUploader.getAsInt();

    assertThat(countComponents(repository.getName()), is(startingCount));

    cleanupTestHelper.waitForComponentsIndexed(startingCount);

    runCleanupTask();

    int endCount = endNameVersionsMap.values().stream().mapToInt(v -> v.length).sum();

    await().untilAsserted(() -> assertThat(countComponents(repository.getName()), is(endCount)));


    endNameVersionsMap.forEach((name, versions) -> assertThat(
            Arrays.stream(versions).map(version ->
                    componentAssetTestHelper.componentExists(repository, name, version)).collect(Collectors.toList()),
            everyItem(is(true))));
  }

  protected void assertRetainByDateComponentsCleanUp(
          final Repository repository,
          final IntSupplier initialArtifactUploader,
          final IntSupplier secondaryArtifactUploader,
          final Map<String, String[]> endNameVersionsMap) throws Exception
  {
    int assetCount = initialArtifactUploader.getAsInt();

    assertThat(countComponents(repository.getName()), is(assetCount));

    cleanupTestHelper.waitForComponentsIndexed(assetCount);

    assetCount += secondaryArtifactUploader.getAsInt();

    assertThat(countComponents(repository.getName()), is(assetCount));

    cleanupTestHelper.waitForComponentsIndexed(assetCount);

    runCleanupTask();

    int endCount = endNameVersionsMap.values().stream().mapToInt(v -> v.length).sum();

    await().untilAsserted(() -> assertThat(countComponents(repository.getName()), is(endCount)));

    String components = repository.facet(ContentFacet.class).components().browse(100, null).stream()
            .map(Component::toStringExternal).collect(Collectors.joining("|"));

    for (String name: endNameVersionsMap.keySet()) {
      for (String version: endNameVersionsMap.get(name)) {
        assertTrue(String.format("Component does not exist in (%s): %s:%s", components, name, version),
                componentAssetTestHelper.componentExists(repository, name, version));
      }
    }
  }

  protected String[] generateVersions(int versionCount) {
    if (versionCount < 1) {
      throw new IllegalArgumentException("versionCount must be at least 1");
    }
    return generateVersions(1, versionCount);
  }

  /**
   * Generates a String array holding sequential semantic versions between the first major and the last major *INCLUSIVE*
   * Note: Minor and Revision fields are always zero
   *
   * @param firstMajorVersion the first major version to include
   * @param lastMajorVersion the last major version to include
   * @return a String array containing the generated versions.
   */
  protected String[] generateVersions(int firstMajorVersion, int lastMajorVersion) {
    if (lastMajorVersion < firstMajorVersion) {
      throw new IllegalArgumentException("lastMajorVersion must be greater or equal to firstMajorVersion");
    }
    if (firstMajorVersion < 0) {
      throw new IllegalArgumentException("firstMajorVersion must be zero or greater");
    }

    int versionCount = lastMajorVersion - firstMajorVersion + 1;
    String[] versions = new String[versionCount];
    for (int i = 0; i < versionCount; i++) {
      versions[i] = String.format("%d.0.0", firstMajorVersion + i);
    }
    return versions;
  }

  public static String randomName() {
    return RandomStringUtils.random(5, "abcdefghijklmnopqrstuvwxyz".toCharArray());
  }
}
