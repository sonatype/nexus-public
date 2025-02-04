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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.group.BlobStoreGroup;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class WriteToFirstMemberFillPolicyTest
    extends TestSupport
{
  @Parameter
  public boolean available;

  @Parameter(1)
  public boolean writable;

  @Parameter(2)
  public String chosenBlobStoreName;

  private final WriteToFirstMemberFillPolicy underTest = new WriteToFirstMemberFillPolicy();

  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        {false, false, "three"},
        {false, true, "three"},
        {true, false, "three"},
        {true, true, "one"}
    });
  }

  @Test
  public void itShouldSkipNonAvailableAndNonWritableMembers() {
    BlobStoreGroup blobStoreGroup = mock(BlobStoreGroup.class);
    List<BlobStore> mockedMembers =
        Arrays.asList(mockMember("one", available, writable), mockMember("two", available, writable),
            mockMember("three", true, true));
    when(blobStoreGroup.getMembers()).thenReturn(mockedMembers);
    assertThat(underTest.chooseBlobStore(blobStoreGroup, new HashMap<>()).getBlobStoreConfiguration().getName(),
        is(chosenBlobStoreName));
  }

  private BlobStore mockMember(final String name, final boolean available, final boolean writable) {
    BlobStore member = mock(BlobStore.class);
    BlobStoreConfiguration blobStoreConfiguration = mock(BlobStoreConfiguration.class);
    when(member.getBlobStoreConfiguration()).thenReturn(blobStoreConfiguration);
    when(blobStoreConfiguration.getName()).thenReturn(name);
    when(member.isStorageAvailable()).thenReturn(available);
    when(member.isWritable()).thenReturn(writable);
    return member;
  }
}
