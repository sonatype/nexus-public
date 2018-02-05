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
package org.sonatype.nexus.common.property

import org.sonatype.goodies.testsupport.TestSupport

import org.joda.time.format.DateTimeFormat
import org.junit.Before
import org.junit.Test

/**
 * Tests for {@link PropertiesFile}
 */
class PropertiesFileTest
    extends TestSupport
{
  private File file

  private PropertiesFile underTest

  @Before
  void setUp() {
    file = util.createTempFile()
    log("File: $file")
    underTest = new PropertiesFile(file)
  }

  @Test
  void 'store properties'() {
    underTest.setProperty('foo', 'bar')
    underTest.store()
    assert file.exists() && file.text
    
    //expect the first 'comment' line of the file to contain a Date
    def firstLine = file.readLines()[0]
    assert DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss,SSSZ").parseDateTime(firstLine - '#').isBeforeNow()

    def props = new Properties()
    file.withInputStream {
      props.load(it)
    }

    assert props.size() == 1
    assert props.getProperty('foo') == 'bar'
  }

  @Test
  void 'store properties with explicit comment'() {
    def comment = 'At the hundredth meridian where the great plains begin'
    underTest.store(comment)
    assert file.exists() && file.text

    //expect the first 'comment' line of the file to contain the specified message
    def firstLine = file.readLines()[0]
    assert (firstLine - '#') == comment
  }

  @Test
  void 'load properties'() {
    def props = new Properties()
    props.setProperty('foo', 'bar')

    file.withOutputStream {
      props.store(it, null)
    }

    underTest.load()
    assert props.size() == 1
    assert props.getProperty('foo') == 'bar'
  }
}
