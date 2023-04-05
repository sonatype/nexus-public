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
package org.sonatype.nexus.cleanup.internal.orient.service;

import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.cleanup.internal.orient.method.CleanupMethod;
import org.sonatype.nexus.cleanup.internal.orient.search.elasticsearch.OrientCleanupComponentBrowse;
import org.sonatype.nexus.cleanup.storage.CleanupPolicy;
import org.sonatype.nexus.cleanup.storage.CleanupPolicyStorage;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.storage.DefaultComponentMaintenanceImpl.DeletionProgress;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.types.GroupType;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.elasticsearch.search.SearchContextMissingException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.google.common.collect.Sets.newLinkedHashSet;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.stream.Stream.empty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.LAST_BLOB_UPDATED_KEY;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.LAST_DOWNLOADED_KEY;
import static org.sonatype.nexus.testcommon.matchers.NexusMatchers.streamContains;

public class OrientCleanupServiceImplTest
    extends TestSupport
{
  private static final String POLICY_1_NAME = "policy1";

  private static final String POLICY_2_NAME = "policy2";

  private static final int RETRY_LIMIT = 3;

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private Repository repository1, repository2, repository3;

  @Mock
  private OrientCleanupComponentBrowse browseService;

  @Mock
  private CleanupPolicyStorage cleanupPolicyStorage;

  @Mock
  private CleanupPolicy cleanupPolicy1, cleanupPolicy2;

  @Mock
  private CleanupMethod cleanupMethod;

  @Mock
  private StorageFacet storageFacet;

  @Mock
  private EntityId component1, component2, component3;

  @Mock
  private Type type;

  @Mock
  private StorageTx tx;

  @Mock
  private BooleanSupplier cancelledCheck;

  @Mock
  private DeletionProgress deletionProgress;

  private OrientCleanupServiceImpl underTest;

  @Before
  public void setup() throws Exception {
    underTest = new OrientCleanupServiceImpl(repositoryManager, browseService, cleanupPolicyStorage, cleanupMethod,
        new GroupType(), RETRY_LIMIT);

    setupRepository(repository1, POLICY_1_NAME);
    setupRepository(repository2, POLICY_2_NAME);
    setupRepository(repository3, null);

    when(storageFacet.txSupplier()).thenReturn(() -> tx);

    when(repositoryManager.browse()).thenReturn(ImmutableList.of(repository1, repository2));

    when(cleanupPolicyStorage.get(POLICY_1_NAME)).thenReturn(cleanupPolicy1);
    when(cleanupPolicyStorage.get(POLICY_2_NAME)).thenReturn(cleanupPolicy2);

    when(cleanupPolicy1.getCriteria()).thenReturn(ImmutableMap.of(LAST_BLOB_UPDATED_KEY, "1"));
    when(cleanupPolicy2.getCriteria()).thenReturn(ImmutableMap.of(LAST_DOWNLOADED_KEY, "2"));

    when(browseService.browse(cleanupPolicy1, repository1)).thenReturn(ImmutableList.of(component1, component2).stream());
    when(browseService.browse(cleanupPolicy2, repository2)).thenReturn(ImmutableList.of(component3).stream());

    when(cancelledCheck.getAsBoolean()).thenReturn(false);

    when(deletionProgress.isFailed()).thenReturn(false);
    when(cleanupMethod.run(any(), any(), any())).thenReturn(deletionProgress);
  }

  @Test
  public void fetchPolicyForEachRepositoryAndRunCleanup() throws Exception {
    underTest.cleanup(cancelledCheck);

    verify(cleanupMethod).run(eq(repository1), argThat(streamContains(component1,  component2)), eq(cancelledCheck));
    verify(cleanupMethod).run(eq(repository2), argThat(streamContains(component3)), eq(cancelledCheck));
  }

  @Test
  public void fetchMultiplePoliciesForEachRepositoryAndRunCleanup() {
    String[] policyNamesForRepo1 = {"abc", "def", "ghi"};
    Stream<EntityId> componentsForRepo1 = setupForMultiplePolicies(repository1, policyNamesForRepo1);

    String[] policyNamesForRepo2 = {"qwe", "rty", "uio"};
    Stream<EntityId> componentsForRepo2 = setupForMultiplePolicies(repository2, policyNamesForRepo2);

    underTest.cleanup(cancelledCheck);

    verify(cleanupMethod, times(policyNamesForRepo1.length)).run(repository1, componentsForRepo1, cancelledCheck);
    verify(cleanupMethod, times(policyNamesForRepo2.length)).run(repository2, componentsForRepo2, cancelledCheck);
  }

  @Test
  public void ignoreRepositoryWhenPolicyNull() throws Exception {
    when(cleanupPolicyStorage.get(POLICY_2_NAME)).thenReturn(null);

    underTest.cleanup(cancelledCheck);

    verify(cleanupMethod).run(eq(repository1), argThat(streamContains(component1,  component2)), eq(cancelledCheck));
  }

  @Test
  public void ignoreRepositoryWhenPolicyNameNull() throws Exception {
    setupRepository(repository2, new String[]{null});
    when(cleanupPolicyStorage.get(null)).thenThrow(new NullPointerException());

    underTest.cleanup(cancelledCheck);

    verify(cleanupMethod).run(eq(repository1), argThat(streamContains(component1,  component2)), eq(cancelledCheck));
  }

  @Test
  public void ignoreRepositoryWhenPolicyNameListIsNull() throws Exception {
    when(repositoryManager.browse()).thenReturn(ImmutableList.of(repository3));

    underTest.cleanup(cancelledCheck);

    verify(cleanupMethod, never()).run(eq(repository3), argThat(streamContains(component2)), eq(cancelledCheck));
  }

  @Test
  public void ignoreRepositoryWhenPolicyNameAttributeIsNotPresent() throws Exception {
    repository1.getConfiguration().setAttributes(emptyMap());

    underTest.cleanup(cancelledCheck);

    verify(cleanupMethod).run(eq(repository2), argThat(streamContains(component3)), eq(cancelledCheck));
  }

  @Test
  public void ignoreRepositoryWhenAttributesNull() throws Exception {
    when(repository1.getConfiguration()).thenReturn(mock(Configuration.class));

    underTest.cleanup(cancelledCheck);

    verify(cleanupMethod).run(eq(repository2), argThat(streamContains(component3)), eq(cancelledCheck));
  }

  @Test
  public void ignoreRepositoryWhenCancelled() throws Exception {
    doAnswer(i -> {
      when(cancelledCheck.getAsBoolean()).thenReturn(true);
      return deletionProgress;
    }).when(cleanupMethod).run(eq(repository1), any(), eq(cancelledCheck));

    underTest.cleanup(cancelledCheck);

    verify(cleanupMethod).run(eq(repository1), argThat(streamContains(component1,  component2)), eq(cancelledCheck));
    verify(cleanupMethod, never()).run(eq(repository2), argThat(streamContains(component3)), eq(cancelledCheck));
  }

  @Test
  public void doNothingWhenNoComponentsFound() throws Exception {
    when(browseService.browse(cleanupPolicy1, repository1)).thenReturn(empty());
    when(browseService.browse(cleanupPolicy2, repository2)).thenReturn(empty());

    underTest.cleanup(cancelledCheck);

    verify(cleanupMethod, never()).run(eq(repository1), argThat(streamContains(component1,  component2)), eq(cancelledCheck));
    verify(cleanupMethod, never()).run(eq(repository2), argThat(streamContains(component3)), eq(cancelledCheck));
  }

  @Test
  public void doNotDeleteAnythingWhenCriteriaEmpty() throws Exception {
    when(cleanupPolicy1.getCriteria()).thenReturn(emptyMap());
    when(cleanupPolicy2.getCriteria()).thenReturn(emptyMap());

    underTest.cleanup(cancelledCheck);

    verify(cleanupMethod, never()).run(eq(repository1), argThat(streamContains(component1,  component2)), eq(cancelledCheck));
    verify(cleanupMethod, never()).run(eq(repository2), argThat(streamContains(component3)), eq(cancelledCheck));
  }

  @Test
  public void retryDeletionIfFailure() {
    when(deletionProgress.isFailed()).thenReturn(true).thenReturn(false);
    when(repositoryManager.browse()).thenReturn(ImmutableList.of(repository1));
    when(cleanupMethod.run(any(), any(), any())).thenReturn(deletionProgress);

    underTest.cleanup(cancelledCheck);

    verify(cleanupMethod, times(2)).run(any(), any(), any());
  }

  @Test
  public void retryAttemptsExceeded() {
    when(deletionProgress.isFailed()).thenReturn(true);
    when(repositoryManager.browse()).thenReturn(ImmutableList.of(repository1));
    when(cleanupMethod.run(any(), any(), any())).thenReturn(deletionProgress);

    underTest.cleanup(cancelledCheck);

    verify(cleanupMethod, times(3)).run(any(), any(), any());
  }

  @Test
  public void cleanupRetriedOnScrollTimeout() {
    when(cleanupMethod.run(any(), any(), any()))
        .thenThrow(new RuntimeException(new SearchContextMissingException(10L))).thenReturn(deletionProgress);

    underTest.cleanup(cancelledCheck);

    verify(cleanupMethod, times(2)).run(eq(repository1), argThat(streamContains(component1,  component2)), eq(cancelledCheck));
    verify(cleanupMethod).run(eq(repository2), argThat(streamContains(component3)), eq(cancelledCheck));
  }

  @Test
  public void cleanupFailed() {
    when(cleanupMethod.run(any(), any(), any())).thenThrow(new RuntimeException());

    underTest.cleanup(cancelledCheck);

    verify(cleanupMethod, times(3)).run(eq(repository1), argThat(streamContains(component1,  component2)), eq(cancelledCheck));
    verify(cleanupMethod, times(3)).run(eq(repository2), argThat(streamContains(component3)), eq(cancelledCheck));
  }

  @Test
  public void cleanupFailedSearchContextMissingException() {
    when(cleanupMethod.run(any(), any(), any()))
        .thenThrow(new SearchContextMissingException(10L));
    underTest.cleanup(cancelledCheck);

    verify(cleanupMethod, times(3)).run(eq(repository1), argThat(streamContains(component1,  component2)), eq(cancelledCheck));
    verify(cleanupMethod, times(3)).run(eq(repository2), argThat(streamContains(component3)), eq(cancelledCheck));
  }

  private void setupRepository(final Repository repository, final String... policyName) {
    Configuration repositoryConfig = mock(Configuration.class);
    when(repository.getConfiguration()).thenReturn(repositoryConfig);

    ImmutableMap<String, Map<String, Object>> attributes = ImmutableMap
        .of("cleanup", singletonMap("policyName", policyName != null ? newLinkedHashSet(asList(policyName)) : null));
    when(repositoryConfig.getAttributes()).thenReturn(attributes);

    when(repository.getType()).thenReturn(type);

    when(repository.facet(StorageFacet.class)).thenReturn(storageFacet);
  }

  private Stream<EntityId> setupForMultiplePolicies(final Repository repository, final String... policyNames) {
    setupRepository(repository, policyNames);
    return setupComponents(repository, policyNames);
  }

  private Stream<EntityId> setupComponents(final Repository repository,
                                           final String... policyNames)
  {
    Stream<EntityId> components = ImmutableList.of(mock(EntityId.class), mock(EntityId.class)).stream();

    asList(policyNames).forEach(policyName -> {
      CleanupPolicy cleanupPolicy = mock(CleanupPolicy.class);
      when(cleanupPolicy.getCriteria()).thenReturn(ImmutableMap.of(LAST_BLOB_UPDATED_KEY, "1"));
      when(cleanupPolicyStorage.get(policyName)).thenReturn(cleanupPolicy);
      when(browseService.browse(cleanupPolicy, repository)).thenReturn(components);
    });

    return components;
  }
}
