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

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.blobstore.BlobStoreDescriptor
import org.sonatype.nexus.blobstore.api.BlobStore
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration
import org.sonatype.nexus.blobstore.api.BlobStoreException
import org.sonatype.nexus.blobstore.api.BlobStoreManager
import org.sonatype.nexus.blobstore.api.BlobStoreMetrics
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

  @Subject
  BlobStoreComponent blobStoreComponent = new BlobStoreComponent(blobStoreManager: blobStoreManager,
      applicationDirectories: applicationDirectories, repositoryManager: repositoryManager)

  def 'Read types returns descriptor data'() {
    given: 'A blobstore descriptor'
      blobStoreComponent.blobstoreDescriptors =
          [MyType: [getName: { -> 'MyType' }, getFormFields: { -> []}] as BlobStoreDescriptor]

    when: 'Reading blobstore types'
      def types = blobStoreComponent.readTypes()

    then: 'The descriptor information is returned'
      types.collect{[it.id, it.name, it.formFields]} == [['MyType', 'MyType', []]]
  }

  def 'Create blobstore creates and returns new blobstore'() {
    given: 'A blobstore create request'
      BlobStoreXO blobStoreXO = new BlobStoreXO(name: 'myblobs', type: 'File',
          attributes: [file: [path: 'path/to/blobs/myblobs']])
      BlobStoreConfiguration expectedConfig = new BlobStoreConfiguration(name: 'myblobs', type: 'File',
         attributes: [file: [path: 'path/to/blobs/myblobs']])
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
}
