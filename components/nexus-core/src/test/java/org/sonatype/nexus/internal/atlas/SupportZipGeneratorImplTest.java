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
package org.sonatype.nexus.internal.atlas;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

import org.sonatype.goodies.common.ByteSize;
import org.sonatype.goodies.common.Loggers;
import org.sonatype.nexus.common.wonderland.DownloadService;
import org.sonatype.nexus.supportzip.GeneratedContentSourceSupport;
import org.sonatype.nexus.supportzip.SupportBundle;
import org.sonatype.nexus.supportzip.SupportBundleCustomizer;
import org.sonatype.nexus.common.log.SupportZipGeneratorRequest;

import java.io.*;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SupportZipGeneratorImpl}
 */
@RunWith(MockitoJUnitRunner.class)
public class SupportZipGeneratorImplTest
{
  private static final Logger log = Loggers.getLogger(SupportZipGeneratorImplTest.class);

  @Mock
  private DownloadService downloadService;

  @Mock
  private SupportBundleCustomizer mockLogCustomizer;

  @Mock
  private SupportBundleCustomizer mockTaskLogCustomizer;

  @Mock
  private SupportBundleCustomizer mockAuditLogCustomizer;

  @Mock
  private SupportBundleCustomizer mockJmxCustomizer;

  @Mock
  private SupportBundleCustomizer mockSysInfoCustomizer;

  @Mock
  private SupportBundleCustomizer mockDbInfoCustomizer;

  @Mock
  private SupportBundleCustomizer mockArchivedLogCustomizer;

  @Mock
  private SupportBundleCustomizer throwExceptionCustomizer;

  @Mock
  private SupportBundleCustomizer throwExceptionInMiddleCustomizer;

  private TestGeneratedContentSourceSupport archivedLogContentSource;

  private TestGeneratedContentSourceSupport logContentSource;

  private TestGeneratedContentSourceSupport taskLogContentSource;

  private TestGeneratedContentSourceSupport auditLogContentSource;

  private TestGeneratedContentSourceSupport jmxContentSource;

  private TestGeneratedContentSourceSupport dbInfoContentSource;

  private TestGeneratedContentSourceSupport sysInfoContentSource;

  private GeneratedContentSourceSupport throwExceptionSource;

  private TestGeneratedContentSourceSupport throwExceptionGetContentSource;

