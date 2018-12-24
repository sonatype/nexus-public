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
import org.sonatype.nexus.blobstore.group.BlobStoreGroup

import spock.lang.Specification
import spock.lang.Unroll

/**
 * Tests {@link WriteToFirstMemberFillPolicy}.
 */
class WriteToFirstMemberFillPolicyTest
    extends Specification
{
  WriteToFirstMemberFillPolicy underTest = new WriteToFirstMemberFillPolicy()

  @Unroll
  def 'It will skip non available and non writable members'() {
    given: 'A group with 3 members'
      BlobStoreGroup blobStoreGroup = Mock() {
        getMembers() >> [
            mockMember('one', available, writable),
            mockMember('two', available, writable),
            mockMember('three', true, true),
        ]
      }
    when: 'the policy tries to select the blob store member'
      def blobStore = underTest.chooseBlobStore(blobStoreGroup, [:])
    then:
      blobStore.blobStoreConfiguration.name == name

    where:
      available | writable | name
      false     | false    | 'three'
      false     | true     | 'three'
      true      | false    | 'three'
      true      | true     | 'one'
  }

  private BlobStore mockMember(final String name, final boolean available, final boolean writable) {
    Mock(BlobStore) {
      isStorageAvailable() >> available
      isWritable() >> writable
      getBlobStoreConfiguration() >> Mock(BlobStoreConfiguration) { getName() >> name }
    }
  }
}
