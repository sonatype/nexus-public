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
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;
import org.sonatype.nexus.upgrade.datastore.DependsOn;
import org.sonatype.nexus.upgrade.datastore.RepeatableDatabaseMigrationStep;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class SimpleDependencyResolverTest
    extends TestSupport
{
  private final One one = new One();

  private final Two two = new Two();

  private final Three three = new Three();

  private final TestMigrationStep versioned = new TestMigrationStep();

  @Before
  public void setup() {
    assertTrue("Sanity check we expect a versioned step", versioned.version().isPresent());
  }

  @Test
  public void testOrderUpgrades() {
    // we intentionally request ordering with the steps in the wrong order
    assertThat(orderUpgrades(three, one, two, versioned),
        contains("TestMigrationStep", "One", "Z_001_Two", "Z_002_Three"));

    // Request an ordering where a dependency (one) is missing
    assertThrows(IllegalStateException.class, () -> orderUpgrades(three, two, versioned));
  }

  private Collection<String> orderUpgrades(final DatabaseMigrationStep... steps) {
    return new SimpleDependencyResolver(Arrays.asList(steps)).resolve().stream()
        .map(NexusJavaMigration::getDescription)
        .collect(Collectors.toList());
  }

  private static class One extends TestRepeatableDatabaseMigrationStep {}

  @DependsOn(One.class)
  private static class Two extends TestRepeatableDatabaseMigrationStep {}

  @DependsOn(One.class)
  @DependsOn(Two.class)
  private static class Three extends TestRepeatableDatabaseMigrationStep {
    // A step with multiple dependencies
  }

  private abstract static class TestRepeatableDatabaseMigrationStep
      implements RepeatableDatabaseMigrationStep
  {
    @Override
    public void migrate(final Connection connection) throws Exception {
      // not applicable
    }

    @Override
    public Integer getChecksum() {
      return 1;
    }
  }
}
