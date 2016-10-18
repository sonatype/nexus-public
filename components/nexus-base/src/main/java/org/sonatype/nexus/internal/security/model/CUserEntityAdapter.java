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
package org.sonatype.nexus.internal.security.model;

import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.OIndexNameBuilder;
import org.sonatype.nexus.orient.entity.IterableEntityAdapter;
import org.sonatype.nexus.orient.entity.action.DeleteEntityByPropertyAction;
import org.sonatype.nexus.orient.entity.action.ReadEntityByPropertyAction;
import org.sonatype.nexus.security.config.CUser;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

/**
 * {@link CUser} entity adapter.
 *
 * @since 3.0
 */
@Named
@Singleton
public class CUserEntityAdapter
    extends IterableEntityAdapter<CUser>
{
  private static final String DB_CLASS = new OClassNameBuilder()
      .type("user")
      .build();

  private static final String P_ID = "id";

  private static final String P_FIRST_NAME = "firstName";

  private static final String P_LAST_NAME = "lastName";

  private static final String P_PASSWORD = "password";

  private static final String P_STATUS = "status";

  private static final String P_EMAIL = "email";

  private static final String I_ID = new OIndexNameBuilder()
      .type(DB_CLASS)
      .property(P_ID)
      .build();

  private final ReadEntityByPropertyAction<CUser> read = new ReadEntityByPropertyAction<>(this, P_ID);

  private final DeleteEntityByPropertyAction delete = new DeleteEntityByPropertyAction(this, P_ID);

  public CUserEntityAdapter() {
    super(DB_CLASS);
  }

  @Override
  protected void defineType(final OClass type) {
    type.createProperty(P_ID, OType.STRING)
        .setNotNull(true);
    type.createProperty(P_FIRST_NAME, OType.STRING);
    type.createProperty(P_LAST_NAME, OType.STRING);
    type.createProperty(P_PASSWORD, OType.STRING)
        .setNotNull(true);
    type.createProperty(P_STATUS, OType.STRING)
        .setNotNull(true);
    type.createProperty(P_EMAIL, OType.STRING)
        .setNotNull(true);

    type.createIndex(I_ID, INDEX_TYPE.UNIQUE, P_ID);
  }

  @Override
  protected CUser newEntity() {
    return new CUser();
  }

  @Override
  protected void readFields(final ODocument document, final CUser entity) throws Exception {
    entity.setId(document.<String>field(P_ID, OType.STRING));
    entity.setFirstName(document.<String>field(P_FIRST_NAME, OType.STRING));
    entity.setLastName(document.<String>field(P_LAST_NAME, OType.STRING));
    entity.setPassword(document.<String>field(P_PASSWORD, OType.STRING));
    entity.setStatus(document.<String>field(P_STATUS, OType.STRING));
    entity.setEmail(document.<String>field(P_EMAIL, OType.STRING));

    entity.setVersion(String.valueOf(document.getVersion()));
  }

  @Override
  protected void writeFields(final ODocument document, final CUser entity) throws Exception {
    document.field(P_ID, entity.getId());
    document.field(P_FIRST_NAME, entity.getFirstName());
    document.field(P_LAST_NAME, entity.getLastName());
    document.field(P_STATUS, entity.getStatus());
    document.field(P_EMAIL, entity.getEmail());
    document.field(P_PASSWORD, entity.getPassword());
  }

  //
  // TODO: Sort out API below with EntityAdapter, do not expose ODocument
  //

  private static final String READ_QUERY = String.format("SELECT FROM %s WHERE %s = ?", DB_CLASS, P_ID);

  @Nullable
  @Deprecated
  public ODocument readDocument(final ODatabaseDocumentTx db, final String id) {
    OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<>(READ_QUERY);
    List<ODocument> results = db.command(query).execute(id);
    if (results.isEmpty()) {
      return null;
    }
    return results.get(0);
  }

  //
  // Actions
  //

  /**
   * @since 3.1
   */
  @Nullable
  public CUser read(final ODatabaseDocumentTx db, final String id) {
    return read.execute(db, id);
  }

  /**
   * @since 3.1
   */
  public boolean delete(final ODatabaseDocumentTx db, final String id) {
    return delete.execute(db, id);
  }
}
