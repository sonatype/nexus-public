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
package org.sonatype.nexus.blobstore.group.internal

import org.sonatype.nexus.blobstore.api.BlobStore
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration
import org.sonatype.nexus.blobstore.api.BlobStoreException
import org.sonatype.nexus.blobstore.group.BlobStoreGroup
import org.sonatype.nexus.blobstore.group.InvalidBlobStoreGroupConfiguration

import spock.lang.Specification

class FirstBlobStoreCacheFillPolicyTest extends Specification
{
  FirstBlobStoreCacheFillPolicy underTest = new FirstBlobStoreCacheFillPolicy()

  def 'temp blob creates go to the first blob store'(){
    given: 'A group with 2 members'
      BlobStoreGroup blobStoreGroup = Mock() {
        getMembers() >> [
            mockMember('one', true, true),
            mockMember('two', true, true),
        ]
      }
    when: 'a blob store is retrieved for creating a temp blob'
      def blobStore = underTest.chooseBlobStoreForCreate(blobStoreGroup, [(BlobStore.TEMPORARY_BLOB_HEADER):''])
    then:
      blobStore.blobStoreConfiguration.name == 'one'
  }

  def 'non-temp blob creates go to the second blob store'(){
    given: 'A group with 2 members'
      BlobStoreGroup blobStoreGroup = Mock() {
        getMembers() >> [
            mockMember('one', true, true),
            mockMember('two', true, true),
        ]
      }
    when: 'a blob store is retrieved for creating a temp blob'
      def blobStore = underTest.chooseBlobStoreForCreate(blobStoreGroup, [:])
    then:
      blobStore.blobStoreConfiguration.name == 'two'
  }

  def 'blob copies go to the first blob store'(){
    given: 'A group with 2 members'
      BlobStore firstBlobStore = mockMember('one', true, true)
      BlobStoreGroup blobStoreGroup = Mock() {
        getMembers() >> [
            firstBlobStore,
            mockMember('two', true, true),
        ]
      }
    when: 'a blob store is retrieved for copying a blob'
      def blobStore = underTest.chooseBlobStoreForCopy(blobStoreGroup, firstBlobStore, [:])
    then:
      blobStore.blobStoreConfiguration.name == 'two'
  }

  def 'group with less than 2 members throws exception on validate'(){
    given: 'A group with 1 member'
      BlobStore firstBlobStore = mockMember('one', true, true)
      BlobStoreGroup blobStoreGroup = Mock() {
        getMembers() >> [
            firstBlobStore
        ]
        getBlobStoreConfiguration() >> Mock(BlobStoreConfiguration) { getName() >> 'group' }
      }
    when: 'the fill policy attempts to validate the group'
      underTest.validateBlobStoreGroup(blobStoreGroup)
    then:
      thrown(InvalidBlobStoreGroupConfiguration)
  }

  private BlobStore mockMember(final String name, final boolean available, final boolean writable) {
    Mock(BlobStore) {
      isStorageAvailable() >> available
      isWritable() >> writable
      getBlobStoreConfiguration() >> Mock(BlobStoreConfiguration) { getName() >> name }
    }
  }
}
