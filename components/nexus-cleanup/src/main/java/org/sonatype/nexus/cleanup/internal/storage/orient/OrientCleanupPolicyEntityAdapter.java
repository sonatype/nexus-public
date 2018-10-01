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
package org.sonatype.nexus.cleanup.internal.storage.orient;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.cleanup.storage.CleanupPolicy;
import org.sonatype.nexus.cleanup.storage.event.CleanupPolicyCreatedEvent;
import org.sonatype.nexus.cleanup.storage.event.CleanupPolicyDeletedEvent;
import org.sonatype.nexus.cleanup.storage.event.CleanupPolicyUpdatedEvent;
import org.sonatype.nexus.common.entity.EntityEvent;
import org.sonatype.nexus.common.entity.EntityMetadata;
import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.OIndexNameBuilder;
import org.sonatype.nexus.orient.entity.AttachedEntityMetadata;
import org.sonatype.nexus.orient.entity.IterableEntityAdapter;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.sonatype.nexus.cleanup.storage.CleanupPolicy.ALL_CLEANUP_POLICY_FORMAT;

/**
 * Entity adapter for {@link CleanupPolicy}
 *
 * @since 3.14
 */
@Named
@Singleton
public class OrientCleanupPolicyEntityAdapter
    extends IterableEntityAdapter<CleanupPolicy>
{
  private static final String DB_CLASS = new OClassNameBuilder()
      .type("cleanup")
      .build();

  private static final String MAX_NAME_LENGTH = "255";

  private static final String P_NAME = "name";

  private static final String P_NOTES = "notes";

  private static final String P_FORMAT = "format";

  private static final String P_MODE = "mode";

  private static final String P_CRITERIA = "criteria";

  private static final String I_NAME = new OIndexNameBuilder()
      .type(DB_CLASS)
      .property(P_NAME)
      .build();

  private static final String I_FORMAT = new OIndexNameBuilder()
      .type(DB_CLASS)
      .property(P_FORMAT)
      .build();

  private static final String BROWSE_BY_FORMAT_WHERE_CLAUSE =
      format(" WHERE (format = :format OR format = \"%s\")", ALL_CLEANUP_POLICY_FORMAT);

  public OrientCleanupPolicyEntityAdapter() {
    super(DB_CLASS);
  }

  @Override
  protected void defineType(final OClass type) {
    type.createProperty(P_NAME, OType.STRING).setMandatory(true).setMax(MAX_NAME_LENGTH).setNotNull(true);
    type.createProperty(P_NOTES, OType.STRING).setMandatory(false).setNotNull(false);
    type.createProperty(P_FORMAT, OType.STRING).setMandatory(true).setNotNull(true);
    type.createProperty(P_MODE, OType.STRING).setMandatory(true).setNotNull(true);
    type.createProperty(P_CRITERIA, OType.EMBEDDEDMAP).setMandatory(true).setNotNull(true);

    type.createIndex(I_NAME, INDEX_TYPE.UNIQUE, P_NAME);
    type.createIndex(I_FORMAT, INDEX_TYPE.NOTUNIQUE, P_FORMAT);
  }

  @Override
  protected CleanupPolicy newEntity() {
    return new CleanupPolicy();
  }

  @Override
  protected void readFields(final ODocument document, final CleanupPolicy entity) {
    entity.setName(document.field(P_NAME, OType.STRING));
    entity.setNotes(document.field(P_NOTES, OType.STRING));
    entity.setFormat(document.field(P_FORMAT, OType.STRING));
    entity.setMode(document.field(P_MODE, OType.STRING));
    entity.setCriteria(document.field(P_CRITERIA, OType.EMBEDDEDMAP));
  }

  @Override
  protected void writeFields(final ODocument document, final CleanupPolicy entity) {
    document.field(P_NAME, entity.getName());
    document.field(P_NOTES, entity.getNotes());
    document.field(P_FORMAT, entity.getFormat());
    document.field(P_MODE, entity.getMode());
    document.field(P_CRITERIA, entity.getCriteria());
  }

  @Nullable
  private ODocument findDocument(final ODatabaseDocumentTx db, final String id) {
    ORID rid = getRecordIdObfuscator().decode(getSchemaType(), id);
    return db.getRecord(rid);
  }

  public CleanupPolicy get(final ODatabaseDocumentTx db, final String name) {
    checkNotNull(db);
    checkNotNull(name);

    String whereClause = " WHERE (name = :name) LIMIT 1";
    Iterable<ODocument> result = buildAndRunQuery(db, whereClause, ImmutableMap.of("name", name));
    Iterable<CleanupPolicy> policies = transform(result);

    return Iterables.getFirst(policies, null); //NOSONAR
  }

  public boolean exists(final ODatabaseDocumentTx db, final String name) {
    checkNotNull(db);
    checkNotNull(name);

    ImmutableMap<String, String> parameters = ImmutableMap.of("name", name.toUpperCase());
    StringBuilder query = new StringBuilder("SELECT COUNT(*) FROM " + DB_CLASS + " WHERE (name.toUpperCase() = :name)");

    log.debug("Counting {}s with query: {}, parameters: {}", getTypeName(), query, parameters);

    List<ODocument> result = db.command(new OCommandSQL(query.toString())).execute(parameters);
    return ((long) result.get(0).field("COUNT")) != 0L;
  }

  public Iterable<CleanupPolicy> browseByFormat(final ODatabaseDocumentTx db, final String format) {
    checkNotNull(format);
    Iterable<ODocument> result = buildAndRunQuery(db, BROWSE_BY_FORMAT_WHERE_CLAUSE, ImmutableMap.of("format", format));
    return transform(result);
  }

  private Iterable<ODocument> buildAndRunQuery(final ODatabaseDocumentTx db,
                                               final String whereClause,
                                               final Map<String, String> parameters)
  {
    StringBuilder query = new StringBuilder("SELECT FROM " + DB_CLASS + whereClause);
    log.debug("Finding {}s with query: {}, parameters: {}", getTypeName(), query, parameters);
    return db.command(new OCommandSQL(query.toString())).execute(parameters);
  }

  @Override
  public boolean sendEvents() {
    return true;
  }

  @Nullable
  @Override
  public EntityEvent newEvent(final ODocument document, final EventKind eventKind) {
    EntityMetadata metadata = new AttachedEntityMetadata(this, document);

    switch (eventKind) {
      case CREATE:
        return new CleanupPolicyCreatedEvent(metadata);
      case UPDATE:
        return new CleanupPolicyUpdatedEvent(metadata);
      case DELETE:
        return new CleanupPolicyDeletedEvent(metadata);
      default:
        return null;
    }
  }
}
