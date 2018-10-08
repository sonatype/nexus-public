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

import javax.inject.Provider

import org.sonatype.nexus.blobstore.api.Blob
import org.sonatype.nexus.blobstore.api.BlobId
import org.sonatype.nexus.blobstore.api.BlobStore
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration
import org.sonatype.nexus.blobstore.api.BlobStoreManager
import org.sonatype.nexus.blobstore.group.BlobStoreGroup

import com.google.common.hash.HashCode
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import static java.util.stream.Collectors.toList

/**
 * {@link BlobStoreGroup} tests.
 */
class BlobStoreGroupTest
    extends Specification
{

  BlobStoreManager blobStoreManager = Mock()

  FillPolicy writeToFirstPolicy = new WriteToFirstMemberFillPolicy()

  FillPolicy testFillPolicy = Mock()

  Map<String, Provider<FillPolicy>> fillPolicyFactories = [
      writeToFirst: { -> writeToFirstPolicy} as Provider,
      test: { -> testFillPolicy } as Provider
  ]

  @Shared Blob blobOne = Mock()
  
  @Shared Blob blobTwo = Mock()

  BlobStore one = Mock()

  BlobStore two = Mock()

  BlobStoreGroup blobStore = new BlobStoreGroup(blobStoreManager, fillPolicyFactories)

  def config = new BlobStoreConfiguration()

  def 'Get with no members'() {
    given: 'An empty group'
      config.attributes = [group: [members: [], fillPolicy: 'test']]
      blobStore.init(config)
      blobStore.doStart()
      BlobId blobId = new BlobId('doesntexist')

    when: 'get called for non-existant blob'
      def foundBlob = blobStore.get(blobId)

    then: 'no blob is found'
      foundBlob == null
  }

  @Unroll
  def 'Get with two members with id #blobId'() {
    given: 'A group with two members'
      config.attributes = [group: [members: ['one', 'two'], fillPolicy: 'test']]
      blobStore.init(config)
      blobStore.doStart()
      blobStoreManager.get('one') >> one
      blobStoreManager.get('two') >> two
      one.exists(_) >> { BlobId id -> id == new BlobId('in_one') }
      two.exists(_) >> { BlobId id -> id == new BlobId('in_two') }
      one.get(new BlobId('in_one')) >> blobOne
      two.get(new BlobId('in_two')) >> blobTwo

    when: 'get called for blobId'
      def foundBlob = blobStore.get(new BlobId(blobId))

    then: 'blob is found in blobstore one'
      foundBlob == expectedBlob

    where:
      blobId         || expectedBlob
      'doesntexists' || null
      'in_one'       || blobOne
      'in_two'       || blobTwo
  }

  @Unroll
  def 'Two-param get with two members with id #blobId'() {
    given: 'A group with two members'
      config.attributes = [group: [members: ['one', 'two'], fillPolicy: 'test']]
      blobStore.init(config)
      blobStore.doStart()
      blobStoreManager.get('one') >> one
      blobStoreManager.get('two') >> two
      one.exists(_) >> { BlobId id -> id == new BlobId('in_one') }
      two.exists(_) >> { BlobId id -> id == new BlobId('in_two') }
      one.get(new BlobId('in_one'), false) >> blobOne
      two.get(new BlobId('in_two'), false) >> blobTwo
      two.get(new BlobId('in_one'), true) >> blobOne

    when: 'get called for blobId'
      def foundBlob = blobStore.get(new BlobId(blobId), false)

    then: 'blob is found in blobstore one'
      foundBlob == expectedBlob

    where:
      blobId         || expectedBlob
      'doesntexists' || null
      'in_one'       || blobOne
      'in_two'       || blobTwo
  }

  @Unroll
  def 'Two-param get id #blobId, include deleted: #includeDeleted'() {
    given: 'A group with two members'
      config.attributes = [group: [members: ['one', 'two'], fillPolicy: 'test']]
      blobStore.init(config)
      blobStore.doStart()
      blobStoreManager.get('one') >> one
      blobStoreManager.get('two') >> two
      one.exists(_) >> { BlobId id -> id == new BlobId('in_one') }
      two.exists(_) >> { BlobId id -> id == new BlobId('in_two') || id == new BlobId('deleted_in_two') }
      one.get(new BlobId('in_one'), _) >> blobOne
      two.get(new BlobId('in_two'), _) >> blobTwo
      two.get(new BlobId('deleted_in_two'), true) >> blobTwo

    when: 'get called for blobId'
      def foundBlob = blobStore.get(new BlobId(blobId), includeDeleted)

    then: 'blob is found in blobstore one'
      foundBlob == expectedBlob

    where:
      blobId           | includeDeleted || expectedBlob
      'doesntexists'   | false          || null
      'in_one'         | false          || blobOne
      'in_two'         | false          || blobTwo
      'deleted_in_two' | false          || null
      'doesntexists'   | true           || null
      'in_one'         | true           || blobOne
      'in_two'         | true           || blobTwo
      'deleted_in_two' | true           || blobTwo
  }

  def 'Create with stream delegates to the member chosen by the fill policy'() {
    given: 'A group with two members'
      config.attributes = [group: [members: ['one', 'two'], fillPolicy: 'test']]
      blobStore.init(config)
      blobStore.doStart()
      blobStoreManager.get('one') >> one
      blobStoreManager.get('two') >> two
      def byteStream = new ByteArrayInputStream("".bytes)
      def blob = Mock(Blob)

    when: 'create called'
      blobStore.create(byteStream, [:])

    then:
      1 * testFillPolicy.chooseBlobStore(blobStore, [:]) >> two
      0 * one.create(_, _)
      1 * two.create(byteStream, [:]) >> blob
      blob.getId() >> new BlobId('created')
  }

  def 'Create with path delegates to the member chosen by the fill policy'() {
    given: 'A group with two members'
      config.attributes = [group: [members: ['one', 'two'], fillPolicy: 'test']]
      blobStore.init(config)
      blobStore.doStart()
      blobStoreManager.get('one') >> one
      blobStoreManager.get('two') >> two
      def path = new File(".").toPath()
      def size = 0l
      def hashCode = HashCode.fromInt(0)
      def blob = Mock(Blob)

    when: 'create called'
      blobStore.create(path, [:], size, hashCode)

    then:
      1 * testFillPolicy.chooseBlobStore(blobStore, [:]) >> two
      0 * one.create(_, _)
      1 * two.create(path, [:], size, hashCode) >> blob
      blob.getId() >> new BlobId('created')
  }

  def 'getBlobStreamId with two blobstores'() {
    given: 'A group with two members'
      config.attributes = [group: [members: ['one', 'two'], fillPolicy: 'test']]
      blobStore.init(config)
      blobStore.doStart()
      blobStoreManager.get('one') >> one
      blobStoreManager.get('two') >> two
      one.getBlobIdStream() >> [new BlobId('a'), new BlobId('b'), new BlobId('c')].stream()
      two.getBlobIdStream() >> [new BlobId('d'), new BlobId('e'), new BlobId('f')].stream()

      blobStore.init(config)
      blobStore.doStart()

    when: 'getBlobIdStream called'
      def stream = blobStore.getBlobIdStream()

    then: 'stream is concatenated streams of members'
      stream.collect(toList())*.toString() == ['a', 'b', 'c', 'd', 'e', 'f']
  }

  @Unroll
  def 'delete with two members with id #blobId'() {
    given: 'A group with two members'
      config.attributes = [group: [members: ['one', 'two'], fillPolicy: 'test']]
      blobStore.init(config)
      blobStore.doStart()
      blobStoreManager.get('one') >> one
      blobStoreManager.get('two') >> two
      one.exists(_) >> { BlobId id -> id == new BlobId('in_one') || id == new BlobId('in_both') }
      two.exists(_) >> { BlobId id -> id == new BlobId('in_two') || id == new BlobId('in_both') }
      one.delete(new BlobId('in_one'), _) >> { println "x" ; true }
      one.delete(new BlobId('in_both'), _) >> { println "y" ; true }
      two.delete(new BlobId('in_two'), _) >> { println "z" ; true }
      two.delete(new BlobId('in_both'), _) >> { println "w" ;  false }

    when: 'delete called for blobId'
      println "deleting $blobId"
      def deleted = blobStore.delete(new BlobId(blobId), 'just because')

    then: 'blob is found in blobstore one'
      deleted == expectedDeleted

    where:
      blobId         || expectedDeleted
      'doesntexists' || false
      'in_one'       || true
      'in_two'       || true
      'in_both'      || false // not deleted from two
  }

  @Unroll
  def 'delete hard with two members with id #blobId'() {
    given: 'A group with two members'
      config.attributes = [group: [members: ['one', 'two'], fillPolicy: 'test']]
      blobStore.init(config)
      blobStore.doStart()
      blobStoreManager.get('one') >> one
      blobStoreManager.get('two') >> two
      one.exists(_) >> { BlobId id -> id == new BlobId('in_one') || id == new BlobId('in_both') }
      two.exists(_) >> { BlobId id -> id == new BlobId('in_two') || id == new BlobId('in_both') }
      one.deleteHard(new BlobId('in_one')) >> { println "x" ; true }
      one.deleteHard(new BlobId('in_both')) >> { println "y" ; true }
      two.deleteHard(new BlobId('in_two')) >> { println "z" ; true }
      two.deleteHard(new BlobId('in_both')) >> { println "w" ;  false }

    when: 'delete called for blobId'
      println "deleting $blobId"
      def deleted = blobStore.deleteHard(new BlobId(blobId))

    then: 'blob is found in blobstore one'
      deleted == expectedDeleted

    where:
      blobId         || expectedDeleted
      'doesntexists' || false
      'in_one'       || true
      'in_two'       || true
      'in_both'      || false // not deleted from two
  }

  def 'fall back on default fill policy if named policy not found'() {
    given: 'a config with a bad fill policy name'
      config.attributes = [group: [members: ['one', 'two'], fillPolicy: 'nosuch']]

    when: 'the blob store is initialized'
      blobStore.init(config)

    then: 'a fall back fill policy is used'
      blobStore.fillPolicy instanceof WriteToFirstMemberFillPolicy
  }
}
