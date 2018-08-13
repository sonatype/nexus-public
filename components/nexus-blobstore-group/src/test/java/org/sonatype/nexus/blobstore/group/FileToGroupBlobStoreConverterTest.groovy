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
package org.sonatype.nexus.blobstore.group

import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration
import org.sonatype.nexus.blobstore.api.BlobStoreException
import org.sonatype.nexus.blobstore.api.BlobStoreManager
import org.sonatype.nexus.blobstore.file.FileBlobStore
import org.sonatype.nexus.blobstore.group.internal.BlobStoreGroup

import spock.lang.Specification

import static org.sonatype.nexus.blobstore.group.internal.BlobStoreGroup.CONFIG_KEY
import static org.sonatype.nexus.blobstore.group.internal.BlobStoreGroup.MEMBERS_KEY

/**
 * Tests {@link FileToGroupBlobStoreConverter}
 */
class FileToGroupBlobStoreConverterTest
    extends Specification
{

  BlobStoreManager blobStoreManager = Mock()

  def 'happy path'() {
    setup:
      FileToGroupBlobStoreConverter converter = new FileToGroupBlobStoreConverter(blobStoreManager)
      FileBlobStore from = Mock()
      BlobStoreGroup to = Mock()
      BlobStoreConfiguration fromConfig = new BlobStoreConfiguration()
      fromConfig.name = 'test'
      fromConfig.type = FileBlobStore.TYPE

    when: 'trying to convert'
      BlobStoreGroup result = converter.convert(from)

    then: 'blobStoreManager is called correctly'
      1 * from.getBlobStoreConfiguration() >> fromConfig
      1 * blobStoreManager.delete('test')
      1 * blobStoreManager.create(fromConfig)
      1 * blobStoreManager.create({ it.name == 'test' &&
          it.type == BlobStoreGroup.TYPE &&
          it.attributes(CONFIG_KEY).require(MEMBERS_KEY).toString() == 'test-promoted'}) >> to

    and: 'config values are updated appropriately'
      assert fromConfig.name == 'test-promoted'

    and: 'result is returned'
      assert result == to
  }

  def 'delete of original file blob store fails'() {
    setup:
      FileToGroupBlobStoreConverter converter = new FileToGroupBlobStoreConverter(blobStoreManager)
      FileBlobStore from = Mock()
      BlobStoreConfiguration fromConfig = new BlobStoreConfiguration()
      fromConfig.name = 'test'
      fromConfig.type = FileBlobStore.TYPE

    when: 'trying to convert'
      converter.convert(from)

    then: 'blobStoreManager fails to delete original'
      1 * from.getBlobStoreConfiguration() >> fromConfig
      1 * blobStoreManager.delete('test') >> { String ignored ->
        throw new BlobStoreException('testing failure', null)
      }

    and: 'wrapped exception is thrown'
      def e = thrown(BlobStoreException)
      e.message == 'during promotion to group, failed to stop existing file blob store: test, Cause: testing failure'
  }

  def 'create of original file blob store with new name fails'() {
    setup:
      FileToGroupBlobStoreConverter converter = new FileToGroupBlobStoreConverter(blobStoreManager)
      FileBlobStore from = Mock()
      BlobStoreConfiguration fromConfig = new BlobStoreConfiguration()
      fromConfig.name = 'test'
      fromConfig.type = FileBlobStore.TYPE

    when: 'trying to convert'
      converter.convert(from)

    then: 'blobStoreManager fails to create file blob store with new name, based on original configuration'
      1 * from.getBlobStoreConfiguration() >> fromConfig
      1 * blobStoreManager.delete('test')
      1 * blobStoreManager.create(fromConfig) >> { BlobStoreConfiguration ignored ->
        throw new RuntimeException('testing failure')
      }
      1 * blobStoreManager.create({ it.name == 'test' && it.type == FileBlobStore.TYPE })

    and: 'wrapped exception is thrown'
      def e = thrown(BlobStoreException)
      e.message == 'during promotion to group, failed to stop existing file blob store: test, Cause: testing failure'
  }

  def 'create of promoted group blob store fails'() {
    setup:
      FileToGroupBlobStoreConverter converter = new FileToGroupBlobStoreConverter(blobStoreManager)
      FileBlobStore from = Mock()
      BlobStoreConfiguration fromConfig = new BlobStoreConfiguration()
      fromConfig.name = 'test'
      fromConfig.type = FileBlobStore.TYPE

    when: 'trying to convert'
      converter.convert(from)

    then: 'blobStoreManager fails to create file blob store with new name, based on original configuration'
      1 * from.getBlobStoreConfiguration() >> fromConfig

      1 * blobStoreManager.delete('test')
      1 * blobStoreManager.create(fromConfig)
      1 * blobStoreManager.create(_ as BlobStoreConfiguration) >> { BlobStoreConfiguration ignored ->
        throw new RuntimeException('testing failure')
      }
      1 * blobStoreManager.delete('test-promoted')
      1 * blobStoreManager.create({ it.name == 'test' && it.type == FileBlobStore.TYPE })

    and: 'wrapped exception is thrown'
      def e = thrown(BlobStoreException)
      e.message == 'failed to create group configuration, Cause: testing failure'
  }
}
