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
package org.sonatype.nexus.quartz.internal.store;

import java.sql.Connection;
import java.sql.SQLException;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.content.testsuite.groups.SQLTestGroup;
import org.sonatype.nexus.testdb.DataSessionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

/**
 * Test the {@link ConfigStoreConnectionProvider}.
 */
@Category(SQLTestGroup.class)
public class ConfigStoreConnectionProviderTest
    extends TestSupport
{
  @Rule
  public DataSessionRule sessionRule = new DataSessionRule();

  private ConfigStoreConnectionProvider underTest;

  @Before
  public void setUp() {
    underTest = new ConfigStoreConnectionProvider(sessionRule);
    underTest.initialize();
  }

  @After
  public void tearDown() {
    underTest.shutdown();
    underTest = null;
  }

  @Test
  public void testCanOpenConnection() throws SQLException {
    try (Connection connection = underTest.getConnection()) {
      if (System.getProperty("test.postgres") != null) {
        assertThat(connection.getMetaData().getURL(), startsWith("jdbc:postgresql:"));
      }
      else {
        assertThat(connection.getMetaData().getURL(), is("jdbc:h2:mem:nexus"));
      }
    }
  }
}
