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
package org.sonatype.nexus.repository.assetdownloadcount;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule;
import org.sonatype.nexus.repository.assetdownloadcount.internal.AssetDownloadCount;
import org.sonatype.nexus.repository.assetdownloadcount.internal.AssetDownloadCountEntityAdapter;
import org.sonatype.nexus.repository.assetdownloadcount.internal.DateUtils;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;

public class AssetDownloadCountEntityAdapterTest
    extends TestSupport
{
  @Rule
  public DatabaseInstanceRule database = DatabaseInstanceRule.inMemory("test");

  @Mock
  private NodeAccess nodeAccess;

  private AssetDownloadCountEntityAdapter underTest;

  @Before
  public void setUp() {
    when(nodeAccess.getId()).thenReturn("nodeid");
    underTest = new AssetDownloadCountEntityAdapter(nodeAccess, 1000);
  }

  @Test
  public void testRegister() {
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      underTest.register(db);
      OSchema schema = db.getMetadata().getSchema();
      assertThat(schema.getClass(underTest.getTypeName()), is(notNullValue()));
    }
  }

  @Test
  public void testIncrementAndGet() {
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      underTest.register(db);
      underTest.incrementCount(db, "myrepo", "myassetname", 10L);
      assertThat(underTest.getCount(db, "myrepo", "myassetname"), is(10L));
      underTest.incrementCount(db, "myrepo", "myassetname", 10L);
      assertThat(underTest.getCount(db, "myrepo", "myassetname"), is(20L));
    }
  }

  @Test
  public void testIncrementAndGetByDate() {
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      underTest.register(db);
      underTest.incrementCount(db, "myrepo", "myassetname", 10L);
      assertThat(underTest.getCount(db, "myrepo", "myassetname", DateType.DAY, new DateTime()), is(10L));
      assertThat(underTest.getCount(db, "myrepo", "myassetname", DateType.MONTH, new DateTime()), is(10L));
      underTest.incrementCount(db, "myrepo", "myassetname", 10L);
      assertThat(underTest.getCount(db, "myrepo", "myassetname", DateType.DAY, new DateTime()), is(20L));
      assertThat(underTest.getCount(db, "myrepo", "myassetname", DateType.MONTH, new DateTime()), is(20L));
    }
  }

  @Test
  public void testIncrementAndGetByDate_differentRepositories() {
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      underTest.register(db);
      underTest.incrementCount(db, "myrepo", "myassetname", 10L);
      assertThat(underTest.getCount(db, "myrepo", "myassetname", DateType.DAY, new DateTime()), is(10L));
      assertThat(underTest.getCount(db, "myrepo", "myassetname", DateType.MONTH, new DateTime()), is(10L));
      underTest.incrementCount(db, "myrepo", "myassetname", 10L);
      assertThat(underTest.getCount(db, "myrepo", "myassetname", DateType.DAY, new DateTime()), is(20L));
      assertThat(underTest.getCount(db, "myrepo", "myassetname", DateType.MONTH, new DateTime()), is(20L));
      underTest.incrementCount(db, "myrepo2", "myassetname", 10L);
      assertThat(underTest.getCount(db, "myrepo", "myassetname", DateType.DAY, new DateTime()), is(20L));
      assertThat(underTest.getCount(db, "myrepo", "myassetname", DateType.MONTH, new DateTime()), is(20L));
      assertThat(underTest.getCount(db, "myrepo2", "myassetname", DateType.DAY, new DateTime()), is(10L));
      assertThat(underTest.getCount(db, "myrepo2", "myassetname", DateType.MONTH, new DateTime()), is(10L));
    }
  }

  @Test
  public void testIncrementAndGetByDate_differentAssets() {
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      underTest.register(db);
      underTest.incrementCount(db, "myrepo", "myassetname", 10L);
      assertThat(underTest.getCount(db, "myrepo", "myassetname", DateType.DAY, new DateTime()), is(10L));
      assertThat(underTest.getCount(db, "myrepo", "myassetname", DateType.MONTH, new DateTime()), is(10L));
      underTest.incrementCount(db, "myrepo", "myassetname", 10L);
      assertThat(underTest.getCount(db, "myrepo", "myassetname", DateType.DAY, new DateTime()), is(20L));
      assertThat(underTest.getCount(db, "myrepo", "myassetname", DateType.MONTH, new DateTime()), is(20L));
      underTest.incrementCount(db, "myrepo", "myassetname2", 10L);
      assertThat(underTest.getCount(db, "myrepo", "myassetname", DateType.DAY, new DateTime()), is(20L));
      assertThat(underTest.getCount(db, "myrepo", "myassetname", DateType.MONTH, new DateTime()), is(20L));
      assertThat(underTest.getCount(db, "myrepo", "myassetname2", DateType.DAY, new DateTime()), is(10L));
      assertThat(underTest.getCount(db, "myrepo", "myassetname2", DateType.MONTH, new DateTime()), is(10L));
    }
  }

  @Test
  public void testIncrementAndGetByDate_differentDates() {
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      underTest.register(db);
      //need to set an older date, so have to manually insert the record
      underTest.addEntity(db,
          new AssetDownloadCount()
              .withRepositoryName("myrepo")
              .withAssetName("myassetname")
              .withNodeId("nodeid")
              .withDateType(DateType.DAY)
              .withDate(DateType.DAY.standardizeDate(new DateTime()).minusDays(10))
              .withCount(100));
      underTest.addEntity(db,
          new AssetDownloadCount()
              .withRepositoryName("myrepo")
              .withAssetName("myassetname")
              .withNodeId("nodeid")
              .withDateType(DateType.MONTH)
              .withDate(DateType.DAY.standardizeDate(new DateTime()).minusDays(40))
              .withCount(200));
      underTest.incrementCount(db, "myrepo", "myassetname", 10L);
      assertThat(underTest.getCount(db, "myrepo", "myassetname", DateType.DAY, new DateTime()), is(10L));
      assertThat(underTest.getCount(db, "myrepo", "myassetname", DateType.MONTH, new DateTime()), is(10L));
    }
  }

  @Test
  public void testIncrementAndGetByDate_differentNodeids() {
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      underTest.register(db);
      //need to set a different nodeid, so have manually insert the record
      underTest.addEntity(db,
          new AssetDownloadCount()
              .withRepositoryName("myrepo")
              .withAssetName("myassetname")
              .withNodeId("nodeid2")
              .withDateType(DateType.DAY)
              .withDate(DateType.DAY.standardizeDate(new DateTime()))
              .withCount(100));
      underTest.addEntity(db,
          new AssetDownloadCount()
              .withRepositoryName("myrepo")
              .withAssetName("myassetname")
              .withNodeId("nodeid2")
              .withDateType(DateType.MONTH)
              .withDate(DateType.MONTH.standardizeDate(new DateTime()))
              .withCount(200));
      underTest.incrementCount(db, "myrepo", "myassetname", 10L);
      assertThat(underTest.getCount(db, "myrepo", "myassetname", DateType.DAY, new DateTime()), is(110L));
      assertThat(underTest.getCount(db, "myrepo", "myassetname", DateType.MONTH, new DateTime()), is(210L));
    }
  }

  @Test
  public void testGetForWholeRepository() {
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      underTest.register(db);
      underTest.addEntity(db,
          new AssetDownloadCount().withRepositoryName("myrepo").withNodeId("nodeid2").withDateType(DateType.MONTH)
              .withDate(DateType.MONTH.standardizeDate(new DateTime())).withCount(200));
      assertThat(underTest.getCount(db, "myrepo", DateType.MONTH, new DateTime()), is(200L));
    }
  }

  @Test
  public void testGetForWholeRepositoryEntireHistory() {
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      underTest.register(db);
      underTest.addEntity(db,
          new AssetDownloadCount().withRepositoryName("myrepo").withNodeId("nodeid1").withDateType(DateType.DAY)
              .withDate(DateType.DAY.standardizeDate(new DateTime().minusDays(10))).withCount(100));
      underTest.addEntity(db,
          new AssetDownloadCount().withRepositoryName("myrepo").withNodeId("nodeid1").withDateType(DateType.MONTH)
              .withDate(DateType.MONTH.standardizeDate(new DateTime().minusMonths(10))).withCount(200));
      underTest.addEntity(db,
          new AssetDownloadCount().withRepositoryName("myrepo").withNodeId("nodeid1").withDateType(DateType.DAY)
              .withDate(DateType.DAY.standardizeDate(new DateTime())).withCount(100));
      underTest.addEntity(db,
          new AssetDownloadCount().withRepositoryName("myrepo").withNodeId("nodeid1").withDateType(DateType.MONTH)
              .withDate(DateType.MONTH.standardizeDate(new DateTime())).withCount(200));
      underTest.addEntity(db,
          new AssetDownloadCount().withRepositoryName("myrepo").withNodeId("nodeid2").withDateType(DateType.DAY)
              .withDate(DateType.DAY.standardizeDate(new DateTime())).withCount(100));
      underTest.addEntity(db,
          new AssetDownloadCount().withRepositoryName("myrepo").withNodeId("nodeid2").withDateType(DateType.MONTH)
              .withDate(DateType.MONTH.standardizeDate(new DateTime())).withCount(200));

      long[] dayCounts = underTest.getCounts(db, "myrepo", DateType.DAY);
      long[] monthCounts = underTest.getCounts(db, "myrepo", DateType.MONTH);

      assertThat(dayCounts.length, is(30));
      assertThat(monthCounts.length, is(14));

      assertThat(dayCounts,
          is(new long[]{200, 0, 0, 0, 0, 0, 0, 0, 0, 0, 100, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}));
      assertThat(monthCounts, is(new long[]{400, 0, 0, 0, 0, 0, 0, 0, 0, 0, 200, 0, 0, 0}));
    }
  }

  @Test
  public void testGetCounts() {
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      underTest.register(db);
      underTest.addEntity(db,
          new AssetDownloadCount()
              .withRepositoryName("myrepo")
              .withAssetName("myassetname")
              .withNodeId("nodeid1")
              .withDateType(DateType.DAY)
              .withDate(DateType.DAY.standardizeDate(new DateTime().minusDays(10)))
              .withCount(100));
      underTest.addEntity(db,
          new AssetDownloadCount()
              .withRepositoryName("myrepo")
              .withAssetName("myassetname")
              .withNodeId("nodeid1")
              .withDateType(DateType.MONTH)
              .withDate(DateType.MONTH.standardizeDate(new DateTime().minusMonths(10)))
              .withCount(200));
      underTest.addEntity(db,
          new AssetDownloadCount()
              .withRepositoryName("myrepo")
              .withAssetName("myassetname")
              .withNodeId("nodeid1")
              .withDateType(DateType.DAY)
              .withDate(DateType.DAY.standardizeDate(new DateTime()))
              .withCount(100));
      underTest.addEntity(db,
          new AssetDownloadCount()
              .withRepositoryName("myrepo")
              .withAssetName("myassetname")
              .withNodeId("nodeid1")
              .withDateType(DateType.MONTH)
              .withDate(DateType.MONTH.standardizeDate(new DateTime()))
              .withCount(200));
      underTest.addEntity(db,
          new AssetDownloadCount()
              .withRepositoryName("myrepo")
              .withAssetName("myassetname")
              .withNodeId("nodeid2")
              .withDateType(DateType.DAY)
              .withDate(DateType.DAY.standardizeDate(new DateTime()))
              .withCount(100));
      underTest.addEntity(db,
          new AssetDownloadCount()
              .withRepositoryName("myrepo")
              .withAssetName("myassetname")
              .withNodeId("nodeid2")
              .withDateType(DateType.MONTH)
              .withDate(DateType.MONTH.standardizeDate(new DateTime()))
              .withCount(200));

      long[] dayCounts = underTest.getCounts(db, "myrepo", "myassetname", DateType.DAY);
      long[] monthCounts = underTest.getCounts(db, "myrepo", "myassetname", DateType.MONTH);

      assertThat(dayCounts.length, is(30));
      assertThat(monthCounts.length, is(14));

      assertThat(dayCounts,
          is(new long[]{200, 0, 0, 0, 0, 0, 0, 0, 0, 0, 100, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}));
      assertThat(monthCounts, is(new long[]{400, 0, 0, 0, 0, 0, 0, 0, 0, 0, 200, 0, 0, 0}));
    }
  }

  @Test
  public void testGetCounts_noData() {
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      underTest.register(db);

      long[] dayCounts = underTest.getCounts(db, "myrepo", "myassetname", DateType.DAY);
      long[] monthCounts = underTest.getCounts(db, "myrepo", "myassetname", DateType.MONTH);

      assertThat(dayCounts.length, is(30));
      assertThat(monthCounts.length, is(14));

      assertThat(dayCounts,
          is(new long[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}));
      assertThat(monthCounts, is(new long[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}));
    }
  }

  @Test
  public void testRemoveOldRecords() {
    DateTime dateTime = new DateTime();

    List<DateTime> dates = new ArrayList<>();
    dates.add(dateTime.minusYears(10));
    dates.add(dateTime.minusYears(2));
    dates.add(dateTime.minusYears(1));
    dates.add(dateTime.minusMonths(6));
    dates.add(dateTime.minusDays(31));
    dates.add(dateTime.minusDays(30));
    dates.add(dateTime.minusDays(10));
    dates.add(dateTime.minusDays(1));
    dates.add(dateTime);

    Set<DateTime> monthsAdded = new HashSet<>();

    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      underTest.register(db);
      for (DateTime date : dates) {
        underTest.addEntity(db,
            new AssetDownloadCount()
                .withRepositoryName("myrepo")
                .withAssetName("myassetname")
                .withNodeId("nodeid")
                .withDateType(DateType.DAY)
                .withDate(DateType.DAY.standardizeDate(date))
                .withCount(100));
        DateTime clearedDate = DateUtils.clearDayAndTime(date);
        if (!monthsAdded.contains(clearedDate)) {
          monthsAdded.add(clearedDate);
          underTest.addEntity(db,
              new AssetDownloadCount()
                  .withRepositoryName("myrepo")
                  .withAssetName("myassetname")
                  .withNodeId("nodeid")
                  .withDateType(DateType.MONTH)
                  .withDate(DateType.MONTH.standardizeDate(date))
                  .withCount(100));
          underTest.addEntity(db,
              new AssetDownloadCount()
                  .withRepositoryName("myrepo")
                  .withNodeId("nodeid")
                  .withDateType(DateType.MONTH_WHOLE_REPO)
                  .withDate(DateType.MONTH_WHOLE_REPO.standardizeDate(date))
                  .withCount(100));
          underTest.addEntity(db,
              new AssetDownloadCount()
                  .withRepositoryName("myrepo")
                  .withNodeId("nodeid")
                  .withDateType(DateType.MONTH_WHOLE_REPO_VULNERABLE)
                  .withDate(DateType.MONTH_WHOLE_REPO_VULNERABLE.standardizeDate(date))
                  .withCount(100));
        }
      }

      assertThat(underTest.getCount(db, "myrepo", "myassetname", DateType.DAY), is(dates.size() * 100L));
      assertThat(underTest.getCount(db, "myrepo", "myassetname", DateType.MONTH), is(monthsAdded.size() * 100L));
      long total = 0;
      for (DateTime monthAdded : monthsAdded) {
        total += underTest.getCount(db, "myrepo", DateType.MONTH_WHOLE_REPO, monthAdded);
      }
      assertThat(total, is(monthsAdded.size() * 100L));
      total = 0;
      for (DateTime monthAdded : monthsAdded) {
        total += underTest.getCount(db, "myrepo", DateType.MONTH_WHOLE_REPO_VULNERABLE, monthAdded);
      }
      assertThat(total, is(monthsAdded.size() * 100L));

      underTest.removeOldRecords(db, DateType.DAY);
      underTest.removeOldRecords(db, DateType.MONTH);
      underTest.removeOldRecords(db, DateType.MONTH_WHOLE_REPO);
      underTest.removeOldRecords(db, DateType.MONTH_WHOLE_REPO_VULNERABLE);

      //5 records are older than 30 days, so will get dumped
      assertThat(underTest.getCount(db, "myrepo", "myassetname", DateType.DAY), is((dates.size() - 5) * 100L));
      //2 records are older than 1 year, so will get dumped
      assertThat(underTest.getCount(db, "myrepo", "myassetname", DateType.MONTH), is((monthsAdded.size() - 2) * 100L));
      total = 0;
      for (DateTime monthAdded : monthsAdded) {
        total += underTest.getCount(db, "myrepo", DateType.MONTH_WHOLE_REPO, monthAdded);
      }
      assertThat(total, is((monthsAdded.size() - 2) * 100L));
      total = 0;
      for (DateTime monthAdded : monthsAdded) {
        total += underTest.getCount(db, "myrepo", DateType.MONTH_WHOLE_REPO_VULNERABLE, monthAdded);
      }
      assertThat(total, is((monthsAdded.size() - 2) * 100L));
    }
  }

  @Test
  public void testSetCount() {
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      underTest.register(db);

      underTest.setCount(db, "myrepo", DateType.MONTH_WHOLE_REPO_VULNERABLE, DateTime.now(), 50L);

      assertThat(underTest.getCount(db, "myrepo", DateType.MONTH_WHOLE_REPO_VULNERABLE, DateTime.now()), is(50L));

      underTest.setCount(db, "myrepo", DateType.MONTH_WHOLE_REPO_VULNERABLE, DateTime.now(), 100L);

      assertThat(underTest.getCount(db, "myrepo", DateType.MONTH_WHOLE_REPO_VULNERABLE, DateTime.now()), is(100L));

      underTest.setCount(db, "myrepo", DateType.MONTH_WHOLE_REPO, DateTime.now(), 50L);

      assertThat(underTest.getCount(db, "myrepo", DateType.MONTH_WHOLE_REPO, DateTime.now()), is(50L));

      underTest.setCount(db, "myrepo", DateType.MONTH_WHOLE_REPO, DateTime.now(), 100L);

      assertThat(underTest.getCount(db, "myrepo", DateType.MONTH_WHOLE_REPO, DateTime.now()), is(100L));
    }
  }
}
