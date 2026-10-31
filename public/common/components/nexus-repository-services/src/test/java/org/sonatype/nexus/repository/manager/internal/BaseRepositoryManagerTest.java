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
package org.sonatype.nexus.repository.manager.internal;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.EntityMetadata;
import org.sonatype.nexus.common.event.EventHelper;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.common.stateguard.InvalidStateException;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.MissingRepositoryException;
import org.sonatype.nexus.repository.Recipe;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationFacet;
import org.sonatype.nexus.repository.config.ConfigurationStore;
import org.sonatype.nexus.repository.config.internal.ConfigurationData;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.manager.ConfigurationValidator;
import org.sonatype.nexus.repository.manager.DefaultRepositoriesContributor;
import org.sonatype.nexus.repository.manager.RepositoryCreatedEvent;
import org.sonatype.nexus.repository.manager.RepositoryRestoredEvent;
import org.sonatype.nexus.repository.manager.RepositoryDeletedEvent;
import org.sonatype.nexus.repository.rest.api.ProxyRepositoryApiRequestToConfigurationConverter;
import org.sonatype.nexus.repository.rest.api.model.ProxyRepositoryApiRequest;
import org.sonatype.nexus.repository.routing.RoutingRuleStore;
import org.sonatype.nexus.repository.search.index.SearchIndexFacet;

import jakarta.inject.Provider;
import jakarta.validation.ConstraintViolation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import static com.google.common.collect.Iterables.size;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.blobstore.api.BlobStoreManager.DEFAULT_BLOBSTORE_NAME;
import static org.sonatype.nexus.repository.manager.internal.BaseRepositoryManager.CLEANUP_ATTRIBUTES_KEY;
import static org.sonatype.nexus.repository.manager.internal.BaseRepositoryManager.CLEANUP_NAME_KEY;

@RunWith(MockitoJUnitRunner.Silent.class)
public class BaseRepositoryManagerTest
{
  private static final String GROUP_NAME = "group";

  private static final String PARENT_GROUP_NAME = "parentGroup";

  private static final String CYCLE_A_NAME = "cycleA";

  private static final String CYCLE_B_NAME = "cycleB";

  private static final String MAVEN_CENTRAL_NAME = "maven-central";

  private static final String APACHE_SNAPSHOTS_NAME = "apache-snapshots";

  private static final String THIRD_PARTY_NAME = "third-party";

  private static final String UNGROUPED_REPO_NAME = "upgrouped";

  @Mock
  private EventManager eventManager;

  @Mock
  private ConfigurationStore configurationStore;

  @Mock
  private RepositoryFactory repositoryFactory;

  @Mock
  private Provider<ConfigurationFacet> configurationFacetProvider;

  @Mock
  private RepositoryAdminSecurityContributor securityContributor;

  private List<DefaultRepositoriesContributor> defaultRepositoriesContributorList;

  @Mock
  private DefaultRepositoriesContributor defaultRepositoriesContributor;

  @Mock
  private ConfigurationData mavenCentralConfiguration;

  @Mock
  private Repository mavenCentralRepository;

  @Mock
  private Configuration apacheSnapshotsConfiguration;

  @Mock
  private Repository apacheSnapshotsRepository;

  @Mock
  private Configuration thirdPartyConfiguration;

  @Mock
  private Repository thirdPartyRepository;

  @Mock
  private Configuration groupConfiguration;

  @Mock
  private Repository groupRepository;

  @Mock
  private Configuration parentGroupConfiguration;

  @Mock
  private Repository parentGroupRepository;

  @Mock
  private Configuration cycleGroupAConfiguration;

  @Mock
  private Repository cycleGroupA;

  @Mock
  private Configuration cycleGroupBConfiguration;

  @Mock
  private Repository cycleGroupB;

  @Mock
  private Configuration ungroupedRepoConfiguration;

  @Mock
  private Repository ungroupedRepository;

  // Recipe for creating repositories
  @Mock
  private Recipe recipe;

  String recipeName = "mockRecipe";

  @Mock
  private Type type;

  @Mock
  private Type groupType;

  @Mock
  private Format format;

  @Mock
  private NodeAccess nodeAccess;

  @Mock
  private BlobStoreManager blobStoreManager;

  @Mock
  private EntityMetadata entityMetadata;

  @Mock
  private GroupMemberMappingCache groupMemberMappingCache;

  @Mock
  private HttpAuthenticationSecretEncoder httpAuthenticationSecretEncoder;

  @Mock
  private FailedRepositoryTracker failedRepositoryTracker;

  // Subject of the test
  private BaseRepositoryManager<BlobStoreManager> repositoryManager;

  private MockedStatic<QualifierUtil> mockedStatic;

  @Before
  public void setup() {
    mockedStatic = mockStatic(QualifierUtil.class);
    setupRecipe();
    setupRepositories();
    blobstoreProvisionDefaults(false, false);
  }

  @After
  public void tearDown() {
    mockedStatic.close();
  }

  private void setupRecipe() {
    when(recipe.getType()).thenReturn(type);
    when(recipe.getFormat()).thenReturn(format);
  }

  private void setupRepositories() {
    when(defaultRepositoriesContributor.getRepositoryConfigurations())
        .thenReturn(asList(mavenCentralConfiguration, apacheSnapshotsConfiguration, thirdPartyConfiguration,
            groupConfiguration, parentGroupConfiguration, cycleGroupAConfiguration, cycleGroupBConfiguration,
            ungroupedRepoConfiguration));
    defaultRepositoriesContributorList = List.of(defaultRepositoriesContributor);

    mockRepository(mavenCentralConfiguration, mavenCentralRepository, MAVEN_CENTRAL_NAME, "default");
    mockRepository(apacheSnapshotsConfiguration, apacheSnapshotsRepository, APACHE_SNAPSHOTS_NAME, "default");
    mockRepository(thirdPartyConfiguration, thirdPartyRepository, THIRD_PARTY_NAME, "third-party");
    mockRepository(groupConfiguration, groupRepository, GROUP_NAME, "group");
    mockRepository(parentGroupConfiguration, parentGroupRepository, PARENT_GROUP_NAME, "group");
    mockRepository(cycleGroupAConfiguration, cycleGroupA, CYCLE_A_NAME, "group");
    mockRepository(cycleGroupBConfiguration, cycleGroupB, CYCLE_B_NAME, "group");
    mockRepository(ungroupedRepoConfiguration, ungroupedRepository, UNGROUPED_REPO_NAME, "default");

    when(repositoryFactory.create(type, format)).thenReturn(mavenCentralRepository,
        apacheSnapshotsRepository,
        thirdPartyRepository,
        groupRepository,
        parentGroupRepository,
        cycleGroupA,
        cycleGroupB,
        ungroupedRepository);

    when(groupType.getValue()).thenReturn("group");
    setupGroupRepository(groupRepository, groupConfiguration, mavenCentralRepository, apacheSnapshotsRepository);
    setupGroupRepository(parentGroupRepository, parentGroupConfiguration, groupRepository, apacheSnapshotsRepository);
    setupGroupRepository(cycleGroupA, cycleGroupAConfiguration, cycleGroupB, apacheSnapshotsRepository);
    setupGroupRepository(cycleGroupB, cycleGroupBConfiguration, cycleGroupA, apacheSnapshotsRepository);

    setupConfigurationCopy(mavenCentralConfiguration, apacheSnapshotsConfiguration, thirdPartyConfiguration,
        groupConfiguration, parentGroupConfiguration, cycleGroupAConfiguration, cycleGroupBConfiguration,
        ungroupedRepoConfiguration);
  }

  private void setupGroupRepository(
      final Repository repository,
      final Configuration configuration,
      final Repository... members)
  {
    List<Repository> memberRepos = Arrays.asList(members);
    GroupFacet facet = mock(GroupFacet.class);
    NestedAttributesMap attributesMap = mock(NestedAttributesMap.class);
    when(repository.optionalFacet(GroupFacet.class)).thenReturn(Optional.of(facet));
    when(repository.getType()).thenReturn(groupType);
    when(configuration.attributes("group")).thenReturn(attributesMap);
    List<String> memberNames = memberRepos.stream().map(Repository::getName).collect(Collectors.toList());
    when(attributesMap.get("memberNames", Collection.class)).thenReturn(memberNames);
    for (Repository memberRepo : memberRepos) {
      when(facet.member(memberRepo)).thenReturn(true);
    }
    when(facet.members()).thenReturn(memberRepos);
  }

  private void mockRepository(
      final Configuration configuration,
      final Repository repository,
      final String name,
      final String blobstoreName)
  {
    when(configuration.getRecipeName()).thenReturn(recipeName);
    when(configuration.getRepositoryName()).thenReturn(name);

    Map<String, Map<String, Object>> attr = new HashMap<>();
    attr.put("storage", Map.of("blobStoreName", blobstoreName));
    when(configuration.getAttributes()).thenReturn(attr);
    when(repository.getConfiguration()).thenReturn(configuration);
    when(repository.getName()).thenReturn(name);
    when(repository.optionalFacet(any(Class.class))).thenReturn(Optional.empty());
  }

  private BaseRepositoryManager<BlobStoreManager> buildRepositoryManagerImpl(
      final boolean defaultsConfigured,
      final boolean skipDefaultRepositories) throws Exception
  {
    if (defaultsConfigured) {
      when(configurationStore.list())
          .thenReturn(asList(mavenCentralConfiguration, apacheSnapshotsConfiguration, thirdPartyConfiguration,
              groupConfiguration, parentGroupConfiguration, cycleGroupAConfiguration, cycleGroupBConfiguration,
              ungroupedRepoConfiguration));
    }

    return initializeAndStartRepositoryManager(skipDefaultRepositories);
  }

  private BaseRepositoryManager<BlobStoreManager> initializeAndStartRepositoryManager(
      final boolean skipDefaultRepositories) throws Exception
  {
    return initializeAndStartRepositoryManager(skipDefaultRepositories, List.of());
  }

  private BaseRepositoryManager<BlobStoreManager> initializeAndStartRepositoryManager(
      final boolean skipDefaultRepositories,
      final List<ConfigurationValidator> validators) throws Exception
  {
    when(QualifierUtil.buildQualifierBeanMap(any())).thenReturn(Map.of(recipeName, recipe));
    repositoryManager = new BaseRepositoryManager<>(eventManager, configurationStore, repositoryFactory,
        configurationFacetProvider, List.of(), securityContributor,
        defaultRepositoriesContributorList, skipDefaultRepositories, blobStoreManager,
        groupMemberMappingCache, validators, httpAuthenticationSecretEncoder, failedRepositoryTracker)
    {
    };

    repositoryManager.setNodeAccess(nodeAccess);

    repositoryManager.start();
    return repositoryManager;
  }

  private BaseRepositoryManager<BlobStoreManager> buildRepositoryManagerImpl(
      final boolean defaultsConfigured) throws Exception
  {
    return buildRepositoryManagerImpl(defaultsConfigured, false);
  }

  private void blobstoreProvisionDefaults(final boolean provisionDefaults, final boolean clustered) {
    when(blobStoreManager.exists(DEFAULT_BLOBSTORE_NAME)).thenReturn(provisionDefaults || !clustered);
  }

  private void setupConfigurationCopy(final Configuration... configurations) {
    for (Configuration config : configurations) {
      when(config.copy()).thenReturn(config);
    }
  }

  @Test
  public void testLoadsExistingConfigurationOnStartup() throws Exception {
    repositoryManager = buildRepositoryManagerImpl(true);
    when(configurationStore.list())
        .thenReturn(asList(mavenCentralConfiguration, apacheSnapshotsConfiguration, thirdPartyConfiguration));

    assertThat(size(repositoryManager.browse()), equalTo(8));

    verify(mavenCentralRepository).init(mavenCentralConfiguration);
    verify(mavenCentralRepository).start();

    verify(apacheSnapshotsRepository).init(apacheSnapshotsConfiguration);
    verify(apacheSnapshotsRepository).start();

    verify(thirdPartyRepository).init(thirdPartyConfiguration);
    verify(thirdPartyRepository).start();
  }

  @Test
  public void testStartup_createsDefaultRepositoriesWhenEmpty() throws Exception {
    repositoryManager = buildRepositoryManagerImpl(false);

    verify(configurationStore).create(mavenCentralConfiguration);
    verify(configurationStore).create(apacheSnapshotsConfiguration);
    verify(configurationStore).create(thirdPartyConfiguration);
  }

