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
package org.sonatype.nexus.repository.content.search.capability;

import java.sql.Connection;
import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SearchCapabilityStep_2_1Test
    extends TestSupport
{
  @Mock
  private Connection connection;

  private SearchCapabilityStep_2_1 underTest;

  @Before
  public void setUp() {
    underTest = new SearchCapabilityStep_2_1();
  }

  @Test
  public void testVersion() {
    Optional<String> version = underTest.version();
    assertTrue(version.isPresent());
    assertEquals("2.1", version.get());
  }

  @Test
  public void testMigrate_isNoOp() throws Exception {
    // migrate is a no-op; verify it does not throw
    underTest.migrate(connection);
  }
}
