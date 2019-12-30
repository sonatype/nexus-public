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
package com.sonatype.nexus.ssl.plugin.internal.keystore.orient;

import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.entity.EntityEvent;
import org.sonatype.nexus.common.entity.EntityMetadata;
import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.OIndexNameBuilder;
import org.sonatype.nexus.orient.entity.AttachedEntityMetadata;
import org.sonatype.nexus.orient.entity.IterableEntityAdapter;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

/**
 * {@link OrientKeyStoreData} entity-adapter.
 *
 * since 3.1
 */
@Named
@Singleton
public class OrientKeyStoreDataEntityAdapter
    extends IterableEntityAdapter<OrientKeyStoreData>
{
  private static final String DB_CLASS = new OClassNameBuilder().type("key_store").build();

  private static final String P_NAME = "name";

  private static final String P_BYTES = "bytes";

  private static final String I_NAME = new OIndexNameBuilder().type(DB_CLASS).property(P_NAME).build();

  public OrientKeyStoreDataEntityAdapter() {
    super(DB_CLASS);
  }

  @Override
  protected void defineType(final OClass type) {
    type.createProperty(P_NAME, OType.STRING).setMandatory(true).setNotNull(true);
    type.createProperty(P_BYTES, OType.BINARY).setMandatory(true).setNotNull(true);
    type.createIndex(I_NAME, INDEX_TYPE.UNIQUE, P_NAME);
  }

  @Override
  protected OrientKeyStoreData newEntity() {
    return new OrientKeyStoreData();
  }

  @Override
  protected void readFields(final ODocument document, final OrientKeyStoreData entity) {
    entity.setName(document.field(P_NAME, OType.STRING));
    entity.setBytes(document.<byte[]> field(P_BYTES, OType.BINARY).clone());
  }

  @Override
  protected void writeFields(final ODocument document, final OrientKeyStoreData entity) {
    document.field(P_NAME, entity.getName());
    document.field(P_BYTES, entity.getBytes().clone());
  }

  @Nullable
  private ODocument findDocument(final ODatabaseDocumentTx db, final String name) {
    String query = "SELECT FROM " + DB_CLASS + " WHERE " + P_NAME + " = ?";
    List<ODocument> results = db.command(new OSQLSynchQuery<>(query)).execute(name);
    return results.stream().findFirst().orElse(null);
  }

  @Nullable
  public OrientKeyStoreData load(final ODatabaseDocumentTx db, final String name) {
    ODocument document = findDocument(db, name);
    if (document != null) {
      return readEntity(document);
    }
    return null;
  }

  public void save(final ODatabaseDocumentTx db, final OrientKeyStoreData entity) {
    ODocument document = findDocument(db, entity.getName());
    if (document != null) {
      writeEntity(document, entity);
    }
    else {
      addEntity(db, entity);
    }
  }

  @Override
  public boolean sendEvents() {
    return true;
  }

  @Nullable
  @Override
  public EntityEvent newEvent(final ODocument document, final EventKind eventKind) {
    EntityMetadata metadata = new AttachedEntityMetadata(this, document);
    String name = document.field(P_NAME);
    EntityEvent event;
    switch (eventKind) {
      case CREATE:
        event = new OrientKeyStoreDataCreatedEvent(metadata, name);
        break;
      case UPDATE:
        event = new OrientKeyStoreDataUpdatedEvent(metadata, name);
        break;
      default:
        event = null;
    }
    if (event != null) {
      log.debug("Emitted {} event for key store {} with metadata {}", eventKind, name, metadata);
    }
    return event;
  }
}
