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

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule;
import org.sonatype.nexus.repository.assetdownloadcount.internal.AssetDownloadCountEntityAdapter;
import org.sonatype.nexus.repository.assetdownloadcount.internal.AssetDownloadHistoricDataCleaner;

import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.SubjectRunnable;
import org.apache.shiro.util.ThreadContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AssetDownloadHistoricDataCleanerTest
    extends TestSupport
{
  @Rule
  public DatabaseInstanceRule database = DatabaseInstanceRule.inMemory("test");

  @Mock
  private AssetDownloadCountEntityAdapter assetDownloadCountEntityAdapter;

  @Mock
  private Subject subject;

  private AssetDownloadHistoricDataCleaner underTest;

  @Before
  public void setUp() throws Exception {
    when(subject.getPrincipal()).thenReturn("admin");
    when(subject.associateWith(any(Runnable.class)))
        .thenAnswer(invoc -> new SubjectRunnable(subject, (Runnable) invoc.getArguments()[0]));
    ThreadContext.bind(subject);

    when(assetDownloadCountEntityAdapter.getMaxDeleteSize()).thenReturn(1000);

    underTest = new AssetDownloadHistoricDataCleaner(database.getInstanceProvider(), assetDownloadCountEntityAdapter,
        5000);
  }

  @After
  public void cleanup() {
    ThreadContext.unbindSubject();
    underTest.stop();
  }

  @Test
  public void testStart() throws Exception {
    startAndWait();
    assertThat(underTest.isRunning(), is(true));
    verify(assetDownloadCountEntityAdapter).removeOldRecords(any(), eq(DateType.DAY));
    verify(assetDownloadCountEntityAdapter).removeOldRecords(any(), eq(DateType.MONTH));
    verify(assetDownloadCountEntityAdapter).removeOldRecords(any(), eq(DateType.MONTH_WHOLE_REPO));
    verify(assetDownloadCountEntityAdapter).removeOldRecords(any(), eq(DateType.MONTH_WHOLE_REPO_VULNERABLE));
  }

  @Test
  public void testRestart() throws Exception {
    when(assetDownloadCountEntityAdapter.removeOldRecords(any(), any())).thenThrow(new RuntimeException("doht"));
    startAndWait();
    assertThat(underTest.isRunning(), is(false));
    reset(assetDownloadCountEntityAdapter);
    when(assetDownloadCountEntityAdapter.getMaxDeleteSize()).thenReturn(1000);
    startAndWait();
    assertThat(underTest.isRunning(), is(true));
    verify(assetDownloadCountEntityAdapter).removeOldRecords(any(), eq(DateType.DAY));
  }

  @Test
  public void testMultipleDeleteRequests() throws Exception {
    for (DateType dateType : DateType.values()) {
      when(assetDownloadCountEntityAdapter.removeOldRecords(any(), eq(dateType))).thenReturn(1000).thenReturn(1000)
          .thenReturn(0);
    }

    startAndWait();
    assertThat(underTest.isRunning(), is(true));
    verify(assetDownloadCountEntityAdapter, times(3)).removeOldRecords(any(), eq(DateType.DAY));
    verify(assetDownloadCountEntityAdapter, times(3)).removeOldRecords(any(),
        eq(DateType.MONTH));
    verify(assetDownloadCountEntityAdapter, times(3)).removeOldRecords(any(),
        eq(DateType.MONTH_WHOLE_REPO));
    verify(assetDownloadCountEntityAdapter, times(3)).removeOldRecords(any(),
        eq(DateType.MONTH_WHOLE_REPO_VULNERABLE));
  }

  @Test
  public void testMaxOfZeroDoesntGoInfinite() throws Exception {
    when(assetDownloadCountEntityAdapter.getMaxDeleteSize()).thenReturn(0);
    startAndWait();
    assertThat(underTest.isRunning(), is(true));
    //just make sure each is only called once
    verify(assetDownloadCountEntityAdapter).removeOldRecords(any(), eq(DateType.DAY));
    verify(assetDownloadCountEntityAdapter).removeOldRecords(any(), eq(DateType.MONTH));
    verify(assetDownloadCountEntityAdapter).removeOldRecords(any(), eq(DateType.MONTH_WHOLE_REPO));
    verify(assetDownloadCountEntityAdapter).removeOldRecords(any(), eq(DateType.MONTH_WHOLE_REPO_VULNERABLE));
  }

  private void startAndWait() throws Exception {
    underTest.start();
    Thread.sleep(100);
  }
}
