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
package org.sonatype.nexus.orient.entity;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.entity.AbstractEntity;
import org.sonatype.nexus.common.entity.EntityMetadata;
import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AttachedEntityMetadata}
 */
@RunWith(Parameterized.class)
public class AttachedEntityMetadataTest
    extends TestSupport
{
  @Rule
  public DatabaseInstanceRule database = DatabaseInstanceRule.inMemory("test");

  @Mock
  private Appender<ILoggingEvent> mockAppender;

  private TestEntityAdapter entityAdapter;

  @Parameters
  public static Object[] data() {
    return new Object[] { false /* test full loading */, true /* test partial loading */ };
  }

  public AttachedEntityMetadataTest(boolean partialLoading) {
    entityAdapter = new TestEntityAdapter(partialLoading);
  }

  @Before
  public void setUp() {
    when(mockAppender.getName()).thenReturn("MOCK");

    ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).addAppender(mockAppender);
  }

  static class TestEntity
      extends AbstractEntity
  {
    String primary;

    String secondary;
  }

  static class TestEntityAdapter
      extends SingletonEntityAdapter<TestEntity>
  {
    static final String DB_CLASS = new OClassNameBuilder().type("test").build();

    private boolean partialEntity;

    TestEntityAdapter(final boolean partialLoading) {
      super(DB_CLASS);
      if (partialLoading) {
        enablePartialLoading();
        partialEntity = true;
      }
    }

    public boolean isPartialEntity() {
      return partialEntity;
    }

    @Override
    protected TestEntity newEntity() {
      return new TestEntity();
    }

    @Override
    protected void defineType(OClass type) {
      type.createProperty("primary", OType.STRING);
      type.createProperty("secondary", OType.STRING);
    }

    @Override
    protected void writeFields(ODocument document, TestEntity entity) throws Exception {
      document.field("primary", entity.primary);
      document.field("secondary", entity.secondary);
    }

    @Override
    protected void readFields(ODocument document, TestEntity entity) throws Exception {
      entity.primary = document.field("primary");
      // deliberately leave secondary field unread to test full vs. partial document loading
    }
  }

  @Test
  public void toStringOutsideTxShouldNotLogSerializerError() {
    EntityMetadata metadata;

    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      entityAdapter.register(db);

      // CREATE
      db.begin();
      TestEntity entity = new TestEntity();
      entity.primary = "Hello";
      entity.secondary = "World";
      entityAdapter.set(db, entity);
      db.commit();
    }

    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      metadata = entityAdapter.get(db).getEntityMetadata();
    }

    ODatabaseRecordThreadLocal.INSTANCE.remove(); // remove reference to the originating database

    String toStringOutput = metadata.toString(); // attempt to dump the attached document's fields

    ArgumentCaptor<ILoggingEvent> events = ArgumentCaptor.forClass(ILoggingEvent.class);
    verify(mockAppender, atLeastOnce()).doAppend(events.capture());

    events.getAllValues().forEach(
        event -> assertThat(event.getFormattedMessage(), not(containsString("Error deserializing record"))));

    if (entityAdapter.isPartialEntity()) {
      assertThat(toStringOutput, containsString("{primary:Hello}"));
    }
    else {
      assertThat(toStringOutput, containsString("{primary:Hello,secondary:World}"));
    }
  }
}
