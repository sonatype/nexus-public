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

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.EntityMetadata;
import org.sonatype.nexus.common.event.EventHelper;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.node.NodeAccess;
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
import org.sonatype.nexus.repository.manager.DefaultRepositoriesContributor;

import jakarta.inject.Provider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;

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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.blobstore.api.BlobStoreManager.DEFAULT_BLOBSTORE_NAME;
import static org.sonatype.nexus.repository.manager.internal.BaseRepositoryManager.CLEANUP_ATTRIBUTES_KEY;
import static org.sonatype.nexus.repository.manager.internal.BaseRepositoryManager.CLEANUP_NAME_KEY;

public class BaseRepositoryManagerTest
    extends TestSupport
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
    when(QualifierUtil.buildQualifierBeanMap(any())).thenReturn(Map.of(recipeName, recipe));
    repositoryManager = new BaseRepositoryManager<>(eventManager, configurationStore, repositoryFactory,
        configurationFacetProvider, List.of(), securityContributor,
        defaultRepositoriesContributorList, skipDefaultRepositories, blobStoreManager,
        groupMemberMappingCache, List.of(), httpAuthenticationSecretEncoder)
    {
    };

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
  public void getFunctionalityShouldFallBackToDbIfMissing() throws Exception {
    repositoryManager = buildRepositoryManagerImpl(false, true);

    when(configurationStore.list()).thenReturn(List.of(mavenCentralConfiguration));

    Repository repository = repositoryManager.get(MAVEN_CENTRAL_NAME);

    assertThat(repository.getName(), is(MAVEN_CENTRAL_NAME));
  }

  @Test
  public void repoNotInCacheOrDbReturnsNullForGet() throws Exception {
    repositoryManager = buildRepositoryManagerImpl(false, true);

    when(configurationStore.readByNames(any(Set.class))).thenReturn(Set.of());

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

}
