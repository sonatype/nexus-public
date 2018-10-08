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

import javax.validation.ValidationException

import org.sonatype.nexus.blobstore.api.BlobStore
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration
import org.sonatype.nexus.blobstore.api.BlobStoreManager
import org.sonatype.nexus.blobstore.file.FileBlobStore
import org.sonatype.nexus.blobstore.group.BlobStoreGroup
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.manager.RepositoryManager

import spock.lang.Specification
import spock.lang.Unroll

/**
 * {@link BlobStoreGroupDescriptor} tests.
 */
class BlobStoreGroupDescriptorTest
    extends Specification
{

  BlobStoreManager blobStoreManager = Mock()

  RepositoryManager repositoryManager = Mock()

  BlobStoreGroupDescriptor blobStoreGroupDescriptor = new BlobStoreGroupDescriptor(true, blobStoreManager, repositoryManager)

  def blobStores = [:]

  def setup() {
    blobStoreManager.get(_) >> { String name -> blobStores.computeIfAbsent(name, { k -> mockBlobStore(k, 'mock') }) }
    blobStoreManager.browse() >> blobStores.values()
    repositoryManager.browseForBlobStore(_) >> []
  }

  @Unroll
  def 'Validate with valid members #members'() {
    given: 'A config'
      def config = new BlobStoreConfiguration()

    when: 'the config is validated'
      config.name = 'self'
      config.attributes = [group: [members: members]]
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
      def config = new BlobStoreConfiguration()
      blobStores.nested = mockBlobStore('nested', BlobStoreGroup.TYPE, [group: [members: ['self']]], false)

    when: 'the config is validated'
      config.name = 'self'
      config.attributes = [group: [members: members]]
      blobStoreGroupDescriptor.validateConfig(config)

    then: 'a validation exception is thrown'
      def exception = thrown(ValidationException)
      exception.message == expectedMessage

    where:
      members    || expectedMessage
      []         || '''Blob Store 'self' cannot be empty'''
      ['self']   || '''Blob Store 'self' cannot contain itself'''
      ['nested'] || '''Blob Store 'nested' is of type 'Group' and is not eligible to be a group member'''
  }

  def 'blob stores can only be members of one group'() {
    given: 'an existing blob store group with one member'
      blobStores.store1 = mockBlobStore('store1', FileBlobStore.TYPE)
      blobStores.group1 = mockBlobStoreGroup('group1', [blobStores.store1])

    when: 'a new group is created with the same member as the existing group'
      blobStoreGroupDescriptor.
          validateConfig(new BlobStoreConfiguration(name: 'invalidGroup', type: BlobStoreGroup.TYPE,
              attributes: [group: [members: ['store1']]]))

    then: 'a validation exception is thrown'
      def exception = thrown(ValidationException)
      exception.message == "Blob Store 'store1' is already a member of Blob Store Group 'group1'"
  }

  def 'blob stores cant be group members if set as repo storage'() {
    when: 'attempting to create a group with that blob store'
      blobStoreGroupDescriptor.validateConfig(new BlobStoreConfiguration(name: 'invalidGroup', type: BlobStoreGroup.TYPE,
        attributes: [group: [members: ['store1']]]))

    then: 'a validation exception is thrown'
      repositoryManager.browseForBlobStore('store1') >> [Mock(Repository)]
      def exception = thrown(ValidationException)
      exception.message ==
          "Blob Store 'store1' is set as storage for 1 repositories and is not eligible to be a group member"
  }

  private BlobStoreGroup mockBlobStoreGroup(final String name, final List<BlobStore> members) {
    def config = new BlobStoreConfiguration(name: name, type: BlobStoreGroup.TYPE,
        attributes: [group: [members: members.collect { it.blobStoreConfiguration.name }]])
    def group = Mock(BlobStoreGroup)
    group.isGroupable() >> false
    group.getBlobStoreConfiguration() >> config
    group.getMembers() >> members
    group
  }

  private BlobStore mockBlobStore(final String name, final String type, attributes = [:], Boolean groupable = true) {
    def blobStore = Mock(BlobStore)
    def config = new BlobStoreConfiguration()
    blobStore.getBlobStoreConfiguration() >> config
    blobStore.isGroupable() >> groupable
    config.name = name
    config.type = type
    config.attributes = attributes
    blobStore
  }
}
