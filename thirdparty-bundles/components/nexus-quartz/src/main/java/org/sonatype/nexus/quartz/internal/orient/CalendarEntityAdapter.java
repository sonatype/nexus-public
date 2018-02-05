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

import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.OIndexNameBuilder;
import org.sonatype.nexus.orient.entity.action.BrowsePropertyAction;
import org.sonatype.nexus.orient.entity.action.DeleteEntitiesAction;
import org.sonatype.nexus.orient.entity.action.DeleteEntityByPropertyAction;
import org.sonatype.nexus.orient.entity.action.ReadEntityByPropertyAction;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.quartz.Calendar;
import org.quartz.CronExpression;

/**
 * {@link CalendarEntity} adapter.
 *
 * @since 3.0
 */
@Named
@Singleton
public class CalendarEntityAdapter
    extends MarshalledEntityAdapter<CalendarEntity>
{
  private static final String DB_CLASS = new OClassNameBuilder()
      .prefix("quartz")
      .type("calendar")
      .build();

  private static final String P_NAME = "name";

  private static final String I_NAME = new OIndexNameBuilder()
      .type(DB_CLASS)
      .property(P_NAME)
      .build();

  private final BrowsePropertyAction<String> browseNames = new BrowsePropertyAction<>(this, P_NAME);

  private final ReadEntityByPropertyAction<CalendarEntity> readByName = new ReadEntityByPropertyAction<>(this, P_NAME);

  private final DeleteEntityByPropertyAction deleteByName = new DeleteEntityByPropertyAction(this, P_NAME);

  private final DeleteEntitiesAction deleteAll = new DeleteEntitiesAction(this);

  @Inject
  public CalendarEntityAdapter() {
    super(DB_CLASS, createMarshaller(), Calendar.class.getClassLoader());
  }

  private static Marshaller createMarshaller() {
    return new JacksonMarshaller(new FieldObjectMapper()
        // special handling for CronExpression deserialization
        .addMixIn(CronExpression.class, CronExpressionMixin.class)
    );
  }

  @Override
  protected void defineType(final OClass type) {
    super.defineType(type);

    type.createProperty(P_NAME, OType.STRING)
        .setMandatory(true)
        .setNotNull(true);

    type.createIndex(I_NAME, INDEX_TYPE.UNIQUE, P_NAME);
  }

  @Override
  protected CalendarEntity newEntity() {
    return new CalendarEntity();
  }

  @Override
  protected void readFields(final ODocument document, final CalendarEntity entity) throws Exception {
    super.readFields(document, entity);

    entity.setName(document.field(P_NAME));
  }

  @Override
  protected void writeFields(final ODocument document, final CalendarEntity entity) throws Exception {
    super.writeFields(document, entity);

    document.field(P_NAME, entity.getName());
  }

  //
  // Actions
  //

  /**
   * Browse all entities with given calendar name.
   * 
   * @since 3.1
   */
  public List<String> browseNames(final ODatabaseDocumentTx db) {
    return browseNames.execute(db);
  }

  /**
   * Read a single entity by calendar name.
   * 
   * @since 3.1
   */
  @Nullable
  public CalendarEntity readByName(final ODatabaseDocumentTx db, final String name) {
    return readByName.execute(db, name);
  }

  /**
   * Delete a single entity by calendar name.
   * 
   * @since 3.1
   */
  public boolean deleteByName(final ODatabaseDocumentTx db, final String name) {
    return deleteByName.execute(db, name);
  }

  /**
   * Delete all entities.
   * 
   * @since 3.1
   */
  public void deleteAll(final ODatabaseDocumentTx db) {
    deleteAll.execute(db);
  }
}
