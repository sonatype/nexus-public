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
package org.sonatype.nexus.repository.purge;

import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.entity.EntityMetadata;
import org.sonatype.nexus.orient.entity.AttachedEntityMetadata;
import org.sonatype.nexus.orient.entity.EntityAdapter;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.ComponentEntityAdapter;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.scheduling.CancelableHelper;
import org.sonatype.nexus.scheduling.TaskInterruptedException;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.model.MultipleFailureException;
import org.mockito.Mock;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Thread.sleep;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PurgeUnusedFacetImplTest
    extends TestSupport
{
  @Mock
  private ComponentEntityAdapter componentEntityAdapter;

  @Mock
  private Repository repository;

  @Mock
  private StorageFacet storageFacet;

  @Mock
  private StorageTx tx;

  @Mock
  private Iterable<ODocument> componentDocsIterable;

  @Mock
  private Iterator<ODocument> componentDocsIterator;

  @Mock
  private Iterable<Asset> assetIterable;

  @Mock
  private Iterator<Asset> assetIterator;

  private List<Throwable> uncaught;

  private PurgeUnusedFacetImpl underTest;

  @Before
  public void setup() throws Exception {
    Bucket bucket = mockBucket();
    when(tx.findBucket(repository)).thenReturn(bucket);
    when(tx.browse(any(), any())).thenReturn(componentDocsIterable);
    when(tx.findAssets(any(), any(), any(), any())).thenReturn(assetIterable);
    when(componentDocsIterable.iterator()).thenReturn(componentDocsIterator);
    when(assetIterable.iterator()).thenReturn(assetIterator);
    when(repository.facet(StorageFacet.class)).thenReturn(storageFacet);
    when(storageFacet.txSupplier()).thenReturn(() -> tx);

    uncaught = newArrayList();

    underTest = new PurgeUnusedFacetImpl(componentEntityAdapter);
    underTest.attach(repository);
  }

  @Test
  public void testCancellable() throws Exception {
    // infinite item loops
    when(componentDocsIterator.hasNext()).thenReturn(true);
    when(componentDocsIterator.next()).thenReturn(mock(ODocument.class));
    when(assetIterator.hasNext()).thenReturn(true);
    when(assetIterator.next()).thenReturn(mock(Asset.class));

    List<Runnable> cancelables = Arrays.asList(
        () -> underTest.deleteUnusedComponents(new Date()),
        () -> underTest.deleteUnusedAssets(new Date())
    );

    for (Runnable cancelable : cancelables) {
      AtomicBoolean canceled = new AtomicBoolean(false);
      Thread t = createTaskThread(cancelable, canceled);
      t.start();

      sleep((long) (Math.random() * 1000)); // sleep for up to a second (emulate task running)
      canceled.set(true); // cancel the task
      t.join(5000); // ensure task thread ends

      if (t.isAlive()) {
        fail("Task did not cancel");
      }

      if (uncaught.size() > 0) {
        throw new MultipleFailureException(uncaught);
      }
    }
  }

  private Thread createTaskThread(final Runnable action, final AtomicBoolean cancelFlag) {
    Thread t = new Thread(() -> {
      CancelableHelper.set(cancelFlag);
      UnitOfWork.beginBatch(tx);

      action.run();
    });

    t.setUncaughtExceptionHandler((thread, ex) -> {
      if (ex instanceof TaskInterruptedException) {
        return;
      }
      uncaught.add(ex);
    });

    return t;
  }

  private Bucket mockBucket() {
    EntityAdapter owner = mock(EntityAdapter.class);
    ODocument document = mock(ODocument.class);
    ORID orID = new ORecordId(1, 1);
    when(document.getIdentity()).thenReturn(orID);
    EntityMetadata entityMetadata = new AttachedEntityMetadata(owner, document);

    Bucket bucket = mock(Bucket.class);
    when(bucket.getEntityMetadata()).thenReturn(entityMetadata);
    return bucket;
  }
}
