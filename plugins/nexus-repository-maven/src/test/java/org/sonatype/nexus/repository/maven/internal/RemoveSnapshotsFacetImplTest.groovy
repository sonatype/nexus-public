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
package org.sonatype.nexus.repository.maven.internal

import org.sonatype.nexus.repository.maven.tasks.RemoveSnapshotsConfig
import org.sonatype.nexus.repository.storage.Component
import org.sonatype.nexus.repository.storage.ComponentEntityAdapter
import org.sonatype.nexus.repository.storage.StorageTx
import org.sonatype.nexus.repository.types.GroupType

import org.joda.time.DateTime
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import static org.sonatype.nexus.repository.maven.internal.RemoveSnapshotsFacetImpl.GAV

class RemoveSnapshotsFacetImplTest
    extends Specification
{
  ComponentEntityAdapter componentEntityAdapter = Mock()

  StorageTx tx = Mock()

  @Subject
  RemoveSnapshotsFacetImpl removeSnapshotsFacet =
      Spy(RemoveSnapshotsFacetImpl, constructorArgs: [componentEntityAdapter, new GroupType(), 500])

  /**
   * Test which GAVs are marked for future processing based on the deletion of Components.
   */
  @Unroll
  def "#desc"() {
    when: 'Processing snapshots'
      def gavs = removeSnapshotsFacet.processSnapshots(null, config, tx)

    then: 'Only affected GAVs are returned for further processing'
      // stubbing out Spy internal methods here to avoid need to overly mock data layer
      removeSnapshotsFacet.findSnapshotCandidates(_, _, _) >> candidates
      removeSnapshotsFacet.findSnapshots(_, _, _) >> comps
      gavs == expected
      1 * removeSnapshotsFacet.processSnapshots(_, _, _)
      dels * tx.deleteComponent(_)
      commits * tx.commit()
      commits * tx.begin()
      0 * _  // no other interactions on Mocks/Spies

    where:
      // @formatter:off
      config | candidates | comps || expected | dels | commits | desc
      config() | []         | []                                              || [] as HashSet | 0 | 0 | 'No candidate snaphshots should result in no GAVs with deletions'
      config() | [gav()]    | [component()]                                   || [] as HashSet | 0 | 0 | 'Not enough candidate snapshots should result in no GAVs deleted'
      config() | [gav(2)]   | [component(), component('1.0-20161110.233023')] || [gav(2)] as HashSet | 1 | 1 | 'If available candidate qualifies (based on lastUpdated) we should see deletions'
      config(1, 1, false, 0) | [gav(2)]   | [component(), component('1.0-20161110.233023')] || [] as HashSet | 0 | 0 | 'If available candidates are disqualified (based on lastUpdated) we should see no deletions'
      config() | [gav(501)] | components(501)                                 || [gav(501)] as HashSet | 500 | 2 | 'More than 500 results should require more than one commit'
      // @formatter:on 
  }
  
  def 'Number of commits are based on batch size and number of GAVs'() {
    given: 'A facet configured with a specific batch size'
      RemoveSnapshotsFacetImpl facet =
          Spy(RemoveSnapshotsFacetImpl, constructorArgs: [componentEntityAdapter, new GroupType(), 2])
      def candidates = [gav(3), gav(4), gav(5)]
      def components = [components(3), components(4), components(5)]
      def expectedDeleteCount = candidates.sum { it.count - 1 }
      def expectedCommitCount = Math.floor(expectedDeleteCount) / 2 + candidates.size()
    
    when: 'When triggered with multiple GAVs'
      def gavs = facet.processSnapshots(null, config(), tx)

    then: 'We expect a commit per GAV and 1 commit each time "batchSize" records are deleted'
      // stubbing out Spy internal methods here to avoid need to overly mock data layer
      facet.findSnapshotCandidates(_, _, _) >> candidates
      facet.findSnapshots(_, _, _) >> components[0] >> components[1] >> components[2]
      1 * facet.processSnapshots(_, _, _)
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

  GAV gav(int count = 1, String baseVersion = '1.0', String group = 'a', String name = 'b') {
    new GAV(group, name, baseVersion, count)
  }

  Component component(String version = '1.0-20160101.000000', String group = 'a', String name = 'b') {
    new Component(group: group, version: version).name(name).lastUpdated(DateTime.now())
  }

  RemoveSnapshotsConfig config(int minimumRetained = 1, int snapshotRetentionDays = 0, boolean removeIfReleased = false,
                               int gracePeriod = 0)
  {
    new RemoveSnapshotsConfig(minimumRetained, snapshotRetentionDays, removeIfReleased, gracePeriod)
  }
}
