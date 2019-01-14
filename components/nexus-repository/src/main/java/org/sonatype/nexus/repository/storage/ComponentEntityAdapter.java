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

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.entity.EntityEvent;
import org.sonatype.nexus.common.entity.EntityMetadata;
import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.OIndexBuilder;
import org.sonatype.nexus.orient.OIndexNameBuilder;
import org.sonatype.nexus.orient.entity.AttachedEntityMetadata;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.orientechnologies.orient.core.collate.OCaseInsensitiveCollate;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.sonatype.nexus.common.entity.EntityHelper.id;
import static org.sonatype.nexus.repository.storage.BucketEntityAdapter.P_REPOSITORY_NAME;

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
  public static final String DB_CLASS = new OClassNameBuilder()
      .type("component")
      .build();

  /**
   * Key of {@link Component} group coordinate.
   */
  public static final String P_GROUP = "group";

  /**
   * Key of {@link Component} version coordinate.
   */
  public static final String P_VERSION = "version";

  /**
   * Key of {@link Component} ci_name (case-insensitive name) field.
   */
  public static final String P_CI_NAME = "ci_name";

  private static final String I_BUCKET_GROUP_NAME_VERSION = new OIndexNameBuilder()
      .type(DB_CLASS)
      .property(P_BUCKET)
      .property(P_GROUP)
      .property(P_NAME)
      .property(P_VERSION)
      .build();

  private static final String I_BUCKET_NAME_VERSION = new OIndexNameBuilder()
      .type(DB_CLASS)
      .property(P_BUCKET)
      .property(P_NAME)
      .property(P_VERSION)
      .build();

  private static final String I_CI_NAME_CASE_INSENSITIVE = new OIndexNameBuilder()
      .type(DB_CLASS)
      .property(P_CI_NAME)
      .caseInsensitive()
      .build();

  public static final String I_GROUP_NAME_VERSION_INSENSITIVE = new OIndexNameBuilder()
      .type(DB_CLASS)
      .property(P_GROUP)
      .property(P_NAME)
      .property(P_VERSION)
      .caseInsensitive()
      .build();

  private static final String EXISTS_QUERY_STRING =
      format("select from index:%1$s where key = [:%2$s, :%3$s, :%4$s, :%5$s]",
          I_BUCKET_GROUP_NAME_VERSION, P_BUCKET, P_GROUP, P_NAME, P_VERSION);

  private final ComponentFactory componentFactory;

  private final Set<ComponentEntityAdapterExtension> componentEntityAdapterExtensions;

  @Inject
  public ComponentEntityAdapter(final BucketEntityAdapter bucketEntityAdapter,
                                final ComponentFactory componentFactory,
                                final Set<ComponentEntityAdapterExtension> componentEntityAdapterExtensions)
  {
    super(DB_CLASS, bucketEntityAdapter);
    this.componentFactory = componentFactory;
    this.componentEntityAdapterExtensions = checkNotNull(componentEntityAdapterExtensions);
  }

  @Override
  protected void defineType(final ODatabaseDocumentTx db, final OClass type) {
    super.defineType(type);
    type.createProperty(P_GROUP, OType.STRING);
    type.createProperty(P_NAME, OType.STRING)
        .setMandatory(true)
        .setNotNull(true);
    type.createProperty(P_VERSION, OType.STRING);
    type.createProperty(P_CI_NAME, OType.STRING)
        .setCollate(new OCaseInsensitiveCollate())
        .setMandatory(true)
        .setNotNull(true);

    ODocument metadata = db.newInstance()
        .field("ignoreNullValues", false)
        .field("mergeKeys", false);
    type.createIndex(I_BUCKET_GROUP_NAME_VERSION, INDEX_TYPE.UNIQUE.name(), null, metadata,
        new String[]{P_BUCKET, P_GROUP, P_NAME, P_VERSION});
    type.createIndex(I_BUCKET_NAME_VERSION, INDEX_TYPE.NOTUNIQUE.name(), null, metadata,
        new String[]{P_BUCKET, P_NAME, P_VERSION});

    new OIndexBuilder(type, I_GROUP_NAME_VERSION_INSENSITIVE, INDEX_TYPE.NOTUNIQUE)
        .property(P_GROUP, OType.STRING)
        .property(P_NAME, OType.STRING)
        .property(P_VERSION, OType.STRING)
        .caseInsensitive()
        .build(db);

    new OIndexBuilder(type, I_CI_NAME_CASE_INSENSITIVE, INDEX_TYPE.NOTUNIQUE)
        .property(P_CI_NAME, OType.STRING)
        .caseInsensitive()
        .build(db);

    componentEntityAdapterExtensions.forEach(d -> d.defineType(db, type));
  }

  public Iterable<Component> browseByNameCaseInsensitive(final ODatabaseDocumentTx db,
                                                         final String name,
                                                         @Nullable final Iterable<Bucket> buckets,
                                                         @Nullable final String querySuffix)
  {
    checkNotNull(name);

    String whereClause = " where (ci_name = :name)";
    StringBuilder query = new StringBuilder("select from " + DB_CLASS + whereClause);

    addBucketConstraints(whereClause, buckets, query);

    if (querySuffix != null) {
      query.append(' ').append(querySuffix);
    }

    Map<String, Object> parameters = ImmutableMap.of("name", name);
    log.debug("Finding {}s with query: {}, parameters: {}", getTypeName(), query, parameters);
    return transform(db.command(new OCommandSQL(query.toString())).execute(parameters));
  }

  /**
   * Check for the existence of a component with {@code group}, {@code name}, and {@code version} in {@code bucket}.
   *
   * @since 3.8
   */
  public boolean exists(final ODatabaseDocumentTx db,
                        @Nullable final String group,
                        final String name,
                        @Nullable final String version,
                        final Bucket bucket)
  {
    Map<String, Object> params = Maps.newHashMap();
    params.put(P_GROUP, group);
    params.put(P_NAME, checkNotNull(name));
    params.put(P_VERSION, version);
    params.put(P_BUCKET, recordIdentity(id(checkNotNull(bucket))));
    OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<>(EXISTS_QUERY_STRING, 1);
    return !Iterables.isEmpty(db.command(query).<Iterable<ODocument>>execute(params));
  }

  @Override
  protected Component newEntity() {
    return componentFactory.createComponent();
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

    componentEntityAdapterExtensions.forEach(d -> d.readFields(document, entity));
  }

  @Override
  protected void writeFields(final ODocument document, final Component entity) {
    super.writeFields(document, entity);

    document.field(P_GROUP, entity.group());
    document.field(P_NAME, entity.name());
    document.field(P_VERSION, entity.version());

    // need to lowercase ID value as workaround for https://www.prjhub.com/#/issues/8750 (case-insensitive querying)
    // indirect queries against CI fields when another table is involved lose their case-insensitiveness, so we store
    // as lowercase to permit those kinds of queries to query on lowercase as a workaround for now.
    document.field(P_CI_NAME, entity.name().toLowerCase(Locale.ENGLISH));

    componentEntityAdapterExtensions.forEach(d -> d.writeFields(document, entity));
  }

  @Override
  public boolean sendEvents() {
    return true;
  }

  @Override
  public EntityEvent newEvent(final ODocument document, final EventKind eventKind) {
    EntityMetadata metadata = new AttachedEntityMetadata(this, document);

    String repositoryName = ((ODocument) document.field(P_BUCKET)).field(P_REPOSITORY_NAME);

    // prefetch any lazy/linked fields that the extensions want to capture
    componentEntityAdapterExtensions.forEach(d -> d.prefetchFields(document));

    switch (eventKind) {
      case CREATE:
        return new ComponentCreatedEvent(metadata, repositoryName);
      case UPDATE:
        return new ComponentUpdatedEvent(metadata, repositoryName);
      case DELETE:
        return new ComponentDeletedEvent(metadata, repositoryName);
      default:
        return null;
    }
  }

  /**
   * Custom affinity based on name, so recreated components will have the same affinity.
   */
  @Override
  public String eventAffinity(final ODocument document) {
    ORID bucketId = document.field(P_BUCKET, ORID.class);
    return bucketId + "@" + document.field(P_NAME, OType.STRING);
  }

  /**
   * Enables deconfliction of component metadata.
   */
  @Override
  public boolean resolveConflicts() {
    return true;
  }
}
