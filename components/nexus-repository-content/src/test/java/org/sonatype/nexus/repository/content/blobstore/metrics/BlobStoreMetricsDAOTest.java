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
package org.sonatype.nexus.repository.content.blobstore.metrics;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.metrics.BlobStoreMetricsEntity;
import org.sonatype.nexus.content.testsuite.groups.SQLTestGroup;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.datastore.api.DuplicateKeyException;
import org.sonatype.nexus.testdb.DataSessionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

@Category(SQLTestGroup.class)
public class BlobStoreMetricsDAOTest
    extends TestSupport
{

  @Rule
  public DataSessionRule sessionRule = new DataSessionRule(DEFAULT_DATASTORE_NAME).access(BlobStoreMetricsDAO.class);

  private DataSession<?> dataSession;

  private BlobStoreMetricsDAO dao;

  @Before
  public void setup() {
    dataSession = sessionRule.openSession(DEFAULT_DATASTORE_NAME);
    dataSession.getTransaction().begin();
    dao = dataSession.access(BlobStoreMetricsDAO.class);
  }

  @After
  public void teardown() {
    dataSession.getTransaction().rollback();
    dataSession.close();
  }

  @Test(expected = Test.None.class)
  public void canRecreateSchemaMultipleTimes() {
    dao.createSchema();
    dao.createSchema();
  }

  @Test
  public void initializeMetrics(){
    dao.initializeMetrics("test");
    assertThat(dao.get("test"), is(notNullValue()));
    assertThat(dao.get("test").getBlobStoreName(), is("test"));
  }

  @Test(expected = DuplicateKeyException.class)
  public void initializeMetricsFailsIfAlreadyInitialized(){
    dao.initializeMetrics("test");
    dao.initializeMetrics("test");
  }

  @Test
  public void removalOfMetricsSucceeds(){
    dao.initializeMetrics("test");
    dao.remove("test");

    assertThat(dao.get("test"), is(nullValue()));
  }

  @Test
  public void incrementalUpdateOfMetricsSucceeds(){
    dao.initializeMetrics("test");
    BlobStoreMetricsEntity blobStoreMetricsEntity = new BlobStoreMetricsEntity();

    blobStoreMetricsEntity.setBlobStoreName("test");

    //generic
    blobStoreMetricsEntity.setBlobCount(1);
    blobStoreMetricsEntity.setTotalSize(100);

    //upload
    blobStoreMetricsEntity.setUploadBlobSize(100);
    blobStoreMetricsEntity.setUploadSuccessfulRequests(1);
    blobStoreMetricsEntity.setUploadTimeOnRequests(500);
    blobStoreMetricsEntity.setUploadErrorRequests(2);

    //download
    blobStoreMetricsEntity.setDownloadSuccessfulRequests(2);
    blobStoreMetricsEntity.setDownloadBlobSize(200);
    blobStoreMetricsEntity.setDownloadTimeOnRequests(50);
    blobStoreMetricsEntity.setDownloadErrorRequests(5);

    dao.updateMetrics(blobStoreMetricsEntity);
    BlobStoreMetricsEntity updatedBlobStoreMetricsEntity = dao.get("test");

    //generic
    assertThat(updatedBlobStoreMetricsEntity.getBlobCount(), is(blobStoreMetricsEntity.getBlobCount()));
    assertThat(updatedBlobStoreMetricsEntity.getTotalSize(), is(blobStoreMetricsEntity.getTotalSize()));

    //upload
    assertThat(updatedBlobStoreMetricsEntity.getUploadBlobSize(), is(blobStoreMetricsEntity.getUploadBlobSize()));
    assertThat(updatedBlobStoreMetricsEntity.getUploadSuccessfulRequests(), is(blobStoreMetricsEntity.getUploadSuccessfulRequests()));
    assertThat(updatedBlobStoreMetricsEntity.getUploadTimeOnRequests(), is(blobStoreMetricsEntity.getUploadTimeOnRequests()));
    assertThat(updatedBlobStoreMetricsEntity.getUploadErrorRequests(), is(blobStoreMetricsEntity.getUploadErrorRequests()));

    //download
    assertThat(updatedBlobStoreMetricsEntity.getDownloadBlobSize(), is(blobStoreMetricsEntity.getDownloadBlobSize()));
    assertThat(updatedBlobStoreMetricsEntity.getDownloadSuccessfulRequests(), is(blobStoreMetricsEntity.getDownloadSuccessfulRequests()));
    assertThat(updatedBlobStoreMetricsEntity.getDownloadTimeOnRequests(), is(blobStoreMetricsEntity.getDownloadTimeOnRequests()));
    assertThat(updatedBlobStoreMetricsEntity.getDownloadErrorRequests(), is(blobStoreMetricsEntity.getDownloadErrorRequests()));

    //updated metrics again
    dao.updateMetrics(blobStoreMetricsEntity);
    updatedBlobStoreMetricsEntity = dao.get("test");

    assertThat(updatedBlobStoreMetricsEntity.getBlobCount(), is(blobStoreMetricsEntity.getBlobCount() * 2));
    assertThat(updatedBlobStoreMetricsEntity.getTotalSize(), is(blobStoreMetricsEntity.getTotalSize() * 2));

    //upload
    assertThat(updatedBlobStoreMetricsEntity.getUploadBlobSize(), is(blobStoreMetricsEntity.getUploadBlobSize() * 2));
    assertThat(updatedBlobStoreMetricsEntity.getUploadSuccessfulRequests(), is(blobStoreMetricsEntity.getUploadSuccessfulRequests() * 2));
    assertThat(updatedBlobStoreMetricsEntity.getUploadTimeOnRequests(), is(blobStoreMetricsEntity.getUploadTimeOnRequests() * 2));
    assertThat(updatedBlobStoreMetricsEntity.getUploadErrorRequests(), is(blobStoreMetricsEntity.getUploadErrorRequests() * 2));

    //download
    assertThat(updatedBlobStoreMetricsEntity.getDownloadBlobSize(), is(blobStoreMetricsEntity.getDownloadBlobSize() * 2));
    assertThat(updatedBlobStoreMetricsEntity.getDownloadSuccessfulRequests(), is(blobStoreMetricsEntity.getDownloadSuccessfulRequests() * 2));
    assertThat(updatedBlobStoreMetricsEntity.getDownloadTimeOnRequests(), is(blobStoreMetricsEntity.getDownloadTimeOnRequests() * 2));
    assertThat(updatedBlobStoreMetricsEntity.getDownloadErrorRequests(), is(blobStoreMetricsEntity.getDownloadErrorRequests() * 2));
  }

  @Test
  public void decrementOfMetricsSucceeds(){
    dao.initializeMetrics("test");
    BlobStoreMetricsEntity blobStoreMetricsEntity = new BlobStoreMetricsEntity();

    blobStoreMetricsEntity.setBlobStoreName("test");

    //generic
    blobStoreMetricsEntity.setBlobCount(2);
    blobStoreMetricsEntity.setTotalSize(200);

    dao.updateMetrics(blobStoreMetricsEntity);

    blobStoreMetricsEntity.setBlobCount(-1);
    blobStoreMetricsEntity.setTotalSize(-30);

    //updated metrics again
    dao.updateMetrics(blobStoreMetricsEntity);
    BlobStoreMetricsEntity updatedBlobStoreMetricsEntity = dao.get("test");
    assertThat(updatedBlobStoreMetricsEntity.getBlobCount(), is(1L));
    assertThat(updatedBlobStoreMetricsEntity.getTotalSize(), is(170L));
  }

  @Test
  public void testClearOperationalMetrics() {
    dao.initializeMetrics("test");

    BlobStoreMetricsEntity blobStoreMetricsEntity = new BlobStoreMetricsEntity();
    blobStoreMetricsEntity.setBlobStoreName("test");
    // we want to ensure non-operational metrics are not cleared, so non-zero values
    blobStoreMetricsEntity.setBlobCount(2);
    blobStoreMetricsEntity.setTotalSize(200);
    blobStoreMetricsEntity.setDownloadBlobSize(1);
    blobStoreMetricsEntity.setDownloadErrorRequests(1);
    blobStoreMetricsEntity.setDownloadSuccessfulRequests(1);
    blobStoreMetricsEntity.setDownloadTimeOnRequests(1);
    blobStoreMetricsEntity.setUploadBlobSize(1);
    blobStoreMetricsEntity.setUploadErrorRequests(1);
    blobStoreMetricsEntity.setUploadSuccessfulRequests(1);
    blobStoreMetricsEntity.setUploadTimeOnRequests(1);

    dao.updateMetrics(blobStoreMetricsEntity);
    dao.clearOperationMetrics("test");

    BlobStoreMetricsEntity actual = dao.get("test");
    // Verify persistent metrics are maintained
    assertThat(actual.getBlobStoreName(), is("test"));
    assertThat(actual.getBlobCount(), is(2L));
    assertThat(actual.getTotalSize(), is(200L));

    // Verify Operational Metrics have been reset
    assertThat(actual.getDownloadBlobSize(), is(0L));
    assertThat(actual.getDownloadErrorRequests(), is(0L));
    assertThat(actual.getDownloadSuccessfulRequests(), is(0L));
    assertThat(actual.getDownloadTimeOnRequests(), is(0L));

    assertThat(actual.getUploadBlobSize(), is(0L));
    assertThat(actual.getUploadErrorRequests(), is(0L));
    assertThat(actual.getUploadSuccessfulRequests(), is(0L));
    assertThat(actual.getUploadTimeOnRequests(), is(0L));
  }
}
