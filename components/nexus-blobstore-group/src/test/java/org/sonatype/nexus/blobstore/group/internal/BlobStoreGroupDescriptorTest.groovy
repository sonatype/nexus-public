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

import spock.lang.Specification
import spock.lang.Unroll

/**
 * {@link BlobStoreGroupDescriptor} tests.
 */
class BlobStoreGroupDescriptorTest
    extends Specification
{

  BlobStoreManager blobStoreManager = Mock()

  BlobStoreGroupDescriptor blobStoreGroupDescriptor = new BlobStoreGroupDescriptor(true, blobStoreManager)

  def blobStores = [:]

  def setup() {
    blobStoreManager.get(_) >> { String name -> blobStores.computeIfAbsent(name, { k -> mockBlobStore(k, 'mock') }) }
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
      blobStores.nested = mockBlobStore('nested', BlobStoreGroup.TYPE, [group: [members: ['self']]])

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
      ['nested'] || '''Blob Store 'self' cannot contain itself'''
  }

  private BlobStore mockBlobStore(final String name, final String type, attributes = [:]) {
    def blobStore = Mock(BlobStore)
    def config = new BlobStoreConfiguration()
    blobStore.getBlobStoreConfiguration() >> config
    config.name = name
    config.type = type
    config.attributes = attributes
    blobStore
  }
}
