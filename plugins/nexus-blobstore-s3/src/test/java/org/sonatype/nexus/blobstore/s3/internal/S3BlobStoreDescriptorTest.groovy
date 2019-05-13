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
package org.sonatype.nexus.blobstore.s3.internal

import javax.validation.ValidationException

import org.sonatype.nexus.blobstore.api.BlobStore
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration
import org.sonatype.nexus.blobstore.api.BlobStoreManager
import org.sonatype.nexus.blobstore.quota.BlobStoreQuotaService


import spock.lang.Specification
import spock.lang.Unroll

import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.BUCKET_PREFIX_KEY
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.CONFIG_KEY

/**
 * {@link S3BlobStoreDescriptor} tests.
 */
class S3BlobStoreDescriptorTest
    extends Specification
{
  BlobStoreQuotaService quotaService = Mock()

  BlobStoreManager blobStoreManager = Mock()

  S3BlobStoreDescriptor s3BlobStoreDescriptor = new S3BlobStoreDescriptor(quotaService, blobStoreManager)

  def blobStores = [:]

  def setup() {
    blobStoreManager.get(_) >> { String name -> blobStores.computeIfAbsent(name, { k -> mockBlobStore(k, 'mock') }) }
    blobStoreManager.browse() >> blobStores.values()
  }

  def 'a S3 blob store validates its quota'() {
    given:
      def config = new BlobStoreConfiguration()

    when: 'attempting to create a S3 blob store'
      s3BlobStoreDescriptor.validateConfig(config)

    then: 'quota validity is checked'
      1 * quotaService.validateSoftQuotaConfig(*_)
  }

  def 'a single s3 configuration is valid'() {
    given: 'A config'
      def config = new BlobStoreConfiguration()

    when: 'the config is validated'
      config.name = 'self'
      config.attributes = [s3: [bucket: 'bucket']]
      s3BlobStoreDescriptor.validateConfig(config)

    then: 'validate succeeds'
      noExceptionThrown()
  }

  def 'a config that shares a bucket with non-overlapping prefixes is valid'() {
    given: 'A config'
      def config = new BlobStoreConfiguration()
      blobStores.other = mockBlobStore('other', S3BlobStore.TYPE, [s3: [bucket: 'bucket', prefix: 'prefix']])

    when: 'the config is validated'
      config.name = 'self'
      config.attributes = [s3: [bucket: 'bucket', prefix: 'foo']]
      s3BlobStoreDescriptor.validateConfig(config)

    then: 'validate succeeds'
      noExceptionThrown()
  }

  def 'a config that shares a bucket with no prefix is invalid'() {
    given: 'A config'
      def config = new BlobStoreConfiguration()
      blobStores.other = mockBlobStore('other', S3BlobStore.TYPE, [s3: [bucket: 'foobar']])

    when: 'the config is validated'
      config.name = 'self'
      config.attributes = [s3: [bucket: 'foobar']]
      s3BlobStoreDescriptor.validateConfig(config)

    then: 'a validation exception is thrown'
      def exception = thrown(ValidationException)
      exception.message == "Blob Store 'other' is already using bucket 'foobar'"
  }

  @Unroll
  def 'a config that shares a bucket with overlapping prefixes #existingPrefix and #newPrefix invalid'() {
    given: 'A config'
      def config = new BlobStoreConfiguration()

    when: 'the config is validated'
      blobStores.other = mockBlobStore('other', S3BlobStore.TYPE, [s3: [bucket: 'bucket', prefix: existingPrefix]])
      config.name = 'self'
      config.attributes = [s3: [bucket: 'bucket', prefix: newPrefix]]
      s3BlobStoreDescriptor.validateConfig(config)

    then: 'a validation exception is thrown'
      def exception = thrown(ValidationException)
      exception.message == ("Blob Store 'other' is already using bucket 'bucket' with prefix '$existingPrefix'")

    where:
      existingPrefix | newPrefix
      'foo'          | 'foo'
      ''             | 'foo'
      'foo'          | ''
      'foo/bar'      | 'foo'
      'foo'          | 'foo/bar'
  }

  @Unroll
  def 'a config that shares a bucket with non-overlapping prefixes #existingPrefix and #newPrefix valid'() {
    given: 'A config'
      def config = new BlobStoreConfiguration()

    when: 'the config is validated'
      blobStores.other = mockBlobStore('other', S3BlobStore.TYPE, [s3: [bucket: 'bucket', prefix: existingPrefix]])
      config.name = 'self'
      config.attributes = [s3: [bucket: 'bucket', prefix: newPrefix]]
      s3BlobStoreDescriptor.validateConfig(config)

    then: 'a validation exception is not thrown'
      noExceptionThrown()

    where:
      existingPrefix | newPrefix
      'foo'          | 'bar'
      'foo'          | 'bar/foo'
      'foo'          | 'foo_bar'
  }

  def 'a config that shares a bucket name with different endpoints is valid'() {
    given: 'A config'
      def config = new BlobStoreConfiguration()
      blobStores.other = mockBlobStore('other', S3BlobStore.TYPE, [s3: [bucket: 'bucket', endpoint: 'aws']])

    when: 'the config is validated'
      config.name = 'self'
      config.attributes = [s3: [bucket: 'bucket', endpoint: 'non-aws']]
      s3BlobStoreDescriptor.validateConfig(config)

    then: 'validate succeeds'
      noExceptionThrown()
  }

  @Unroll
  def 'It will transform #prefix into #expected by trimming and collapsing duplicate slashes'() {
    given: 'an S3 blob store config with prefix'
      def config = new BlobStoreConfiguration()
      config.attributes(CONFIG_KEY).set(BUCKET_PREFIX_KEY, prefix)
    when: 'the config is sanitized'
      s3BlobStoreDescriptor.sanitizeConfig(config)
    then: 'the prefix will be as expected'
      config.attributes(CONFIG_KEY).get(BUCKET_PREFIX_KEY, String) == expected

    where:
      prefix           | expected
      null             | ''
      ''               | ''
      ' '              | ' '
      '/test'          | 'test'
      '/test/'         | 'test'
      ' /test/ '       | 'test'
      '/ test /'       | 'test'
      '///test///'     | 'test'
      '///te/st///'    | 'te/st'
      'te////st'       | 'te/st'
      '///te////st///' | 'te/st'
      '//////'         | ''
  }

  private BlobStore mockBlobStore(final String name,
                                  final String type,
                                  attributes = [:]) {
    def blobStore = Mock(BlobStore)
    def config = new BlobStoreConfiguration()
    blobStore.getBlobStoreConfiguration() >> config
    config.name = name
    config.type = type
    config.attributes = attributes
    blobStore
  }
}
