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
package org.sonatype.nexus.internal.node;

import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.node.NodeConfiguration;
import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.OIndexNameBuilder;
import org.sonatype.nexus.orient.entity.IterableEntityAdapter;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link NodeConfiguration} entity adapter.
 *
 * @since 3.6
 */
@Named
@Singleton
public class NodeConfigurationEntityAdapter
    extends IterableEntityAdapter<NodeConfiguration>
{
  private static final String DB_CLASS = new OClassNameBuilder()
      .type("nodes")
      .build();

  private static final String P_ID = "id";

  private static final String P_FRIENDLY_NAME = "friendly_name";

  private static final String I_ID = new OIndexNameBuilder()
      .type(DB_CLASS)
      .property(P_ID)
      .build();

  public NodeConfigurationEntityAdapter() {
    super(DB_CLASS);
  }

  @Override
  protected void defineType(final OClass type) {
    // nodeconfig table
    type.createProperty(P_ID, OType.STRING).setMandatory(true).setNotNull(true);
    type.createProperty(P_FRIENDLY_NAME, OType.STRING).setNotNull(false);
    // index
    type.createIndex(I_ID, INDEX_TYPE.UNIQUE, P_ID);
  }

  @Override
  protected NodeConfiguration newEntity() {
    return new NodeConfiguration();
  }

  @Override
  protected void readFields(final ODocument document, final NodeConfiguration entity) throws Exception {
    entity.setId(document.field(P_ID, OType.STRING));
    entity.setFriendlyNodeName(document.field(P_FRIENDLY_NAME, OType.STRING));
  }

  @Override
  protected void writeFields(final ODocument document, final NodeConfiguration entity) throws Exception {
    document.field(P_ID, entity.getId());
    document.field(P_FRIENDLY_NAME, entity.getFriendlyNodeName());
  }

  public ODocument selectById(final ODatabaseDocumentTx db, final String id) {
    checkNotNull(db);
    checkNotNull(id);
    final OSQLSynchQuery<ODocument> query =
        new OSQLSynchQuery<>(String.format("SELECT FROM %s WHERE %s = ?", DB_CLASS, P_ID));
    final List<ODocument> results = db.command(query).execute(id);
    if (!results.isEmpty()) {
      return results.get(0);
    }
    return null;
  }
}
