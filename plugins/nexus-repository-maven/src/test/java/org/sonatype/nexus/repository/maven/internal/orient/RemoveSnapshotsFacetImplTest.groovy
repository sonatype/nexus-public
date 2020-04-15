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
package org.sonatype.nexus.repository.maven.internal.orient

import org.sonatype.nexus.common.collect.NestedAttributesMap
import org.sonatype.nexus.common.entity.EntityMetadata
import org.sonatype.nexus.orient.entity.AttachedEntityMetadata
import org.sonatype.nexus.orient.entity.EntityAdapter
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.maven.internal.orient.RemoveSnapshotsFacetImpl.GAV
import org.sonatype.nexus.repository.maven.tasks.RemoveSnapshotsConfig
import org.sonatype.nexus.repository.storage.Bucket
import org.sonatype.nexus.repository.storage.Component
import org.sonatype.nexus.repository.storage.ComponentEntityAdapter
import org.sonatype.nexus.repository.storage.DefaultComponent
import org.sonatype.nexus.repository.storage.StorageTx
import org.sonatype.nexus.repository.types.GroupType
import org.sonatype.nexus.transaction.UnitOfWork

import com.google.common.collect.Maps
import com.orientechnologies.orient.core.id.ORID
import com.orientechnologies.orient.core.id.ORecordId
import com.orientechnologies.orient.core.record.impl.ODocument
import org.joda.time.DateTime
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import static org.sonatype.nexus.repository.maven.internal.Attributes.P_BASE_VERSION
import static org.sonatype.nexus.repository.maven.internal.Maven2Format.NAME
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_ATTRIBUTES

