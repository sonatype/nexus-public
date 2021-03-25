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

import org.sonatype.nexus.blobstore.BlobStoreUtil
import org.sonatype.nexus.blobstore.MockBlobStoreConfiguration
import org.sonatype.nexus.blobstore.api.BlobId
import org.sonatype.nexus.blobstore.api.BlobStore
import org.sonatype.nexus.blobstore.api.BlobStoreManager
import org.sonatype.nexus.blobstore.api.BlobStoreMetrics
import org.sonatype.nexus.blobstore.group.BlobStoreGroup
import org.sonatype.nexus.blobstore.group.BlobStoreGroupService
import org.sonatype.nexus.blobstore.group.FillPolicy
import org.sonatype.nexus.blobstore.quota.BlobStoreQuotaService
import org.sonatype.nexus.rest.ValidationErrorsException

import spock.lang.Specification
import spock.lang.Unroll

/**
 * {@link BlobStoreGroupDescriptor} tests.
 */
class BlobStoreGroupDescriptorTest
    extends Specification
{
  static final String FILE = 'File'

  BlobStoreManager blobStoreManager = Mock()

  BlobStoreUtil blobStoreUtil = Mock()

  BlobStoreGroupService blobStoreGroupService = Mock()

  BlobStoreQuotaService quotaService = Mock()

  Map<String, FillPolicy> fillPolicies = [
      (RoundRobinFillPolicy.TYPE)        : new RoundRobinFillPolicy(),
      (WriteToFirstMemberFillPolicy.TYPE): new WriteToFirstMemberFillPolicy()
  ]

  BlobStoreGroupDescriptor blobStoreGroupDescriptor = new BlobStoreGroupDescriptor(
      blobStoreManager,
      blobStoreUtil,
      { blobStoreGroupService },
      quotaService,
      fillPolicies
  )

  def blobStores = [:]

  def setup() {
    blobStoreManager.get(_) >> { String name -> blobStores.computeIfAbsent(name, { k -> mockBlobStore(k, 'mock') }) }
    blobStoreManager.browse() >> blobStores.values()
    blobStoreManager.getParent(_) >> Optional.empty()
    blobStoreUtil.usageCount(_) >> 0
    blobStoreGroupService.isEnabled() >> true
  }

  @Unroll
  def 'Validate with valid members #members'() {
    given: 'A config'
      def config = new MockBlobStoreConfiguration()

    when: 'the config is validated'
      config.name = 'self'
      config.attributes = [group: [members: members, fillPolicy: WriteToFirstMemberFillPolicy.TYPE]]
      blobStoreGroupDescriptor.validateConfig(config)

    then: 'validate succeeds'
      noExceptionThrown()

    where:
      members                || _
      ['single']             || _
      ['multiple', 'unique'] || _
  }

  @Unroll
  def 'Validate invalid members #members'() {
    given: 'A config'
      def config = new MockBlobStoreConfiguration()
      blobStores.nested = mockBlobStore('nested', BlobStoreGroup.TYPE, [group: [members: ['self'], fillPolicy: WriteToFirstMemberFillPolicy.TYPE]], false)

    when: 'the config is validated'
      config.name = 'self'
      config.attributes = [group: [members: members, fillPolicy: WriteToFirstMemberFillPolicy.TYPE]]
      blobStoreGroupDescriptor.validateConfig(config)

    then: 'a validation exception is thrown'
      def exception = thrown(ValidationErrorsException)
      exception.message == expectedMessage

    where:
      members    || expectedMessage
      []         || '''Blob Store 'self' cannot be empty'''
      ['self']   || '''Blob Store 'self' cannot contain itself'''
      ['nested'] || '''Blob Store 'nested' is of type 'Group' and is not eligible to be a group member'''
  }

  def 'blob stores can only be members of one group'() {
    given: 'an existing blob store group with one member'
      blobStores.store1 = mockBlobStore('store1', FILE)
      blobStores.group1 = mockBlobStoreGroup('group1', [blobStores.store1])

    when: 'a new group is created with the same member as the existing group'
      blobStoreGroupDescriptor.
          validateConfig(new MockBlobStoreConfiguration(name: 'invalidGroup', type: BlobStoreGroup.TYPE,
              attributes: [group: [members: ['store1'], fillPolicy: WriteToFirstMemberFillPolicy.TYPE]]))

    then: 'a validation exception is thrown'
      blobStoreManager.getParent('store1') >> Optional.of('group1')
      def exception = thrown(ValidationErrorsException)
      exception.message == "Blob Store 'store1' is already a member of Blob Store Group 'group1'"
  }

  def 'blob stores cant be group members if set as repo storage'() {
    when: 'attempting to create a group with that blob store'
      blobStoreGroupDescriptor.
          validateConfig(new MockBlobStoreConfiguration(name: 'invalidGroup', type: BlobStoreGroup.TYPE,
              attributes: [group: [members: ['store1'], fillPolicy: WriteToFirstMemberFillPolicy.TYPE]]))

    then: 'a validation exception is thrown'
      blobStoreUtil.usageCount('store1') >> 1
      def exception = thrown(ValidationErrorsException)
      exception.message ==
          "Blob Store 'store1' is set as storage for 1 repositories and is not eligible to be a group member"
  }

  def 'members cant be removed directly unless read only and empty'() {
    given: 'an existing blob store group with two members'
      blobStores.store1 = mockBlobStore('store1', FILE)
      blobStores.nonEmptyStore = mockBlobStore('nonEmptyStore', FILE, [:], true)
      blobStores.nonEmptyStore.getBlobIdStream() >> [Mock(BlobId)].stream()
      blobStores.group1 = mockBlobStoreGroup('group1', [blobStores.store1, blobStores.nonEmptyStore])

    when: 'the member is removed'
      blobStoreGroupDescriptor.
          validateConfig(new MockBlobStoreConfiguration(name: 'group1', type: BlobStoreGroup.TYPE,
              attributes: [group: [members: ['store1'], fillPolicy: WriteToFirstMemberFillPolicy.TYPE]]))

    then: 'a validation exception is thrown'
      def exception = thrown(ValidationErrorsException)
      exception.message ==
          "Blob Store 'nonEmptyStore' cannot be removed from Blob Store Group 'group1', " +
          "use 'Admin - Remove a member from a blob store group' task instead"
  }

  def 'a group blob store validates its quota'() {
    when: 'attempting to create a group'
      blobStoreGroupDescriptor.validateConfig(new MockBlobStoreConfiguration(name: 'group', type: BlobStoreGroup.TYPE,
          attributes: [group: [members: ['single'], fillPolicy: WriteToFirstMemberFillPolicy.TYPE]]))

    then: 'quota validity is checked'
      1 * quotaService.validateSoftQuotaConfig(*_)
  }

  private BlobStoreGroup mockBlobStoreGroup(final String name, final List<BlobStore> members) {
    def config = new MockBlobStoreConfiguration(name: name, type: BlobStoreGroup.TYPE,
        attributes: [group: [members: members.collect { it.blobStoreConfiguration.name }], fillPolicy: WriteToFirstMemberFillPolicy.TYPE])
    def group = Mock(BlobStoreGroup)
    group.isGroupable() >> false
    group.getBlobStoreConfiguration() >> config
    group.getMembers() >> members
    group
  }

  private BlobStore mockBlobStore(final String name,
                                  final String type,
                                  attributes = [:],
                                  Boolean groupable = true,
                                  BlobStoreMetrics metrics = null)
  {
    def blobStore = Mock(BlobStore)
    def config = new MockBlobStoreConfiguration()
    blobStore.getBlobStoreConfiguration() >> config
    blobStore.isGroupable() >> groupable
    blobStore.getMetrics() >> (metrics ?: Mock(BlobStoreMetrics))
    config.name = name
    config.type = type
    config.attributes = attributes
    blobStore
  }
}
