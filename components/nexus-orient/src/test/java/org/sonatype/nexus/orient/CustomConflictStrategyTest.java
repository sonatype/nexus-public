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

import java.util.concurrent.atomic.AtomicInteger;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule;

import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.conflict.ORecordConflictStrategy;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.storage.OStorage;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Exercises custom conflict strategies.
 */
public class CustomConflictStrategyTest
    extends TestSupport
{
  private static final String DB_CLASS = "entity";

  private static final String P_NAME = "name";

  private int mvccCount;

  public CustomConflictStrategyTest() {

    Orient.instance().getRecordConflictStrategy().registerImplementation("test",
        new ORecordConflictStrategy()
        {
          @Override
          public byte[] onUpdate(final OStorage storage,
                                 final byte recordType,
                                 final ORecordId rid,
                                 final int recordVersion,
                                 final byte[] recordContent,
                                 final AtomicInteger dbVersion)
          {
            mvccCount++; // record MVCC, then let change happen
            return null;
          }

          @Override
          public String getName() {
            return "test";
          }
        });
  }

  @Rule
  public DatabaseInstanceRule database = DatabaseInstanceRule.inFilesystem("test");

  @Test
  public void customStrategyIsUsed() throws Exception {
    ODocument entity;

    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      OSchema schema = db.getMetadata().getSchema();
      OClass entityType = schema.createClass(DB_CLASS);
      entityType.createProperty(P_NAME, OType.STRING);
      entity = db.newInstance(DB_CLASS);
      entity.field(P_NAME, "test");
      entity.save();
    }

    assertThat(mvccCount, is(0));

    // make change using a local copy so we don't bump original
    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      db.begin();
      entity.copy().field(P_NAME, "testing").save();
      db.commit();
    }

    assertThat(mvccCount, is(0));

    // should record MVCC but still allow change to be committed
    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      db.begin();
      entity.copy().field(P_NAME, "test!").save();
      db.commit();
    }

    assertThat(mvccCount, is(1));

    // should record MVCC but still allow change to be committed
    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      db.begin();
      entity.copy().field(P_NAME, "test!").save();
      db.commit();
    }

    assertThat(mvccCount, is(2));
  }
}
