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

  StorageTx tx = Mock ()

  @Subject
  RemoveSnapshotsFacetImpl removeSnapshotsFacet =
      Spy(RemoveSnapshotsFacetImpl, constructorArgs: [componentEntityAdapter, new GroupType()])
  
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

    where:
      config                 | candidates | comps                                           || expected      | desc
      config(1, 0, false, 0) | []         | []                                              || [] as HashSet |
          'No candidate snaphshots should result in no GAVs with deletions'
      config(1, 0, false, 0) | [gav()]    | [component()]                                   || [] as HashSet |
          'Not enough candidate snapshots should result in no GAVs deleted'
      config(1, 0, false, 0) | [gav(2)]   | [component(), component('1.0-20161110.233023')] ||
          [gav(2)] as HashSet                                                                               |
          'If available candidate qualifies (based on lastUpdated) we should see deletions'
      config(1, 1, false, 0) | [gav(2)]   | [component(), component('1.0-20161110.233023')] || [] as HashSet |
          'If available candidates are disqualified (based on lastUpdated) we should see no deletions'
  }

  GAV gav(int count = 1, String baseVersion = '1.0', String group = 'a', String name = 'b') {
    new GAV(group, name, baseVersion, count)
  }

  Component component(String version = '1.0-20160101.000000', String group = 'a', String name = 'b') {
    new Component(group: group, version: version).name(name).lastUpdated(DateTime.now())
  }

  RemoveSnapshotsConfig config(int minimumRetained, int snapshotRetentionDays, boolean removeIfReleased,
                               int gracePeriod)
  {
    new RemoveSnapshotsConfig(minimumRetained, snapshotRetentionDays, removeIfReleased, gracePeriod)
  }
}