  @Before
  public void setUp() {
    archivedLogContentSource = new TestGeneratedContentSourceSupport(SupportBundle.ContentSource.Type.ARCHIVEDLOG,
        "log/archived-logs/archive-log.log", SupportBundle.ContentSource.Priority.OPTIONAL);
    logContentSource = new TestGeneratedContentSourceSupport(SupportBundle.ContentSource.Type.LOG, "log/nexus.log",
        SupportBundle.ContentSource.Priority.OPTIONAL);
    taskLogContentSource = new TestGeneratedContentSourceSupport(SupportBundle.ContentSource.Type.TASKLOG,
        "log/tasks/task.log", SupportBundle.ContentSource.Priority.OPTIONAL);
    auditLogContentSource = new TestGeneratedContentSourceSupport(SupportBundle.ContentSource.Type.AUDITLOG,
        "log/audit.log", SupportBundle.ContentSource.Priority.OPTIONAL);
    jmxContentSource = new TestGeneratedContentSourceSupport(SupportBundle.ContentSource.Type.JMX, "info/jmx.json",
        SupportBundle.ContentSource.Priority.OPTIONAL);
    dbInfoContentSource = new TestGeneratedContentSourceSupport(SupportBundle.ContentSource.Type.DBINFO,
        "info/dbFileInfo.txt", SupportBundle.ContentSource.Priority.OPTIONAL);
    sysInfoContentSource = new TestGeneratedContentSourceSupport(SupportBundle.ContentSource.Type.SYSINFO,
        "info/sysinfo.json", SupportBundle.ContentSource.Priority.OPTIONAL);

    throwExceptionSource = new GeneratedContentSourceSupport(SupportBundle.ContentSource.Type.JMX, "info/jmx.json",
        SupportBundle.ContentSource.Priority.OPTIONAL)
    {
      @Override
      protected void generate(File file) throws Exception {
        throw new RuntimeException("I fail to generate");
      }
    };

    throwExceptionGetContentSource = new TestGeneratedContentSourceSupport(SupportBundle.ContentSource.Type.JMX,
        "info/jmx.json", SupportBundle.ContentSource.Priority.OPTIONAL)
    {
      @Override
      public InputStream getContent() throws Exception {
        throw new IOException("Failed to get content.");
      }
    };

    doAnswer(invocation -> {
      SupportBundle bundle = invocation.getArgument(0);
      bundle.add(logContentSource);
      return bundle;
    }).when(mockLogCustomizer).customize(any());

    doAnswer(invocation -> {
      SupportBundle bundle = invocation.getArgument(0);
      bundle.add(taskLogContentSource);
      return bundle;
    }).when(mockTaskLogCustomizer).customize(any());

    doAnswer(invocation -> {
      SupportBundle bundle = invocation.getArgument(0);
      bundle.add(auditLogContentSource);
      return bundle;
    }).when(mockAuditLogCustomizer).customize(any());

    doAnswer(invocation -> {
      SupportBundle bundle = invocation.getArgument(0);
      bundle.add(jmxContentSource);
      return bundle;
    }).when(mockJmxCustomizer).customize(any());

    doAnswer(invocation -> {
      SupportBundle bundle = invocation.getArgument(0);
      bundle.add(sysInfoContentSource);
      return bundle;
    }).when(mockSysInfoCustomizer).customize(any());

    doAnswer(invocation -> {
      SupportBundle bundle = invocation.getArgument(0);
      bundle.add(dbInfoContentSource);
      return bundle;
    }).when(mockDbInfoCustomizer).customize(any());

    doAnswer(invocation -> {
      SupportBundle bundle = invocation.getArgument(0);
      bundle.add(archivedLogContentSource);
      return bundle;
    }).when(mockArchivedLogCustomizer).customize(any());

    doAnswer(invocation -> {
      SupportBundle bundle = invocation.getArgument(0);
      bundle.add(throwExceptionSource);
      return bundle;
    }).when(throwExceptionCustomizer).customize(any());

    doAnswer(invocation -> {
      SupportBundle bundle = invocation.getArgument(0);
      bundle.add(logContentSource);
      bundle.add(throwExceptionGetContentSource);
      bundle.add(taskLogContentSource);
      return bundle;
    }).when(throwExceptionInMiddleCustomizer).customize(any());
  }

