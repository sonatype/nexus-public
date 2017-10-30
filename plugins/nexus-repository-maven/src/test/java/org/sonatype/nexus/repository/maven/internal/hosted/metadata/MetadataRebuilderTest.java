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
package org.sonatype.nexus.repository.maven.internal.hosted.metadata;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.entity.EntityMetadata;
import org.sonatype.nexus.orient.entity.AttachedEntityMetadata;
import org.sonatype.nexus.orient.entity.EntityAdapter;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.maven.MavenFacet;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;

import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static java.util.Collections.emptyList;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MetadataRebuilderTest
    extends TestSupport
{
  @Mock
  private StorageFacet storageFacet;

  @Mock
  private MavenFacet mavenFacet;

  @Mock
  private Repository repository;

  @Mock
  private StorageTx storageTx;

  private Bucket bucket;

  private static EntityMetadata mockBucketEntityMetadata() {
    EntityAdapter owner = mock(EntityAdapter.class);
    ODocument document = mock(ODocument.class);
    when(document.getIdentity()).thenReturn(new ORecordId(1, 1));
    return new AttachedEntityMetadata(owner, document);
  }

  @Before
  public void setup() {
    bucket = new Bucket();
    bucket.setEntityMetadata(mockBucketEntityMetadata());

    when(storageFacet.txSupplier()).thenReturn(() -> storageTx);
    when(repository.facet(StorageFacet.class)).thenReturn(storageFacet);
    when(repository.facet(MavenFacet.class)).thenReturn(mavenFacet);
    when(storageTx.findBucket(repository)).thenReturn(bucket);
  }

  @Test
  public void verifyAsyncControlArguments() {
    ArgumentCaptor<Integer> bufferSizeCaptor = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Long> timeoutSecondsCaptor = ArgumentCaptor.forClass(Long.class);

    when(storageTx
        .browse(anyString(), anyMapOf(String.class, Object.class), bufferSizeCaptor.capture(),
            timeoutSecondsCaptor.capture()))
        .thenReturn(emptyList());

    new MetadataRebuilder(10, 20).rebuild(repository, true, false, null, null, null);

    assertThat(bufferSizeCaptor.getValue(), equalTo(10));
    assertThat(timeoutSecondsCaptor.getValue(), equalTo(20L));
  }
}