class RemoveSnapshotsFacetImplTest
    extends Specification
{
  ComponentEntityAdapter componentEntityAdapter = Mock()

  StorageTx tx = Mock()

  Repository repository = Stub()

  Bucket bucket = Mock()

  AttachedEntityMetadata entityMetadata = Mock(AttachedEntityMetadata.class)

  ORID bucketId = new ORecordId(1, 1)

  @Subject
  RemoveSnapshotsFacetImpl removeSnapshotsFacet =
      Spy(RemoveSnapshotsFacetImpl, constructorArgs: [componentEntityAdapter, new GroupType(), 500])

  def setup() {
    UnitOfWork.beginBatch(tx)
    repository.getName() >> "test"
  }

  def cleanup() {
    UnitOfWork.end()
  }

  /**
   * Test processRepository: which GAVs are marked for future processing based on the deletion of Components.
   */
  @Unroll
  def "#desc"() {
    when: 'Processing snapshots'
      def gavs = removeSnapshotsFacet.processRepository(repository, config)

    then: 'Only affected GAVs are returned for further processing'
      // stubbing out Spy internal methods here to avoid need to overly mock data layer
      removeSnapshotsFacet.findSnapshotCandidates(_, _) >> candidates
      candidates.size() * removeSnapshotsFacet.findComponentsForGav(_, _, _) >> comps
      removeSnapshotsFacet.getSnapshotsToDelete(_, _) >>
          comps // return the same set of components for this test. getSnapshotsToDelete is another test.
      gavs == expected
      1 * removeSnapshotsFacet.processRepository(_, _)
      dels * tx.deleteComponent(_)
      commits * tx.commit()
      commits * tx.begin()
      0 * _  // no other interactions on Mocks/Spies

    where:
      // @formatter:off
      desc                                                        | config   | candidates | comps                                           | expected              | dels | commits
      'No candidates results in no deletions'                     | config() | []         | []                                              | [] as HashSet         | 0    | 1
      'A candidate with no components results in no deletions'    | config() | [gav()]    | []                                              | [] as HashSet         | 0    | 1
      'A candidate with 1 component results in 1 deletion'        | config() | [gav()]    | [component()]                                   | [gav(1)] as HashSet   | 1    | 1
      'A candidate with 2 component results in 2 deletions'       | config() | [gav(2)]   | [component(), component('1.0-20161110.233023')] | [gav(2)] as HashSet   | 2    | 1
      'More than 500 results should require more than one commit' | config() | [gav(501)] | components(501)                                 | [gav(501)] as HashSet | 501  | 2
      // @formatter:on 
  }

  @Shared
  def testGavWithRelease = [// purposefully out of order to test sorting
                             component('1.0-20160220.000001', 10),
                             component('1.0-20160301.000001', 1),
                             component('1.0-20160101.000001', 60),
                             component('1.0-20160228.000001', 2),
                             component('1.0', 0, '1.0'), // release artifact
                             component('1.0-20160228.000002', 2), // 2nd artifact on day 2
                             component('1.0-20160201.000001', 30)] as HashSet

  /**
   * Test getSnapshotsToDelete: which components for a GAV should actually be deleted
   */
  @Unroll
  def '#description'() {
    when: 'Processing snapshots to delete'
      def snapshotsToDelete = removeSnapshotsFacet.getSnapshotsToDelete(config, components)

    then: 'Only appropriate snaphots are returned for deletions'
      snapshotsToDelete.size() == expectedDeletions.size()
      if (expectedDeletions) {
        expectedDeletions.each {
          def component = snapshotsToDelete.find { c -> c.version() == it }
          assert component?.version() == it
        }
      }
    where:
      // @formatter:off
      description                                | config                  | components                                                                                             | expectedDeletions
      'empty set ok'                             | config()                | [] as HashSet                                                                                          | []
      'release with no snapshots is ok'          | config()                | [component('1.0', 0, '1.0')] as HashSet                                                                | []
      'minimum snapshot count of 1 deletes 5'    | config(1)               | testGavWithRelease                                                                                     | ['1.0-20160101.000001', '1.0-20160201.000001', '1.0-20160220.000001', '1.0-20160228.000001', '1.0-20160228.000002']
      'minimum snapshot count of 2 deletes 4'    | config(2)               | testGavWithRelease                                                                                     | ['1.0-20160101.000001', '1.0-20160201.000001', '1.0-20160220.000001', '1.0-20160228.000001']
      'minimum snapshot count of -1 retains all' | config(-1)              | testGavWithRelease                                                                                     | []
      'snapshot retention of 3 days deletes 3'   | config(1, 3)            | testGavWithRelease                                                                                     | ['1.0-20160101.000001', '1.0-20160201.000001', '1.0-20160220.000001']
      'snapshot retention of 3 days, but keep 5' | config(5, 3)            | testGavWithRelease                                                                                     | ['1.0-20160101.000001']
      'remove if released, no grace deletes all' | config(3, 0, true)      | testGavWithRelease                                                                                     | ['1.0-20160101.000001', '1.0-20160201.000001', '1.0-20160220.000001', '1.0-20160228.000001', '1.0-20160228.000002', '1.0-20160301.000001']
      'remove if released, grace of 2 deletes 5' | config(3, 0, true, 2)   | testGavWithRelease                                                                                     | ['1.0-20160101.000001', '1.0-20160201.000001', '1.0-20160220.000001', '1.0-20160228.000001', '1.0-20160228.000002']
      'config scenario 1 day is 30 deleted'      | config(2, 25, true, 40) | testGavWithRelease                                                                                     | ['1.0-20160101.000001', '1.0-20160201.000001']
      'config scenario 2 day is 30 deleted'      | config(2, 40, true, 25) | testGavWithRelease                                                                                     | ['1.0-20160101.000001', '1.0-20160201.000001']
      'remove if released only, 1 deleted'       | config(-1, 0, true, 0)  | [component('1.0-20160101', 0, '1.0-SNAPSHOT'),component('1.0', 0, '1.0')] as HashSet                   | ['1.0-20160101']
      'remove if released only with no release'  | config(-1, 0, true, 0)  | [component('2.0-20160101', 0, '2.0-SNAPSHOT'),component('2.0-20160102', 0, '2.0-SNAPSHOT')] as HashSet | []
      // @formatter:on
  }

  def 'Number of commits are based on batch size and number of GAVs'() {
    given: 'A facet configured with a specific batch size'
      RemoveSnapshotsFacetImpl facet =
          Spy(RemoveSnapshotsFacetImpl, constructorArgs: [componentEntityAdapter, new GroupType(), 2])
      def candidates = [gav(3), gav(4), gav(5)]
      def components = [components(3), components(4), components(5)]
      def expectedDeleteCount = candidates.sum { it.count - 1 }
      def expectedCommitCount = Math.floor(expectedDeleteCount) / 2 + 1
    
    when: 'When triggered with multiple GAVs'
      def gavs = facet.processRepository(repository, config())

    then: 'We expect a commit per GAV and 1 commit each time "batchSize" records are deleted'
      // stubbing out Spy internal methods here to avoid need to overly mock data layer
      facet.findSnapshotCandidates(_, _) >> candidates
      facet.findComponentsForGav(_, _, _) >> components[0] >> components[1] >> components[2]
      // for components to delete, return components found minus 1
      facet.getSnapshotsToDelete(_, _) >> components[0].subList(1, 3) >> components[1].subList(1, 4) >>
          components[2].subList(1, 5)

      1 * facet.processRepository(_, _)
      gavs == candidates as HashSet
      candidates.sum { it.count - 1 } * tx.deleteComponent(_) // leave one per GAV
      expectedCommitCount * tx.commit() // 1 commit/begin per involved GAV + 1 for every 2 deletions 
      expectedCommitCount * tx.begin()
      0 * _  // no other interactions on Mocks/Spies
  }

  def components(final int i) {
    def results = []
    i.times {
      results << component()
    }
    results
  }

  GAV gav(int count = 1, String baseVersion = '1.0-SNAPSHOT', String group = 'a', String name = 'b') {
    new GAV(group, name, baseVersion, count)
  }

  Component component(String version = '1.0-20160101.000000', int lastUpdatedAge = 0,
                      String baseVersion = '1.0-SNAPSHOT', String group = 'a', String name = 'b')
  {

    NestedAttributesMap attributes = new NestedAttributesMap(P_ATTRIBUTES, Maps.<String, Object> newHashMap())
    attributes.child(NAME).set(P_BASE_VERSION, baseVersion)

    // add five minutes to avoid timing issues with fast test executions where the timestamp might end up being the same
    new DefaultComponent(group: group, version: version).name(name).attributes(attributes).
        lastUpdated(DateTime.now().minusDays(lastUpdatedAge).minusMinutes(5))
  }

  RemoveSnapshotsConfig config(int minimumRetained = 1, int snapshotRetentionDays = 0, boolean removeIfReleased = false,
                               int gracePeriod = 0)
  {
    new RemoveSnapshotsConfig(minimumRetained, snapshotRetentionDays, removeIfReleased, gracePeriod)
  }

  EntityMetadata mockBucketEntityMetadata() {
    EntityAdapter owner = Mock(EntityAdapter.class)
    ODocument document = Mock(ODocument.class)
    ORID orID = new ORecordId(1, 1)
    document.getIdentity() >> orID
    EntityMetadata entityMetadata = new AttachedEntityMetadata(owner, document)
    return entityMetadata
  }
}
