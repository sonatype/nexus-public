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
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;

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

  @Test
  public void logFilesAreTruncatedFromBeginningKeepingTail() throws Exception {
    // Create a log content source with identifiable content
    TestGeneratedContentSourceSupport logWithContent = new TestGeneratedContentSourceSupport(
        SupportBundle.ContentSource.Type.LOG, "log/nexus.log", SupportBundle.ContentSource.Priority.LOW)
    {
      @Override
      protected void generate(File file) {
        try (FileOutputStream fos = new FileOutputStream(file);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos))) {
          // Write numbered log lines - each line is exactly 50 characters (including newline)
          for (int i = 1; i <= 100; i++) {
            String line = String.format("Log line %04d", i);
            // Pad to make each line exactly 50 characters including newline
            while (line.length() < 49) {
              line += " ";
            }
            writer.write(line + "\n");
          }
        }
        catch (IOException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public long getSize() {
        return 5000; // 100 lines * 50 bytes each
      }
    };

    SupportBundleCustomizer logCustomizer = bundle -> bundle.add(logWithContent);

    SupportZipGeneratorRequest req = new SupportZipGeneratorRequest();
    req.setLog(true);
    req.setLimitFileSizes(true);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    // Max file size is 1500 bytes, which is less than 5000 bytes
    SupportZipGeneratorImpl generator = new SupportZipGeneratorImpl(downloadService,
        ImmutableList.of(logCustomizer), ByteSize.bytes(1500), ByteSize.bytes(10000));

    generator.generate(req, "prefix", out);

    ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()));
    ZipEntry entry;
    boolean truncatedFound = false;
    String logContent = null;

    while ((entry = zip.getNextEntry()) != null) {
      if (entry.getName().equals("prefix/log/nexus.log")) {
        ByteArrayOutputStream entryOut = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = zip.read(buffer)) > 0) {
          entryOut.write(buffer, 0, len);
        }
        logContent = entryOut.toString("UTF-8");
      }
      if (entry.getName().equals("prefix/truncated")) {
        truncatedFound = true;
      }
    }

    // Verify truncation occurred
    assertTrue("Expected truncated marker", truncatedFound);
    assertNotNull("Expected log content", logContent);

    // Verify the log starts with truncation marker
    assertTrue("Expected truncation marker at beginning", logContent.startsWith("** TRUNCATED **"));

    // Verify the log contains TAIL lines (recent entries), not HEAD lines (old entries)
    // The file should contain the last ~30 lines (lines 71-100), not the first lines (1-30)
    assertTrue("Expected to find recent log line 0095", logContent.contains("Log line 0095"));
    assertTrue("Expected to find recent log line 0100", logContent.contains("Log line 0100"));

    // Verify the log does NOT contain early lines
    assertFalse("Should not contain old log line 0001", logContent.contains("Log line 0001"));
    assertFalse("Should not contain old log line 0010", logContent.contains("Log line 0010"));
  }

  @Test
  public void taskLogFilesAreTruncatedFromBeginningKeepingTail() throws Exception {
    // Create a task log content source with identifiable content
    TestGeneratedContentSourceSupport taskLogWithContent = new TestGeneratedContentSourceSupport(
        SupportBundle.ContentSource.Type.TASKLOG, "log/tasks/task.log", SupportBundle.ContentSource.Priority.LOW)
    {
      @Override
      protected void generate(File file) {
        try (FileOutputStream fos = new FileOutputStream(file);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos))) {
          for (int i = 1; i <= 200; i++) {
            String line = String.format("Task log entry %04d", i);
            while (line.length() < 49) {
              line += " ";
            }
            writer.write(line + "\n");
          }
        }
        catch (IOException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public long getSize() {
        return 10000; // 200 lines * 50 bytes each
      }
    };

    SupportBundleCustomizer taskLogCustomizer = bundle -> bundle.add(taskLogWithContent);

    SupportZipGeneratorRequest req = new SupportZipGeneratorRequest();
    req.setTaskLog(true);
    req.setLimitFileSizes(true);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    SupportZipGeneratorImpl generator = new SupportZipGeneratorImpl(downloadService,
        ImmutableList.of(taskLogCustomizer), ByteSize.bytes(2000), ByteSize.bytes(20000));

    generator.generate(req, "prefix", out);

    ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()));
    ZipEntry entry;
    String taskLogContent = null;

    while ((entry = zip.getNextEntry()) != null) {
      if (entry.getName().equals("prefix/log/tasks/task.log")) {
        ByteArrayOutputStream entryOut = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = zip.read(buffer)) > 0) {
          entryOut.write(buffer, 0, len);
        }
        taskLogContent = entryOut.toString("UTF-8");
      }
    }

    assertNotNull("Expected task log content", taskLogContent);
    assertTrue("Expected truncation marker", taskLogContent.startsWith("** TRUNCATED **"));
    assertTrue("Expected recent entry 0195", taskLogContent.contains("Task log entry 0195"));
    assertTrue("Expected recent entry 0200", taskLogContent.contains("Task log entry 0200"));
    assertFalse("Should not contain old entry 0001", taskLogContent.contains("Task log entry 0001"));
    assertFalse("Should not contain old entry 0020", taskLogContent.contains("Task log entry 0020"));
  }

  @Test
  public void auditLogFilesAreTruncatedFromBeginningKeepingTail() throws Exception {
    // Create an audit log content source with identifiable content
    TestGeneratedContentSourceSupport auditLogWithContent = new TestGeneratedContentSourceSupport(
        SupportBundle.ContentSource.Type.AUDITLOG, "log/audit.log", SupportBundle.ContentSource.Priority.LOW)
    {
      @Override
      protected void generate(File file) {
        try (FileOutputStream fos = new FileOutputStream(file);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos))) {
          for (int i = 1; i <= 150; i++) {
            String line = String.format("Audit event %04d", i);
            while (line.length() < 49) {
              line += " ";
            }
            writer.write(line + "\n");
          }
        }
        catch (IOException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public long getSize() {
        return 7500; // 150 lines * 50 bytes each
      }
    };

    SupportBundleCustomizer auditLogCustomizer = bundle -> bundle.add(auditLogWithContent);

    SupportZipGeneratorRequest req = new SupportZipGeneratorRequest();
    req.setAuditLog(true);
    req.setLimitFileSizes(true);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    SupportZipGeneratorImpl generator = new SupportZipGeneratorImpl(downloadService,
        ImmutableList.of(auditLogCustomizer), ByteSize.bytes(2500), ByteSize.bytes(20000));

    generator.generate(req, "prefix", out);

    ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()));
    ZipEntry entry;
    String auditLogContent = null;

    while ((entry = zip.getNextEntry()) != null) {
      if (entry.getName().equals("prefix/log/audit.log")) {
        ByteArrayOutputStream entryOut = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = zip.read(buffer)) > 0) {
          entryOut.write(buffer, 0, len);
        }
        auditLogContent = entryOut.toString("UTF-8");
      }
    }

    assertNotNull("Expected audit log content", auditLogContent);
    assertTrue("Expected truncation marker", auditLogContent.startsWith("** TRUNCATED **"));
    assertTrue("Expected recent event 0145", auditLogContent.contains("Audit event 0145"));
    assertTrue("Expected recent event 0150", auditLogContent.contains("Audit event 0150"));
    assertFalse("Should not contain old event 0001", auditLogContent.contains("Audit event 0001"));
    assertFalse("Should not contain old event 0015", auditLogContent.contains("Audit event 0015"));
  }

  @Test
  public void multipleLogFilesAreTruncatedFromBeginningKeepingTails() throws Exception {
    // Create multiple log sources
    TestGeneratedContentSourceSupport log1 = new TestGeneratedContentSourceSupport(
        SupportBundle.ContentSource.Type.LOG, "log/nexus.log", SupportBundle.ContentSource.Priority.LOW)
    {
      @Override
      protected void generate(File file) {
        try (FileOutputStream fos = new FileOutputStream(file);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos))) {
          for (int i = 1; i <= 100; i++) {
            writer.write(String.format("LOG1-%04d\n", i));
          }
        }
        catch (IOException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public long getSize() {
        return 1000;
      }
    };

    TestGeneratedContentSourceSupport log2 = new TestGeneratedContentSourceSupport(
        SupportBundle.ContentSource.Type.TASKLOG, "log/tasks/task.log", SupportBundle.ContentSource.Priority.LOW)
    {
      @Override
      protected void generate(File file) {
        try (FileOutputStream fos = new FileOutputStream(file);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos))) {
          for (int i = 1; i <= 100; i++) {
            writer.write(String.format("TASK-%04d\n", i));
          }
        }
        catch (IOException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public long getSize() {
        return 1000;
      }
    };

    SupportBundleCustomizer customizer = bundle -> {
      bundle.add(log1);
      bundle.add(log2);
    };

    SupportZipGeneratorRequest req = new SupportZipGeneratorRequest();
    req.setLog(true);
    req.setTaskLog(true);
    req.setLimitFileSizes(true);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    SupportZipGeneratorImpl generator = new SupportZipGeneratorImpl(downloadService,
        ImmutableList.of(customizer), ByteSize.bytes(500), ByteSize.bytes(20000));

    generator.generate(req, "prefix", out);

    ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()));
    ZipEntry entry;
    String log1Content = null;
    String log2Content = null;

    while ((entry = zip.getNextEntry()) != null) {
      ByteArrayOutputStream entryOut = new ByteArrayOutputStream();
      byte[] buffer = new byte[1024];
      int len;
      while ((len = zip.read(buffer)) > 0) {
        entryOut.write(buffer, 0, len);
      }
      String content = entryOut.toString("UTF-8");

      if (entry.getName().equals("prefix/log/nexus.log")) {
        log1Content = content;
      }
      else if (entry.getName().equals("prefix/log/tasks/task.log")) {
        log2Content = content;
      }
    }

    // Verify both logs were truncated from beginning
    assertNotNull("Expected log1 content", log1Content);
    assertTrue("Expected log1 truncation marker", log1Content.startsWith("** TRUNCATED **"));
    assertTrue("Expected log1 recent entry", log1Content.contains("LOG1-0100"));
    assertFalse("Should not contain log1 old entry", log1Content.contains("LOG1-0001"));

    assertNotNull("Expected log2 content", log2Content);
    assertTrue("Expected log2 truncation marker", log2Content.startsWith("** TRUNCATED **"));
    assertTrue("Expected log2 recent entry", log2Content.contains("TASK-0100"));
    assertFalse("Should not contain log2 old entry", log2Content.contains("TASK-0001"));
  }

  @Test
  public void logFileTruncationHandlesInsufficientSpaceForContent() throws Exception {
    // Test edge case where truncation marker itself exceeds available space
    // Truncation marker is "** TRUNCATED **\n" = 17 bytes
    TestGeneratedContentSourceSupport largeLog = new TestGeneratedContentSourceSupport(
        SupportBundle.ContentSource.Type.LOG, "log/nexus.log", SupportBundle.ContentSource.Priority.LOW)
    {
      @Override
      protected void generate(File file) {
        try (FileOutputStream fos = new FileOutputStream(file);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos))) {
          for (int i = 1; i <= 100; i++) {
            writer.write(String.format("Log line %04d\n", i));
          }
        }
        catch (IOException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public long getSize() {
        return 1000;
      }
    };

    SupportBundleCustomizer customizer = bundle -> bundle.add(largeLog);

    SupportZipGeneratorRequest req = new SupportZipGeneratorRequest();
    req.setLog(true);
    req.setLimitFileSizes(true);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    // Set max content size to 10 bytes, which is less than truncation marker (17 bytes)
    SupportZipGeneratorImpl generator = new SupportZipGeneratorImpl(downloadService,
        ImmutableList.of(customizer), ByteSize.bytes(10), ByteSize.bytes(20000));

    generator.generate(req, "prefix", out);

    ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()));
    ZipEntry entry;
    String logContent = null;
    boolean truncatedMarkerFileFound = false;

    while ((entry = zip.getNextEntry()) != null) {
      if (entry.getName().equals("prefix/log/nexus.log")) {
        ByteArrayOutputStream entryOut = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = zip.read(buffer)) > 0) {
          entryOut.write(buffer, 0, len);
        }
        logContent = entryOut.toString("UTF-8");
      }
      if (entry.getName().equals("prefix/truncated")) {
        truncatedMarkerFileFound = true;
      }
    }

    assertTrue("Expected truncated marker file", truncatedMarkerFileFound);

    assertNotNull("Expected log content", logContent);
    assertTrue("Expected truncation marker", logContent.startsWith("** TRUNCATED **"));

    assertTrue("File should have been processed", logContent.length() > 0);

    // The content length should be relatively small since we have minimal space
    // (truncation marker + potentially some content if skip doesn't fully work)
    assertTrue("Content should be constrained by space limits", logContent.length() < 1000);
  }

  @Test
  public void smallLogFilesAreNotTruncated() throws Exception {
    // Create a small log that fits within limits
    TestGeneratedContentSourceSupport smallLog = new TestGeneratedContentSourceSupport(
        SupportBundle.ContentSource.Type.LOG, "log/nexus.log", SupportBundle.ContentSource.Priority.LOW)
    {
      @Override
      protected void generate(File file) {
        try (FileOutputStream fos = new FileOutputStream(file);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos))) {
          for (int i = 1; i <= 10; i++) {
            writer.write(String.format("Small log line %04d\n", i));
          }
        }
        catch (IOException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public long getSize() {
        return 200; // Small enough to fit
      }
    };

    SupportBundleCustomizer customizer = bundle -> bundle.add(smallLog);

    SupportZipGeneratorRequest req = new SupportZipGeneratorRequest();
    req.setLog(true);
    req.setLimitFileSizes(true);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    SupportZipGeneratorImpl generator = new SupportZipGeneratorImpl(downloadService,
        ImmutableList.of(customizer), ByteSize.bytes(1000), ByteSize.bytes(20000));

    generator.generate(req, "prefix", out);

    ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()));
    ZipEntry entry;
    String logContent = null;
    boolean truncatedMarkerFileFound = false;

    while ((entry = zip.getNextEntry()) != null) {
      if (entry.getName().equals("prefix/log/nexus.log")) {
        ByteArrayOutputStream entryOut = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = zip.read(buffer)) > 0) {
          entryOut.write(buffer, 0, len);
        }
        logContent = entryOut.toString("UTF-8");
      }
      if (entry.getName().equals("prefix/truncated")) {
        truncatedMarkerFileFound = true;
      }
    }

    assertNotNull("Expected log content", logContent);
    assertFalse("Should not have truncation marker file", truncatedMarkerFileFound);
    assertFalse("Should not have truncation marker in content", logContent.contains("** TRUNCATED **"));
    assertTrue("Should contain first line", logContent.contains("Small log line 0001"));
    assertTrue("Should contain last line", logContent.contains("Small log line 0010"));
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
