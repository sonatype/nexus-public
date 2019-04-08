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
package org.sonatype.nexus.repository.routing.internal;

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
import org.sonatype.nexus.orient.entity.action.ReadEntityByPropertyAction;
import org.sonatype.nexus.repository.routing.RoutingMode;
import org.sonatype.nexus.repository.routing.RoutingRule;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;

/**
 * @since 3.next
 */
@Named
@Singleton
public class RoutingRuleEntityAdapter
    extends IterableEntityAdapter<RoutingRule>
{
  static final String DB_CLASS = new OClassNameBuilder().prefix("repository").type("routingrule").build();

  private static final String P_NAME = "name";

  private static final String P_MATCHERS = "matchers";

  private static final String P_MODE = "mode";

  private static final String P_DESCRIPTION = "description";

  static final String I_NAME = new OIndexNameBuilder().type(DB_CLASS).property(P_NAME).build();

  private final ReadEntityByPropertyAction<RoutingRule> read = new ReadEntityByPropertyAction<>(this, P_NAME);

  public RoutingRuleEntityAdapter() {
    super(DB_CLASS);
  }

  @Override
  protected void defineType(final OClass type) {
    type.createProperty(P_DESCRIPTION, OType.STRING).setMandatory(true).setNotNull(true);
    type.createProperty(P_NAME, OType.STRING).setMandatory(true).setNotNull(true);
    type.createProperty(P_MATCHERS, OType.EMBEDDEDLIST).setMandatory(true).setNotNull(true);
    type.createProperty(P_MODE, OType.STRING).setMandatory(true).setNotNull(true);
  }

  @Override
  protected void defineType(final ODatabaseDocumentTx db, final OClass type) {
    defineType(type);

    // primary index that guarantees path uniqueness for nodes in a given repository
    type.createIndex(I_NAME, INDEX_TYPE.UNIQUE, P_NAME);
  }

  @Override
  protected RoutingRule newEntity() {
    return new RoutingRule();
  }

  @Override
  protected void readFields(final ODocument document, final RoutingRule entity) throws Exception {
    String name = document.field(P_NAME, OType.STRING);
    String description = document.field(P_DESCRIPTION, OType.STRING);
    List<String> matchers = document.field(P_MATCHERS, OType.EMBEDDEDLIST);
    RoutingMode mode = RoutingMode.valueOf(document.field(P_MODE, OType.STRING));

    entity.description(description);
    entity.matchers(matchers);
    entity.name(name);
    entity.mode(mode);
  }

  @Override
  protected void writeFields(final ODocument document, final RoutingRule entity) throws Exception {
    document.field(P_MODE, entity.mode().toString());
    document.field(P_DESCRIPTION, entity.description());
    document.field(P_NAME, entity.name());
    document.field(P_MATCHERS, entity.matchers());
  }

  @Nullable
  public RoutingRule read(final ODatabaseDocumentTx db, final String name) {
    return read.execute(db, name);
  }

  @Override
  public boolean sendEvents() {
    return true;
  }

  @Nullable
  @Override
  public EntityEvent newEvent(final ODocument document, final EventKind eventKind) {
    final EntityMetadata metadata = new AttachedEntityMetadata(this, document);

    log.trace("newEvent: eventKind: {}, metadata: {}", eventKind, metadata);
    switch (eventKind) {
      case UPDATE:
        return new RoutingRuleUpdatedEvent(metadata);
      case DELETE:
        return new RoutingRuleDeletedEvent(metadata);
      default:
        return super.newEvent(document, eventKind);
    }
  }
}