  /**
   * NEXUS-52062: Default repositories must be loaded into memory after first start.
   * Previously, provisionDefaultRepositories() only wrote configs to the store but
   * didn't restore them into the in-memory repositories map, causing 404s on access.
   */
  @Test
  public void testStartup_defaultRepositoriesAreLoadedIntoMemory() throws Exception {
    // Simulate first start: initially empty store, then returns provisioned configs
    when(configurationStore.list())
        .thenReturn(List.of()) // First call: empty store triggers provisioning
        .thenReturn(asList(mavenCentralConfiguration, apacheSnapshotsConfiguration, thirdPartyConfiguration,
            groupConfiguration, parentGroupConfiguration, cycleGroupAConfiguration, cycleGroupBConfiguration,
            ungroupedRepoConfiguration)); // Second call: after provisioning

    repositoryManager = initializeAndStartRepositoryManager(false);

    // Verify repositories are created in the store
    verify(configurationStore).create(mavenCentralConfiguration);
    verify(configurationStore).create(apacheSnapshotsConfiguration);
    verify(configurationStore).create(thirdPartyConfiguration);

    // NEXUS-52062: Verify repositories are also loaded into memory and started
    // (This is the critical fix - previously they were created in DB but not loaded)
    assertThat(repositoryManager.get(MAVEN_CENTRAL_NAME), is(notNullValue()));
    assertThat(repositoryManager.get(APACHE_SNAPSHOTS_NAME), is(notNullValue()));
    assertThat(repositoryManager.get(THIRD_PARTY_NAME), is(notNullValue()));

    // Verify repositories were initialized and started
    verify(mavenCentralRepository).init(mavenCentralConfiguration);
    verify(mavenCentralRepository).start();
    verify(apacheSnapshotsRepository).init(apacheSnapshotsConfiguration);
    verify(apacheSnapshotsRepository).start();
    verify(thirdPartyRepository).init(thirdPartyConfiguration);
    verify(thirdPartyRepository).start();

    // Verify browse() returns the provisioned repositories
    assertThat(size(repositoryManager.browse()), equalTo(8)); // 3 defaults + 5 groups from setup
  }

  @Test
  public void testStartup_obeysSkipDefaults() throws Exception {
    repositoryManager = buildRepositoryManagerImpl(false, true);

    verify(configurationStore, times(0)).create(any(Configuration.class));
  }

  @Test
  public void testStartup_clusteredSkipsDefaults() throws Exception {
    when(nodeAccess.isClustered()).thenReturn(true);
    blobstoreProvisionDefaults(false, true);
    repositoryManager = buildRepositoryManagerImpl(false);

    verify(configurationStore, times(0)).create(any(Configuration.class));
  }

  @Test
  public void testStartup_clusteredCreatesDefaultsWhenBlobStoreExists() throws Exception {
    when(nodeAccess.isClustered()).thenReturn(true);
    blobstoreProvisionDefaults(true, true);
    repositoryManager = buildRepositoryManagerImpl(false);

    verify(configurationStore).create(mavenCentralConfiguration);
    verify(configurationStore).create(apacheSnapshotsConfiguration);
    verify(configurationStore).create(thirdPartyConfiguration);
  }

  @Test
  public void testStartup_noDefaultsProvided() throws Exception {
    when(defaultRepositoriesContributor.getRepositoryConfigurations()).thenReturn(List.of());

    repositoryManager = buildRepositoryManagerImpl(false, false);

    verify(configurationStore, times(0)).create(any(Configuration.class));
  }

  @Test
  public void testExists_dbFallback() throws Exception {
    repositoryManager = buildRepositoryManagerImpl(true);
    when(configurationStore.exists("not-yet-loaded")).thenReturn(true);
    assertThat(repositoryManager.exists("not-yet-loaded"), is(true));
  }

  @Test
  public void testExists_caseInsensitivity() throws Exception {
    repositoryManager = buildRepositoryManagerImpl(true);
    assertThat(repositoryManager.exists(MAVEN_CENTRAL_NAME), is(true));
    assertThat(repositoryManager.exists(MAVEN_CENTRAL_NAME.toUpperCase()), is(true));
    assertThat(repositoryManager.exists("missing-repository"), is(false));
  }

  @Test
  public void testBlobStoreUsageCount() throws Exception {
    repositoryManager = buildRepositoryManagerImpl(true);
    assertThat(repositoryManager.blobstoreUsageCount("default"), equalTo(3L));
    assertThat(repositoryManager.blobstoreUsageCount("third-party"), equalTo(1L));
  }

  @Test
  public void testDelete_successfulDeleteRepository() throws Exception {
    repositoryManager = buildRepositoryManagerImpl(true);
    repositoryManager.delete("maven-central");
  }

  @Test
  public void testDelete_remotesFromGroupRepositories() throws Exception {
    buildRepositoryManagerImpl(true).delete(MAVEN_CENTRAL_NAME);
    assertFalse(
        groupConfiguration.attributes("group").get("memberNames", Collection.class).contains(mavenCentralRepository));
  }

  @Test
  public void testDelete_updateGroupRepositoryWhenMemberDeleted() throws Exception {
    buildRepositoryManagerImpl(true).delete(MAVEN_CENTRAL_NAME);
    verify(configurationStore).update(groupConfiguration);
  }

  @Test
  public void testCreate_concurrentCreatesShouldNotFail() throws Exception {
    BaseRepositoryManager<BlobStoreManager> repositoryManager = initializeAndStartRepositoryManager(true);
    repositoryManager.create(makeRepo("r1"));
    repositoryManager.create(makeRepo("r2"));

    // open an iterator to simulate concurrent access to the private repositories map in RepositoryManagerImpl
    Iterator<Entry<String, Repository>> iterator = reflectRepositories().entrySet().iterator();
    iterator.next();
    repositoryManager.create(makeRepo("r3"));
    // this call will fail with ConcurrentModificationException if the private repositories map is not thread safe
    iterator.next();
  }

