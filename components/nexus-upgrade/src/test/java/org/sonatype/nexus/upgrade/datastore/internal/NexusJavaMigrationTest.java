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
package org.sonatype.nexus.upgrade.datastore.internal;

import java.sql.Connection;
import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.sonatype.nexus.upgrade.datastore.internal.NexusJavaMigration.nameMatcher;

public class NexusJavaMigrationTest
    extends TestSupport
{

  @Test
  public void testNameMatcher() {
    assertTrue(nameMatcher(AStep.class).test(toDescription(new AStep(), null)));
    assertTrue(nameMatcher(AStep.class).test(toDescription(new AStep(), 1)));

    assertFalse(nameMatcher(AStepA.class).test(toDescription(new AStep(), null)));
  }

  private static String toDescription(final DatabaseMigrationStep step, final Integer round) {
    return new NexusJavaMigration(step, round).getDescription();
  }

  private static class AStep
      extends TestStep
  {
  }

  private static class AStepA
      extends TestStep
  {
  }

  private static class TestStep
      implements DatabaseMigrationStep
  {

    @Override
    public Optional<String> version() {
      return Optional.empty();
    }

    @Override
    public void migrate(final Connection connection) throws Exception {
    }
  }
}
