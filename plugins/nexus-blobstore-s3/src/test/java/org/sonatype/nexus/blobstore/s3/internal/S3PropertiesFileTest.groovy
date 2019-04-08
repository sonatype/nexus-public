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
package org.sonatype.nexus.blobstore.s3.internal

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.S3Object
import com.amazonaws.services.s3.model.S3ObjectInputStream
import spock.lang.Specification


/**
 * {@link S3PropertiesFile} tests.
 */
public class S3PropertiesFileTest
    extends Specification
{

  AmazonS3 s3 = Mock()

  String testProperties = 'propertyName = value\n'

  def "Load ingests properties from s3 object"() {
    given:
      S3PropertiesFile propertiesFile = new S3PropertiesFile(s3, 'mybucket', 'mykey')
      S3Object s3Object = Mock()

    when:
      propertiesFile.load()

    then:
      propertiesFile.getProperty('propertyName') == 'value'
      1 * s3.getObject('mybucket', 'mykey') >> s3Object
      1 * s3Object.getObjectContent() >> new S3ObjectInputStream(new ByteArrayInputStream(testProperties.bytes), null)
  }

  def "Store writes properties to s3 object"() {
    given:
      S3PropertiesFile propertiesFile = new S3PropertiesFile(s3, 'mybucket', 'mykey')
      S3Object s3Object = Mock()

    when:
      propertiesFile.setProperty('testProperty', 'newValue')
      propertiesFile.store()

    then:
      1 * s3.putObject('mybucket', 'mykey', _, _) >> { bucket, key, bytes, metadata ->
        def text = bytes.text
        assert text.contains('testProperty=newValue' + System.lineSeparator())
        assert metadata.contentLength == text.length()
      }
  }

  def "The toString is formatted properly"() {
    given:
      S3PropertiesFile propertiesFile = new S3PropertiesFile(s3, 'mybucket', 'mykey/with/nesting/')

    when:
      propertiesFile.setProperty('testProperty', 'newValue')
      propertiesFile.setProperty('otherKey', 'otherValue')

    then:
      assert propertiesFile.toString() == "s3://mybucket/mykey/with/nesting/ {testProperty=newValue, otherKey=otherValue}"
  }
}

