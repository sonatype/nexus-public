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
package org.sonatype.nexus.upgrade.internal;

import java.sql.Connection;

import org.sonatype.nexus.testdb.DataSessionConfiguration;
import org.sonatype.nexus.testdb.TestDataSessionSupplier;

import org.sonatype.nexus.testdb.DatabaseTest;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoInteractions;

class DistributedEventsUpgradeTest
{
  @DataSessionConfiguration(daos = {})
  TestDataSessionSupplier supplier;

  @DatabaseTest
  void shouldBeNoOp() throws Exception {
    DistributedEventsUpgrade underTest = new DistributedEventsUpgrade();
    try (Connection conn = supplier.openConnection()) {
      Connection spyConn = spy(conn);
      underTest.migrate(spyConn);
      verifyNoInteractions(spyConn);
    }
  }
}
