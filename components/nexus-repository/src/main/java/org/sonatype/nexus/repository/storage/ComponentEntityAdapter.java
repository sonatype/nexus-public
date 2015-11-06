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
package org.sonatype.nexus.repository.storage;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.OIndexNameBuilder;
import org.sonatype.nexus.orient.entity.AttachedEntityMetadata;
import org.sonatype.nexus.orient.entity.EntityEvent;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.hook.ORecordHook.TYPE;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OResultSet;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

import static org.sonatype.nexus.repository.storage.StorageFacet.P_BUCKET;
import static org.sonatype.nexus.repository.storage.StorageFacet.P_GROUP;
import static org.sonatype.nexus.repository.storage.StorageFacet.P_NAME;
import static org.sonatype.nexus.repository.storage.StorageFacet.P_REPOSITORY_NAME;
import static org.sonatype.nexus.repository.storage.StorageFacet.P_VERSION;

/**
 * {@link Component} entity-adapter.
 *
 * @since 3.0
 */
@Named
@Singleton
public class ComponentEntityAdapter
    extends MetadataNodeEntityAdapter<Component>
{
  private static final String DB_CLASS = new OClassNameBuilder()
      .type(Component.class)
      .build();

  private static final String I_BUCKET_GROUP_NAME_VERSION = new OIndexNameBuilder()
      .type(DB_CLASS)
      .property(P_BUCKET)
      .property(P_GROUP)
      .property(P_NAME)
      .property(P_VERSION)
      .build();

  @Inject
  public ComponentEntityAdapter(final BucketEntityAdapter bucketEntityAdapter) {
    super(DB_CLASS, bucketEntityAdapter);
  }

  @Override
  protected void defineType(final ODatabaseDocumentTx db, final OClass type) {
    super.defineType(type);
    type.createProperty(P_GROUP, OType.STRING);
    type.createProperty(P_NAME, OType.STRING)
        .setMandatory(true)
        .setNotNull(true);
    type.createProperty(P_VERSION, OType.STRING);

    ODocument metadata = db.newInstance()
        .field("ignoreNullValues", false)
        .field("mergeKeys", false);
    type.createIndex(I_BUCKET_GROUP_NAME_VERSION, INDEX_TYPE.UNIQUE.name(), null, metadata,
        new String[]{P_BUCKET, P_GROUP, P_NAME, P_VERSION});
  }

  @Override
  protected Component newEntity() {
    return new Component();
  }

  @Override
  protected void readFields(final ODocument document, final Component entity) {
    super.readFields(document, entity);

    String group = document.field(P_GROUP, OType.STRING);
    String name = document.field(P_NAME, OType.STRING);
    String version = document.field(P_VERSION, OType.STRING);

    entity.group(group);
    entity.name(name);
    entity.version(version);
  }

  @Override
  protected void writeFields(final ODocument document, final Component entity) {
    super.writeFields(document, entity);

    document.field(P_GROUP, entity.group());
    document.field(P_NAME, entity.name());
    document.field(P_VERSION, entity.version());
  }

  @Override
  public boolean sendEvents() {
    return true;
  }

  @Override
  public EntityEvent newEvent(ODocument document, TYPE eventType) {
    final AttachedEntityMetadata metadata = new AttachedEntityMetadata(this, document);
    final String repositoryName = ((ODocument) document.field(P_BUCKET)).field(P_REPOSITORY_NAME);

    switch (eventType) {
      case AFTER_CREATE:
        return new ComponentCreatedEvent(metadata, repositoryName);
      case AFTER_UPDATE:
        return new ComponentUpdatedEvent(metadata, repositoryName);
      case AFTER_DELETE:
        return new ComponentDeletedEvent(metadata, repositoryName);
      default:
        return null;
    }
  }

  private static final String DISTINCT_COMPONENT_NAMES = String
      .format("SELECT DISTINCT(%s) AS %s FROM %s", P_NAME, P_NAME, DB_CLASS);
  
  private static final String ORDER_BY_NAME = String.format(" ORDER BY %s", P_NAME);

  /**
   * List the distinct names of components in the given buckets.
   */
  public List<String> getUniqueComponentNames(final ODatabaseDocumentTx db, final Iterable<Bucket> buckets) {
    StringBuilder query = new StringBuilder(DISTINCT_COMPONENT_NAMES);
    addBucketConstraints(null, buckets, query);
    query.append(ORDER_BY_NAME);
    final OResultSet<ODocument> resultSet = db
        .command(new OSQLSynchQuery<>(query.toString()))
        .execute();
    return Lists.newArrayList(Iterables.transform(
        resultSet, (ODocument input) -> input == null ? null : input.field(P_NAME, String.class)
    ));
  }
}
