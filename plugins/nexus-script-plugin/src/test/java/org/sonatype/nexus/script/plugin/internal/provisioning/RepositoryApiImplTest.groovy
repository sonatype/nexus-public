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
package org.sonatype.nexus.script.plugin.internal.provisioning

import org.sonatype.nexus.blobstore.MockBlobStoreConfiguration
import org.sonatype.nexus.blobstore.api.BlobStore
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration
import org.sonatype.nexus.blobstore.api.BlobStoreManager
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.config.Configuration
import org.sonatype.nexus.repository.manager.RepositoryManager

import spock.lang.Specification
import spock.lang.Subject

/**
 * @since 3.0
 */
class RepositoryApiImplTest
    extends Specification
{
  RepositoryManager repositoryManager = Mock()

  BlobStoreManager blobStoreManager = Mock()

  @Subject
  RepositoryApiImpl api = new RepositoryApiImpl(repositoryManager: repositoryManager,
      blobStoreManager: blobStoreManager)

  void 'Cannot validate a BlobStore that does not exist'() {
    when:
      api.validateBlobStore(configWithAttributes([storage: [blobStoreName: 'foo']]))

    then:
      1 * blobStoreManager.browse() >> []
      thrown IllegalArgumentException
  }

  void 'Can validate given an existing BlobStore'() {
    given:
      BlobStore blobStore = Mock()
      BlobStoreConfiguration configuration = new MockBlobStoreConfiguration(name: 'foo')
    
    when:
      api.validateBlobStore(configWithAttributes([storage: [blobStoreName: 'foo']]))

    then:
      1 * blobStoreManager.browse() >> [blobStore]
      1 * blobStore.blobStoreConfiguration >> configuration
  }

  void 'Group memberNames must be a Collection'() {
    when: 'Accidentally using a String instead of a Collection'
      api.validateGroupMembers(configWithAttributes([group:[memberNames: 'foo']]))

    then: 'An exception is thrown during validation'
      thrown ClassCastException
  }
  
  void 'Cannot validate a group that contains non-existent members'() {
    when: 'Including a member that does not exist'
      api.validateGroupMembers(configWithAttributes([group:[memberNames: ['foo']]]))

    then: 'An exception is thrown during validation'
      1 * repositoryManager.browse() >> []
      thrown IllegalStateException
  }

  void 'Can validate a group with existing members'() {
    given:
     Repository repository = Mock()

    when:
      api.validateGroupMembers(configWithAttributes([group:[memberNames: ['foo']]]))

    then:
      1 * repositoryManager.browse() >> [repository]
      repository.name >> 'foo'
  }

  void 'Non-group repositories pass group validation trivially'() {
    when:
      api.validateGroupMembers(configWithAttributes([:]))
    then:
      true
  }

  private Configuration configWithAttributes(final Map attributes) {
    Configuration config = Mock()
    config.getAttributes() >> attributes
    return config
  }
}
