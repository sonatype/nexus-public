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
package org.sonatype.nexus.common.app.softdelete;

import java.sql.SQLException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoftDeleteMigrationHelperTest
{
  @Test
  void isMissingDeletedOnColumn_postgresqlErrorMessage_returnsTrue() {
    SQLException e = new SQLException("ERROR: column \"deleted_on\" does not exist");
    assertTrue(SoftDeleteMigrationHelper.isMissingDeletedOnColumn(e));
  }

  @Test
  void isMissingDeletedOnColumn_h2ErrorMessage_returnsTrue() {
    SQLException e = new SQLException("Column \"DELETED_ON\" not found; SQL statement: ...");
    assertTrue(SoftDeleteMigrationHelper.isMissingDeletedOnColumn(e));
  }

  @Test
  void isMissingDeletedOnColumn_nestedCause_returnsTrue() {
    SQLException inner = new SQLException("ERROR: column \"deleted_on\" does not exist");
    RuntimeException outer = new RuntimeException("Query failed", inner);
    assertTrue(SoftDeleteMigrationHelper.isMissingDeletedOnColumn(outer));
  }

  @Test
  void isMissingDeletedOnColumn_unrelatedError_returnsFalse() {
    SQLException e = new SQLException("ERROR: relation \"foo\" does not exist");
    assertFalse(SoftDeleteMigrationHelper.isMissingDeletedOnColumn(e));
  }

  @Test
  void isMissingDeletedOnColumn_mentionsDeletedOnButDifferentError_returnsFalse() {
    SQLException e = new SQLException("Unique constraint deleted_on violation");
    assertFalse(SoftDeleteMigrationHelper.isMissingDeletedOnColumn(e));
  }

  @Test
  void isMissingDeletedOnColumn_nullMessage_returnsFalse() {
    SQLException e = new SQLException((String) null);
    assertFalse(SoftDeleteMigrationHelper.isMissingDeletedOnColumn(e));
  }

  @Test
  void isMissingDeletedOnColumn_deepNestedCause_returnsTrue() {
    SQLException root = new SQLException("Column \"DELETED_ON\" not found");
    RuntimeException mid = new RuntimeException("DB error", root);
    RuntimeException outer = new RuntimeException("Service failure", mid);
    assertTrue(SoftDeleteMigrationHelper.isMissingDeletedOnColumn(outer));
  }
}
