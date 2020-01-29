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
package org.sonatype.nexus.blobstore

import spock.lang.Specification

class PerformanceLoggingInputStreamTest
    extends Specification
{
  def 'Reads and close are passed through to underlying InputStream'() {
    given:
      def source = Mock(InputStream)
      def logger = Mock(PerformanceLogger)
      def buffer1 = new byte[100]
      def buffer2 = new byte[100]
      def underTest = new PerformanceLoggingInputStream(source, logger)

    when:
      def i = underTest.read()
      def len1 = underTest.read(buffer1)
      def len2 = underTest.read(buffer1, 7, 29)
      underTest.close()

    then:
     i == 123
     len1 == 99
     len2 == 29

     1 * source.read() >> 123
     1 * source.read(buffer1, 0, 100) >> 99
     1 * source.read(buffer2, 7, 29) >> 29
     1 * source.close()
  }

  def 'Performance data is logged on close'() {
    given:
      def source = Mock(InputStream)
      def logger = Mock(PerformanceLogger)
      def underTest = new PerformanceLoggingInputStream(source, logger)

    when:
      underTest.close()

    then:
      1 * logger.logRead(0, _);
  }
}
