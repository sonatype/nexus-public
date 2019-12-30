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
package org.sonatype.nexus.internal.selector.orient;

import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.entity.EntityEvent;
import org.sonatype.nexus.common.entity.EntityMetadata;
import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.OIndexNameBuilder;
import org.sonatype.nexus.orient.entity.AttachedEntityMetadata;
import org.sonatype.nexus.orient.entity.IterableEntityAdapter;
import org.sonatype.nexus.selector.OrientSelectorConfiguration;
import org.sonatype.nexus.selector.SelectorConfiguration;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;

/**
 * {@link OrientSelectorConfiguration} entity-adapter.
 *
 * since 3.0
 */
@Named
@Singleton
public class OrientSelectorConfigurationEntityAdapter
    extends IterableEntityAdapter<OrientSelectorConfiguration>
{
  private static final String DB_CLASS = new OClassNameBuilder()
      .prefix("selector")
      .type("selector")
      .build();

  private static final String P_NAME = "name";

  private static final String P_TYPE = "type";

  private static final String P_DESCRIPTION = "description";

  private static final String P_ATTRIBUTES = "attributes";

  private static final String FIND_BY_NAME = String.format("select from %s where %s = :%s", DB_CLASS, P_NAME, P_NAME);

  @VisibleForTesting
  static final String I_NAME = new OIndexNameBuilder()
      .type(DB_CLASS)
      .property(P_NAME)
      .build();

  public OrientSelectorConfigurationEntityAdapter() {
    super(DB_CLASS);
  }

  @Override
  protected void defineType(final OClass type) {
    type.createProperty(P_NAME, OType.STRING)
        .setMandatory(true)
        .setNotNull(true);
    type.createProperty(P_TYPE, OType.STRING)
        .setMandatory(true)
        .setNotNull(true);
    type.createProperty(P_DESCRIPTION, OType.STRING);
    type.createProperty(P_ATTRIBUTES, OType.EMBEDDEDMAP)
        .setMandatory(true)
        .setNotNull(true);
    type.createIndex(I_NAME, INDEX_TYPE.UNIQUE, P_NAME);
  }

  /**
   * @since 3.6
   */
  @Nullable
  public SelectorConfiguration getByName(final ODatabaseDocumentTx db, final String name) {
    Map<String, Object> parameters = ImmutableMap.of(P_NAME, name);

    Iterable<ODocument> docs = db.command(new OCommandSQL(FIND_BY_NAME)).execute(parameters);

    if (!docs.iterator().hasNext()) {
      return null;
    }

    return this.readEntity(Iterables.getFirst(docs, null));
  }

  @Override
  protected OrientSelectorConfiguration newEntity() {
    return new OrientSelectorConfiguration();
  }

  @Override
  protected void readFields(final ODocument document, final OrientSelectorConfiguration entity) {
    String name = document.field(P_NAME, OType.STRING);
    String type = document.field(P_TYPE, OType.STRING);
    String description = document.field(P_DESCRIPTION, OType.STRING);
    Map<String, Object> attributes = document.field(P_ATTRIBUTES, OType.EMBEDDEDMAP);

    entity.setName(name);
    entity.setType(type);
    entity.setDescription(description);
    entity.setAttributes(detachable(attributes));
  }

  @Override
  protected void writeFields(final ODocument document, final OrientSelectorConfiguration entity) {
    document.field(P_NAME, entity.getName());
    document.field(P_TYPE, entity.getType());
    document.field(P_DESCRIPTION, entity.getDescription());
    document.field(P_ATTRIBUTES, entity.getAttributes());
  }

  @Override
  public boolean sendEvents() {
    return true;
  }

  @Override
  public EntityEvent newEvent(final ODocument document, final EventKind eventKind) {
    EntityMetadata metadata = new AttachedEntityMetadata(this, document);

    switch (eventKind) {
      case CREATE:
        return new OrientSelectorConfigurationCreatedEvent(metadata);
      case UPDATE:
        return new OrientSelectorConfigurationUpdatedEvent(metadata);
      case DELETE:
        return new OrientSelectorConfigurationDeletedEvent(metadata);
      default:
        return null;
    }
  }
}
