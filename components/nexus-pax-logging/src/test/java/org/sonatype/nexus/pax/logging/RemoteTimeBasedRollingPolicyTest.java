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
package org.sonatype.nexus.pax.logging;

import java.util.Collections;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RemoteTimeBasedRollingPolicyTest
    extends TestSupport
{
  @Mock
  private NexusLogActivator mockNexusLogActivator;

  @Mock
  private BundleContext bundleContext;

  private RemoteTimeBasedRollingPolicy<Object> underTest;

  @Before
  public void setUp() {
    NexusLogActivator.INSTANCE = mockNexusLogActivator;
    when(NexusLogActivator.INSTANCE.getContext()).thenReturn(bundleContext);

    underTest = new RemoteTimeBasedRollingPolicy<>();
    underTest.setMaxHistory(5);
  }

  @Test
  public void startSetupPolicyCorrectly() {
    startPolicy("/my-test/path", "/my-test/path/log/test/test-%d{yyyy-MM-dd_HH-mm}.log.gz");

    assertThat(underTest.getContextPrefix(), equalTo("log/test/"));
    assertThat(underTest.getFilenameDateFormat().toPattern(), equalTo("yyyy-MM-dd_HH-mm"));
    assertNotNull(underTest.getExecutor());
    assertNotNull(underTest.getNonUploadedFiles());
  }

  @Test
  public void testUploadWorksAsExpected() throws Exception {
    startPolicy("/example-test/initial-data", "/example-test/initial-data/log-test/audit/audit-%d{yyyy-MM-dd}.log.gz");

    RollingPolicyUploader mockUploader = mock(RollingPolicyUploader.class);
    ServiceReference<RollingPolicyUploader> mockServiceReference = mock(ServiceReference.class);

    when(bundleContext.getServiceReferences(eq(RollingPolicyUploader.class), anyString())).thenReturn(
        Collections.singletonList(mockServiceReference));
    when(bundleContext.getService(eq(mockServiceReference))).thenReturn(mockUploader);

    underTest.doUpload("/example-test/initial-data/log-test/audit/audit-2020-01-01.log.gz");

    await().atMost(10, TimeUnit.SECONDS).until(this::IdleExecutor);

    verify(mockUploader).rollover("log-test/audit/", "2020/1/1/",
        "/example-test/initial-data/log-test/audit/audit-2020-01-01.log.gz");
    verify(bundleContext).ungetService(mockServiceReference);

    assertThat(underTest.getNonUploadedFiles(), empty());
  }

  @Test
  public void testUploadWaitSinceNoServiceReference() throws Exception {
    startPolicy("/example-test/initial-data", "/example-test/initial-data/log-test/audit/audit-%d{yyyy-MM-dd}.log.gz");

    when(bundleContext.getServiceReferences(eq(RollingPolicyUploader.class), anyString())).thenReturn(
        Collections.emptyList());

    underTest.doUpload("/example-test/initial-data/log-test/audit/audit-2010-11-26.log.gz");

    await().atMost(10, TimeUnit.SECONDS).until(this::IdleExecutor);

    verify(bundleContext, never()).getService(any(ServiceReference.class));
    verify(bundleContext, never()).ungetService(any(ServiceReference.class));
    assertThat(underTest.getNonUploadedFiles().size(), equalTo(1));
  }

  @Test
  public void testUploadSendMultipleNonUploadedFiles() throws Exception {
    startPolicy("/example-test-4/initial-data-4",
        "/example-test-4/initial-data-4/log/nexus/nexus-%d{yyyy-MM-dd}.log.gz");

    RollingPolicyUploader mockUploader = mock(RollingPolicyUploader.class);
    ServiceReference<RollingPolicyUploader> mockServiceReference = mock(ServiceReference.class);

    when(bundleContext.getServiceReferences(eq(RollingPolicyUploader.class), anyString())).thenReturn(
        Collections.emptyList());

    underTest.doUpload("/example-test-4/initial-data-4/log/nexus/nexus-2200-12-24.log.gz");
    await().atMost(10, TimeUnit.SECONDS).until(this::IdleExecutor);

    underTest.doUpload("/example-test-4/initial-data-4/log/nexus/nexus-2200-12-25.log.gz");
    await().atMost(10, TimeUnit.SECONDS).until(this::IdleExecutor);

    underTest.doUpload("/example-test-4/initial-data-4/log/nexus/nexus-2200-12-26.log.gz");
    await().atMost(10, TimeUnit.SECONDS).until(this::IdleExecutor);

    verify(bundleContext, never()).getService(any(ServiceReference.class));
    verify(bundleContext, never()).ungetService(any(ServiceReference.class));
    // 3 files are not uploaded
    assertThat(underTest.getNonUploadedFiles().size(), equalTo(3));

    // mock to simulate the service reference is available
    when(bundleContext.getServiceReferences(eq(RollingPolicyUploader.class), anyString())).thenReturn(
        Collections.singletonList(mockServiceReference));
    when(bundleContext.getService(eq(mockServiceReference))).thenReturn(mockUploader);

    underTest.doUpload("/example-test-4/initial-data-4/log/nexus/nexus-2200-12-27.log.gz");

    await().atMost(10, TimeUnit.SECONDS).until(this::IdleExecutor);

    verify(mockUploader, times(4)).rollover(eq("log/nexus/"), anyString(), anyString());
    verify(bundleContext).ungetService(mockServiceReference);

    assertThat(underTest.getNonUploadedFiles(), empty());
  }

  @After
  public void tearDown() {
    System.getProperties().clear();
  }

  private Boolean IdleExecutor() {
    ThreadPoolExecutor threadPool = (ThreadPoolExecutor) underTest.getExecutor();
    return threadPool.getQueue().isEmpty() && threadPool.getActiveCount() == 0;
  }

  private void startPolicy(final String karafData, final String fileNamePattern) {
    //set to test the correct setup of the policy
    System.setProperty("karaf.data", karafData);
    underTest.setFileNamePattern(fileNamePattern);
    underTest.doStart();
  }
}
