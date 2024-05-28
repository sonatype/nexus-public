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
import org.sonatype.nexus.blobstore.MockBlobStoreConfiguration
import org.sonatype.nexus.blobstore.api.BlobStore
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration
import org.sonatype.nexus.blobstore.api.BlobStoreException
import org.sonatype.nexus.blobstore.api.BlobStoreManager
import org.sonatype.nexus.blobstore.api.BlobStoreMetrics
import org.sonatype.nexus.blobstore.api.tasks.BlobStoreTaskService
import org.sonatype.nexus.blobstore.group.BlobStoreGroupService
import org.sonatype.nexus.common.app.ApplicationDirectories
import org.sonatype.nexus.rapture.PasswordPlaceholder
import org.sonatype.nexus.repository.manager.RepositoryManager

import spock.lang.Specification
import spock.lang.Subject

import static java.lang.Math.pow

/**
 * Test for {@link BlobStoreComponent}
 */
class BlobStoreComponentTest
    extends Specification
{

  BlobStoreManager blobStoreManager = Mock()

  ApplicationDirectories applicationDirectories = Mock()

  RepositoryManager repositoryManager = Mock()

  BlobStoreGroupService blobStoreGroupService = Mock()

  BlobStoreTaskService blobStoreTaskService = Mock()

  @Subject
  BlobStoreComponent blobStoreComponent = new BlobStoreComponent(blobStoreManager: blobStoreManager,
      applicationDirectories: applicationDirectories, repositoryManager: repositoryManager,
      blobStoreGroupService: { blobStoreGroupService }, blobStoreTaskService: blobStoreTaskService)

  def 'Read types returns descriptor data'() {
    given: 'A blobstore descriptor'
      blobStoreComponent.blobStoreDescriptors =
          [MyType: [getName: { -> 'MyType' }, getFormFields: { -> []}] as BlobStoreDescriptor]

    when: 'Reading blobstore types'
      def types = blobStoreComponent.readTypes()

    then: 'The descriptor information is returned'
      types.collect{[it.id, it.name, it.formFields]} == [['MyType', 'MyType', []], ['', '', null]]
  }

  def 'Create blobstore creates and returns new blobstore'() {
    given: 'A blobstore create request'
      BlobStoreXO blobStoreXO = new BlobStoreXO(name: 'myblobs', type: 'File',
          isQuotaEnabled: true, quotaType: 'spaceUsedQuota', quotaLimit: 10L,
          attributes: [file: [path: 'path/to/blobs/myblobs']])
      BlobStoreConfiguration expectedConfig = new MockBlobStoreConfiguration(name: 'myblobs', type: 'File',
          attributes: [file: [path: 'path/to/blobs/myblobs'], blobStoreQuotaConfig: [quotaType: 'spaceUsedQuota',
                                                                                     quotaLimit: 10L]])
      BlobStore blobStore = Mock()
      1 * blobStoreManager.newConfiguration() >> Mock(BlobStoreConfiguration)
      1 * blobStoreManager.getByName() >> ['myblobs' : blobStore]

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
      def blobDirectory = new File('path/to/blobs')

    when: 'The blob directory is requested'
      def defaultWorkDirectory = blobStoreComponent.defaultWorkDirectory()

    then: 'The blob directory is returned with the system specific separator'
      1 * applicationDirectories.getWorkDirectory('blobs') >> blobDirectory
      new File(defaultWorkDirectory.path) == blobDirectory
      defaultWorkDirectory.fileSeparator == File.separator
  }

  def 'given a blob store with a quota, create a proper blobStoreXO'() {
    setup:
      def config = new MockBlobStoreConfiguration(name: "test",
            attributes: [file: [path: 'path'], blobStoreQuotaConfig: [quotaType: 'spaceUsedQuota', quotaLimitBytes:
                quotaLimitBytes]])
      def blobStore = Mock(BlobStore) {
        getBlobStoreConfiguration() >> config
        getMetrics() >> Mock(BlobStoreMetrics) {
          getBlobCount() >> 1L
          getTotalSize() >> 500L
          getAvailableSpace() >> 450L
          isUnlimited() >> false
        }
      }
      1 * blobStoreManager.getByName() >> ['test' : blobStore]

    when: 'create the XO'
      def blobStoreXO = blobStoreComponent.asBlobStoreXO(config)

    then: 'proper object created'
      blobStoreXO.isQuotaEnabled == true
      blobStoreXO.quotaType == 'spaceUsedQuota'
      blobStoreXO.quotaLimit == expectedQuotaLimit

    where:
      quotaLimitBytes            | expectedQuotaLimit
      ((Long) (10 * pow(10, 6))) | 10L
      ((Integer) (1))            | 0L
  }

  def 'given a blob store XO with a quota, create a proper blob store config'() {
    def blobStoreConfig = Mock(BlobStoreConfiguration)
    setup:
      def blobStoreXO = Mock(BlobStoreXO) {
        getName() >> 'xoTest'
        getType() >> 'type'
        getIsQuotaEnabled() >> true
        getQuotaLimit() >> 10L
        getQuotaType() >> 'properType'
        getAttributes() >> [blobStoreQuotaConfig: [quotaType: 'shouldBeClobbered', quotaLimitBytes: 7]]
      }
      1 * blobStoreManager.newConfiguration() >> blobStoreConfig

    when: 'create the config'
      blobStoreComponent.asConfiguration(blobStoreXO)

    then: 'proper object created'
      1 * blobStoreConfig.setName('xoTest')
      1 * blobStoreConfig.setType('type')
      1 * blobStoreConfig.setAttributes([blobStoreQuotaConfig: [quotaType: 'properType', quotaLimitBytes: 10 * pow(10, 6)]])
  }

  def 'requesting blobstore names only does not set other properties'() {
    setup:
      def config = new MockBlobStoreConfiguration(name: "test",
            attributes: [file: [path: 'path'], blobStoreQuotaConfig: [quotaType: 'spaceUsedQuota', quotaLimitBytes: 7]])
      def blobStore = Mock(BlobStore) {
        getBlobStoreConfiguration() >> config
      }
      1 * blobStoreManager.getByName() >> ['test' : blobStore]

    when: 'create the XO with namesOnly'
      def blobStoreXO = blobStoreComponent.asBlobStoreXO(config, [])

    then: 'only name is set'
      blobStoreXO.name == 'test'
      blobStoreXO.type == null
      0 * blobStore.getMetrics()
  }

  def 'updating an s3 blobstore with the password placeholder does not alter the secret access key'() {
    given: 'A blobstore update request'
      def originalSecret = 'hello'
      BlobStoreXO blobStoreXO = new BlobStoreXO(name: 'myblobs', type: 'S3',
          attributes: [s3: [accessKeyId: 'test', secretAccessKey: PasswordPlaceholder.get()]])
      BlobStoreConfiguration existingConfig = new MockBlobStoreConfiguration(name: 'myblobs', type: 'S3',
          attributes: [s3: [accessKeyId: 'test', secretAccessKey: originalSecret]])
      BlobStore blobStore = Mock()
      1 * blobStoreManager.get('myblobs') >> blobStore
      1 * blobStoreManager.newConfiguration() >> new MockBlobStoreConfiguration();
      1 * blobStoreManager.getByName() >> ['myblobs' : blobStore]

    when: 'The blobstore is updated'
      def updatedXO = blobStoreComponent.update(blobStoreXO)

    then: 'The blobstore is updated with the original secret access key'
      _ * blobStore.getBlobStoreConfiguration() >> existingConfig
      _ * blobStore.getMetrics() >> Mock(BlobStoreMetrics)
      1 * blobStoreManager.update(_) >> { args ->
        assert args[0].attributes.s3.secretAccessKey == originalSecret
        blobStore
      }
      updatedXO.attributes.s3.secretAccessKey == PasswordPlaceholder.get()
  }

  def 'updating an azure blobstore with the password placeholder does not alter the account key'() {
    given: 'A blobstore update request'
      def originalSecret = 'hello'
      BlobStoreXO blobStoreXO = new BlobStoreXO(name: 'myblobs', type: 'Azure Cloud Storage',
          attributes: ['azure cloud storage': [accountKey: 'test', accountKey: PasswordPlaceholder.get()]])
      BlobStoreConfiguration existingConfig = new MockBlobStoreConfiguration(name: 'myblobs', type: 'Azure Cloud Storage',
          attributes: ['azure cloud storage': [accountKey: 'test', accountKey: originalSecret]])
      BlobStore blobStore = Mock()
      1 * blobStoreManager.get('myblobs') >> blobStore
      1 * blobStoreManager.newConfiguration() >> new MockBlobStoreConfiguration();
      1 * blobStoreManager.getByName() >> ['myblobs' : blobStore]

    when: 'The blobstore is updated'
      def updatedXO = blobStoreComponent.update(blobStoreXO)

    then: 'The blobstore is updated with the original secret access key'
      _ * blobStore.getBlobStoreConfiguration() >> existingConfig
      _ * blobStore.getMetrics() >> Mock(BlobStoreMetrics)
      1 * blobStoreManager.update(_) >> { args ->
        assert args[0].attributes.'azure cloud storage'.accountKey == originalSecret
        blobStore
      }
      updatedXO.attributes.'azure cloud storage'.accountKey == PasswordPlaceholder.get()
  }

  def 'Remove blobstore does not remove blobstores part of a move repository task'() {
    when: 'Attempting to remove an used blobstore'
      blobStoreComponent.remove('used_in_move')

    then: 'It aint'
      thrown BlobStoreException
      1 * blobStoreTaskService.countTasksInUseForBlobStore('used_in_move') >> 2
      0 * blobStoreManager.delete('used_in_move')
  }
}