  @Test
  public void supportZipIsGeneratedFromRequestedSources() throws Exception {
    SupportZipGeneratorRequest req = new SupportZipGeneratorRequest();
    req.setLog(true);
    req.setTaskLog(true);
    req.setAuditLog(true);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    SupportZipGeneratorImpl generator = new SupportZipGeneratorImpl(downloadService,
        ImmutableList.of(mockLogCustomizer, mockTaskLogCustomizer, mockAuditLogCustomizer, mockJmxCustomizer),
        ByteSize.bytes(0), ByteSize.bytes(0));

    generator.generate(req, "prefix", out);
    ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()));
    ZipEntry entry;
    boolean logFound = false, taskLogFound = false, jmxFound = false, auditLogFound = false;

    while ((entry = zip.getNextEntry()) != null) {
      if (entry.getName().equals("prefix/log/nexus.log"))
        logFound = true;
      if (entry.getName().equals("prefix/log/tasks/task.log"))
        taskLogFound = true;
      if (entry.getName().equals("prefix/info/jmx.json"))
        jmxFound = true;
      if (entry.getName().equals("prefix/log/audit.log"))
        auditLogFound = true;
    }

    assertTrue(logFound);
    assertTrue(taskLogFound);
    assertFalse(jmxFound);
    assertTrue(auditLogFound);
  }

  @Test
  public void supportZipIsTruncatedIfContentTooLarge() throws Exception {
    logContentSource.setContentSize(2000);
    taskLogContentSource.setContentSize(1000);
    jmxContentSource.setContentSize(1000);

    SupportZipGeneratorRequest req = new SupportZipGeneratorRequest();
    req.setLog(true);
    req.setTaskLog(true);
    req.setJmx(true);
    req.setLimitFileSizes(true);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    SupportZipGeneratorImpl generator = new SupportZipGeneratorImpl(downloadService,
        ImmutableList.of(mockLogCustomizer, mockTaskLogCustomizer, mockJmxCustomizer),
        ByteSize.bytes(1000), ByteSize.bytes(0));

    generator.generate(req, "prefix", out);

    ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()));
    ZipEntry entry;
    boolean truncatedFound = false;

    while ((entry = zip.getNextEntry()) != null) {
      if (entry.getName().equals("prefix/log/nexus.log") && entry.getSize() < 2000) {
        assertNotNull(entry);
      }
      if (entry.getName().equals("prefix/log/tasks/task.log") && entry.getSize() == 1000) {
        assertNotNull(entry);
      }
      if (entry.getName().equals("prefix/info/jmx.json") && entry.getSize() == 1000) {
        assertNotNull(entry);
      }
      if (entry.getName().equals("prefix/truncated")) {
        truncatedFound = true;
      }
    }

    assertTrue(truncatedFound);
  }

  @Test
  public void supportZipIsTruncatedIfZipfileTooLarge() throws Exception {
    logContentSource.setContentSize(1000);
    taskLogContentSource.setContentSize(1000);
    jmxContentSource.setContentSize(1000);
    archivedLogContentSource.setContentSize(1000);
    SupportZipGeneratorRequest req = new SupportZipGeneratorRequest();
    req.setLog(true);
    req.setTaskLog(true);
    req.setJmx(true);
    req.setLimitFileSizes(true);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    SupportZipGeneratorImpl generator = new SupportZipGeneratorImpl(downloadService,
        ImmutableList.of(mockArchivedLogCustomizer, mockLogCustomizer, mockTaskLogCustomizer, mockJmxCustomizer),
        ByteSize.bytes(0), ByteSize.bytes(2500));

    generator.generate(req, "prefix", out);
    ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()));
    ZipEntry entry;
    boolean truncatedFound = false;

    while ((entry = zip.getNextEntry()) != null) {
      if (entry.getName().equals("prefix/truncated"))
        truncatedFound = true;
    }

    assertTrue(truncatedFound);
  }

  @Test
  public void sourceFailuresWillNotBlockGenerationOfSupportZip() throws Exception {
    SupportZipGeneratorRequest req = new SupportZipGeneratorRequest();
    req.setLog(true);
    req.setTaskLog(true);
    req.setJmx(true);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    SupportZipGeneratorImpl generator = new SupportZipGeneratorImpl(downloadService,
        ImmutableList.of(mockLogCustomizer, mockTaskLogCustomizer, throwExceptionCustomizer),
        ByteSize.bytes(0), ByteSize.bytes(0));

    generator.generate(req, "prefix", out);
    ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()));
    ZipEntry entry;
    boolean logFound = false, taskLogFound = false, jmxFound = false;

    while ((entry = zip.getNextEntry()) != null) {
      if (entry.getName().equals("prefix/log/nexus.log"))
        logFound = true;
      if (entry.getName().equals("prefix/log/tasks/task.log"))
        taskLogFound = true;
      if (entry.getName().equals("prefix/info/jmx.json"))
        jmxFound = true;
    }

    assertTrue(logFound);
    assertTrue(taskLogFound);
    assertFalse(jmxFound);
  }

  @Test
  public void sourceFailuresWillNotBlockOtherSourcesFromBeingIncluded() throws Exception {
    SupportZipGeneratorRequest req = new SupportZipGeneratorRequest();
    req.setLog(true);
    req.setTaskLog(true);
    req.setJmx(true);
    req.setLimitFileSizes(true);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    SupportZipGeneratorImpl generator = new SupportZipGeneratorImpl(downloadService,
        ImmutableList.of(mockArchivedLogCustomizer, throwExceptionInMiddleCustomizer), ByteSize.bytes(1000000),
        ByteSize.bytes(1000000));

    generator.generate(req, "prefix", out);
    ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()));
    ZipEntry entry;
    boolean logFound = false, taskLogFound = false, jmxFound = false;

    while ((entry = zip.getNextEntry()) != null) {
      if (entry.getName().equals("prefix/log/nexus.log"))
        logFound = true;
      if (entry.getName().equals("prefix/log/tasks/task.log"))
        taskLogFound = true;
      if (entry.getName().equals("prefix/info/jmx.json"))
        jmxFound = true;
    }

    assertTrue(logFound);
    assertTrue(taskLogFound);
    assertTrue(jmxFound);
  }

  @Test
  public void nonLogFilesAreNotTruncatedRegardlessOfSize() throws Exception {
    sysInfoContentSource.setContentSize(50000);
    SupportZipGeneratorRequest req = new SupportZipGeneratorRequest();
    req.setSystemInformation(true);
    req.setLimitFileSizes(true);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    SupportZipGeneratorImpl generator = new SupportZipGeneratorImpl(downloadService,
        ImmutableList.of(mockSysInfoCustomizer, mockDbInfoCustomizer), ByteSize.bytes(30000), ByteSize.bytes(50000));

    generator.generate(req, "prefix", out);
    ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()));
    ZipEntry entry;
    boolean sysInfoFound = false, truncatedFound = false;

    while ((entry = zip.getNextEntry()) != null) {
      if (entry.getName().equals("prefix/info/sysinfo.json"))
        sysInfoFound = true;
      if (entry.getName().equals("prefix/truncated"))
        truncatedFound = true;
    }

    assertTrue(sysInfoFound);
    assertFalse(truncatedFound);
  }

  @Test
  public void logFilesAreTruncatedIfTheyExceedMaximumFileSize() throws Exception {
    logContentSource.setContentSize(40000);
    SupportZipGeneratorRequest req = new SupportZipGeneratorRequest();
    req.setLog(true);
    req.setLimitFileSizes(true);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    SupportZipGeneratorImpl generator = new SupportZipGeneratorImpl(downloadService,
        ImmutableList.of(mockLogCustomizer), ByteSize.bytes(30000), ByteSize.bytes(50000));

    generator.generate(req, "prefix", out);
    ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()));
    ZipEntry entry;
    boolean logFound = false, truncatedFound = false;

    while ((entry = zip.getNextEntry()) != null) {
      if (entry.getName().equals("prefix/log/nexus.log"))
        logFound = true;
      if (entry.getName().equals("prefix/truncated"))
        truncatedFound = true;
    }

    assertTrue(logFound);
    assertTrue(truncatedFound);
  }

  @Test
  public void validateLogFilesArentCompletelyTruncatedIfAboveFileSizeLimit() throws Exception {
    logContentSource.setContentSize(40000); // Expected to be truncated
    taskLogContentSource.setContentSize(15000); // Expected not to be truncated
    auditLogContentSource.setContentSize(80000); // Expected to be truncated
    jmxContentSource.setContentSize(50000); // Expected not to be truncated
    sysInfoContentSource.setContentSize(60000); // Expected not to be truncated

    SupportZipGeneratorRequest req = new SupportZipGeneratorRequest();
    req.setSystemInformation(true);
    req.setJmx(true);
    req.setLog(true);
    req.setTaskLog(true);
    req.setAuditLog(true);
    req.setLimitFileSizes(true);
    req.setLimitZipSize(true);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    SupportZipGeneratorImpl generator = new SupportZipGeneratorImpl(downloadService,
        ImmutableList.of(mockLogCustomizer, mockTaskLogCustomizer, mockAuditLogCustomizer, mockJmxCustomizer,
            mockSysInfoCustomizer),
        ByteSize.bytes(30000), ByteSize.bytes(50000));

    generator.generate(req, "prefix", out);

    ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()));
    ZipEntry entry;
    boolean truncatedFound = false;

    while ((entry = zip.getNextEntry()) != null) {
      if (entry.getName().equals("prefix/log/nexus.log") && entry.getSize() < 40000) {
        assertNotNull(entry);
      }
      if (entry.getName().equals("prefix/log/tasks/task.log") && entry.getSize() == 15000) {
        assertNotNull(entry);
      }
      if (entry.getName().equals("prefix/log/audit.log") && entry.getSize() < 80000) {
        assertNotNull(entry);
      }
      if (entry.getName().equals("prefix/info/jmx.json") && entry.getSize() == 50000) {
        assertNotNull(entry);
      }
      if (entry.getName().equals("prefix/info/sysinfo.json") && entry.getSize() == 60000) {
        assertNotNull(entry);
      }
      if (entry.getName().equals("prefix/truncated")) {
        truncatedFound = true;
      }
    }

    assertTrue(truncatedFound);
  }

  @Test
  public void validateLogTruncationAndInclusionOfOtherFilesWithoutTruncation() throws Exception {
    logContentSource.setContentSize(40000); // Expected to be truncated
    taskLogContentSource.setContentSize(15000); // Expected not to be truncated
    auditLogContentSource.setContentSize(80000); // Expected to be truncated
    jmxContentSource.setContentSize(50000); // Expected not to be truncated
    sysInfoContentSource.setContentSize(60000); // Expected not to be truncated

    SupportZipGeneratorRequest req = new SupportZipGeneratorRequest();
    req.setSystemInformation(true);
    req.setJmx(true);
    req.setLog(true);
    req.setTaskLog(true);
    req.setAuditLog(true);
    req.setLimitFileSizes(true);
    req.setLimitZipSize(true);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    SupportZipGeneratorImpl generator = new SupportZipGeneratorImpl(downloadService,
        ImmutableList.of(mockLogCustomizer, mockTaskLogCustomizer, mockAuditLogCustomizer, mockJmxCustomizer,
            mockSysInfoCustomizer),
        ByteSize.bytes(30000), ByteSize.bytes(50000));

    generator.generate(req, "prefix", out);

    ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()));
    ZipEntry entry;
    boolean truncatedFound = false;

    while ((entry = zip.getNextEntry()) != null) {
      if (entry.getName().equals("prefix/log/nexus.log") && entry.getSize() < 40000) {
        assertNotNull(entry);
      }
      if (entry.getName().equals("prefix/log/tasks/task.log") && entry.getSize() == 15000) {
        assertNotNull(entry);
      }
      if (entry.getName().equals("prefix/log/audit.log") && entry.getSize() < 80000) {
        assertNotNull(entry);
      }
      if (entry.getName().equals("prefix/info/jmx.json") && entry.getSize() == 50000) {
        assertNotNull(entry);
      }
      if (entry.getName().equals("prefix/info/sysinfo.json") && entry.getSize() == 60000) {
        assertNotNull(entry);
      }
      if (entry.getName().equals("prefix/truncated")) {
        truncatedFound = true;
      }
    }

    assertTrue(truncatedFound);
  }

  static class TestGeneratedContentSourceSupport
      extends GeneratedContentSourceSupport
  {
    int contentSize = 0;

    TestGeneratedContentSourceSupport(
        final Type type,
        final String path,
        final Priority priority)
    {
      super(type, path, priority);
    }

    @Override
    protected void generate(File file) {
      try {
        Files.write(file.toPath(), new byte[contentSize]);
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    public void setContentSize(final int contentSize) {
      this.contentSize = contentSize;
    }

    @Override
    public long getSize() {
      return contentSize;
    }
  }
}
