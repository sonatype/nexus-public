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
package org.sonatype.nexus.repository.mime

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.common.io.InputStreamSupplier
import org.sonatype.nexus.mime.MimeRulesSource
import org.sonatype.nexus.mime.internal.DefaultMimeSupport
import org.sonatype.nexus.repository.InvalidContentException
import org.sonatype.nexus.repository.view.ContentTypes

import org.junit.Test

import static org.hamcrest.CoreMatchers.equalTo
import static org.hamcrest.MatcherAssert.assertThat

/**
 * Tests for {@link DefaultContentValidator}.
 */
class DefaultContentValidatorTest
    extends TestSupport
{
  private DefaultContentValidator testSubject = new DefaultContentValidator(new DefaultMimeSupport())

  private byte[] emptyZip = [80, 75, 05, 06, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00]

  InputStreamSupplier supplier(byte[] bytes) {
    ByteArrayInputStream bis = new ByteArrayInputStream(bytes)
    return new InputStreamSupplier() {
      @Override
      InputStream get() {
        return bis
      }
    }
  }

  @Test
  void 'simple text non-strict with declared'() {
    def type = testSubject.determineContentType(
        false,
        supplier('simple text'.bytes),
        MimeRulesSource.NOOP,
        'test.txt',
        ContentTypes.TEXT_PLAIN
    )
    assertThat(type, equalTo(ContentTypes.TEXT_PLAIN))
  }

  @Test
  void 'simple text non-strict with undeclared'() {
    def type = testSubject.determineContentType(
        false,
        supplier('simple text'.bytes),
        MimeRulesSource.NOOP,
        'test.txt',
        null
    )
    assertThat(type, equalTo(ContentTypes.TEXT_PLAIN))
  }

  @Test
  void 'simple text non-strict with wrong declared'() {
    def type = testSubject.determineContentType(
        false,
        supplier('simple text'.bytes),
        MimeRulesSource.NOOP,
        'test.txt',
        'application/zip'
    )
    assertThat(type, equalTo(ContentTypes.TEXT_PLAIN))
  }

  @Test
  void 'simple text strict with wrong declared'() {
    def type = testSubject.determineContentType(
        true,
        supplier('simple text'.bytes),
        MimeRulesSource.NOOP,
        'test.txt',
        'application/zip'
    )
    assertThat(type, equalTo(ContentTypes.TEXT_PLAIN))
  }

  @Test
  void 'simple zip non-strict with undeclared'() {
    def type = testSubject.determineContentType(
        false,
        supplier(emptyZip),
        MimeRulesSource.NOOP,
        'test.zip',
        null
    )
    assertThat(type, equalTo('application/zip'))
  }

  @Test
  void 'simple zip non-strict with declared'() {
    def type = testSubject.determineContentType(
        false,
        supplier(emptyZip),
        MimeRulesSource.NOOP,
        'test.zip',
        'application/zip'
    )
    assertThat(type, equalTo('application/zip'))
  }

  @Test
  void 'simple zip strict with declared'() {
    def type = testSubject.determineContentType(
        true,
        supplier(emptyZip),
        MimeRulesSource.NOOP,
        'test.zip',
        'application/zip'
    )
    assertThat(type, equalTo('application/zip'))
  }

  @Test
  void 'simple zip strict with wrong declared'() {
    def type = testSubject.determineContentType(
        true,
        supplier(emptyZip),
        MimeRulesSource.NOOP,
        'test.zip',
        ContentTypes.TEXT_PLAIN
    )
    assertThat(type, equalTo('application/zip'))
  }

  @Test(expected = InvalidContentException)
  void 'strict wrong zip content as text'() {
    testSubject.determineContentType(
        true,
        supplier(emptyZip),
        MimeRulesSource.NOOP,
        'test.txt',
        ContentTypes.TEXT_PLAIN
    )
  }

  @Test(expected = InvalidContentException)
  void 'strict wrong text content as zip'() {
    testSubject.determineContentType(
        true,
        supplier('simple text'.bytes),
        MimeRulesSource.NOOP,
        'test.zip',
        'application/zip'
    )
  }

  @Test
  void 'non-strict wrong zip content as text'() {
    def type = testSubject.determineContentType(
        false,
        supplier(emptyZip),
        MimeRulesSource.NOOP,
        'test.txt',
        ContentTypes.TEXT_PLAIN
    )
    assertThat(type, equalTo(ContentTypes.TEXT_PLAIN))
  }

  @Test
  void 'non-strict wrong text content as zip'() {
    def type = testSubject.determineContentType(
        false,
        supplier('simple text'.bytes),
        MimeRulesSource.NOOP,
        'test.zip',
        'application/zip'
    )
    assertThat(type, equalTo('application/zip'))
  }

  @Test(expected = InvalidContentException)
  void 'strict wrong zip content as text undeclared'() {
    testSubject.determineContentType(
        true,
        supplier(emptyZip),
        MimeRulesSource.NOOP,
        'test.txt',
        null
    )
  }

  @Test(expected = InvalidContentException)
  void 'strict wrong text content as zip undeclared'() {
    testSubject.determineContentType(
        true,
        supplier('simple text'.bytes),
        MimeRulesSource.NOOP,
        'test.zip',
        null
    )
  }

  @Test
  void 'non-strict wrong zip content as text undeclared'() {
    def type = testSubject.determineContentType(
        false,
        supplier(emptyZip),
        MimeRulesSource.NOOP,
        'test.txt',
        null
    )
    assertThat(type, equalTo(ContentTypes.TEXT_PLAIN))
  }

  @Test
  void 'non-strict wrong text content as zip undeclared'() {
    def type = testSubject.determineContentType(
        false,
        supplier('simple text'.bytes),
        MimeRulesSource.NOOP,
        'test.zip',
        null
    )
    assertThat(type, equalTo('application/zip'))
  }

  @Test
  void 'declared charset missing'() {
    def type = testSubject.determineContentType(
        true,
        supplier('simple text'.bytes),
        MimeRulesSource.NOOP,
        'test.txt',
        ContentTypes.TEXT_PLAIN + '; charset='
    )
    assertThat(type, equalTo(ContentTypes.TEXT_PLAIN))
  }

  @Test(expected = InvalidContentException)
  void 'completely invalid'() {
    def type = testSubject.determineContentType(
        true,
        supplier('simple text'.bytes),
        MimeRulesSource.NOOP,
        'test.txt',
        '@#$*(#&%$*(%)k;lasj;klfjsdfas'
    )
  }

  @Test
  void 'strict maven pom having content body with no xml declaration and containing the text "html"'() {
    def content = '''<project xmlns="http://maven.apache.org/POM/4.0.0"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
      <properties>
        <htmlunit.version>2.4</htmlunit.version>
      </properties>
    </project>
    '''

    def type = testSubject.determineContentType(
        true,
        supplier(content.bytes),
        MimeRulesSource.NOOP,
        'org/jboss/weld/weld-core-parent/1.1.12.Final/weld-core-parent-1.1.12.Final.pom.xml',
        'text/xml'
    )
    assertThat(type, equalTo(ContentTypes.APPLICATION_XML))
  }

  @Test
  void 'binary content'() {
    byte[] binary_file = [01, 02, 03, 04, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00]

    def type = testSubject.determineContentType(
        false,
        supplier(binary_file),
        MimeRulesSource.NOOP,
        'vim',
        null
    )
    assertThat(type, equalTo(ContentTypes.APPLICATION_OCTET_STREAM))
  }

  @Test(expected = InvalidContentException)
  void 'binary content strict'() {
    byte[] binary_file = [01, 02, 03, 04, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00]

    def type = testSubject.determineContentType(
        true,
        supplier(binary_file),
        MimeRulesSource.NOOP,
        'vim',
        null
    )
  }
}
