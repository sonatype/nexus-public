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
 * {@link RoundRobinFillPolicy} tests.
 */
class RoundRobinFillPolicyTest
    extends Specification
{
  RoundRobinFillPolicy roundRobinFillPolicy = new RoundRobinFillPolicy()

  @Unroll
  def 'nextIndex gives expected value when starting at #initialValue'() {
    when: 'a roundRobinFillPolicy has a given value'
      roundRobinFillPolicy.sequence.set(initialValue)
      def currentIndex = roundRobinFillPolicy.nextIndex()
      def nextIndex = roundRobinFillPolicy.nextIndex()

    then: 'the next value wraps around to 0 to avoid negative indexes'
      currentIndex == expectedCurrentIndex
      nextIndex == expectedNextIndex

    where:
      initialValue      || expectedCurrentIndex | expectedNextIndex
      0                 || 0                    | 1
      Integer.MAX_VALUE || Integer.MAX_VALUE    | 0
  }

  def "It will skip blob stores that are not writable"() {
    given: 'a blob store with some members currently not writable'
      BlobStoreGroup blobStoreGroup = Mock() {
        getMembers() >> [
            mockMember(false, 'one'),
            mockMember(false, 'two'),
            mockMember(true, 'three'),
            mockMember(true, 'four'),
        ]
      }
    when: 'the policy selects the next writable blob store'
      def blobStore = roundRobinFillPolicy.chooseBlobStore(blobStoreGroup, [:])
    then: 'it selects the correct member and the next index is correct'
      blobStore.getBlobStoreConfiguration().getName() == 'three'
      roundRobinFillPolicy.nextIndex() == 1
  }

  def "It will return null if no members are writable"() {
    given: 'a blob store with members currently not writable'
      BlobStoreGroup blobStoreGroup = Mock() {
        getMembers() >> [
            mockMember(false, 'one', 1),
            mockMember(false, 'two', 1),
        ]
      }
    and: 'the following initial sequence'
      roundRobinFillPolicy.sequence.set(initialSequence)
    when: 'the policy tries to select the blob store member'
      def blobStore = roundRobinFillPolicy.chooseBlobStore(blobStoreGroup, [:])
    then: 'the selected blobstore is null and the next index is correct'
      blobStore == null
      roundRobinFillPolicy.nextIndex() == expectedNextIndex

    where:
      initialSequence | expectedNextIndex
      0               | 1
      1               | 2
      2               | 3
      3               | 4
      4               | 5
  }

  @Unroll
  def "It will return null if the group has no member"() {
    given: 'A group with no members'
      BlobStoreGroup blobStoreGroup = Mock() {
        getMembers() >> []
      }
    when: 'the policy tries to select the blob store member'
      def blobStore = roundRobinFillPolicy.chooseBlobStore(blobStoreGroup, [:])
    then: 'the result is null'
      blobStore == null
  }

  private BlobStore mockMember(availability, name, numCalled = _) {
    Mock(BlobStore) {
      numCalled * isStorageAvailable() >> availability
      isWritable() >> true
      getBlobStoreConfiguration() >> Mock(BlobStoreConfiguration) { getName() >> name }
    }
  }

  def 'It will skip read only members'() {
    given: 'A group with a read only members'
      BlobStoreGroup blobStoreGroup = Mock() {
        getMembers() >> [
            mockMember('one', false),
            mockMember('two', true),
            mockMember('three', false),
            mockMember('four', false),
            mockMember('five', true)
        ]
      }
    and: 'the following initial sequence'
      roundRobinFillPolicy.sequence.set(initialSequence)
    when: 'the policy tries to select the blob store member'
      def blobStore = roundRobinFillPolicy.chooseBlobStore(blobStoreGroup, [:])
    then: 'the result is as expected'
      blobStore.blobStoreConfiguration.name == expectedName
    where:
      initialSequence | expectedName
      0               | 'two'
      1               | 'five'
      2               | 'five'
      3               | 'five'
      4               | 'two'
  }

  private BlobStore mockMember(final String name, final boolean writable) {
    Mock(BlobStore) {
      isStorageAvailable() >> true
      isWritable() >> writable
      getBlobStoreConfiguration() >> Mock(BlobStoreConfiguration) { getName() >> name }
    }
  }
}
