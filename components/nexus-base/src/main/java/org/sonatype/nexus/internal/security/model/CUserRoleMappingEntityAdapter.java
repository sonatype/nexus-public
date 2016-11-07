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
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.OIndexNameBuilder;
import org.sonatype.nexus.orient.entity.IterableEntityAdapter;
import org.sonatype.nexus.orient.entity.action.DeleteEntityByPropertyAction;
import org.sonatype.nexus.orient.entity.action.ReadEntityByPropertyAction;
import org.sonatype.nexus.security.config.CUserRoleMapping;

import com.google.common.collect.Sets;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

/**
 * {@link CUserRoleMapping} entity adapter.
 *
 * @since 3.0
 */
@Named
@Singleton
public class CUserRoleMappingEntityAdapter
    extends IterableEntityAdapter<CUserRoleMapping>
{
  private static final String DB_CLASS = new OClassNameBuilder()
      .type("user_role_mapping")
      .build();

  private static final String P_USER_ID = "userId";

  private static final String P_SOURCE = "source";

  private static final String P_ROLES = "roles";

  private static final String I_USER_ID_SOURCE = new OIndexNameBuilder()
      .type(DB_CLASS)
      .property(P_USER_ID)
      .property(P_SOURCE)
      .build();

  private final ReadEntityByPropertyAction<CUserRoleMapping> read = new ReadEntityByPropertyAction<>(this, P_USER_ID,
      P_SOURCE);

  private final DeleteEntityByPropertyAction delete = new DeleteEntityByPropertyAction(this, P_USER_ID, P_SOURCE);

  public CUserRoleMappingEntityAdapter() {
    super(DB_CLASS);
  }

  @Override
  protected void defineType(final OClass type) {
    type.createProperty(P_USER_ID, OType.STRING)
        .setNotNull(true);
    type.createProperty(P_SOURCE, OType.STRING)
        .setNotNull(true);
    type.createProperty(P_ROLES, OType.EMBEDDEDSET);

    type.createIndex(I_USER_ID_SOURCE, INDEX_TYPE.UNIQUE, P_USER_ID, P_SOURCE);
  }

  @Override
  protected CUserRoleMapping newEntity() {
    return new CUserRoleMapping();
  }

  @Override
  protected void readFields(final ODocument document, final CUserRoleMapping entity) throws Exception {
    entity.setUserId(document.<String>field(P_USER_ID, OType.STRING));
    entity.setSource(document.<String>field(P_SOURCE, OType.STRING));
    entity.setRoles(Sets.newHashSet(document.<Set<String>>field(P_ROLES, OType.EMBEDDEDSET)));

    entity.setVersion(String.valueOf(document.getVersion()));
  }

  @Override
  protected void writeFields(final ODocument document, final CUserRoleMapping entity) throws Exception {
    document.field(P_USER_ID, entity.getUserId());
    document.field(P_SOURCE, entity.getSource());
    document.field(P_ROLES, entity.getRoles());
  }

  //
  // TODO: Sort out API below with EntityAdapter, do not expose ODocument
  //

  private static final String READ_QUERY =
      String.format("SELECT FROM %s WHERE %s = ? AND %s = ?", DB_CLASS, P_USER_ID, P_SOURCE);

  @Nullable
  public CUserRoleMapping read(final ODatabaseDocumentTx db, final String userId, final String source) {
    return read.execute(db, userId, source);
  }

  @Nullable
  @Deprecated
  public ODocument readDocument(final ODatabaseDocumentTx db, final String userId, final String source) {
    OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<>(READ_QUERY);
    List<ODocument> results = db.command(query).execute(userId, source);
    if (results.isEmpty()) {
      return null;
    }
    return results.get(0);
  }

  public boolean delete(final ODatabaseDocumentTx db, final String userId, final String source) {
    return delete.execute(db, userId, source);
  }
}
