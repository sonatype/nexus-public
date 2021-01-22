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
package org.sonatype.nexus.repository.maven.internal.content

import java.time.OffsetDateTime

import org.sonatype.nexus.common.collect.NestedAttributesMap
import org.sonatype.nexus.content.maven.MavenContentFacet
import org.sonatype.nexus.content.maven.internal.snapshot.RemoveSnapshotsFacetImpl
import org.sonatype.nexus.content.maven.store.GAV
import org.sonatype.nexus.content.maven.store.Maven2ComponentData
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.maven.tasks.RemoveSnapshotsConfig
import org.sonatype.nexus.repository.types.GroupType

import com.google.common.collect.Maps
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
  Repository repository = Stub()

  MavenContentFacet facet = Mock()

  @Subject
  RemoveSnapshotsFacetImpl removeSnapshotsFacet =
      Spy(RemoveSnapshotsFacetImpl, constructorArgs: [new GroupType()])

  def setup() {
    repository.getName() >> "test"
    repository.facet(MavenContentFacet.class) >> facet
  }

  /**
   * Test processRepository: which GAVs are marked for future processing based on the deletion of Components.
   */
  @Unroll
  def "#desc"() {
    when: 'Processing snapshots'
      removeSnapshotsFacet.processRepository(repository, config)

    then: 'Only affected GAVs are returned for further processing'
      // stubbing out Spy internal methods here to avoid need to overly mock data layer
      removeSnapshotsFacet.findSnapshotCandidates(_, _) >> candidates
      candidates.size() * removeSnapshotsFacet.findComponentsForGav(_, _) >> comps
      removeSnapshotsFacet.getSnapshotsToDelete(_, _) >>
          comps // return the same set of components for this test. getSnapshotsToDelete is another test.
      1 * removeSnapshotsFacet.processRepository(_, _)
      0 * facet.deleteSnapshotsForReleasedComponents(_, _)
      dels * facet.deleteComponents(_)

    where:
      desc                                                        | config   | candidates | comps                                           | dels
      'No candidates results in no deletions'                     | config() | []         | []                                              | 0
      'A candidate with no components results in no deletions'    | config() | [gav()]    | []                                              | 0
      'A candidate with 1 component results in 1 deletion'        | config() | [gav()]    | [component()]                                   | 1
      'A candidate with 2 component results in 1 deletions'       | config() | [gav(2)]   | [component(), component('1.0-20161110.233023')] | 1
  }

  @Shared
  def testGavWithRelease = [// purposefully out of order to test sorting
                            component('1.0-20160301.000001', 1),
                            component('1.0-20160228.000002', 2), // 2nd artifact on 2016-02-28
                            component('1.0-20160228.000001', 2),
                            component('1.0-20160220.000001', 10),
                            component('1.0-20160201.000001', 30),
                            component('1.0-20160101.000001', 60)] as ArrayList// release artifact

  ///**
  // * Test getSnapshotsToDelete: which components for a GAV should actually be deleted
  // **/
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
      'empty set ok'                             | config()                | [] as ArrayList                                                                                          | []
      'release with no snapshots is ok'          | config()                | [component('1.0', 0, '1.0')] as ArrayList                                                                | []
      'minimum snapshot count of 1 deletes 5'    | config(1)               | testGavWithRelease                                                                                     | ['1.0-20160101.000001', '1.0-20160201.000001', '1.0-20160220.000001', '1.0-20160228.000001', '1.0-20160228.000002']
      'minimum snapshot count of 2 deletes 4'    | config(2)               | testGavWithRelease                                                                                     | ['1.0-20160101.000001', '1.0-20160201.000001', '1.0-20160220.000001', '1.0-20160228.000001']
      'minimum snapshot count of -1 retains all' | config(-1)              | testGavWithRelease                                                                                     | []
      'snapshot retention of 3 days deletes 3'   | config(1, 3)            | testGavWithRelease                                                                                     | ['1.0-20160101.000001', '1.0-20160201.000001', '1.0-20160220.000001']
      'snapshot retention of 3 days, but keep 5' | config(5, 3)            | testGavWithRelease                                                                                     | ['1.0-20160101.000001']
      'remove if released, no grace deletes all' | config(3, 0, true)      | testGavWithRelease                                                                                     | ['1.0-20160101.000001', '1.0-20160201.000001', '1.0-20160220.000001']
      'remove if released, grace of 2 deletes 5' | config(3, 0, true, 2)   | testGavWithRelease                                                                                     | ['1.0-20160101.000001', '1.0-20160201.000001', '1.0-20160220.000001']
      'config scenario 1 day is 30 deleted'      | config(2, 25, true, 40) | testGavWithRelease                                                                                     | ['1.0-20160101.000001', '1.0-20160201.000001']
      'config scenario 2 day is 30 deleted'      | config(2, 40, true, 25) | testGavWithRelease                                                                                     | ['1.0-20160101.000001']
      'remove if released only, 1 deleted'       | config(-1, 0, true, 0)  | [component('1.0-20160101', 0, '1.0-SNAPSHOT')] as ArrayList                   | []
      'remove if released only with no release'  | config(-1, 0, true, 0)  | [component('2.0-20160101', 0, '2.0-SNAPSHOT'),component('2.0-20160102', 0, '2.0-SNAPSHOT')] as ArrayList | []
      // @formatter:on
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

  Maven2ComponentData component(String version = '1.0-20160101.000000', int lastUpdatedAge = 0,
                                String baseVersion = '1.0-SNAPSHOT', String group = 'a', String name = 'b')
  {

    NestedAttributesMap attributes = new NestedAttributesMap(P_ATTRIBUTES, Maps.<String, Object> newHashMap())
    attributes.child(NAME).set(P_BASE_VERSION, baseVersion)
    Maven2ComponentData component = new Maven2ComponentData()
    component.setComponentId(1)
    component.setName(name)
    component.setNamespace(group)
    component.setVersion(version)
    component.setAttributes(attributes)
    // add five minutes to avoid timing issues with fast test executions where the timestamp might end up being the same
    component.setLastUpdated(OffsetDateTime.now().minusDays(lastUpdatedAge).minusMinutes(5))
    return component

  }

  RemoveSnapshotsConfig config(int minimumRetained = 1, int snapshotRetentionDays = 0, boolean removeIfReleased = false,
                               int gracePeriod = 0)
  {
    new RemoveSnapshotsConfig(minimumRetained, snapshotRetentionDays, removeIfReleased, gracePeriod)
  }
}
