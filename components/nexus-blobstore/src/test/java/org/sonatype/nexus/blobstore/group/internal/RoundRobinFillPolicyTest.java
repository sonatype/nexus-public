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
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.group.BlobStoreGroup;
import org.sonatype.nexus.blobstore.quota.BlobStoreQuotaResult;
import org.sonatype.nexus.blobstore.quota.BlobStoreQuotaService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

public class RoundRobinFillPolicyTest
    extends TestSupport
{

  @Mock
  private BlobStoreQuotaService blobStoreQuotaService;

  @InjectMocks
  private RoundRobinFillPolicy roundRobinFillPolicy;

  private BlobStoreQuotaResult blobStoreQuotaResult;

  @Before
  public void setup() {
    blobStoreQuotaResult = new BlobStoreQuotaResult(true, "", "");
  }

  @Test
  public void nextIndexGivesExpectedValueWhenStartingAtInitialValue() {
    roundRobinFillPolicy.sequence.set(0);
    assertThat(roundRobinFillPolicy.nextIndex(), is(0));
    assertThat(roundRobinFillPolicy.nextIndex(), is(1));

    roundRobinFillPolicy.sequence.set(Integer.MAX_VALUE);
    assertThat(roundRobinFillPolicy.nextIndex(), is(Integer.MAX_VALUE));
    assertThat(roundRobinFillPolicy.nextIndex(), is(0));
  }

  @Test
  public void itWillSkipBlobStoresThatAreNotWritable() {
    BlobStoreGroup blobStoreGroup = mock(BlobStoreGroup.class);
    List<BlobStore> members = Arrays.asList(
        mockMemberWithAvailability("one", false),
        mockMemberWithAvailability("two", false),
        mockMemberWithAvailability("three", true),
        mockMemberWithAvailability("four", true));
    when(blobStoreGroup.getMembers()).thenReturn(members);

    BlobStore blobStore = roundRobinFillPolicy.chooseBlobStore(blobStoreGroup, Collections.emptyMap());
    assertThat(blobStore.getBlobStoreConfiguration().getName(), is("three"));
    assertThat(roundRobinFillPolicy.nextIndex(), is(1));
  }

  @Test
  public void itWillReturnNullIfNoMembersAreWritable() {
    BlobStoreGroup blobStoreGroup = mock(BlobStoreGroup.class);
    List<BlobStore> members = Arrays.asList(
        mockMemberWithAvailability("one", false),
        mockMemberWithAvailability("two", false));
    when(blobStoreGroup.getMembers()).thenReturn(members);

    roundRobinFillPolicy.sequence.set(0);
    assertNull(roundRobinFillPolicy.chooseBlobStore(blobStoreGroup, Collections.emptyMap()));
    assertThat(roundRobinFillPolicy.nextIndex(), is(1));

    roundRobinFillPolicy.sequence.set(1);
    assertNull(roundRobinFillPolicy.chooseBlobStore(blobStoreGroup, Collections.emptyMap()));
    assertThat(roundRobinFillPolicy.nextIndex(), is(2));
  }

  @Test
  public void itWillReturnNullIfTheGroupHasNoMember() {
    BlobStoreGroup blobStoreGroup = mock(BlobStoreGroup.class);
    when(blobStoreGroup.getMembers()).thenReturn(Collections.emptyList());

    assertNull(roundRobinFillPolicy.chooseBlobStore(blobStoreGroup, Collections.emptyMap()));
  }

  @Test
  public void itWillSkipReadOnlyMembers() {
    BlobStoreGroup blobStoreGroup = mock(BlobStoreGroup.class);
    List<BlobStore> members = Arrays.asList(
        mockMemberWithWritable("one", false),
        mockMemberWithWritable("two", true),
        mockMemberWithWritable("three", false),
        mockMemberWithWritable("four", false),
        mockMemberWithWritable("five", true));
    when(blobStoreGroup.getMembers()).thenReturn(members);

    roundRobinFillPolicy.sequence.set(0);
    assertThat(roundRobinFillPolicy.chooseBlobStore(blobStoreGroup, Collections.emptyMap())
        .getBlobStoreConfiguration()
        .getName(), is("two"));

    roundRobinFillPolicy.sequence.set(1);
    assertThat(roundRobinFillPolicy.chooseBlobStore(blobStoreGroup, Collections.emptyMap())
        .getBlobStoreConfiguration()
        .getName(), is("five"));
  }

  @Test
  public void itWillNotSkipMembersWithQuotaViolation() {
    BlobStoreGroup blobStoreGroup = mock(BlobStoreGroup.class);
    List<BlobStore> members = Arrays.asList(
        mockMember("One", blobStoreQuotaResult),
        mockMember("Two", null),
        mockMember("Three", blobStoreQuotaResult));
    when(blobStoreGroup.getMembers()).thenReturn(members);

    BlobStore store = roundRobinFillPolicy.chooseBlobStore(blobStoreGroup, Collections.emptyMap());
    assertThat(store.getBlobStoreConfiguration().getName(), is("One"));
  }

  @Test
  public void itWillSkipAllMembersWithQuotaViolation() {
    BlobStoreGroup blobStoreGroup = mock(BlobStoreGroup.class);
    List<BlobStore> members = Arrays.asList(
        mockMember("One", blobStoreQuotaResult),
        mockMember("Three", blobStoreQuotaResult));
    when(blobStoreGroup.getMembers()).thenReturn(members);

    roundRobinFillPolicy.skipOnSoftQuotaViolation = true;
    assertNull(roundRobinFillPolicy.chooseBlobStore(blobStoreGroup, Collections.emptyMap()));
  }

  @Test
  public void itWillSkipMembersWithQuotaViolation() {
    BlobStoreGroup blobStoreGroup = mock(BlobStoreGroup.class);
    List<BlobStore> members = Arrays.asList(
        mockMember("One", blobStoreQuotaResult),
        mockMember("Two", null),
        mockMember("Three", blobStoreQuotaResult),
        mockMember("Four", null));
    when(blobStoreGroup.getMembers()).thenReturn(members);

    roundRobinFillPolicy.skipOnSoftQuotaViolation = true;
    assertThat(roundRobinFillPolicy.chooseBlobStore(blobStoreGroup, Collections.emptyMap())
        .getBlobStoreConfiguration()
        .getName(), is("Two"));
    assertThat(roundRobinFillPolicy.chooseBlobStore(blobStoreGroup, Collections.emptyMap())
        .getBlobStoreConfiguration()
        .getName(), is("Four"));
  }

  private BlobStore mockMemberWithWritable(final String name, final boolean writable) {
    BlobStore blobStore = mock(BlobStore.class);
    when(blobStore.isStorageAvailable()).thenReturn(true);
    when(blobStore.isWritable()).thenReturn(writable);
    BlobStoreConfiguration config = mock(BlobStoreConfiguration.class);
    when(config.getName()).thenReturn(name);
    when(blobStore.getBlobStoreConfiguration()).thenReturn(config);
    return blobStore;
  }

  private BlobStore mockMemberWithAvailability(final String name, final boolean availability) {
    BlobStore blobStore = mock(BlobStore.class);
    when(blobStore.isStorageAvailable()).thenReturn(availability);
    when(blobStore.isWritable()).thenReturn(true);
    BlobStoreConfiguration config = mock(BlobStoreConfiguration.class);
    when(config.getName()).thenReturn(name);
    when(blobStore.getBlobStoreConfiguration()).thenReturn(config);
    return blobStore;
  }

  private BlobStore mockMember(final String name, final BlobStoreQuotaResult result) {
    BlobStore blobStore = mock(BlobStore.class);
    when(blobStore.isStorageAvailable()).thenReturn(true);
    when(blobStore.isWritable()).thenReturn(true);
    BlobStoreConfiguration config = mock(BlobStoreConfiguration.class);
    when(config.getName()).thenReturn(name);
    when(blobStore.getBlobStoreConfiguration()).thenReturn(config);
    when(blobStoreQuotaService.checkQuota(blobStore)).thenReturn(result);
    return blobStore;
  }
}
