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
package org.sonatype.nexus.orient;

import java.io.File;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

public class DatabaseCheckpointSupportTest
    extends TestSupport
{
  private static final String DB_NAME = "test";

  @Rule
  public DatabaseInstanceRule database = DatabaseInstanceRule.inFilesystem(DB_NAME);

  @Mock
  private ApplicationDirectories appDirectories;

  private DatabaseCheckpointSupport checkpoint;

  private File upgradeDir;

  @Before
  public void setUp() {
    upgradeDir = util.createTempDir();
    when(appDirectories.getWorkDirectory("upgrades/" + DB_NAME)).thenReturn(upgradeDir);
    checkpoint = new DatabaseCheckpointSupport(DB_NAME, database.getInstanceProvider(), appDirectories)
    {
    };
  }

  @Test
  public void testBegin() throws Exception {
    checkpoint.begin("1.1");
    assertThat(new File(upgradeDir, DB_NAME + "-1.1-backup.zip").isFile(), is(true));
  }

  @Test
  public void testRollback_BeforeCommitWasEvenCalled() throws Exception {
    checkpoint.begin("1.1");
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      OSchema schema = db.getMetadata().getSchema();
      assertThat(schema.createClass("new_class"), is(notNullValue()));
    }
    checkpoint.rollback();
    assertThat(new File(upgradeDir, DB_NAME + "-failed.zip").isFile(), is(true));
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      OSchema schema = db.getMetadata().getSchema();
      assertThat(schema.getClass("new_class"), is(nullValue()));
    }
  }

  @Test
  public void testRollback_AfterCommitWasAlreadyCalled() throws Exception {
    checkpoint.begin("1.1");
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      OSchema schema = db.getMetadata().getSchema();
      assertThat(schema.createClass("new_class"), is(notNullValue()));
    }
    checkpoint.commit();
    checkpoint.rollback();
    assertThat(new File(upgradeDir, DB_NAME + "-failed.zip").isFile(), is(true));
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      OSchema schema = db.getMetadata().getSchema();
      assertThat(schema.getClass("new_class"), is(nullValue()));
    }
  }

  @Test
  public void testEnd() throws Exception {
    checkpoint.begin("1.1");
    assertThat(new File(upgradeDir, DB_NAME + "-1.1-backup.zip").isFile(), is(true));
    checkpoint.commit();
    checkpoint.end();
    assertThat(new File(upgradeDir, DB_NAME + "-1.1-backup.zip").exists(), is(false));
    assertThat(upgradeDir.exists(), is(false));
  }
}