  private Map<String, Repository> reflectRepositories() {
    try {
      Field field = BaseRepositoryManager.class.getDeclaredField("repositories");
      field.setAccessible(true);
      return (Map<String, Repository>) field.get(repositoryManager);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Configuration makeRepo(final String repositoryName) {
    Configuration config = mock(Configuration.class);
    when(config.getRepositoryName()).thenReturn(repositoryName);
    when(config.getRecipeName()).thenReturn("mockRecipe");
    return config;
  }

  @Test
  public void testLoadsRepositoryWithCleanupPolicy() throws Exception {
    repositoryManager = buildRepositoryManagerImpl(true);

    String cleanupPolicy1 = randomUUID().toString().replace("-", "");

    Set<String> cleanupPolicies = newHashSet(cleanupPolicy1);

    Map<String, Object> cleanupAttributes = newHashMap();
    cleanupAttributes.put(CLEANUP_NAME_KEY, cleanupPolicies);

    mavenCentralConfiguration.getAttributes().put(CLEANUP_ATTRIBUTES_KEY, cleanupAttributes);

    List<Repository> repositories = repositoryManager.browseForCleanupPolicy(cleanupPolicy1).collect(toList());

    assertRepositoryByCleanupPolicy(repositories, cleanupPolicy1);

    String cleanupPolicy2 = randomUUID().toString().replace("-", "");
    cleanupPolicies.add(cleanupPolicy2);

    // proof we can still search for the first one added
    assertRepositoryByCleanupPolicy(repositories, cleanupPolicy1);

    // proof we can find it by the another added cleanup policies
    assertRepositoryByCleanupPolicy(repositories, cleanupPolicy2);
  }

  @Test
  public void testMultipleRepositoryWithSameCleanupPolicy() throws Exception {
    repositoryManager = buildRepositoryManagerImpl(true);

    String name = randomUUID().toString().replace("-", "");

    Map<String, Object> cleanupAttributes = newHashMap();
    cleanupAttributes.put(CLEANUP_NAME_KEY, newHashSet(name));

    mavenCentralConfiguration.getAttributes().put(CLEANUP_ATTRIBUTES_KEY, cleanupAttributes);
    apacheSnapshotsConfiguration.getAttributes().put(CLEANUP_ATTRIBUTES_KEY, cleanupAttributes);

    List<Repository> repositories = repositoryManager.browseForCleanupPolicy(name).collect(toList());

    assertThat(repositories.size(), equalTo(2));
    assertThat(repositories.get(0).getConfiguration(), equalTo(mavenCentralConfiguration));
    assertThat(repositories.get(1).getConfiguration(), equalTo(apacheSnapshotsConfiguration));
  }

  @Test
  public void testNoRepositoriesLoadedForUnknownCleanupPolicy() throws Exception {
    repositoryManager = buildRepositoryManagerImpl(true);

    Stream<Repository> stream = repositoryManager.browseForCleanupPolicy(randomUUID().toString());

    assertThat(stream.count(), equalTo(0L));
  }

  @Test
  public void testMemberToGroupCacheFunctionsWithNoRepositories() throws Exception {
    repositoryManager = buildRepositoryManagerImpl(false, true);

    // this would throw an NPE previously
    repositoryManager.findContainingGroups("test");
  }

  @Test
  public void getReturnsNullWhenRepositoryNotLoaded() throws Exception {
    // NEXUS-51389: get() no longer lazily loads from DB - it's a pure lookup
    repositoryManager = buildRepositoryManagerImpl(false, true);

    // Repository not loaded during startup
    Repository repository = repositoryManager.get(MAVEN_CENTRAL_NAME);

    // Should return null (no lazy loading)
    assertThat(repository, is(nullValue()));
  }

  @Test
  public void getReturnsNullWhenRepositoryNotInCache() throws Exception {
    // NEXUS-51389: get() is now a pure lookup, returns null if not loaded
    repositoryManager = buildRepositoryManagerImpl(false, true);

    Repository repository = repositoryManager.get("maven-central");

    assertThat(repository, is(nullValue()));
  }

  @Test
  public void repoNotInCacheReturnsNullForSoftGet() throws Exception {
    repositoryManager = buildRepositoryManagerImpl(false, true);

    Repository repository = repositoryManager.softGet("maven-central");

    assertThat(repository, is(nullValue()));
  }

  @Test
  public void repoInCacheReturnsObjectForSoftGet() throws Exception {
    repositoryManager = buildRepositoryManagerImpl(true, false);

    Repository repository = repositoryManager.softGet(MAVEN_CENTRAL_NAME);

    assertThat(repository, is(notNullValue()));
    assertThat(repository.getName(), is(MAVEN_CENTRAL_NAME));
  }

  @SuppressWarnings("unchecked")
  private void assertRepositoryByCleanupPolicy(final List<Repository> repositories, final String cleanupPolicy) {
    assertThat(repositories.size(), equalTo(1));
    assertThat(repositories.get(0).getConfiguration(), equalTo(mavenCentralConfiguration));
    assertThat((Collection<String>) repositories
        .get(0)
        .getConfiguration()
        .getAttributes()
        .get(CLEANUP_ATTRIBUTES_KEY)
        .get(CLEANUP_NAME_KEY),
        hasItems(cleanupPolicy));
  }

  @Test
  public void testDelete_continuesWhenOneGroupUpdateFails() throws Exception {
    // Setup: Create a scenario where updating one group fails
    repositoryManager = buildRepositoryManagerImpl(true);

    // Mock the parent group to throw an exception when updated
    doThrow(new RuntimeException("Test exception")).when(configurationStore).update(parentGroupConfiguration);

    // Delete apache-snapshots which is a member of both 'group' and 'parentGroup'
    repositoryManager.delete(APACHE_SNAPSHOTS_NAME);

    // Verify: The regular group was still updated successfully
    verify(configurationStore).update(groupConfiguration);

    // Verify: Repository was still deleted despite parent group update failure
    verify(apacheSnapshotsRepository).stopSafe();
    verify(apacheSnapshotsRepository).delete();
    verify(apacheSnapshotsRepository).destroy();
    verify(configurationStore).delete(apacheSnapshotsConfiguration);
  }

  @Test
  public void testDelete_handlesMultipleGroupUpdateFailures() throws Exception {
    // Setup: Both groups fail to update
    repositoryManager = buildRepositoryManagerImpl(true);

    doThrow(new RuntimeException("First group failure")).when(configurationStore).update(groupConfiguration);
    doThrow(new RuntimeException("Second group failure")).when(configurationStore).update(parentGroupConfiguration);

    // Delete apache-snapshots which is a member of multiple groups
    repositoryManager.delete(APACHE_SNAPSHOTS_NAME);

    // Verify: Repository deletion still completes despite all group update failures
    verify(apacheSnapshotsRepository).stopSafe();
    verify(apacheSnapshotsRepository).delete();
    verify(apacheSnapshotsRepository).destroy();
    verify(configurationStore).delete(apacheSnapshotsConfiguration);

    // Verify: Both group updates were attempted
    verify(configurationStore).update(groupConfiguration);
    verify(configurationStore).update(parentGroupConfiguration);
  }

  @Test
  public void testDelete_successfullyRemovesFromAllGroups() throws Exception {
    // Setup: Normal scenario where all group updates succeed
    repositoryManager = buildRepositoryManagerImpl(true);

    // Delete apache-snapshots which is a member of multiple groups
    repositoryManager.delete(APACHE_SNAPSHOTS_NAME);

    // Verify: All groups were updated
    verify(configurationStore).update(groupConfiguration);
    verify(configurationStore).update(parentGroupConfiguration);
    verify(configurationStore).update(cycleGroupAConfiguration);
    verify(configurationStore).update(cycleGroupBConfiguration);

    // Verify: Repository was deleted
    verify(apacheSnapshotsRepository).stopSafe();
    verify(apacheSnapshotsRepository).delete();
    verify(apacheSnapshotsRepository).destroy();
    verify(configurationStore).delete(apacheSnapshotsConfiguration);
  }

  @Test
  public void testDelete_repositoryNotInAnyGroup() throws Exception {
    // Setup: Delete a repository that is not a member of any group
    repositoryManager = buildRepositoryManagerImpl(true);

    // Delete ungrouped repository
    repositoryManager.delete(UNGROUPED_REPO_NAME);

    // Verify: No group updates were attempted
    verify(configurationStore, never()).update(groupConfiguration);
    verify(configurationStore, never()).update(parentGroupConfiguration);

    // Verify: Repository was still deleted successfully
    verify(ungroupedRepository).stopSafe();
    verify(ungroupedRepository).delete();
    verify(ungroupedRepository).destroy();
    verify(configurationStore).delete(ungroupedRepoConfiguration);
  }

  @Test
  public void testDelete_alreadyDeletedDuringReplication_shouldSkip() throws Exception {
    try (MockedStatic<EventHelper> eventHelper = mockStatic(EventHelper.class)) {
      eventHelper.when(EventHelper::isReplicating).thenReturn(true);

      repositoryManager = buildRepositoryManagerImpl(true);

      repositoryManager.delete("non-existent-repo");

      // Verify: Should NOT call configurationStore.delete()
      verify(configurationStore, never()).delete(any());
    }
  }

  @Test(expected = MissingRepositoryException.class)
  public void testDelete_alreadyDeletedDirectAPI_shouldThrow() throws Exception {
    try (MockedStatic<EventHelper> eventHelper = mockStatic(EventHelper.class)) {
      eventHelper.when(EventHelper::isReplicating).thenReturn(false);

      repositoryManager = buildRepositoryManagerImpl(true);

      repositoryManager.delete("non-existent-repo");
    }
  }

  @Test
  public void testDelete_removeFromGroup_preservesRepositoryId() throws Exception {
    ConfigurationData copiedGroupConfig = mock(ConfigurationData.class);
    EntityId groupRepositoryId = mock(EntityId.class);

    when(groupConfiguration.copy()).thenReturn(copiedGroupConfig);
    when(groupConfiguration.getRepositoryId()).thenReturn(groupRepositoryId);

    NestedAttributesMap copiedAttributes = mock(NestedAttributesMap.class);
    Collection<String> memberNames = newHashSet(MAVEN_CENTRAL_NAME, APACHE_SNAPSHOTS_NAME);
    when(copiedGroupConfig.attributes("group")).thenReturn(copiedAttributes);
    when(copiedAttributes.get("memberNames", Collection.class)).thenReturn(memberNames);
    when(copiedGroupConfig.getRepositoryName()).thenReturn(GROUP_NAME);

    repositoryManager = buildRepositoryManagerImpl(true);

    // Delete a repository that's in a group (triggers removeRepositoryFromGroup)
    repositoryManager.delete(MAVEN_CENTRAL_NAME);

    verify(groupConfiguration, times(2)).copy();

    // ensuring repositoryId is preserved after copy()
    verify(copiedGroupConfig).setId(groupRepositoryId);

    ArgumentCaptor<Configuration> configCaptor = ArgumentCaptor.forClass(Configuration.class);
    verify(configurationStore).update(configCaptor.capture());

    // Verify the captured config is the one with setId() called on it
    assertThat(configCaptor.getValue(), is(copiedGroupConfig));
  }

  @Test
  public void testDelete_originatingNode_updatesGroups() throws Exception {
    // Test NEXUS-49996: Originating node should update groups during deletion
    repositoryManager = buildRepositoryManagerImpl(true);

    try (MockedStatic<EventHelper> eventHelperMock = mockStatic(EventHelper.class)) {
      // Originating node - NOT replicating
      eventHelperMock.when(EventHelper::isReplicating).thenReturn(false);

      // Delete apache-snapshots which is a member of multiple groups
      repositoryManager.delete(APACHE_SNAPSHOTS_NAME);

      // Verify: Groups were updated (originating node responsible for group updates)
      verify(configurationStore).update(groupConfiguration);
      verify(configurationStore).update(parentGroupConfiguration);
      verify(configurationStore).update(cycleGroupAConfiguration);
      verify(configurationStore).update(cycleGroupBConfiguration);

      // Verify: Repository configuration was deleted
      verify(configurationStore).delete(apacheSnapshotsConfiguration);
    }
  }

  @Test
  public void testDelete_replicatingNode_skipsGroupUpdates() throws Exception {
    // Test NEXUS-49996: Remote nodes should skip group updates to prevent HA race condition
    repositoryManager = buildRepositoryManagerImpl(true);

    try (MockedStatic<EventHelper> eventHelperMock = mockStatic(EventHelper.class)) {
      // Remote node - IS replicating
      eventHelperMock.when(EventHelper::isReplicating).thenReturn(true);

      // Delete apache-snapshots which is a member of multiple groups
      repositoryManager.delete(APACHE_SNAPSHOTS_NAME);

      // Verify: Group updates were SKIPPED (remote node should not update groups)
      verify(configurationStore, never()).update(groupConfiguration);
      verify(configurationStore, never()).update(parentGroupConfiguration);
      verify(configurationStore, never()).update(cycleGroupAConfiguration);
      verify(configurationStore, never()).update(cycleGroupBConfiguration);

      // Verify: Repository configuration deletion was SKIPPED (replication handles this)
      verify(configurationStore, never()).delete(apacheSnapshotsConfiguration);

      // Verify: Repository lifecycle methods were still called
      verify(apacheSnapshotsRepository).stopSafe();
      verify(apacheSnapshotsRepository).delete();
      verify(apacheSnapshotsRepository).destroy();
    }
  }

  @Test
  public void testDelete_haScenario_preventsDuplicateWork() throws Exception {
    // Test NEXUS-49996: Simulate HA scenario where both nodes process same deletion
    repositoryManager = buildRepositoryManagerImpl(true);

    try (MockedStatic<EventHelper> eventHelperMock = mockStatic(EventHelper.class)) {
      // Scenario 1: Originating node deletes repository
      eventHelperMock.when(EventHelper::isReplicating).thenReturn(false);
      repositoryManager.delete(MAVEN_CENTRAL_NAME);

      // Verify: Originating node updated group
      verify(configurationStore).update(groupConfiguration);
      verify(configurationStore).delete(mavenCentralConfiguration);

      // Reset mocks for second scenario
      org.mockito.Mockito.clearInvocations(configurationStore);

      // Scenario 2: Remote node receives replication event for same repository
      // (In real HA, this would be apache-snapshots, but for test simplicity we use same pattern)
      eventHelperMock.when(EventHelper::isReplicating).thenReturn(true);
      repositoryManager.delete(APACHE_SNAPSHOTS_NAME);

      // Verify: Remote node did NOT update groups (preventing "Invalid state: DELETED" error)
      verify(configurationStore, never()).update(any(Configuration.class));
      verify(configurationStore, never()).delete(any(Configuration.class));
    }
  }

  /**
   * NEXUS-53816: Regression test proving that a guarded .member() failure no longer aborts delete.
   * When a group is transiently STOPPED/DELETED during concurrent operations, the @Guarded(by = STARTED)
   * check in GroupFacetImpl.member() throws InvalidStateException. This test verifies the fix that
   * moves the try/catch to wrap the entire loop body in removeRepositoryFromAllGroups().
   */
  @Test
  public void testDelete_continuesWhenGroupMemberCheckThrowsInvalidState() throws Exception {
    repositoryManager = buildRepositoryManagerImpl(true);

    // Simulate a group caught mid-transition: its @Guarded member() check throws, exactly as in
    // the NEXUS-53816 stack trace (GroupFacetImpl.member -> InvalidStateException STOPPED).
    GroupFacet stoppedGroupFacet = groupRepository.optionalFacet(GroupFacet.class).get();
    doThrow(new InvalidStateException("STOPPED", new String[]{"STARTED"}))
        .when(stoppedGroupFacet)
        .member(apacheSnapshotsRepository);

    // Delete must still complete despite the stopped group.
    repositoryManager.delete(APACHE_SNAPSHOTS_NAME);

    // Repository lifecycle completed and its config was removed.
    verify(apacheSnapshotsRepository).stopSafe();
    verify(apacheSnapshotsRepository).delete();
    verify(apacheSnapshotsRepository).destroy();
    verify(configurationStore).delete(apacheSnapshotsConfiguration);

    // Other healthy groups were still updated (the stopped one was skipped, not fatal).
    verify(configurationStore).update(parentGroupConfiguration);
  }

  /**
   * NEXUS-54229: isBlobstoreUsed() must consult the shared configuration store (authoritative across HA nodes)
   * rather than this node's in-memory repository cache, which can lag a peer node's create/delete and yield a
   * false "in use" (blocking a legitimate blob store delete) or a false "not used".
   */
  @Test
  public void testIsBlobstoreUsed_readsFromConfigurationStore() throws Exception {
    repositoryManager = buildRepositoryManagerImpl(true);

    when(blobStoreManager.blobStoreUsageCount("used")).thenReturn(0L);
    when(blobStoreManager.blobStoreUsageCount("unused")).thenReturn(0L);
    when(configurationStore.countByBlobStore("used")).thenReturn(1);
    when(configurationStore.countByBlobStore("unused")).thenReturn(0);

    assertThat(repositoryManager.isBlobstoreUsed("used"), is(true));
    assertThat(repositoryManager.isBlobstoreUsed("unused"), is(false));

    // proves the authoritative store was consulted, not the in-memory browse() cache
    verify(configurationStore).countByBlobStore("used");
    verify(configurationStore).countByBlobStore("unused");
  }

  /**
   * NEXUS-54229: blobstoresInUse() resolves the distinct set of referenced blob store names from the shared
   * store in a single query, so the blob store list UI does not need an isBlobstoreUsed() call per blob store.
   */
  @Test
  public void testBlobStoresInUse_readsDistinctNamesFromStore() throws Exception {
    repositoryManager = buildRepositoryManagerImpl(true);

    Configuration usesA1 = mock(Configuration.class);
    when(usesA1.getAttributes()).thenReturn(Map.of("storage", Map.of("blobStoreName", (Object) "bs-a")));
    Configuration usesA2 = mock(Configuration.class);
    when(usesA2.getAttributes()).thenReturn(Map.of("storage", Map.of("blobStoreName", (Object) "bs-a")));
    Configuration usesB = mock(Configuration.class);
    when(usesB.getAttributes()).thenReturn(Map.of("storage", Map.of("blobStoreName", (Object) "bs-b")));
    Configuration group = mock(Configuration.class); // no storage.blobStoreName -- must be ignored
    when(group.getAttributes()).thenReturn(Map.of("group", Map.of("memberNames", (Object) "x")));
    when(configurationStore.list()).thenReturn(List.of(usesA1, usesA2, usesB, group));

    assertThat(repositoryManager.blobstoresInUse(), equalTo(Set.of("bs-a", "bs-b")));
  }

  @Test
  public void testStartup_groupMemberMappingCacheInitAlwaysCalled_withSkipDefaults() throws Exception {
    // Test NEXUS-50379: Ensure groupMemberMappingCache.init is called even when skipping default repositories
    repositoryManager = buildRepositoryManagerImpl(false, true);

    verify(groupMemberMappingCache).init(repositoryManager);
  }

  @Test
  public void testStartup_groupMemberMappingCacheInitAlwaysCalled_withNoBlobStore() throws Exception {
    // Test NEXUS-50379: Ensure groupMemberMappingCache.init is called when default blobstore doesn't exist
    blobstoreProvisionDefaults(false, false);
    repositoryManager = buildRepositoryManagerImpl(false, false);

    verify(groupMemberMappingCache).init(repositoryManager);
  }

  @Test
  public void testStartup_groupMemberMappingCacheInitAlwaysCalled_withNoDefaultsProvided() throws Exception {
    // Test NEXUS-50379: Ensure groupMemberMappingCache.init is called when no default repositories are provided
    when(defaultRepositoriesContributor.getRepositoryConfigurations()).thenReturn(List.of());
    repositoryManager = buildRepositoryManagerImpl(false, false);

    verify(groupMemberMappingCache).init(repositoryManager);
  }

  @Test
  public void testStartup_groupMemberMappingCacheInitAlwaysCalled_withExistingRepositories() throws Exception {
    // Test NEXUS-50379: Ensure groupMemberMappingCache.init is called when repositories are loaded on startup
    repositoryManager = buildRepositoryManagerImpl(true, false);

    verify(groupMemberMappingCache).init(repositoryManager);
  }

  @Test
  public void testRetryFailedRepository_successfulRetry() throws Exception {
    // Setup: Repository has a recorded failure and retry succeeds
    repositoryManager = buildRepositoryManagerImpl(true);

    String failedRepoName = "failed-repo";
    Configuration failedRepoConfig = mock(Configuration.class);
    Repository failedRepo = mock(Repository.class);

    when(failedRepoConfig.getRepositoryName()).thenReturn(failedRepoName);
    when(failedRepoConfig.getRecipeName()).thenReturn(recipeName);
    when(failedRepo.getName()).thenReturn(failedRepoName);
    when(failedRepo.getConfiguration()).thenReturn(failedRepoConfig);

    // Mock the configuration store to return the failed repo config
    when(configurationStore.list()).thenReturn(asList(failedRepoConfig));

    // Mock the factory to create the repository
    when(repositoryFactory.create(type, format)).thenReturn(failedRepo);

    // Mock the failedRepositoryTracker
    when(failedRepositoryTracker.hasFailed(failedRepoName)).thenReturn(true);

    // Reset mock to clear setup interactions
    reset(eventManager);

    // Execute: Retry the failed repository
    Optional<Repository> result = repositoryManager.retryFailedRepository(failedRepoName);

    // Verify: Repository was successfully created and started
    assertThat(result.isPresent(), is(true));
    assertThat(result.get(), is(failedRepo));
    verify(failedRepo).start();
    verify(failedRepositoryTracker).clearFailure(failedRepoName);

    // Verify: RepositoryRestoredEvent was posted for the failed repository
    ArgumentCaptor<RepositoryRestoredEvent> eventCaptor = ArgumentCaptor.forClass(RepositoryRestoredEvent.class);
    verify(eventManager).post(eventCaptor.capture());
    assertThat(eventCaptor.getValue().getRepository(), is(failedRepo));
  }

  @Test
  public void testRetryFailedRepository_failedRetry() throws Exception {
    // Setup: Repository has a recorded failure and retry still fails
    repositoryManager = buildRepositoryManagerImpl(true);

    String failedRepoName = "failed-repo";
    Configuration failedRepoConfig = mock(Configuration.class);
    Repository failedRepo = mock(Repository.class);

    when(failedRepoConfig.getRepositoryName()).thenReturn(failedRepoName);
    when(failedRepoConfig.getRecipeName()).thenReturn(recipeName);
    when(failedRepo.getName()).thenReturn(failedRepoName);
    when(failedRepo.getConfiguration()).thenReturn(failedRepoConfig);

    // Mock the configuration store to return the failed repo config
    when(configurationStore.list()).thenReturn(asList(failedRepoConfig));

    // Mock the factory to create the repository but throw on start
    when(repositoryFactory.create(type, format)).thenReturn(failedRepo);
    RuntimeException startException = new RuntimeException("Still broken");
    doThrow(startException).when(failedRepo).start();

    // Mock the failedRepositoryTracker
    when(failedRepositoryTracker.hasFailed(failedRepoName)).thenReturn(true);

    // Reset mocks to clear setup interactions
    reset(eventManager, failedRepositoryTracker);
    when(failedRepositoryTracker.hasFailed(failedRepoName)).thenReturn(true);

    // Execute: Retry the failed repository
    Optional<Repository> result = repositoryManager.retryFailedRepository(failedRepoName);

    // Verify: Retry failed, failure was recorded again
    assertThat(result.isPresent(), is(false));
    verify(failedRepositoryTracker).recordFailure(failedRepoName, startException);
    verify(failedRepositoryTracker, never()).clearFailure(failedRepoName);

    // Verify: No RepositoryRestoredEvent was posted (since retry failed)
    verify(eventManager, never()).post(any(RepositoryRestoredEvent.class));
  }

  @Test
  public void testRetryFailedRepository_alreadyLoaded() throws Exception {
    // Setup: Repository is already loaded (exists in repositories map)
    repositoryManager = buildRepositoryManagerImpl(true);

    // Reset mock to clear setup interactions
    reset(failedRepositoryTracker);

    // Mock the failedRepositoryTracker
    when(failedRepositoryTracker.hasFailed(MAVEN_CENTRAL_NAME)).thenReturn(true);

    // Execute: Retry a repository that's already loaded
    Optional<Repository> result = repositoryManager.retryFailedRepository(MAVEN_CENTRAL_NAME);

    // Verify: Returns the existing repository and clears failure
    assertThat(result.isPresent(), is(true));
    assertThat(result.get(), is(mavenCentralRepository));
    verify(failedRepositoryTracker).clearFailure(MAVEN_CENTRAL_NAME);

    // Verify: No new repository was created
    verify(repositoryFactory, times(8)).create(type, format); // 8 from initial setup
  }

  @Test
  public void testRetryFailedRepository_noRecordedFailure() throws Exception {
    // Setup: Repository has no recorded failure
    repositoryManager = buildRepositoryManagerImpl(true);

    String repoName = "unknown-repo";

    // Reset mock to clear setup interactions
    reset(failedRepositoryTracker);

    // Mock the failedRepositoryTracker to return false for hasFailed
    when(failedRepositoryTracker.hasFailed(repoName)).thenReturn(false);

    // Execute: Retry a repository with no recorded failure
    Optional<Repository> result = repositoryManager.retryFailedRepository(repoName);

    // Verify: Returns empty
    assertThat(result.isPresent(), is(false));
    verify(failedRepositoryTracker, never()).clearFailure(any());
    verify(failedRepositoryTracker, never()).recordFailure(any(), any());
  }

  @Test
  public void testRetryFailedRepository_concurrentRetry_firstWins() throws Exception {
    // Setup: Two threads try to retry the same repository simultaneously
    repositoryManager = buildRepositoryManagerImpl(true);

    String failedRepoName = "failed-repo";
    Configuration failedRepoConfig = mock(Configuration.class);
    Repository failedRepo = mock(Repository.class);

    when(failedRepoConfig.getRepositoryName()).thenReturn(failedRepoName);
    when(failedRepoConfig.getRecipeName()).thenReturn(recipeName);
    when(failedRepo.getName()).thenReturn(failedRepoName);
    when(failedRepo.getConfiguration()).thenReturn(failedRepoConfig);

    // Mock the configuration store
    when(configurationStore.list()).thenReturn(asList(failedRepoConfig));

    // Mock the factory to create the repository
    when(repositoryFactory.create(type, format)).thenReturn(failedRepo);

    // Mock the failedRepositoryTracker with special behavior:
    // - First check (outside lock) returns true for both threads
    // - Second check (inside lock after first thread completes) returns false for second thread
    when(failedRepositoryTracker.hasFailed(failedRepoName))
        .thenReturn(true) // First thread, outer check
        .thenReturn(true) // Second thread, outer check
        .thenReturn(true) // First thread, inner check (inside lock)
        .thenReturn(false); // Second thread, inner check (inside lock, after first completes)

    // Execute first retry (simulates first thread winning the race)
    Optional<Repository> firstResult = repositoryManager.retryFailedRepository(failedRepoName);

    // Verify first retry succeeded
    assertThat(firstResult.isPresent(), is(true));
    verify(failedRepo).start();
    verify(failedRepositoryTracker).clearFailure(failedRepoName);

    // Execute second retry (simulates second thread entering after first completed)
    Optional<Repository> secondResult = repositoryManager.retryFailedRepository(failedRepoName);

    // Verify second retry returns the existing repository (double-check pattern worked)
    assertThat(secondResult.isPresent(), is(true));
    assertThat(secondResult.get(), is(failedRepo));

    // Verify repository was only created once (not twice)
    verify(repositoryFactory, times(9)).create(type, format); // 8 from setup + 1 from first retry

    // Verify failure was cleared (at least once, possibly twice for the double-check)
    verify(failedRepositoryTracker, times(2)).clearFailure(failedRepoName);
  }

  @Test
  public void testRetryFailedRepository_failureClearedBeforeLockAcquired() throws Exception {
    // Setup: TOCTOU scenario - failure is cleared by another thread between outer check and lock acquisition
    repositoryManager = buildRepositoryManagerImpl(true);

    String failedRepoName = "failed-repo";

    // Reset mocks to clear setup interactions
    reset(failedRepositoryTracker, configurationStore, repositoryFactory);

    // Mock the failedRepositoryTracker to simulate TOCTOU race:
    // - Outer check (line 416): returns true → passes outer check
    // - Inner check (line 432): returns false → failure was cleared by another thread
    when(failedRepositoryTracker.hasFailed(failedRepoName))
        .thenReturn(true) // Outer check: failure exists
        .thenReturn(false); // Inner check after lock: failure was cleared

    // Execute: Retry the repository
    Optional<Repository> result = repositoryManager.retryFailedRepository(failedRepoName);

    // Verify: Returns empty because failure was cleared before lock was acquired
    assertThat(result.isPresent(), is(false));

    // Verify: No retry was attempted (defensive check caught the race)
    verify(configurationStore, never()).list();
    verify(repositoryFactory, never()).create(any(), any());
    verify(failedRepositoryTracker, never()).clearFailure(any());
    verify(failedRepositoryTracker, never()).recordFailure(any(), any());
  }

  @Test
  public void testRetryFailedRepository_caseInsensitive() throws Exception {
    // Setup: Repository name is case-insensitive
    repositoryManager = buildRepositoryManagerImpl(true);

    String failedRepoName = "Failed-Repo";
    String lowerCaseName = failedRepoName.toLowerCase();
    Configuration failedRepoConfig = mock(Configuration.class);
    Repository failedRepo = mock(Repository.class);

    when(failedRepoConfig.getRepositoryName()).thenReturn(lowerCaseName);
    when(failedRepoConfig.getRecipeName()).thenReturn(recipeName);
    when(failedRepo.getName()).thenReturn(lowerCaseName);
    when(failedRepo.getConfiguration()).thenReturn(failedRepoConfig);

    // Mock the configuration store
    when(configurationStore.list()).thenReturn(asList(failedRepoConfig));

    // Mock the factory to create the repository
    when(repositoryFactory.create(type, format)).thenReturn(failedRepo);

    // Mock the failedRepositoryTracker (checks both with original case and lowercase)
    when(failedRepositoryTracker.hasFailed(failedRepoName)).thenReturn(true);
    when(failedRepositoryTracker.hasFailed(lowerCaseName)).thenReturn(true);

    // Execute: Retry with mixed-case name
    Optional<Repository> result = repositoryManager.retryFailedRepository(failedRepoName);

    // Verify: Repository was created successfully
    assertThat(result.isPresent(), is(true));
    verify(failedRepo).start();
    verify(failedRepositoryTracker).clearFailure(failedRepoName);
  }

  @Test
  public void testDelete_failedRepository() throws Exception {
    // Setup: A repository that failed during startup and is tracked but not in repositories map
    repositoryManager = buildRepositoryManagerImpl(true);

    String failedRepoName = "failed-repo";
    Configuration failedRepoConfig = mock(Configuration.class);
    when(failedRepoConfig.getRepositoryName()).thenReturn(failedRepoName);
    when(failedRepoConfig.getRecipeName()).thenReturn(recipeName);

    // Add configuration to store so it can be found
    when(configurationStore.list()).thenReturn(List.of(failedRepoConfig));

    // Mock the failedRepositoryTracker to indicate this repository has failed
    when(failedRepositoryTracker.hasFailed(failedRepoName.toLowerCase())).thenReturn(true);

    // Execute: Delete the failed repository
    repositoryManager.delete(failedRepoName);

    // Verify: Configuration was removed from store and failure was cleared
    verify(configurationStore).delete(failedRepoConfig);
    verify(failedRepositoryTracker).clearFailure(failedRepoName);
  }

  @Test
  public void testRestoreRepositories_continuesWhenOneRepositoryFailsToInitialize() throws Exception {
    // Test NEXUS-51389: Ensure repository restore continues when one repository fails during initialization
    when(configurationStore.list())
        .thenReturn(asList(mavenCentralConfiguration, apacheSnapshotsConfiguration, thirdPartyConfiguration));

    // Make apache-snapshots fail during init
    doThrow(new RuntimeException("Invalid secret - repository cannot initialize"))
        .when(apacheSnapshotsRepository)
        .init(apacheSnapshotsConfiguration);

    repositoryManager = initializeAndStartRepositoryManager(false);

    // Verify: maven-central and third-party were initialized and started successfully
    verify(mavenCentralRepository).init(mavenCentralConfiguration);
    verify(mavenCentralRepository).start();
    verify(thirdPartyRepository).init(thirdPartyConfiguration);
    verify(thirdPartyRepository).start();

    // Verify: apache-snapshots init was attempted but failed
    verify(apacheSnapshotsRepository).init(apacheSnapshotsConfiguration);
    // apache-snapshots should NOT have been started or tracked
    verify(apacheSnapshotsRepository, never()).start();

    // Verify: Only successful repositories are browsable (2 out of 3)
    assertThat(size(repositoryManager.browse()), equalTo(2));
  }

  @Test
  public void testRestoreRepositories_skipsRepositoryWithUnavailableRecipe() throws Exception {
    // Test NEXUS-51389: Ensure repositories with unavailable recipes are skipped gracefully
    Configuration unavailableRecipeConfig = mock(Configuration.class);
    when(unavailableRecipeConfig.getRecipeName()).thenReturn("unavailable-recipe");
    when(unavailableRecipeConfig.getRepositoryName()).thenReturn("unavailable-repo");

    when(configurationStore.list())
        .thenReturn(asList(mavenCentralConfiguration, unavailableRecipeConfig));

    repositoryManager = initializeAndStartRepositoryManager(false);

    // Verify: Only maven-central was initialized (unavailable recipe was skipped)
    verify(mavenCentralRepository).init(mavenCentralConfiguration);
    verify(mavenCentralRepository).start();

    // Verify: Only 1 repository loaded (unavailable recipe was skipped)
    assertThat(size(repositoryManager.browse()), equalTo(1));
  }

  @Test
  public void testStartRepositories_continuesWhenOneRepositoryFailsToStart() throws Exception {
    // Test NEXUS-51389: Ensure repository start continues when one repository fails during start()
    when(configurationStore.list())
        .thenReturn(asList(mavenCentralConfiguration, apacheSnapshotsConfiguration, thirdPartyConfiguration));

    // Make apache-snapshots fail during start (after successful init)
    doThrow(new RuntimeException("Failed to start repository - invalid configuration"))
        .when(apacheSnapshotsRepository)
        .start();

    repositoryManager = initializeAndStartRepositoryManager(false);

    // Verify: All three repositories were initialized
    verify(mavenCentralRepository).init(mavenCentralConfiguration);
    verify(apacheSnapshotsRepository).init(apacheSnapshotsConfiguration);
    verify(thirdPartyRepository).init(thirdPartyConfiguration);

    // Verify: All three start() calls were attempted
    verify(mavenCentralRepository).start();
    verify(apacheSnapshotsRepository).start();
    verify(thirdPartyRepository).start();

    // Verify: All three repositories are tracked (even failed ones remain in STOPPED/FAILED state)
    assertThat(size(repositoryManager.browse()), equalTo(3));
  }

  @Test
  public void testStartRepositories_postsEventsOnlyForSuccessfulStarts() throws Exception {
    // Test NEXUS-51389: Verify RepositoryRestoredEvent is only posted for successfully started repositories
    when(configurationStore.list())
        .thenReturn(asList(mavenCentralConfiguration, apacheSnapshotsConfiguration));

    // Make apache-snapshots fail during start
    doThrow(new RuntimeException("Start failed"))
        .when(apacheSnapshotsRepository)
        .start();

    repositoryManager = initializeAndStartRepositoryManager(false);

    // Verify: Events posted (2 RepositoryLoadedEvent + 1 RepositoryRestoredEvent = 3 total)
    ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
    verify(eventManager, times(3)).post(eventCaptor.capture());

    List<Object> events = eventCaptor.getAllValues();
    long restoredEvents = events.stream()
        .filter(e -> e.getClass().getSimpleName().equals("RepositoryRestoredEvent"))
        .count();

    // Only one RepositoryRestoredEvent should be posted (for maven-central, not apache-snapshots)
    assertThat(restoredEvents, equalTo(1L));
  }

  @Test
  public void testRestoreRepositories_allFailuresDontStopNexus() throws Exception {
    // Test NEXUS-51389: Even if all repositories fail, Nexus startup should continue
    when(configurationStore.list())
        .thenReturn(asList(mavenCentralConfiguration, apacheSnapshotsConfiguration));

    // Make both repositories fail during init
    doThrow(new RuntimeException("Init failed - repository 1"))
        .when(mavenCentralRepository)
        .init(mavenCentralConfiguration);
    doThrow(new RuntimeException("Init failed - repository 2"))
        .when(apacheSnapshotsRepository)
        .init(apacheSnapshotsConfiguration);

    // This should NOT throw an exception - Nexus should start successfully
    repositoryManager = initializeAndStartRepositoryManager(false);

    // Verify: Both init attempts were made
    verify(mavenCentralRepository).init(mavenCentralConfiguration);
    verify(apacheSnapshotsRepository).init(apacheSnapshotsConfiguration);

    // Verify: Neither repository was started
    verify(mavenCentralRepository, never()).start();
    verify(apacheSnapshotsRepository, never()).start();

    // Verify: No repositories are browsable (all failed)
    assertThat(size(repositoryManager.browse()), equalTo(0));

    // Verify: groupMemberMappingCache.init was still called (startup completes)
    verify(groupMemberMappingCache).init(repositoryManager);
  }

  @Test
  public void testRestoreRepositories_mixOfSuccessAndFailure() throws Exception {
    // Test NEXUS-51389: Comprehensive test with mix of success, init failure, and start failure
    Configuration fourthRepoConfig = mock(Configuration.class);
    Repository fourthRepository = mock(Repository.class);
    when(fourthRepoConfig.getRecipeName()).thenReturn(recipeName);
    when(fourthRepoConfig.getRepositoryName()).thenReturn("fourth-repo");
    when(fourthRepoConfig.copy()).thenReturn(fourthRepoConfig);
    when(fourthRepository.getName()).thenReturn("fourth-repo");
    when(fourthRepository.getConfiguration()).thenReturn(fourthRepoConfig);
    when(fourthRepository.optionalFacet(any(Class.class))).thenReturn(Optional.empty());

    Map<String, Map<String, Object>> fourthAttr = new HashMap<>();
    fourthAttr.put("storage", Map.of("blobStoreName", "default"));
    when(fourthRepoConfig.getAttributes()).thenReturn(fourthAttr);

    when(configurationStore.list())
        .thenReturn(asList(mavenCentralConfiguration, apacheSnapshotsConfiguration,
            thirdPartyConfiguration, fourthRepoConfig));

    when(repositoryFactory.create(type, format))
        .thenReturn(mavenCentralRepository, apacheSnapshotsRepository, thirdPartyRepository, fourthRepository);

    // maven-central: success
    // apache-snapshots: fails during init
    doThrow(new RuntimeException("Init failure")).when(apacheSnapshotsRepository).init(apacheSnapshotsConfiguration);
    // third-party: success during init, fails during start
    doThrow(new RuntimeException("Start failure")).when(thirdPartyRepository).start();
    // fourth-repo: success

    repositoryManager = initializeAndStartRepositoryManager(false);

    // Verify: All init attempts were made
    verify(mavenCentralRepository).init(mavenCentralConfiguration);
    verify(apacheSnapshotsRepository).init(apacheSnapshotsConfiguration);
    verify(thirdPartyRepository).init(thirdPartyConfiguration);
    verify(fourthRepository).init(fourthRepoConfig);

    // Verify: Only successfully initialized repos had start() called
    verify(mavenCentralRepository).start();
    verify(apacheSnapshotsRepository, never()).start(); // Failed init, no start attempt
    verify(thirdPartyRepository).start(); // Init succeeded, start attempted (but failed)
    verify(fourthRepository).start();

    // Verify: Three repositories are browsable (apache-snapshots failed init, not tracked)
    assertThat(size(repositoryManager.browse()), equalTo(3));
  }

  // ---- Soft-delete / restore / hard-delete tests ----

  @Test
  public void testSoftDelete_stopsUntracksAndMarksInDb() throws Exception {
    repositoryManager = buildRepositoryManagerImpl(true);

    repositoryManager.softDelete(MAVEN_CENTRAL_NAME, "testuser");

    verify(mavenCentralRepository).stopSafe();
    verify(mavenCentralRepository).destroy();
    verify(configurationStore).softDelete(MAVEN_CENTRAL_NAME, "testuser");

    // Verify untracked: browse should no longer contain the repo
    assertThat(size(repositoryManager.browse()), equalTo(7));

    // Verify deletion event posted
    ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
    verify(eventManager, times(18)).post(eventCaptor.capture());
    long deletedEvents = eventCaptor.getAllValues()
        .stream()
        .filter(e -> e instanceof RepositoryDeletedEvent)
        .count();
    assertThat(deletedEvents, equalTo(1L));
  }

  @Test
  public void testSoftDelete_removesFromGroups() throws Exception {
    repositoryManager = buildRepositoryManagerImpl(true);

    repositoryManager.softDelete(APACHE_SNAPSHOTS_NAME, "admin");

    // apache-snapshots is a member of group, parentGroup, cycleA, cycleB
    verify(configurationStore).update(groupConfiguration);
    verify(configurationStore).update(parentGroupConfiguration);
  }

  @Test(expected = MissingRepositoryException.class)
  public void testSoftDelete_throwsForMissingRepository() throws Exception {
    repositoryManager = buildRepositoryManagerImpl(true);
    repositoryManager.softDelete("non-existent", "testuser");
  }

  @Test
  public void testRestoreFromSoftDelete_createsTracksAndStarts() throws Exception {
    repositoryManager = buildRepositoryManagerImpl(true);

    // First soft-delete maven-central
    repositoryManager.softDelete(MAVEN_CENTRAL_NAME, "testuser");
    org.mockito.Mockito.clearInvocations(eventManager, configurationStore, repositoryFactory);

    // Setup for restore: store.readByNames returns the config, factory creates a new repo
    Repository restoredRepo = mock(Repository.class);
    when(restoredRepo.getName()).thenReturn(MAVEN_CENTRAL_NAME);
    when(restoredRepo.getConfiguration()).thenReturn(mavenCentralConfiguration);
    when(restoredRepo.optionalFacet(any(Class.class))).thenReturn(Optional.empty());

    when(configurationStore.readByNames(Set.of(MAVEN_CENTRAL_NAME)))
        .thenReturn(Set.of(mavenCentralConfiguration));

    when(repositoryFactory.create(type, format)).thenReturn(restoredRepo);

    Repository result = repositoryManager.restoreFromSoftDelete(MAVEN_CENTRAL_NAME);

    verify(configurationStore).undoSoftDelete(MAVEN_CENTRAL_NAME);
    verify(restoredRepo).init(mavenCentralConfiguration);
    verify(restoredRepo).start();

    // Verify event posted
    ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
    verify(eventManager).post(eventCaptor.capture());
    assertThat(eventCaptor.getValue().getClass().getSimpleName(), equalTo("RepositoryCreatedEvent"));

    // Verify repo is now browsable again
    assertThat(repositoryManager.get(MAVEN_CENTRAL_NAME), is(restoredRepo));
  }

  @Test(expected = MissingRepositoryException.class)
  public void testRestoreFromSoftDelete_throwsWhenConfigNotFound() throws Exception {
    repositoryManager = buildRepositoryManagerImpl(false, true);

    when(configurationStore.readByNames(Set.of("missing")))
        .thenReturn(Set.of());

    repositoryManager.restoreFromSoftDelete("missing");
  }

  @Test
  public void testHardDelete_runsContentCleanupAndRemovesConfig() throws Exception {
    repositoryManager = buildRepositoryManagerImpl(false, true);

    Configuration deletedConfig = mock(Configuration.class);
    when(deletedConfig.getRecipeName()).thenReturn(recipeName);
    when(deletedConfig.getRepositoryName()).thenReturn("deleted-repo");
    Map<String, Map<String, Object>> attrs = new HashMap<>();
    attrs.put("storage", Map.of("blobStoreName", "default"));
    when(deletedConfig.getAttributes()).thenReturn(attrs);

    when(configurationStore.readDeleted("deleted-repo")).thenReturn(deletedConfig);

    Repository tempRepo = mock(Repository.class);
    when(tempRepo.getName()).thenReturn("deleted-repo");
    when(tempRepo.getConfiguration()).thenReturn(deletedConfig);
    when(tempRepo.optionalFacet(any(Class.class))).thenReturn(Optional.empty());
    when(repositoryFactory.create(type, format)).thenReturn(tempRepo);

    repositoryManager.hardDelete("deleted-repo");

    // Verify temporary repo was created, started, stopped, delete called for content cleanup, then destroyed
    verify(tempRepo).init(deletedConfig);
    verify(tempRepo).start();
    verify(tempRepo).stop();
    verify(tempRepo).delete();
    verify(tempRepo).destroy();

    // Verify secrets removed and config permanently deleted
    verify(httpAuthenticationSecretEncoder).removeSecret(attrs);
    verify(configurationStore).hardDelete("deleted-repo");
  }

  @Test(expected = IllegalStateException.class)
  public void testHardDelete_throwsWhenNotSoftDeleted() throws Exception {
    repositoryManager = buildRepositoryManagerImpl(false, true);

    when(configurationStore.readDeleted("not-deleted")).thenReturn(null);

    repositoryManager.hardDelete("not-deleted");
  }

  @Test
  public void testHardDelete_continuesEvenIfContentCleanupFails() throws Exception {
    repositoryManager = buildRepositoryManagerImpl(false, true);

    Configuration deletedConfig = mock(Configuration.class);
    when(deletedConfig.getRecipeName()).thenReturn(recipeName);
    when(deletedConfig.getRepositoryName()).thenReturn("deleted-repo");
    Map<String, Map<String, Object>> attrs = new HashMap<>();
    attrs.put("storage", Map.of("blobStoreName", "default"));
    when(deletedConfig.getAttributes()).thenReturn(attrs);

    when(configurationStore.readDeleted("deleted-repo")).thenReturn(deletedConfig);

    Repository tempRepo = mock(Repository.class);
    when(tempRepo.getName()).thenReturn("deleted-repo");
    when(tempRepo.getConfiguration()).thenReturn(deletedConfig);
    when(tempRepo.optionalFacet(any(Class.class))).thenReturn(Optional.empty());
    when(repositoryFactory.create(type, format)).thenReturn(tempRepo);

    // Content cleanup fails
    doThrow(new RuntimeException("Content cleanup failed")).when(tempRepo).delete();

    repositoryManager.hardDelete("deleted-repo");

    // When delete() throws, destroy() is called once from the catch block
    verify(tempRepo).destroy();

    // Verify config was still permanently deleted despite content cleanup failure
    verify(httpAuthenticationSecretEncoder).removeSecret(attrs);
    verify(configurationStore).hardDelete("deleted-repo");
  }

  @Test
  public void testSoftDelete_cleansSearchIndex() throws Exception {
    repositoryManager = buildRepositoryManagerImpl(true);

    SearchIndexFacet searchFacet = mock(SearchIndexFacet.class);
    when(mavenCentralRepository.optionalFacet(SearchIndexFacet.class)).thenReturn(Optional.of(searchFacet));

    repositoryManager.softDelete(MAVEN_CENTRAL_NAME, "testuser");

    verify(searchFacet).deleteIndex();
    verify(mavenCentralRepository).stopSafe();
    verify(mavenCentralRepository).destroy();
  }

  @Test
  public void testSoftDelete_continuesWhenSearchCleanupFails() throws Exception {
    repositoryManager = buildRepositoryManagerImpl(true);

    SearchIndexFacet searchFacet = mock(SearchIndexFacet.class);
    when(mavenCentralRepository.optionalFacet(SearchIndexFacet.class)).thenReturn(Optional.of(searchFacet));
    doThrow(new RuntimeException("Search cleanup failed")).when(searchFacet).deleteIndex();

    repositoryManager.softDelete(MAVEN_CENTRAL_NAME, "testuser");

    verify(searchFacet).deleteIndex();
    verify(mavenCentralRepository).destroy();
    verify(configurationStore).softDelete(MAVEN_CENTRAL_NAME, "testuser");
  }

  @Test
  public void testSoftDelete_worksWithoutSearchFacet() throws Exception {
    repositoryManager = buildRepositoryManagerImpl(true);

    // Default mock returns Optional.empty() for optionalFacet

    repositoryManager.softDelete(MAVEN_CENTRAL_NAME, "testuser");

    verify(mavenCentralRepository).stopSafe();
    verify(mavenCentralRepository).destroy();
    verify(configurationStore).softDelete(MAVEN_CENTRAL_NAME, "testuser");
  }

  @Test
  public void testRestoreFromSoftDelete_rebuildsSearchIndex() throws Exception {
    repositoryManager = buildRepositoryManagerImpl(true);

    repositoryManager.softDelete(MAVEN_CENTRAL_NAME, "testuser");
    org.mockito.Mockito.clearInvocations(eventManager, configurationStore, repositoryFactory);

    Repository restoredRepo = mock(Repository.class);
    when(restoredRepo.getName()).thenReturn(MAVEN_CENTRAL_NAME);
    when(restoredRepo.getConfiguration()).thenReturn(mavenCentralConfiguration);

    SearchIndexFacet searchFacet = mock(SearchIndexFacet.class);
    when(restoredRepo.optionalFacet(SearchIndexFacet.class)).thenReturn(Optional.of(searchFacet));
    when(restoredRepo.optionalFacet(GroupFacet.class)).thenReturn(Optional.empty());

    when(configurationStore.readByNames(Set.of(MAVEN_CENTRAL_NAME)))
        .thenReturn(Set.of(mavenCentralConfiguration));
    when(repositoryFactory.create(type, format)).thenReturn(restoredRepo);

    repositoryManager.restoreFromSoftDelete(MAVEN_CENTRAL_NAME);

    verify(restoredRepo).start();
    verify(searchFacet).rebuildIndex();
  }

  @Test
  public void testRestoreFromSoftDelete_continuesWhenSearchRebuildFails() throws Exception {
    repositoryManager = buildRepositoryManagerImpl(true);

    repositoryManager.softDelete(MAVEN_CENTRAL_NAME, "testuser");
    org.mockito.Mockito.clearInvocations(eventManager, configurationStore, repositoryFactory);

    Repository restoredRepo = mock(Repository.class);
    when(restoredRepo.getName()).thenReturn(MAVEN_CENTRAL_NAME);
    when(restoredRepo.getConfiguration()).thenReturn(mavenCentralConfiguration);

    SearchIndexFacet searchFacet = mock(SearchIndexFacet.class);
    when(restoredRepo.optionalFacet(SearchIndexFacet.class)).thenReturn(Optional.of(searchFacet));
    when(restoredRepo.optionalFacet(GroupFacet.class)).thenReturn(Optional.empty());
    doThrow(new RuntimeException("Rebuild failed")).when(searchFacet).rebuildIndex();

    when(configurationStore.readByNames(Set.of(MAVEN_CENTRAL_NAME)))
        .thenReturn(Set.of(mavenCentralConfiguration));
    when(repositoryFactory.create(type, format)).thenReturn(restoredRepo);

    Repository result = repositoryManager.restoreFromSoftDelete(MAVEN_CENTRAL_NAME);

    verify(searchFacet).rebuildIndex();
    assertThat(result, is(restoredRepo));
    assertThat(repositoryManager.get(MAVEN_CENTRAL_NAME), is(restoredRepo));
  }

  /**
   * Security: ConfigurationValidators must run on default-repository provisioning so that
   * edition-specific guards (e.g. OciCloudEditionConfigurationValidator) cannot be bypassed
   * by a DefaultRepositoriesContributor that supplies an illegal configuration. Per-repo
   * rejections are tracked rather than aborting the entire provisioning loop, so a single
   * misconfigured contributor cannot prevent the rest of the manager from booting.
   */
  @Test
  public void testProvisionDefaultRepositories_runsConfigurationValidators() throws Exception {
    // Validator that always rejects to prove provisionDefaultRepositories invokes it.
    ConfigurationValidator alwaysRejects = config -> {
      ConstraintViolation<?> violation = mock(ConstraintViolation.class);
      return violation;
    };

    // Empty store triggers provisionDefaultRepositories on start
    when(configurationStore.list()).thenReturn(List.of());

    initializeAndStartRepositoryManager(false, List.<ConfigurationValidator>of(alwaysRejects));

    // All 8 contributor-supplied defaults are tracked rather than propagated. The exact count
    // assertion proves the provisioning loop did NOT abort on the first failure — a regression
    // to the prior fail-fast behavior would surface here as a count of 1.
    verify(failedRepositoryTracker, times(8)).recordFailure(any(String.class), any(Exception.class));
    verify(configurationStore, never()).create(any(Configuration.class));
  }

  /**
   * Security: a single misconfigured DefaultRepositoriesContributor must not prevent other
   * default repositories (or the rest of the manager) from starting. Verifies the per-repo
   * fail-tracking soften behavior added under the second-pass review (NEXUS-42721).
   */
  @Test
  public void testProvisionDefaultRepositories_oneBadContributorDoesNotBlockOthers() throws Exception {
    // Validator that rejects exactly one default and allows the rest.
    ConfigurationValidator selectiveValidator = config -> {
      if (config != null && MAVEN_CENTRAL_NAME.equals(config.getRepositoryName())) {
        return mock(ConstraintViolation.class);
      }
      return null;
    };

    // Empty store triggers provisionDefaultRepositories on start
    when(configurationStore.list()).thenReturn(List.of());

    initializeAndStartRepositoryManager(false, List.<ConfigurationValidator>of(selectiveValidator));

    // mavenCentral was rejected and tracked.
    verify(failedRepositoryTracker).recordFailure(eq(MAVEN_CENTRAL_NAME), any(Exception.class));
    // The other 7 defaults still got created — provisioning loop did not abort on the rejection.
    // Verifying both an early-list config (apacheSnapshots) AND a late-list config
    // (ungroupedRepoConfiguration) proves the loop ran to completion, not just past the
    // rejection point.
    verify(configurationStore).create(apacheSnapshotsConfiguration);
    verify(configurationStore).create(thirdPartyConfiguration);
    verify(configurationStore).create(ungroupedRepoConfiguration);
    verify(configurationStore, times(7)).create(any(Configuration.class));
    verify(configurationStore, never()).create(mavenCentralConfiguration);
  }

  @Test
  public void testProvisionDefaultRepositories_passesWhenValidatorAllows() throws Exception {
    // Validator that returns null (no violation) for every configuration.
    ConfigurationValidator allowsAll = config -> null;

    // Empty initial store, then return configs after provisioning
    when(configurationStore.list())
        .thenReturn(List.of())
        .thenReturn(asList(mavenCentralConfiguration, apacheSnapshotsConfiguration, thirdPartyConfiguration,
            groupConfiguration, parentGroupConfiguration, cycleGroupAConfiguration, cycleGroupBConfiguration,
            ungroupedRepoConfiguration));

    initializeAndStartRepositoryManager(false, List.<ConfigurationValidator>of(allowsAll));

    // All defaults were created since validator allowed them
    verify(configurationStore).create(mavenCentralConfiguration);
    verify(configurationStore).create(apacheSnapshotsConfiguration);
    verify(configurationStore).create(thirdPartyConfiguration);
  }

  /**
   * Security: ConfigurationValidators must run on restore so a backup taken on a self-hosted
   * node cannot smuggle a now-illegal configuration onto a cloud node. A failure here is
   * caught by the per-repository try/catch block and tracked as a failed repository, while
   * other repositories continue to start.
   */
  @Test
  public void testRestoreRepositories_validatorFailureIsTrackedNotPropagated() throws Exception {
    // Validator that rejects exactly one specific repository (mavenCentral) and allows others.
    ConfigurationValidator selectiveValidator = config -> {
      if (config != null && MAVEN_CENTRAL_NAME.equals(config.getRepositoryName())) {
        return mock(ConstraintViolation.class);
      }
      return null;
    };

    // defaultsConfigured=true: store returns the existing repos and restore is invoked
    when(configurationStore.list())
        .thenReturn(asList(mavenCentralConfiguration, apacheSnapshotsConfiguration, thirdPartyConfiguration,
            groupConfiguration, parentGroupConfiguration, cycleGroupAConfiguration, cycleGroupBConfiguration,
            ungroupedRepoConfiguration));

    initializeAndStartRepositoryManager(false, List.<ConfigurationValidator>of(selectiveValidator));

    // Verify mavenCentral was tracked as a failure (validation rejected it before newRepository was invoked).
    // This is the security-critical assertion: the rejected config is captured by the per-repo
    // try/catch in restoreRepositories and surfaced via FailedRepositoryTracker rather than
    // silently allowed through.
    verify(failedRepositoryTracker).recordFailure(eq(MAVEN_CENTRAL_NAME), any(Exception.class));
  }

  // -----------------------------------------------------------------------------------------------
  // NEXUS-53393 — firewall preservation on partial update.
  //
  // The typed REST API converters build a fresh Configuration from the request body. Any
  // optional sub-section the caller omits is absent from the converted Configuration; without
  // preservation, BaseRepositoryManager.update() then writes that absence to storage, silently
  // wiping the existing firewall.mode. Tests below exercise the static helper
  // BaseRepositoryManager#preserveFirewallFromExistingIfAbsent directly because it is a pure
  // function — keeping the tests focused on the rule ("absent in target ∧ present in existing
  // ⇒ carry forward") rather than on the surrounding update-flow plumbing already covered
  // above. The chain tests below additionally wire the real REST converter into the helper to
  // pin the end-to-end fix shape.
  // -----------------------------------------------------------------------------------------------

  private static final String FIREWALL_TEST_REPO = "firewall-test-repo";

  @Test
  public void preserveFirewallFromExistingIfAbsent_targetOmitsFirewall_existingHasIt_carriesForward() {
    Configuration existing = configWithFirewallMode("PCCS");
    Configuration target = configWithoutFirewall();

    BaseRepositoryManager.preserveFirewallFromExistingIfAbsent(existing, target, FIREWALL_TEST_REPO);

    assertThat(
        "target should now carry the firewall block from the existing config",
        target.getAttributes().containsKey("firewall"), is(true));
    assertThat(target.attributes("firewall").get("mode"), is((Object) "PCCS"));
  }

  @Test
  public void preserveFirewallFromExistingIfAbsent_targetCarriesFirewall_leavesItAlone() {
    // The caller explicitly addressed firewall — even if just to set DISABLED. Honour that.
    Configuration existing = configWithFirewallMode("PCCS");
    Configuration target = configWithFirewallMode("DISABLED");

    BaseRepositoryManager.preserveFirewallFromExistingIfAbsent(existing, target, FIREWALL_TEST_REPO);

    assertThat(target.attributes("firewall").get("mode"), is((Object) "DISABLED"));
  }

  @Test
  public void preserveFirewallFromExistingIfAbsent_existingHasNoFirewall_leavesTargetAlone() {
    // Common case: a repo that has never had firewall configured. Don't synthesise an empty
    // firewall block on update.
    Configuration existing = configWithoutFirewall();
    Configuration target = configWithoutFirewall();

    BaseRepositoryManager.preserveFirewallFromExistingIfAbsent(existing, target, FIREWALL_TEST_REPO);

    Map<String, Map<String, Object>> attrs = target.getAttributes();
    assertThat(attrs == null || !attrs.containsKey("firewall"), is(true));
  }

  @Test
  public void preserveFirewallFromExistingIfAbsent_existingHasEmptyFirewall_leavesTargetAlone() {
    // Defensive: a stray empty firewall block on the existing config (shouldn't happen, but
    // possible from old code paths or hand-edited storage) should not be propagated.
    Configuration existing = configWithEmptyFirewallMap();
    Configuration target = configWithoutFirewall();

    BaseRepositoryManager.preserveFirewallFromExistingIfAbsent(existing, target, FIREWALL_TEST_REPO);

    Map<String, Map<String, Object>> attrs = target.getAttributes();
    assertThat(attrs == null || !attrs.containsKey("firewall"), is(true));
  }

  /**
   * NEXUS-48685 regression: the UI plants {@code firewall: {mode: null}} in the persisted
   * config when a repository is created with the XET feature flag off (or before the flag
   * was introduced). {@link java.util.concurrent.ConcurrentHashMap} — which backs
   * {@link ConfigurationData#attributes(String)} sub-maps — throws
   * {@link NullPointerException} on {@code putAll} whenever any value is {@code null}. A
   * map whose only entry is {@code null}-valued carries no actionable configuration; the
   * helper must treat it the same as an absent/empty firewall block and return early.
   */
  @Test
  public void preserveFirewallFromExistingIfAbsent_existingHasNullModeOnly_leavesTargetAlone() {
    ConfigurationData existing = new ConfigurationData();
    Map<String, Map<String, Object>> existingAttrs = new HashMap<>();
    Map<String, Object> firewall = new HashMap<>();
    firewall.put("mode", null);
    existingAttrs.put("firewall", firewall);
    existing.setAttributes(existingAttrs);

    Configuration target = configWithoutFirewall();

    // Must not throw NullPointerException (the pre-fix bug) and must not synthesise an
    // empty / null-valued firewall block in the target.
    BaseRepositoryManager.preserveFirewallFromExistingIfAbsent(existing, target, FIREWALL_TEST_REPO);

    Map<String, Map<String, Object>> attrs = target.getAttributes();
    assertThat(
        "target must NOT gain a firewall block when existing only has null mode",
        attrs == null || !attrs.containsKey("firewall"), is(true));
  }

  /**
   * NEXUS-48685: when the existing firewall map contains a mix of non-null and null values
   * (e.g. {@code {mode=QUARANTINE, unknownKey=null}}) the helper must preserve the
   * non-null entries and silently drop the null ones. {@link java.util.concurrent.ConcurrentHashMap}
   * rejects null values so a verbatim {@code putAll} would NPE; the null-filtering forEach
   * must copy only the non-null entries.
   */
  @Test
  public void preserveFirewallFromExistingIfAbsent_existingHasMixedNullAndNonNull_preservesNonNullEntries() {
    ConfigurationData existing = new ConfigurationData();
    Map<String, Map<String, Object>> existingAttrs = new HashMap<>();
    Map<String, Object> firewall = new HashMap<>();
    firewall.put("mode", "QUARANTINE");
    firewall.put("unknownKey", null);
    existingAttrs.put("firewall", firewall);
    existing.setAttributes(existingAttrs);

    Configuration target = configWithoutFirewall();

    // Must not throw NullPointerException and must carry the non-null mode forward.
    BaseRepositoryManager.preserveFirewallFromExistingIfAbsent(existing, target, FIREWALL_TEST_REPO);

    assertThat(
        "non-null firewall mode must be preserved",
        target.attributes("firewall").get("mode"), is((Object) "QUARANTINE"));
    assertThat(
        "null-valued key must be silently dropped (ConcurrentHashMap cannot hold null values)",
        target.attributes("firewall").get("unknownKey"), is(nullValue()));
  }

  /**
   * Pin the {@code existingAttrs == null} early-return branch in
   * {@code preserveFirewallFromExistingIfAbsent}. A {@link ConfigurationData} on which
   * {@code setAttributes} has never been called returns {@code null} from {@code getAttributes()};
   * this helper must short-circuit cleanly without NPE.
   */
  @Test
  public void preserveFirewallFromExistingIfAbsent_existingHasNullAttributes_leavesTargetAlone() {
    ConfigurationData existing = new ConfigurationData(); // attributes field defaults to null
    assertThat(existing.getAttributes(), is(nullValue()));
    Configuration target = configWithoutFirewall();

    BaseRepositoryManager.preserveFirewallFromExistingIfAbsent(existing, target, FIREWALL_TEST_REPO);

    Map<String, Map<String, Object>> attrs = target.getAttributes();
    assertThat(attrs == null || !attrs.containsKey("firewall"), is(true));
  }

  /**
   * Pin the {@code targetAttrs == null} fall-through branch in
   * {@code preserveFirewallFromExistingIfAbsent}. The chain tests below incidentally exercise
   * this path (the typed REST converter produces a Configuration with null attributes when the
   * request body has no sub-blocks), but the disjunctive {@code null || !containsKey} sanity
   * assertion does not deterministically pin the branch — a future refactor making the
   * converter populate attributes with a non-firewall key would still pass while this branch
   * went uncovered. This test deterministically pins it: target's attributes are explicitly
   * null, the helper falls through to the write path, and {@code Configuration.attributes(key)}
   * lazy-inits both the outer attributes map and the firewall sub-map.
   */
  @Test
  public void preserveFirewallFromExistingIfAbsent_targetHasNullAttributes_lazilyInitsAndPreserves() {
    Configuration existing = configWithFirewallMode("PCCS");
    ConfigurationData target = new ConfigurationData(); // attributes field defaults to null
    assertThat(target.getAttributes(), is(nullValue()));

    BaseRepositoryManager.preserveFirewallFromExistingIfAbsent(existing, target, FIREWALL_TEST_REPO);

    assertThat(
        "target attributes should have been lazy-initialised by the preservation write",
        target.getAttributes(), is(notNullValue()));
    assertThat(target.attributes("firewall").get("mode"), is((Object) "PCCS"));
  }

  @Test
  public void preserveFirewallFromExistingIfAbsent_carriesAllFirewallKeysNotJustMode() {
    // Future-proof: if the firewall block grows additional keys, all of them must travel.
    ConfigurationData existing = new ConfigurationData();
    Map<String, Map<String, Object>> existingAttrs = new HashMap<>();
    Map<String, Object> firewall = new HashMap<>();
    firewall.put("mode", "QUARANTINE");
    firewall.put("someOtherKey", "someValue");
    existingAttrs.put("firewall", firewall);
    existing.setAttributes(existingAttrs);

    Configuration target = configWithoutFirewall();

    BaseRepositoryManager.preserveFirewallFromExistingIfAbsent(existing, target, FIREWALL_TEST_REPO);

    assertThat(target.attributes("firewall").get("mode"), is((Object) "QUARANTINE"));
    assertThat(target.attributes("firewall").get("someOtherKey"), is((Object) "someValue"));
  }

  @Test
  public void preserveFirewallFromExistingIfAbsent_mutatingExistingDoesNotAffectTarget() {
    // Defensive copy direction 1: mutating EXISTING after preservation must not bleed into
    // TARGET. Models the case where validation/encoder steps further down update() inspect
    // the captured oldConfiguration (e.g. for rollback) after the helper has run.
    Configuration existing = configWithFirewallMode("AUDIT");
    Configuration target = configWithoutFirewall();

    BaseRepositoryManager.preserveFirewallFromExistingIfAbsent(existing, target, FIREWALL_TEST_REPO);

    existing.attributes("firewall").set("mode", "DISABLED");

    assertThat(target.attributes("firewall").get("mode"), is((Object) "AUDIT"));
  }

  /**
   * NEXUS-53393 (M3 review finding): the load-bearing direction for the defensive copy is the
   * inverse of the prior test — subsequent mutations on TARGET (e.g. validation, secret
   * encoding on the {@code httpclient} sub-block, the rollback path) must not bleed back into
   * the captured {@code oldConfiguration}. This test pins that guarantee.
   */
  @Test
  public void preserveFirewallFromExistingIfAbsent_mutatingTargetDoesNotAffectExisting() {
    Configuration existing = configWithFirewallMode("AUDIT");
    Configuration target = configWithoutFirewall();

    BaseRepositoryManager.preserveFirewallFromExistingIfAbsent(existing, target, FIREWALL_TEST_REPO);

    target.attributes("firewall").set("mode", "DISABLED");

    assertThat(existing.attributes("firewall").get("mode"), is((Object) "AUDIT"));
  }

  /**
   * H2b chain test (PCCS): wire the real REST converter into the helper to pin the end-to-end
   * fix shape. A partial PUT body that omits the {@code firewall} field produces a
   * Configuration that does not contain the {@code firewall} key; the helper must then carry
   * the persisted PCCS mode forward. This reproduces the exact bug the user reported.
   */
  @Test
  public void preserveFirewallFromExistingIfAbsent_afterConverterStripsOmittedFirewall_PCCSPreserved() {
    Configuration converted = convertProxyRequestWithoutFirewall("pypi-proxy-test", "pypi");

    // Sanity: converter omitted the firewall block (this is the bug condition).
    Map<String, Map<String, Object>> convertedAttrs = converted.getAttributes();
    assertThat(convertedAttrs == null || !convertedAttrs.containsKey("firewall"), is(true));

    Configuration existing = configWithFirewallMode("PCCS");

    BaseRepositoryManager.preserveFirewallFromExistingIfAbsent(existing, converted, "pypi-proxy-test");

    assertThat(converted.attributes("firewall").get("mode"), is((Object) "PCCS"));
  }

  /**
   * H2b chain test (AUDIT, mode-agnostic variant per the user's pollinator investigation): the
   * pre-fix bug is mode-agnostic; the converter strips an omitted firewall field for any mode.
   * This test pins that the fix preserves AUDIT just as well as PCCS, refuting the
   * selection-effect interpretation of the original repro.
   */
  @Test
  public void preserveFirewallFromExistingIfAbsent_afterConverterStripsOmittedFirewall_AUDITPreserved() {
    Configuration converted = convertProxyRequestWithoutFirewall("maven-proxy-test", "maven2");

    Configuration existing = configWithFirewallMode("AUDIT");

    BaseRepositoryManager.preserveFirewallFromExistingIfAbsent(existing, converted, "maven-proxy-test");

    assertThat(converted.attributes("firewall").get("mode"), is((Object) "AUDIT"));
  }

  /**
   * H3 replication-gate test: the call site in {@link BaseRepositoryManager#update(Configuration)}
   * gates the helper on {@code !EventHelper.isReplicating()}. Replication should faithfully
   * apply the originator's authoritative state, not heuristically merge. This test mirrors
   * the call-site gate inline (the helper itself is unconditional, matching the secret-encoder
   * pattern in the same method) and asserts that {@link EventHelper#asReplicating(Runnable)}
   * causes the gate to skip preservation.
   */
  @Test
  public void preserveFirewallFromExistingIfAbsent_callSiteGate_skipsPreservationDuringReplication() {
    Configuration existing = configWithFirewallMode("PCCS");
    Configuration target = configWithoutFirewall();

    EventHelper.asReplicating(() -> {
      // Mirror the call-site gate from BaseRepositoryManager#update(Configuration) verbatim.
      if (!EventHelper.isReplicating()) {
        BaseRepositoryManager.preserveFirewallFromExistingIfAbsent(existing, target, FIREWALL_TEST_REPO);
      }
    });

    Map<String, Map<String, Object>> attrs = target.getAttributes();
    assertThat(
        "target should NOT have been mutated when replicating",
        attrs == null || !attrs.containsKey("firewall"), is(true));

    // Sanity: outside replication the gate opens and the helper preserves as expected.
    BaseRepositoryManager.preserveFirewallFromExistingIfAbsent(existing, target, FIREWALL_TEST_REPO);
    assertThat(target.attributes("firewall").get("mode"), is((Object) "PCCS"));
  }

  private static Configuration configWithFirewallMode(final String mode) {
    ConfigurationData config = new ConfigurationData();
    Map<String, Map<String, Object>> attrs = new HashMap<>();
    Map<String, Object> firewall = new HashMap<>();
    firewall.put("mode", mode);
    attrs.put("firewall", firewall);
    config.setAttributes(attrs);
    return config;
  }

  private static Configuration configWithoutFirewall() {
    ConfigurationData config = new ConfigurationData();
    config.setAttributes(new HashMap<>());
    return config;
  }

  private static Configuration configWithEmptyFirewallMap() {
    ConfigurationData config = new ConfigurationData();
    Map<String, Map<String, Object>> attrs = new HashMap<>();
    attrs.put("firewall", new HashMap<>());
    config.setAttributes(attrs);
    return config;
  }

  /**
   * Build a {@link ProxyRepositoryApiRequest} with no {@code firewall} field, run it through
   * a real {@link ProxyRepositoryApiRequestToConfigurationConverter} backed by a fresh
   * {@link ConfigurationData}, and return the converted Configuration. Mirrors the bug
   * reproduction shape: a non-frontend client PUTs a partial body that does not re-state
   * firewall.
   */
  private static Configuration convertProxyRequestWithoutFirewall(final String name, final String format) {
    ProxyRepositoryApiRequest request = new ProxyRepositoryApiRequest(
        name, format, /* online */ true,
        /* storage */ null, /* cleanup */ null, /* proxy */ null,
        /* negativeCache */ null, /* httpClient */ null,
        /* routingRuleName */ null, /* replication */ null);
    // Firewall field intentionally NOT set — reproduces the omitted-firewall PUT.

    ConfigurationStore store = mock(ConfigurationStore.class);
    when(store.newConfiguration()).thenReturn(new ConfigurationData());

    ProxyRepositoryApiRequestToConfigurationConverter<ProxyRepositoryApiRequest> converter =
        new ProxyRepositoryApiRequestToConfigurationConverter<>(mock(RoutingRuleStore.class));
    converter.setConfigurationStore(store);

    return converter.convert(request);
  }

  @Test
  public void getMaterializesRepositoryFromStoreOnReadMiss() throws Exception {
    repositoryManager = buildRepositoryManagerImpl(false, true); // empty: nothing cached at start
    when(nodeAccess.isClustered()).thenReturn(true);

    Configuration peerConfig = mock(Configuration.class);
    Repository peerRepo = mock(Repository.class);
    mockRepository(peerConfig, peerRepo, "peer-repo", "default");
    when(configurationStore.readByName("peer-repo")).thenReturn(Optional.of(peerConfig));
    when(repositoryFactory.create(type, format)).thenReturn(peerRepo);

    // get() misses the in-memory cache, materializes from the store, and caches it
    Repository result = repositoryManager.get("peer-repo");

    assertThat(result, is(peerRepo));
    verify(peerRepo).start(); // fully started before being returned
    // now cached: a plain (non-materializing) softGet returns it, proving the cache was updated
    assertThat(repositoryManager.softGet("peer-repo"), is(peerRepo));
  }

  @Test
  public void getTreatsConcurrentlyDeletedRepositoryAsReadMiss() throws Exception {
    repositoryManager = buildRepositoryManagerImpl(false, true);
    when(nodeAccess.isClustered()).thenReturn(true);

    Configuration ghostConfig = mock(Configuration.class);
    Repository ghostRepo = mock(Repository.class);
    mockRepository(ghostConfig, ghostRepo, "ghost-repo", "default");
    // Row is present when materialization starts, then a concurrent peer delete removes it; the post-materialize
    // re-check (NEXUS-54229) must treat this as a miss rather than return a repository that no longer exists.
    when(configurationStore.readByName("ghost-repo")).thenReturn(Optional.of(ghostConfig), Optional.empty());
    when(repositoryFactory.create(type, format)).thenReturn(ghostRepo);

    assertThat(repositoryManager.get("ghost-repo"), is(nullValue()));
  }

  @Test
  public void getReturnsNullForAbsentRepositoryWithoutMaterializing() throws Exception {
    repositoryManager = buildRepositoryManagerImpl(false, true);
    when(nodeAccess.isClustered()).thenReturn(true);
    when(configurationStore.readByName("does-not-exist")).thenReturn(Optional.empty());

    assertThat(repositoryManager.get("does-not-exist"), is(nullValue()));

    // a single store read is an authoritative miss for a genuinely-absent repository -- nothing is materialized
    verify(repositoryFactory, never()).create(any(), any());
  }

  @Test
  public void getReturnsCachedRepositoryWithoutConsultingStore() throws Exception {
    repositoryManager = buildRepositoryManagerImpl(true); // loads maven-central etc. into the cache
    when(nodeAccess.isClustered()).thenReturn(true);

    Repository result = repositoryManager.get(MAVEN_CENTRAL_NAME);

    assertThat(result, is(mavenCentralRepository));
    // a cache hit must not enter the read-through path
    verify(configurationStore, never()).readByName(any());
  }

  @Test
  public void getSkipsReadThroughOnSingleNode() throws Exception {
    repositoryManager = buildRepositoryManagerImpl(false, true);
    when(nodeAccess.isClustered()).thenReturn(false); // single node -- a miss is authoritative

    assertThat(repositoryManager.get("peer-repo"), is(nullValue()));

    // no peers, so a miss must not consult the store at all
    verify(configurationStore, never()).readByName(any());
  }

  @Test
  public void getReadMissMaterializesWithoutPersistingAndFiresEvent() throws Exception {
    repositoryManager = buildRepositoryManagerImpl(false, true);
    when(nodeAccess.isClustered()).thenReturn(true);

    Configuration peerConfig = mock(Configuration.class);
    Repository peerRepo = mock(Repository.class);
    mockRepository(peerConfig, peerRepo, "peer-repo", "default");
    when(configurationStore.readByName("peer-repo")).thenReturn(Optional.of(peerConfig));
    when(repositoryFactory.create(type, format)).thenReturn(peerRepo);

    Repository result = repositoryManager.get("peer-repo");

    assertThat(result, is(peerRepo));
    // materializing an already-persisted peer config must not re-persist it ...
    verify(configurationStore, never()).create(any());
    // ... but must still fire the create event locally so local subscribers react
    verify(eventManager).post(any(RepositoryCreatedEvent.class));
  }

  @Test
  public void replicatingCreateAdoptsAlreadyMaterializedRepository() throws Exception {
    repositoryManager = buildRepositoryManagerImpl(false, true);

    Configuration config = mock(Configuration.class);
    Repository repo = mock(Repository.class);
    mockRepository(config, repo, "shared-repo", "default");
    when(repositoryFactory.create(type, format)).thenReturn(repo);

    // First replicated materialization builds, starts and tracks the repository exactly once.
    Repository first = createReplicating(config);
    assertThat(first, is(repo));

    // A second replicated create for the same name (a get() read-miss racing the config event handler) must
    // adopt the already-tracked instance instead of building/starting/persisting a duplicate -- a duplicate
    // would leak and, for path-routed formats such as Docker, deregister the live instance.
    Repository second = createReplicating(config);

    assertThat(second, is(first));
    verify(repositoryFactory, times(1)).create(type, format); // not rebuilt
    verify(repo, times(1)).start(); // not restarted
    verify(configurationStore, never()).create(any()); // never persisted while replicating
  }

  @Test
  public void createDoesNotReenterMaterializationWhenStartResolvesSelf() throws Exception {
    repositoryManager = buildRepositoryManagerImpl(false, true);
    when(nodeAccess.isClustered()).thenReturn(true);

    Configuration config = mock(Configuration.class);
    Repository repo = mock(Repository.class);
    mockRepository(config, repo, "self-ref", "default");
    when(repositoryFactory.create(type, format)).thenReturn(repo);
    when(configurationStore.exists("self-ref")).thenReturn(true);
    when(configurationStore.readByName("self-ref")).thenReturn(Optional.of(config));

    // Simulate a subscriber/facet resolving get(self) while the repository is starting -- before it is tracked.
    // The read-miss must not re-enter create() (that would self-wait on the in-flight cooperation future); it
    // returns the in-progress instance being configured on this thread.
    Repository[] resolvedDuringStart = new Repository[1];
    doAnswer(invocation -> {
      resolvedDuringStart[0] = repositoryManager.get("self-ref");
      return null;
    }).when(repo).start();

    Repository created = repositoryManager.create(config);

    assertThat(created, is(repo));
    assertThat("get(self) during start must return the in-progress instance, not re-enter materialization",
        resolvedDuringStart[0], is(repo));
    // built exactly once -- the re-entrant read-miss did not spin up a duplicate materialization
    verify(repositoryFactory, times(1)).create(type, format);
  }

  @Test
  public void reentrantCreateReturnsTheInProgressInstanceWithoutSelfWaiting() throws Exception {
    repositoryManager = buildRepositoryManagerImpl(false, true);
    when(nodeAccess.isClustered()).thenReturn(true);

    Configuration config = mock(Configuration.class);
    Repository repo = mock(Repository.class);
    mockRepository(config, repo, "self-ref", "default");
    when(repositoryFactory.create(type, format)).thenReturn(repo);

    // A facet/subscriber that re-enters create(self) while the repository is starting must get the in-progress
    // instance back immediately, not re-enter the cooperation point and wait on this thread's own future.
    Repository[] reentrant = new Repository[1];
    doAnswer(invocation -> {
      try {
        reentrant[0] = repositoryManager.create(config);
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
      return null;
    }).when(repo).start();

    Repository created = repositoryManager.create(config);

    assertThat(created, is(repo));
    assertThat("re-entrant create(self) during start returns the in-progress instance", reentrant[0], is(repo));
    verify(repositoryFactory, times(1)).create(type, format); // built exactly once, no self-wait re-materialize
  }

  private Repository createReplicating(final Configuration configuration) {
    return EventHelper.asReplicating(() -> {
      try {
        return repositoryManager.create(configuration);
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }
}
