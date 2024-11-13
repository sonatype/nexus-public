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
package org.sonatype.nexus.blobstore.file;

import java.time.Duration;

import org.sonatype.nexus.blobstore.file.internal.SoftDeletedBlobsStoreImpl;
import org.sonatype.nexus.blobstore.file.internal.datastore.DatastoreFileBlobDeletionIndex;
import org.sonatype.nexus.blobstore.file.store.SoftDeletedBlobsStore;
import org.sonatype.nexus.blobstore.file.store.internal.SoftDeletedBlobsDAO;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.content.testsuite.groups.SQLTestGroup;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.scheduling.PeriodicJobService;
import org.sonatype.nexus.testdb.DataSessionRule;
import org.sonatype.nexus.transaction.TransactionModule;

import com.google.inject.Guice;
import com.google.inject.Provides;
import org.junit.Before;
import org.junit.Rule;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;

/**
 * {@link FileBlobStore} integration tests.
 */
@Category(SQLTestGroup.class)
public class DatastoreFileBlobStoreIT
    extends FileBlobStoreITSupport
{
  @Rule
  public DataSessionRule sessionRule = new DataSessionRule().access(SoftDeletedBlobsDAO.class);

  @Mock
  private EventManager eventManager;

  @Mock
  private PeriodicJobService periodicJobService;

  private SoftDeletedBlobsStore store;

  @Before
  public void setupPeriodicJobService() {
    // Setup synchronous running of the soft deleted blob index for the tests
    doAnswer(invocation -> {
        invocation.getArgument(0, Runnable.class).run();
        return null;
      })
      .when(periodicJobService)
      .runOnce(any(Runnable.class), anyInt());
  }

  @Override
  protected FileBlobDeletionIndex fileBlobDeletionIndex() {
    if (store == null) {
      store = Guice.createInjector(new TransactionModule()
      {
        @Provides
        DataSessionSupplier getDataSessionSupplier() {
          return sessionRule;
        }

        @Provides
        EventManager getEventManager() {
          return eventManager;
        }
      }).getInstance(SoftDeletedBlobsStoreImpl.class);
    }

    return new DatastoreFileBlobDeletionIndex(store, periodicJobService, Duration.ofSeconds(1), 100);
  }

}
