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
package org.sonatype.nexus.internal.security.model.orient;

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.OIndexNameBuilder;
import org.sonatype.nexus.orient.entity.IterableEntityAdapter;
import org.sonatype.nexus.orient.entity.action.DeleteEntityByPropertyAction;
import org.sonatype.nexus.orient.entity.action.ReadEntitiesByPropertyAction;
import org.sonatype.nexus.orient.entity.action.ReadEntityByPropertyAction;
import org.sonatype.nexus.orient.entity.action.UpdateEntityByPropertyAction;

import com.google.common.collect.Maps;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;

/**
 * {@link OrientCPrivilege} entity adapter.
 *
 * @since 3.0
 */
@Named
@Singleton
public class OrientCPrivilegeEntityAdapter
    extends IterableEntityAdapter<OrientCPrivilege>
{
  private static final String DB_CLASS = new OClassNameBuilder()
      .type("privilege")
      .build();

  private static final String P_ID = "id";

  private static final String P_NAME = "name";

  private static final String P_DESCRIPTION = "description";

  private static final String P_TYPE = "type";

  private static final String P_PROPERTIES = "properties";

  private static final String I_ID = new OIndexNameBuilder()
      .type(DB_CLASS)
      .property(P_ID)
      .build();

  private static final String I_NAME = new OIndexNameBuilder()
      .type(DB_CLASS)
      .property(P_NAME)
      .build();

  private final ReadEntityByPropertyAction<OrientCPrivilege> read = new ReadEntityByPropertyAction<>(this, P_ID);

  private final ReadEntityByPropertyAction<OrientCPrivilege> readByName = new ReadEntityByPropertyAction<>(this, P_NAME);

  private final ReadEntitiesByPropertyAction<OrientCPrivilege> readEntities =
      new ReadEntitiesByPropertyAction<>(this, P_ID);

  private final DeleteEntityByPropertyAction delete = new DeleteEntityByPropertyAction(this, P_ID);

  private final DeleteEntityByPropertyAction deleteByName = new DeleteEntityByPropertyAction(this, P_NAME);

  private final UpdateEntityByPropertyAction<OrientCPrivilege> update = new UpdateEntityByPropertyAction<>(this, P_ID);

  private final UpdateEntityByPropertyAction<OrientCPrivilege> updateByName = new UpdateEntityByPropertyAction<>(this, P_NAME);

  public OrientCPrivilegeEntityAdapter() {
    super(DB_CLASS);
  }

  @Override
  protected void defineType(final OClass type) {
    type.createProperty(P_ID, OType.STRING)
        .setNotNull(true);
    type.createProperty(P_NAME, OType.STRING)
        .setNotNull(true);
    type.createProperty(P_DESCRIPTION, OType.STRING);
    type.createProperty(P_TYPE, OType.STRING)
        .setNotNull(true);
    type.createProperty(P_PROPERTIES, OType.EMBEDDEDMAP)
        .setNotNull(true);

    type.createIndex(I_ID, INDEX_TYPE.UNIQUE, P_ID);
    type.createIndex(I_NAME , INDEX_TYPE.UNIQUE , P_NAME);
  }

  @Override
  protected OrientCPrivilege newEntity() {
    return new OrientCPrivilege();
  }

  @Override
  protected void readFields(final ODocument document, final OrientCPrivilege entity) throws Exception {
    entity.setId(document.<String>field(P_ID, OType.STRING));
    entity.setName(document.<String>field(P_NAME, OType.STRING));
    entity.setDescription(document.<String>field(P_DESCRIPTION, OType.STRING));
    entity.setType(document.<String>field(P_TYPE, OType.STRING));
    entity.setReadOnly(false);
    entity.setProperties(Maps.newHashMap(document.<Map<String, String>>field(P_PROPERTIES, OType.EMBEDDEDMAP)));

    entity.setVersion(document.getVersion());
  }

  @Override
  protected void writeFields(final ODocument document, final OrientCPrivilege entity) throws Exception {
    document.field(P_ID, entity.getId());
    document.field(P_NAME, entity.getName());
    document.field(P_DESCRIPTION, entity.getDescription());
    document.field(P_TYPE, entity.getType());
    document.field(P_PROPERTIES, entity.getProperties());
  }

  //
  // Actions
  //

  /**
   * @since 3.1
   */
  @Nullable
  public OrientCPrivilege read(final ODatabaseDocumentTx db, final String id) {
    return read.execute(db, id);
  }

  public OrientCPrivilege readByName(final ODatabaseDocumentTx db , final String name){
    return readByName.execute(db , name);
  }

  public List<OrientCPrivilege> read(final ODatabaseDocumentTx db, final Set<String> ids) {
    return readEntities.execute(db, ids);
  }

  /**
   * @since 3.1
   */
  public boolean delete(final ODatabaseDocumentTx db, final String id) {
    return delete.execute(db, id);
  }

  public boolean deleteByName(final ODatabaseDocumentTx db, final String name) {
    return deleteByName.execute(db, name);
  }

  /**
   * @since 3.6.1
   */
  public boolean update(final ODatabaseDocumentTx db, final OrientCPrivilege entity) {
    return update.execute(db, entity, entity.getId());
  }

  public boolean updateByName(final ODatabaseDocumentTx db, final OrientCPrivilege entity) {
    return updateByName.execute(db, entity, entity.getName());
  }
}
