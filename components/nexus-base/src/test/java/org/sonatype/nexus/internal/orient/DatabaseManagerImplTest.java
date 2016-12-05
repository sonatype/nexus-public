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
package org.sonatype.nexus.internal.orient;

import java.io.File;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.orient.DatabaseRestorer;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

/**
 * Tests {@link DatabaseManagerImpl}.
 */
public class DatabaseManagerImplTest
    extends TestSupport
{
  @Rule
  public TemporaryFolder tmpDir = new TemporaryFolder();

  private DatabaseManagerImpl databaseManager;

  @Before
  public void setUp() {
    databaseManager = new DatabaseManagerImpl(tmpDir.getRoot(), mock(DatabaseRestorer.class));
  }

  @Test
  public void testConnectionUri() throws Exception {
    String dbPath = new File(tmpDir.getRoot(), "dbName").getCanonicalPath().replace('\\', '/');
    assertThat(databaseManager.connectionUri("dbName"), is("plocal:" + dbPath));
  }
}
