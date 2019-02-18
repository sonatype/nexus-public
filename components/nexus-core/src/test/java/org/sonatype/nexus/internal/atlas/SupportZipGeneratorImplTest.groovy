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

import java.util.zip.ZipInputStream

import org.sonatype.goodies.common.ByteSize
import org.sonatype.nexus.common.wonderland.DownloadService
import org.sonatype.nexus.supportzip.GeneratedContentSourceSupport
import org.sonatype.nexus.supportzip.SupportBundle
import org.sonatype.nexus.supportzip.SupportBundleCustomizer
import org.sonatype.nexus.supportzip.SupportZipGenerator

import groovy.transform.InheritConstructors
import spock.lang.Specification

import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Priority.OPTIONAL
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type.AUDITLOG
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type.JMX
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type.LOG
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
  def logContentSource = new TestGeneratedContentSourceSupport(LOG, 'log/nexus.log', OPTIONAL)
  def taskLogContentSource = new TestGeneratedContentSourceSupport(TASKLOG, 'log/tasks/task.log', OPTIONAL)
  def auditLogContentSource = new TestGeneratedContentSourceSupport(AUDITLOG, 'log/audit.log', OPTIONAL)
  def jmxContentSource = new TestGeneratedContentSourceSupport(JMX, 'info/jmx.json', OPTIONAL)

  def setup() {
      mockLogCustomizer.customize(_) >> { SupportBundle bundle -> bundle << logContentSource }
      mockTaskLogCustomizer.customize(_) >> { SupportBundle bundle -> bundle << taskLogContentSource }
      mockAuditLogCustomizer.customize(_) >> { SupportBundle bundle -> bundle << auditLogContentSource }
      mockJmxCustomizer.customize(_) >> { SupportBundle bundle -> bundle << jmxContentSource }
  }

  def "Support zip is generated from requested sources"() {
    given:
      def req = new SupportZipGenerator.Request(log: true, taskLog: true, auditLog: true, jmx: false)
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
      def req = new SupportZipGenerator.Request(log: true, taskLog: true, jmx: true, limitFileSizes: true)
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
      def req = new SupportZipGenerator.Request(log: true, taskLog: true, jmx: true, limitZipSize: true)
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

  @InheritConstructors
  static class TestGeneratedContentSourceSupport extends GeneratedContentSourceSupport {
    int contentSize = 0
    @Override
    protected void generate(File file) {
      file.bytes = new byte[contentSize]
    }
  }
}
