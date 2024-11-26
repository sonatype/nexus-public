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
package org.sonatype.nexus.blobstore.group.internal;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.BlobStoreUtil;
import org.sonatype.nexus.blobstore.MockBlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.group.BlobStoreGroup;
import org.sonatype.nexus.blobstore.group.BlobStoreGroupService;
import org.sonatype.nexus.blobstore.group.FillPolicy;
import org.sonatype.nexus.blobstore.quota.BlobStoreQuotaService;
import org.sonatype.nexus.rest.ValidationErrorsException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.*;

public class BlobStoreGroupDescriptorTest
    extends TestSupport
{
  private static final String FILE = "File";

  @Mock
  private BlobStoreManager blobStoreManager;

  @Mock
  private BlobStoreUtil blobStoreUtil;

  @Mock
  private BlobStoreGroupService blobStoreGroupService;

  @Mock
  private BlobStoreQuotaService quotaService;

  private BlobStoreGroupDescriptor blobStoreGroupDescriptor;

  private Map<String, BlobStore> blobStores;

  @Before
  public void setup() {
    Map<String, FillPolicy> fillPolicies = new HashMap<>();
    fillPolicies.put(RoundRobinFillPolicy.TYPE, new RoundRobinFillPolicy());
    fillPolicies.put(WriteToFirstMemberFillPolicy.TYPE, new WriteToFirstMemberFillPolicy());

    blobStoreGroupDescriptor = new BlobStoreGroupDescriptor(
        blobStoreManager,
        blobStoreUtil,
        () -> blobStoreGroupService,
        quotaService,
        fillPolicies);

    blobStores = new HashMap<>();

    when(blobStoreManager.get(anyString())).thenAnswer(invocation -> {
      String name = invocation.getArgument(0, String.class);
      return blobStores.computeIfAbsent(name, k -> mockBlobStore(k, "mock"));
    });
    when(blobStoreManager.hasConflictingTasks(anyString())).thenReturn(false);
    when(blobStoreManager.browse()).thenReturn(blobStores.values());
    when(blobStoreManager.getParent(anyString())).thenReturn(Optional.empty());
    when(blobStoreUtil.usageCount(anyString())).thenReturn(0);
    when(blobStoreGroupService.isEnabled()).thenReturn(true);
  }

  @Test
  public void validateWithValidMembers() {
    BlobStoreConfiguration blobConfig =
        buildBlobStoreConfiguration("group", Arrays.asList("store1", "store2"), WriteToFirstMemberFillPolicy.TYPE);
    blobStoreGroupDescriptor.validateConfig(blobConfig);
    verify(blobStoreManager, times(2)).hasConflictingTasks(any());
  }

  @Test
  public void validateInvalidMembers() {
    // members cannot be empty
    BlobStoreConfiguration config = buildBlobStoreConfiguration("self", emptyList(), WriteToFirstMemberFillPolicy.TYPE);

    blobStores.put("nested", mockBlobStore("nested", BlobStoreGroup.TYPE, config.getAttributes(), false));

    ValidationErrorsException exception =
        assertThrows(ValidationErrorsException.class, () -> blobStoreGroupDescriptor.validateConfig(config));
    assertThat(exception.getMessage(), is("Blob Store 'self' cannot be empty"));

    // members cannot contain itself
    BlobStoreConfiguration config2 =
        buildBlobStoreConfiguration("self", singletonList("self"), WriteToFirstMemberFillPolicy.TYPE);

    blobStores.put("nested", mockBlobStore("nested", BlobStoreGroup.TYPE, config2.getAttributes(), false));

    ValidationErrorsException exception2 =
        assertThrows(ValidationErrorsException.class, () -> blobStoreGroupDescriptor.validateConfig(config2));
    assertThat(exception2.getMessage(), is("Blob Store 'self' cannot contain itself"));

    // members cannot be of type nested
    BlobStoreConfiguration config3 =
        buildBlobStoreConfiguration("self", singletonList("nested"), WriteToFirstMemberFillPolicy.TYPE);
    blobStores.put("nested", mockBlobStore("nested", BlobStoreGroup.TYPE, config3.getAttributes(), false));

    ValidationErrorsException exception3 =
        assertThrows(ValidationErrorsException.class, () -> blobStoreGroupDescriptor.validateConfig(config3));
    assertThat(exception3.getMessage(),
        is("Blob Store 'nested' is of type 'Group' and is not eligible to be a group member"));
  }

  @Test
  public void validateBlobStoreWithConflictingTasksRunning() {
    BlobStore hosted0 = mockBlobStore("hosted-0", FILE);
    BlobStore hosted1 = mockBlobStore("hosted-1", FILE);
    blobStores.put("hosted-0", hosted0);
    blobStores.put("hosted-1", hosted1);

    BlobStoreConfiguration config =
        buildBlobStoreConfiguration("self", Arrays.asList("hosted-0", "hosted-1"), RoundRobinFillPolicy.TYPE);

    when(blobStoreManager.hasConflictingTasks("hosted-1")).thenReturn(true);
    ValidationErrorsException exception =
        assertThrows(ValidationErrorsException.class, () -> blobStoreGroupDescriptor.validateConfig(config));
    assertThat(exception.getMessage(),
        is("Blob Store 'hosted-1' has conflicting tasks running and is not eligible to be a group member"));
  }

  @Test
  public void blobStoresCanOnlyBeMembersOfOneGroup() {
    BlobStore store1 = mockBlobStore("store1", FILE);
    BlobStore group1 = mockBlobStoreGroup(singletonList(store1));
    blobStores.put("store1", store1);
    blobStores.put("group1", group1);

    BlobStoreConfiguration config =
        buildBlobStoreConfiguration("invalidGroup", singletonList("store1"), WriteToFirstMemberFillPolicy.TYPE);

    when(blobStoreManager.getParent("store1")).thenReturn(Optional.of("group1"));
    ValidationErrorsException exception =
        assertThrows(ValidationErrorsException.class, () -> blobStoreGroupDescriptor.validateConfig(config));
    assertThat(exception.getMessage(), is("Blob Store 'store1' is already a member of Blob Store Group 'group1'"));
  }

  @Test
  public void blobStoresCantBeGroupMembersIfSetAsRepoStorage() {
    BlobStoreConfiguration config =
        buildBlobStoreConfiguration("invalidGroup", singletonList("store1"), WriteToFirstMemberFillPolicy.TYPE);

    when(blobStoreUtil.usageCount("store1")).thenReturn(1);
    ValidationErrorsException exception =
        assertThrows(ValidationErrorsException.class, () -> blobStoreGroupDescriptor.validateConfig(config));
    assertThat(exception.getMessage(),
        is("Blob Store 'store1' is set as storage for 1 repositories and is not eligible to be a group member"));
  }

  @Test
  public void membersCantBeRemovedDirectlyUnlessReadOnlyAndEmpty() {
    BlobStore store1 = mockBlobStore("store1", FILE);
    BlobStore nonEmptyStore = mockBlobStore("nonEmptyStore", FILE, new HashMap<>(), true);
    when(nonEmptyStore.getBlobIdStream()).thenReturn(Stream.of(mock(BlobId.class)));
    BlobStoreGroup group1 = mockBlobStoreGroup(Arrays.asList(store1, nonEmptyStore));
    blobStores.put("store1", store1);
    blobStores.put("nonEmptyStore", nonEmptyStore);
    blobStores.put("group1", group1);

    BlobStoreConfiguration config =
        buildBlobStoreConfiguration("group1", singletonList("store1"), WriteToFirstMemberFillPolicy.TYPE);

    ValidationErrorsException exception =
        assertThrows(ValidationErrorsException.class, () -> blobStoreGroupDescriptor.validateConfig(config));
    assertThat(exception.getMessage(), is(
        "Blob Store 'nonEmptyStore' cannot be removed from Blob Store Group 'group1', " +
            "use 'Admin - Remove a member from a blob store group' task instead"));
  }

  @Test
  public void aGroupBlobStoreValidatesItsQuota() {
    BlobStoreConfiguration config =
        buildBlobStoreConfiguration("group", singletonList("single"), WriteToFirstMemberFillPolicy.TYPE);
    blobStoreGroupDescriptor.validateConfig(config);

    verify(quotaService).validateSoftQuotaConfig(any());
  }

  private BlobStoreConfiguration buildBlobStoreConfiguration(
      String name,
      List<String> memberNames,
      String fillPolicyName)
  {
    MockBlobStoreConfiguration config = new MockBlobStoreConfiguration();
    Map<String, Map<String, Object>> attributes = new HashMap<>();
    Map<String, Object> group = new HashMap<>();
    group.put("members", memberNames);
    group.put("fillPolicy", fillPolicyName);
    attributes.put("group", group);
    config.setAttributes(attributes);
    config.setName(name);
    return config;
  }

  private BlobStoreGroup mockBlobStoreGroup(final List<BlobStore> members) {
    MockBlobStoreConfiguration config = new MockBlobStoreConfiguration();
    config.setName("group1");
    config.setType(BlobStoreGroup.TYPE);

    Map<String, Object> groupAttributes = new HashMap<>();
    groupAttributes.put("members", members.stream()
        .map(m -> m.getBlobStoreConfiguration().getName())
        .collect(
            Collectors.toList()));
    groupAttributes.put("fillPolicy", WriteToFirstMemberFillPolicy.TYPE);
    Map<String, Map<String, Object>> attributes = new HashMap<>();
    attributes.put("group", groupAttributes);
    config.setAttributes(attributes);

    BlobStoreGroup group = mock(BlobStoreGroup.class);
    when(group.isGroupable()).thenReturn(false);
    when(group.getBlobStoreConfiguration()).thenReturn(config);
    when(group.getMembers()).thenReturn(members);

    return group;
  }

  private BlobStore mockBlobStore(final String name, final String type) {
    return mockBlobStore(name, type, new HashMap<>(), true);
  }

  private BlobStore mockBlobStore(
      final String name,
      final String type,
      Map<String, Map<String, Object>> attributes,
      Boolean groupable)
  {
    BlobStore blobStore = mock(BlobStore.class);
    MockBlobStoreConfiguration config = new MockBlobStoreConfiguration();
    config.setName(name);
    config.setType(type);
    config.setAttributes(attributes);
    when(blobStore.getBlobStoreConfiguration()).thenReturn(config);
    when(blobStore.isGroupable()).thenReturn(groupable);
    return blobStore;
  }
}
