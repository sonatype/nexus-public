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

import org.sonatype.nexus.blobstore.api.BlobStore
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration
import org.sonatype.nexus.blobstore.api.BlobStoreException
import org.sonatype.nexus.blobstore.api.BlobStoreManager
import org.sonatype.nexus.blobstore.file.FileBlobStore

import spock.lang.Specification

import static BlobStoreGroup.CONFIG_KEY
import static BlobStoreGroup.MEMBERS_KEY

/**
 * Tests {@link BlobStorePromoter}
 */
class BlobStorePromoterTest
    extends Specification
{

  BlobStoreManager blobStoreManager = Mock()

  def 'it will promote a #fromType to a group'() {
    setup:
      BlobStorePromoter promoter = new BlobStorePromoter(blobStoreManager)
      BlobStore from = Mock(fromType)
      BlobStoreGroup to = Mock()
      BlobStoreConfiguration fromConfig = new BlobStoreConfiguration()
      fromConfig.name = 'test'
      fromConfig.type = blobStoreType

    when: 'trying to promote'
      BlobStoreGroup result = promoter.promote(from)

    then: 'blobStoreManager is called correctly'
      1 * from.getBlobStoreConfiguration() >> fromConfig
      1 * blobStoreManager.forceDelete('test')
      1 * blobStoreManager.create(fromConfig)
      1 * blobStoreManager.create({ it.name == 'test' &&
          it.type == BlobStoreGroup.TYPE &&
          it.attributes(CONFIG_KEY).require(MEMBERS_KEY) == ['test-promoted']}) >> to

    and: 'config values are updated appropriately'
      assert fromConfig.name == 'test-promoted'

    and: 'result is returned'
      assert result == to

    where:
      fromType       | blobStoreType
      FileBlobStore  | FileBlobStore.TYPE
  }

  def 'delete of original file blob store fails'() {
    setup:
      BlobStorePromoter promoter = new BlobStorePromoter(blobStoreManager)
      FileBlobStore from = Mock()
      BlobStoreConfiguration fromConfig = new BlobStoreConfiguration()
      fromConfig.name = 'test'
      fromConfig.type = FileBlobStore.TYPE

    when: 'trying to promoter'
      promoter.promote(from)

    then: 'blobStoreManager fails to delete original'
      1 * from.getBlobStoreConfiguration() >> fromConfig
      1 * blobStoreManager.forceDelete('test') >> { String ignored ->
        throw new BlobStoreException('testing failure', null)
      }

    and: 'wrapped exception is thrown'
      def e = thrown(BlobStoreException)
      e.message == 'during promotion to group, failed to stop existing blob store: test, Cause: testing failure'
  }

  def 'create of original file blob store with new name fails'() {
    setup:
      BlobStorePromoter promoter = new BlobStorePromoter(blobStoreManager)
      FileBlobStore from = Mock()
      BlobStoreConfiguration fromConfig = new BlobStoreConfiguration()
      fromConfig.name = 'test'
      fromConfig.type = FileBlobStore.TYPE

    when: 'trying to promote'
      promoter.promote(from)

    then: 'blobStoreManager fails to create file blob store with new name, based on original configuration'
      1 * from.getBlobStoreConfiguration() >> fromConfig
      1 * blobStoreManager.forceDelete('test')
      1 * blobStoreManager.create(fromConfig) >> { BlobStoreConfiguration ignored ->
        throw new RuntimeException('testing failure')
      }
      1 * blobStoreManager.create({ it.name == 'test' && it.type == FileBlobStore.TYPE })

    and: 'wrapped exception is thrown'
      def e = thrown(BlobStoreException)
      e.message == 'during promotion to group, failed to stop existing blob store: test, Cause: testing failure'
  }

  def 'create of promoted group blob store fails'() {
    setup:
      BlobStorePromoter promoter = new BlobStorePromoter(blobStoreManager)
      FileBlobStore from = Mock()
      BlobStoreConfiguration fromConfig = new BlobStoreConfiguration()
      fromConfig.name = 'test'
      fromConfig.type = FileBlobStore.TYPE

    when: 'trying to promote'
      promoter.promote(from)

    then: 'blobStoreManager fails to create file blob store with new name, based on original configuration'
      1 * from.getBlobStoreConfiguration() >> fromConfig

      1 * blobStoreManager.forceDelete('test')
      1 * blobStoreManager.create(fromConfig)
      1 * blobStoreManager.create(_ as BlobStoreConfiguration) >> { BlobStoreConfiguration ignored ->
        throw new RuntimeException('testing failure')
      }
      1 * blobStoreManager.forceDelete('test-promoted')
      1 * blobStoreManager.create({ it.name == 'test' && it.type == FileBlobStore.TYPE })

    and: 'wrapped exception is thrown'
      def e = thrown(BlobStoreException)
      e.message == 'failed to create group configuration, Cause: testing failure'
  }
}
