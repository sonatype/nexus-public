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
package org.sonatype.nexus.internal.atlas

import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

import org.sonatype.goodies.common.ByteSize
import org.sonatype.nexus.common.wonderland.DownloadService
import org.sonatype.nexus.supportzip.GeneratedContentSourceSupport
import org.sonatype.nexus.supportzip.SupportBundle
import org.sonatype.nexus.supportzip.SupportBundleCustomizer
import org.sonatype.nexus.common.log.SupportZipGeneratorRequest

import groovy.transform.InheritConstructors
import spock.lang.Specification

import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Priority.OPTIONAL
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type.AUDITLOG
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type.JMX
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type.LOG
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type.SYSINFO
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type.TASKLOG

/**
 * Unit tests for {@link SupportZipGeneratorImpl}
 */
class SupportZipGeneratorImplTest
    extends Specification
{
  def downloadService = Mock(DownloadService)
  def mockLogCustomizer = Mock(SupportBundleCustomizer)
  def mockTaskLogCustomizer = Mock(SupportBundleCustomizer)
  def mockAuditLogCustomizer = Mock(SupportBundleCustomizer)
  def mockJmxCustomizer = Mock(SupportBundleCustomizer)
  def mockSysInfoCustomizer = Mock(SupportBundleCustomizer)
  def throwExceptionCustomizer = Mock(SupportBundleCustomizer)
  def throwExceptionInMiddleCustomizer = Mock(SupportBundleCustomizer)
  def logContentSource = new TestGeneratedContentSourceSupport(LOG, 'log/nexus.log', OPTIONAL)
  def taskLogContentSource = new TestGeneratedContentSourceSupport(TASKLOG, 'log/tasks/task.log', OPTIONAL)
  def auditLogContentSource = new TestGeneratedContentSourceSupport(AUDITLOG, 'log/audit.log', OPTIONAL)
  def jmxContentSource = new TestGeneratedContentSourceSupport(JMX, 'info/jmx.json', OPTIONAL)
  def sysInfoContentSource = new TestGeneratedContentSourceSupport(SYSINFO, 'info/sysinfo.json', OPTIONAL)
  def throwExceptionSource = new GeneratedContentSourceSupport(JMX, 'info/jmx.json', OPTIONAL) {
    @Override
    protected void generate(final File file) throws Exception {
      throw new RuntimeException('I fail to generate')
    }
  }
  def throwExceptionGetContentSource = new TestGeneratedContentSourceSupport(JMX, 'info/jmx.json', OPTIONAL) {
    @Override
    InputStream getContent() throws Exception {
      throw new IOException("Failed to get content.")
    }
  }

  def setup() {
    mockLogCustomizer.customize(_) >> { SupportBundle bundle -> bundle << logContentSource }
    mockTaskLogCustomizer.customize(_) >> { SupportBundle bundle -> bundle << taskLogContentSource }
    mockAuditLogCustomizer.customize(_) >> { SupportBundle bundle -> bundle << auditLogContentSource }
    mockJmxCustomizer.customize(_) >> { SupportBundle bundle -> bundle << jmxContentSource }
    mockSysInfoCustomizer.customize(_) >> { SupportBundle bundle -> bundle << sysInfoContentSource }
    throwExceptionCustomizer.customize(_) >> { SupportBundle bundle -> bundle << throwExceptionSource }
    throwExceptionInMiddleCustomizer.customize(_) >> { SupportBundle bundle ->
      bundle.add(logContentSource)
      bundle.add(throwExceptionGetContentSource)
      bundle.add(taskLogContentSource)

      return bundle
    }
  }

  def "Support zip is generated from requested sources"() {
    given:
      def req = new SupportZipGeneratorRequest(log: true, taskLog: true, auditLog: true, jmx: false)
      def out = new ByteArrayOutputStream()
      def generator = new SupportZipGeneratorImpl(downloadService, [mockLogCustomizer, mockTaskLogCustomizer, mockAuditLogCustomizer, mockJmxCustomizer],
          ByteSize.bytes(0), ByteSize.bytes(0))

    when:
      generator.generate(req, 'prefix', out)
      def zip = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()))
      def entries = []
      for (def entry = zip.getNextEntry(); entry; entry = zip.getNextEntry()) {
        entries << entry
      }

    then:
      entries.find { it.name == 'prefix/log/nexus.log' } != null
      entries.find { it.name == 'prefix/log/tasks/task.log' } != null
      entries.find { it.name == 'prefix/info/jmx.json' } == null
      entries.find { it.name == 'prefix/log/audit.log' } != null
  }

  def "Support zip is truncated if content too large"() {
    given:
      logContentSource.contentSize = 2000
      taskLogContentSource.contentSize = 1000
      jmxContentSource.contentSize = 1000
      def req = new SupportZipGeneratorRequest(log: true, taskLog: true, jmx: true, limitFileSizes: true)
      def out = new ByteArrayOutputStream()
      def generator = new SupportZipGeneratorImpl(downloadService, [mockLogCustomizer, mockTaskLogCustomizer, mockJmxCustomizer],
          ByteSize.bytes(1000), ByteSize.bytes(0))

    when:
      generator.generate(req, 'prefix', out)
      def zip = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()))
      def entries = []
      for (def entry = zip.getNextEntry(); entry; entry = zip.getNextEntry()) {
        entries << entry
      }

    then:
      entries.find { it.name == 'prefix/log/nexus.log' && it.size < 2000 } != null
      entries.find { it.name == 'prefix/log/tasks/task.log' && it.size == 1000 } != null
      entries.find { it.name == 'prefix/info/jmx.json' && it.size == 1000 } != null
      entries.find { it.name == 'prefix/truncated' } != null
  }

  def "Support zip is truncated if zipfile too large"() {
    given:
      logContentSource.contentSize = 1000
      taskLogContentSource.contentSize = 1000
      jmxContentSource.contentSize = 1000
      def req = new SupportZipGeneratorRequest(log: true, taskLog: true, jmx: true, limitZipSize: true)
      def out = new ByteArrayOutputStream()
      def generator = new SupportZipGeneratorImpl(downloadService, [mockLogCustomizer, mockTaskLogCustomizer, mockJmxCustomizer],
          ByteSize.bytes(0), ByteSize.bytes(2500))

    when:
      generator.generate(req, 'prefix', out)
      def zip = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()))
      def entries = []
      for (def entry = zip.getNextEntry(); entry; entry = zip.getNextEntry()) {
        entries << entry
      }

    then:
      entries.find { it.name == 'prefix/truncated' } != null
  }

  def "Source failures will not block generation of support zip"() {
    given:
      def req = new SupportZipGeneratorRequest(log: true, taskLog: true, jmx: true, limitFileSizes: true)
      def out = new ByteArrayOutputStream()
      jmxContentSource.contentSize = 1000
      def generator = new SupportZipGeneratorImpl(downloadService, [mockJmxCustomizer, throwExceptionCustomizer],
          ByteSize.bytes(1000000),
          ByteSize.bytes(1000000))
    when:
      generator.generate(req, 'prefix', out)
      def zip = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()))
      def entries = []
      for (def entry = zip.getNextEntry(); entry; entry = zip.getNextEntry()) {
        entries << entry
      }
    then:
      entries.find { it.name == 'prefix/info/jmx.json' && it.size == 1000 } != null
  }

  def "Source failures will not block other sources from being included"() {
    given:
      def req = new SupportZipGeneratorRequest(log: true, taskLog: true, jmx: true, limitFileSizes: true)
      def out = new ByteArrayOutputStream()
      def generator = new SupportZipGeneratorImpl(downloadService, [throwExceptionInMiddleCustomizer],
          ByteSize.bytes(1000000),
          ByteSize.bytes(1000000))
    when:
      generator.generate(req, 'prefix', out)
      def zip = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()))
      def entries = []
      for (def entry = zip.getNextEntry(); entry; entry = zip.getNextEntry()) {
        entries << entry
      }
    then:
      entries.find { it.name == 'prefix/log/tasks/task.log' } != null
      entries.find { it.name == 'prefix/info/jmx.json' } != null
      entries.find { it.name == 'prefix/log/nexus.log' } != null
  }

  def "Non-log files are not truncated regardless of size"() {
    given:
      def sysInfoContentSource = new TestGeneratedContentSourceSupport(SYSINFO, 'sys/sysinfo.json', OPTIONAL)
      sysInfoContentSource.contentSize = 50000
      def req = new SupportZipGeneratorRequest(systemInformation: true, limitFileSizes: true)
      def out = new ByteArrayOutputStream()
      def generator = new SupportZipGeneratorImpl(downloadService, [mockSysInfoCustomizer],
          ByteSize.bytes(30000), ByteSize.bytes(50000))

    when:
      generator.generate(req, 'prefix', out)
      def zip = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()))
      def entries = []
      for (def entry = zip.getNextEntry(); entry; entry = zip.getNextEntry()) {
        entries << entry
      }

    then:
      entries.find { it.name == 'prefix/info/sysinfo.json' } != null
      entries.find { it.name == 'prefix/truncated' } == null
  }

  def "Log files are truncated if they exceed maximum file size"() {
    given:
      logContentSource.contentSize = 40000
      def req = new SupportZipGeneratorRequest(log: true, limitFileSizes: true)
      def out = new ByteArrayOutputStream()
      def generator = new SupportZipGeneratorImpl(downloadService, [mockLogCustomizer],
          ByteSize.bytes(30000), ByteSize.bytes(50000))  // 30 MB max file size, 50 MB max zip size

    when:
      generator.generate(req, 'prefix', out)
      def zip = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()))
      def entries = []
      for (def entry = zip.getNextEntry(); entry; entry = zip.getNextEntry()) {
        entries << entry
      }

    then:
      entries.find { it.name == 'prefix/log/nexus.log' } != null
      entries.find { it.name == 'prefix/truncated' } != null
  }

  def "Validate log files aren't completely truncated if above file size limit"(){
    given:
      logContentSource.contentSize = 40000 // Should only be truncated up until it hits the file size limit
      taskLogContentSource.contentSize = 40000 // Should only be truncated up until it hits file size limit
      auditLogContentSource.contentSize = 40000 // Should only be truncated up until it hits file size limit
      jmxContentSource.contentSize = 50000  // Expected not to be truncated
      sysInfoContentSource.contentSize = 60000  // Expected not to be truncated

      def req = new SupportZipGeneratorRequest(systemInformation: true, jmx: true, log: true, taskLog: true, auditLog: true, limitFileSizes: true, limitZipSize: true)
      def out = new ByteArrayOutputStream()
      def final TRUNCATED_SIZE = "** TRUNCATED **\n".size()
      def generator = new SupportZipGeneratorImpl(downloadService, [mockLogCustomizer, mockTaskLogCustomizer, mockAuditLogCustomizer, mockJmxCustomizer, mockSysInfoCustomizer],
          ByteSize.bytes(30000), ByteSize.bytes(50000))

    when:
      generator.generate(req, 'prefix', out)
      def zip = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()))
      def entries = []
      ZipEntry entry
      while ((entry = zip.getNextEntry()) != null) {
        entries << entry
      }

      then:
        entries.find { it.name == 'prefix/log/nexus.log' && (it.size > TRUNCATED_SIZE && it.size < 40000) } != null //If the size is <= TRUNCATED_SIZE means we truncated the whole file
        entries.find { it.name == 'prefix/log/tasks/task.log' && (it.size > TRUNCATED_SIZE && it.size < 40000) } != null //If the size is <= TRUNCATED_SIZE means we truncated the whole file
        entries.find { it.name == 'prefix/log/audit.log' && (it.size > TRUNCATED_SIZE && it.size < 40000) } != null //If the size is <= TRUNCATED_SIZE means we truncated the whole file
        entries.find { it.name == 'prefix/info/jmx.json' && it.size == 50000 } != null // Expected to not be truncated
        entries.find { it.name == 'prefix/info/sysinfo.json' && it.size == 60000 } != null // Expected to not be truncated
        entries.find { it.name == 'prefix/truncated' } != null
  }

  def "Validate log truncation and inclusion of other files without truncation"() {
    given:
      logContentSource.contentSize = 40000  // Expected to be truncated
      taskLogContentSource.contentSize = 15000  // Expected not to be truncated
      auditLogContentSource.contentSize = 80000  // Expected to be truncated
      jmxContentSource.contentSize = 50000  // Expected not to be truncated
      sysInfoContentSource.contentSize = 60000  // Expected not to be truncated

      def req = new SupportZipGeneratorRequest(systemInformation: true, jmx: true, log: true, taskLog: true, auditLog: true, limitFileSizes: true, limitZipSize: true)
      def out = new ByteArrayOutputStream()

      def generator = new SupportZipGeneratorImpl(downloadService, [mockLogCustomizer, mockTaskLogCustomizer, mockAuditLogCustomizer, mockJmxCustomizer, mockSysInfoCustomizer],
          ByteSize.bytes(30000), ByteSize.bytes(50000))

    when:
      generator.generate(req, 'prefix', out)
      def zip = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()))
      def entries = []
      ZipEntry entry
      while ((entry = zip.getNextEntry()) != null) {
        entries << entry
      }

    then:
      entries.find { it.name == 'prefix/log/nexus.log' && it.size < 40000 } != null
      entries.find { it.name == 'prefix/log/tasks/task.log' && it.size == 15000 } != null
      entries.find { it.name == 'prefix/log/audit.log' && it.size < 80000 } != null
      entries.find { it.name == 'prefix/info/jmx.json' && it.size == 50000 } != null
      entries.find { it.name == 'prefix/info/sysinfo.json' && it.size == 60000 } != null
      entries.find { it.name == 'prefix/truncated' } != null
  }

  @InheritConstructors
  static class TestGeneratedContentSourceSupport extends GeneratedContentSourceSupport {
    int contentSize = 0
    @Override
    protected void generate(File file) {
      file.bytes = new byte[contentSize]
    }
  }
}
