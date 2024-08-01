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

import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.file.store.SoftDeletedBlobsData;
import org.sonatype.nexus.blobstore.file.store.internal.SoftDeletedBlobsDAO;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.content.testsuite.groups.SQLTestGroup;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.testdb.DataSessionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

@Category(SQLTestGroup.class)
public class SoftDeletedBlobsDAOTest
    extends TestSupport
{
  @Rule
  public DataSessionRule sessionRule = new DataSessionRule().access(SoftDeletedBlobsDAO.class);

  private DataSession<?> session;

  private SoftDeletedBlobsDAO dao;

  private static final String FAKE_BLOB_STORE_NAME = "fakeBlobStore";

  @Before
  public void setup() {
    session = sessionRule.openSession(DEFAULT_DATASTORE_NAME);
    dao = session.access(SoftDeletedBlobsDAO.class);
  }

  @After
  public void cleanup() {
    session.close();
  }

  @Test
  public void testDAOOperations() {
    int limit = 100;
    Continuation<SoftDeletedBlobsData> emptyData = dao.readRecords(null, limit, FAKE_BLOB_STORE_NAME);
    assertThat(emptyData.isEmpty(), is(true));

    dao.createRecord(FAKE_BLOB_STORE_NAME, "blobID");
    Optional<SoftDeletedBlobsData> initialBlobID =
        dao.readRecords(null, limit, FAKE_BLOB_STORE_NAME).stream().findFirst();

    assertThat(initialBlobID.isPresent(), is(true));
    assertThat(initialBlobID.get().getBlobId(), is("blobID"));

    dao.deleteRecord(FAKE_BLOB_STORE_NAME, "blobID");
    Continuation<SoftDeletedBlobsData> newBlobs = dao.readRecords(null, limit, FAKE_BLOB_STORE_NAME);

    assertThat(newBlobs.isEmpty(), is(true));

    dao.createRecord(FAKE_BLOB_STORE_NAME, "blob1");
    dao.createRecord(FAKE_BLOB_STORE_NAME, "blob2");
    dao.createRecord(FAKE_BLOB_STORE_NAME, "blob3");

    assertThat(dao.readRecords(null, limit, FAKE_BLOB_STORE_NAME).size(), is(3));

    dao.deleteAllRecords(FAKE_BLOB_STORE_NAME, "100");

    assertThat(dao.readRecords(null, limit, FAKE_BLOB_STORE_NAME).size(), is(0));
  }
}
