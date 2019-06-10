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
package org.sonatype.nexus.repository.apt.internal;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.apt.internal.hosted.AptHostedFacet;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.BucketStore;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;

import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @since 3.next
 */
public class AptComponentDirectorTest
    extends TestSupport
{
  @Mock
  private BucketStore bucketStore;

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private Component component;

  @Mock
  private Repository source;

  @Mock
  private AptHostedFacet sourceFacet;

  @Mock
  private Repository destination;

  @Mock
  private StorageTx tx;

  @Mock
  private StorageFacet storageFacet;

  @Test
  public void allowMoveTest() {
    AptComponentDirector director = new AptComponentDirector(bucketStore, repositoryManager);
    assertTrue(director.allowMoveTo(destination));
    assertTrue(director.allowMoveFrom(source));

    EntityId bucketId = mock(EntityId.class);
    when(component.bucketId()).thenReturn(bucketId);
    Bucket bucket = mock(Bucket.class);
    when(bucketStore.getById(bucketId)).thenReturn(bucket);
    when(bucket.getRepositoryName()).thenReturn("repo");
    when(repositoryManager.get("repo")).thenReturn(source);

    assertTrue(director.allowMoveTo(component, destination));
  }

  @Test
  public void refreshMetadataAfterMoveTest() throws IOException
  {
    when(destination.facet(StorageFacet.class)).thenReturn(storageFacet);
    when(storageFacet.txSupplier()).thenReturn(() -> tx);
    when(destination.facet(AptHostedFacet.class)).thenReturn(sourceFacet);

    AptComponentDirector director = new AptComponentDirector(bucketStore, repositoryManager);
    director.afterMove(Collections.emptyList(), destination);
    verify(sourceFacet).rebuildIndexes();
  }

  @Test(expected = UncheckedIOException.class)
  public void afterMoveHookIoExceptionTest() throws IOException
  {
    when(destination.facet(StorageFacet.class)).thenReturn(storageFacet);
    when(storageFacet.txSupplier()).thenReturn(() -> tx);
    when(destination.facet(AptHostedFacet.class)).thenReturn(sourceFacet);

    AptComponentDirector director = new AptComponentDirector(bucketStore, repositoryManager);
    doThrow(new IOException()).when(sourceFacet).rebuildIndexes();
    director.afterMove(Collections.emptyList(), destination);
  }
}
