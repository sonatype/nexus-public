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
package org.sonatype.nexus.blobstore.api

import spock.lang.Specification

class BlobStoreConfigurationSpec
    extends Specification
{

  def 'copy works'() {
    when: 'a configuration is created'
      BlobStoreConfiguration source = new BlobStoreConfiguration()
      source.setName('source')
      source.setType('test type')
      source.attributes('config key').set('foo', 'bar')

    and: 'a copy is made'
      BlobStoreConfiguration copy = source.copy('a copy')

    then: 'the high-level items in the copy should be intact'
      assert copy.name == 'a copy'
      assert copy.type == source.type

    and: 'the embedded attributes should be intact'
      assert copy.attributes['config key'] != null
      assert copy.attributes['config key']['foo'] == 'bar'
  }
}
