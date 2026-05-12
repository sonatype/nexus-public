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
package org.sonatype.nexus.scheduling.internal.upgrade.datastore;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Map;

import org.sonatype.nexus.testdb.DataSessionConfiguration;

import org.sonatype.nexus.testdb.TestDataSessionSupplier;

import com.google.common.collect.Iterables;
import org.sonatype.nexus.testdb.DatabaseTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

class UpgradeTaskDAOTest

{
  @DataSessionConfiguration(daos = UpgradeTaskDAO.class)
  TestDataSessionSupplier dataSession;

  @DatabaseTest
  void testBrowse() {
    dataSession.callDAO(UpgradeTaskDAO.class, dao -> {
      dao.create(new UpgradeTaskData("task-one", Map.of()));
      dao.create(new UpgradeTaskData("task-two", Map.of()));
    });

    Collection<UpgradeTaskData> tasks =
        (Collection<UpgradeTaskData>) dataSession.withDAO(UpgradeTaskDAO.class, dao -> dao.browse());

    assertThat(tasks, hasSize(2));
    assertThat(tasks.stream().map(UpgradeTaskData::getTaskId).toList(), contains("task-one", "task-two"));
  }

  @DatabaseTest
  void testBrowseBefore() {
    dataSession.callDAO(UpgradeTaskDAO.class, dao -> dao.create(new UpgradeTaskData("task-one", Map.of())));

    OffsetDateTime before = OffsetDateTime.now();

    dataSession.callDAO(UpgradeTaskDAO.class, dao -> dao.create(new UpgradeTaskData("task-two", Map.of())));

    Collection<UpgradeTaskData> tasks =
        (Collection<UpgradeTaskData>) dataSession.withDAO(UpgradeTaskDAO.class, dao -> dao.browseBefore(before));

    assertThat(tasks, hasSize(1));
    assertThat(Iterables.getOnlyElement(tasks).getTaskId(), is("task-one"));
  }
}
