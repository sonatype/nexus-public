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
package org.sonatype.nexus.coreui

import org.sonatype.nexus.blobstore.BlobStoreDescriptor
import org.sonatype.nexus.blobstore.api.BlobStore
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration
import org.sonatype.nexus.blobstore.api.BlobStoreException
import org.sonatype.nexus.blobstore.api.BlobStoreManager
import org.sonatype.nexus.blobstore.api.BlobStoreMetrics
import org.sonatype.nexus.blobstore.group.BlobStorePromoter
import org.sonatype.nexus.blobstore.group.BlobStoreGroup
import org.sonatype.nexus.common.app.ApplicationDirectories
import org.sonatype.nexus.repository.manager.RepositoryManager

import spock.lang.Specification
import spock.lang.Subject

/**
 * Test for {@link BlobStoreComponent}
 */
class BlobStoreComponentTest
    extends Specification
{

  BlobStoreManager blobStoreManager = Mock()

  ApplicationDirectories applicationDirectories = Mock()
  
  RepositoryManager repositoryManager = Mock()

  BlobStorePromoter blobStoreConverter = Mock()

  @Subject
  BlobStoreComponent blobStoreComponent = new BlobStoreComponent(blobStoreManager: blobStoreManager,
      applicationDirectories: applicationDirectories, repositoryManager: repositoryManager,
      blobStorePromoter: blobStoreConverter)

  def 'Read types returns descriptor data'() {
    given: 'A blobstore descriptor'
      blobStoreComponent.blobStoreDescriptors =
          [MyType: [getName: { -> 'MyType' }, getFormFields: { -> []}] as BlobStoreDescriptor]

    when: 'Reading blobstore types'
      def types = blobStoreComponent.readTypes()

    then: 'The descriptor information is returned'
      types.collect{[it.id, it.name, it.formFields]} == [['MyType', 'MyType', []]]
  }

  def 'Create blobstore creates and returns new blobstore'() {
    given: 'A blobstore create request'
      BlobStoreXO blobStoreXO = new BlobStoreXO(name: 'myblobs', type: 'File',
          isQuotaEnabled: true, quotaType: 'spaceUsedQuota', quotaLimit: 10L,
          attributes: [file: [path: 'path/to/blobs/myblobs']])
      BlobStoreConfiguration expectedConfig = new BlobStoreConfiguration(name: 'myblobs', type: 'File',
          attributes: [file: [path: 'path/to/blobs/myblobs'], blobStoreQuotaConfig: [quotaType: 'spaceUsedQuota',
                                                                                     quotaLimit: 10L]])
      BlobStore blobStore = Mock()

    when: 'The blobstore is created'
      def createdXO = blobStoreComponent.create(blobStoreXO)

    then: 'The blobstore is created with the manager and returned'
      _ * blobStore.getBlobStoreConfiguration() >> expectedConfig
      _ * blobStore.getMetrics() >> Mock(BlobStoreMetrics)
      1 * blobStoreManager.create(_) >> blobStore
      [createdXO.name, createdXO.type, createdXO.attributes] ==
        [expectedConfig.name, expectedConfig.type, expectedConfig.attributes]
  }

  def 'Remove blobstore only removes unused blobstores'() {
    when: 'Attempting to remove an unused blobstore'
      blobStoreComponent.remove("not-used")

    then: 'It is deleted'
      1 * repositoryManager.isBlobstoreUsed('not-used') >> false
      1 * blobStoreManager.delete('not-used')

    when: 'Attempting to remove an used blobstore'
      blobStoreComponent.remove('used')
      
    then: 'It aint'
      thrown BlobStoreException
      1 * repositoryManager.isBlobstoreUsed('used') >> true
      0 * blobStoreManager.delete('used')
  }

  def 'Default work directory returns the blobs directory'() {
    given: 'A blob directory'
      def blobDirectory = 'path/to/blobs'

    when: 'The blob directory is requested'
      def defaultWorkDirectory = blobStoreComponent.defaultWorkDirectory()

    then: 'The blob directory is returned with the system specific separator'
      1 * applicationDirectories.getWorkDirectory('blobs') >> new File(blobDirectory)
      defaultWorkDirectory.path == blobDirectory
      defaultWorkDirectory.fileSeparator == File.separator
  }

  def 'it will promote a blob that is promotable'() {
    setup:
      def groupBlobName = 'myGroup'
      def from = Mock(BlobStore)

    when: 'trying to promote'
      def blobStoreXO = blobStoreComponent.promoteToGroup(groupBlobName)

    then: 'blobStoreManager is called correctly'
      1 * blobStoreManager.get(groupBlobName) >> from
      1 * blobStoreManager.isPromotable(from) >> true
      1 * repositoryManager.blobstoreUsageCount(_ as String) >> 2L
      1 * blobStoreConverter.promote(from) >> Mock(BlobStoreGroup) {
          getBlobStoreConfiguration() >> Mock(BlobStoreConfiguration) {
          getName() >> 'name'
          getType() >> 'type'
          getAttributes() >> ['group': ['members': 'name-promoted']]
        }
        getMetrics() >> Mock(BlobStoreMetrics) {
          getBlobCount() >> 1L
          getTotalSize() >> 500L
          getAvailableSpace() >> 450L
          isUnlimited() >> false
        }
      }

      blobStoreXO.name == 'name'
      blobStoreXO.type == 'type'
      blobStoreXO.attributes == ['group': ['members': 'name-promoted']]
      blobStoreXO.blobCount == 1L
      blobStoreXO.totalSize == 500L
      blobStoreXO.availableSpace == 450L
      !blobStoreXO.unlimited
      blobStoreXO.repositoryUseCount == 2L
  }

  def 'it will not promote a blob store type that is not promotable'() {
    setup:
      def groupBlobName = 'myGroup'
      def blobStore = Mock(BlobStore) {
        getBlobStoreConfiguration() >> new BlobStoreConfiguration(name: groupBlobName, attributes: [:])
      }

    when: 'trying to promote'
      blobStoreComponent.promoteToGroup(groupBlobName)

    then: 'blobStoreManager is called correctly'
      1 * blobStoreManager.get(groupBlobName) >> blobStore
      1 * blobStoreManager.isPromotable(blobStore) >> false
      BlobStoreException exception = thrown()
      exception.message == 'Blob store (myGroup) could not be promoted to a blob store group'
  }
}
