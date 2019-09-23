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
package org.sonatype.nexus.quartz.internal.orient;

import java.sql.Connection;
import java.sql.SQLException;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Test the {@link ConfigOrientConnectionProvider}.
 */
public class ConfigOrientConnectionProviderTest
    extends TestSupport
{
  @Rule
  public DatabaseInstanceRule database = DatabaseInstanceRule.inMemory("config");

  private ConfigOrientConnectionProvider underTest;

  @Before
  public void setUp() throws Exception {
    underTest = new ConfigOrientConnectionProvider(database.getInstanceProvider());
    underTest.initialize();
  }

  @After
  public void tearDown() throws Exception {
    underTest.shutdown();
    underTest = null;
  }

  @Test
  public void testCanOpenConnection() throws SQLException {
    try (Connection connection = underTest.getConnection()) {
      assertThat(connection.getMetaData().getURL(), is("memory:config"));
    }
  }
}
