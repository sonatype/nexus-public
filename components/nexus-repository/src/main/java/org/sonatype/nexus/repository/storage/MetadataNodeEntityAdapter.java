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

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.EntityHelper;
import org.sonatype.nexus.orient.entity.AttachedEntityId;
import org.sonatype.nexus.orient.entity.IterableEntityAdapter;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * {@link MetadataNode} entity-adapter.
 *
 * @since 3.0
 */
public abstract class MetadataNodeEntityAdapter<T extends MetadataNode<?>>
    extends IterableEntityAdapter<T>
{
  /**
   * Key of {@link Bucket}, {@link Component} and {@link Asset} attributes nested map.
   */
  public static final String P_ATTRIBUTES = "attributes";

  /**
   * Key of {@link Component} and {@link Asset} bucket reference attribute.
   */
  public static final String P_BUCKET = "bucket";

  /**
   * Key of {@link Component} and {@link Asset} attribute for format reference.
   */
  public static final String P_FORMAT = "format";

  /**
   * Key of {@link Component} name coordinate.
   */
  public static final String P_NAME = "name";

  /**
   * Key of {@link Component} and {@link Asset} attribute denoting when the record was last updated. This denotes a
   * timestamp when CMA last modified any attribute of the record, and has nothing to do with content change, it's age
   * or it's last modified attributes. This property is present always on {@link Component} and {@link Asset}.
   *
   * @see MetadataNodeEntityAdapter#writeFields(ODocument, MetadataNode)
   */
  static final String P_LAST_UPDATED = "last_updated";

  protected final BucketEntityAdapter bucketEntityAdapter;

  public MetadataNodeEntityAdapter(final String typeName, final BucketEntityAdapter bucketEntityAdapter) {
    super(typeName);
    this.bucketEntityAdapter = bucketEntityAdapter;
  }

  @Override
  protected void defineType(final OClass type) {
    type.createProperty(P_BUCKET, OType.LINK, bucketEntityAdapter.getSchemaType()).setMandatory(true).setNotNull(true);
    type.createProperty(P_FORMAT, OType.STRING).setMandatory(true).setNotNull(true);
    type.createProperty(P_LAST_UPDATED, OType.DATETIME);
    type.createProperty(P_ATTRIBUTES, OType.EMBEDDEDMAP);
  }

  @Override
  protected void readFields(final ODocument document, final T entity) {
    ORID bucketId = document.field(P_BUCKET, ORID.class);
    String format = document.field(P_FORMAT, OType.STRING);
    Date lastUpdated = document.field(P_LAST_UPDATED, OType.DATETIME);
    Map<String, Object> attributes = document.field(P_ATTRIBUTES, OType.EMBEDDEDMAP);

    entity.bucketId(new AttachedEntityId(bucketEntityAdapter, bucketId));
    entity.format(format);
    entity.lastUpdated(new DateTime(lastUpdated));
    entity.attributes(new NestedAttributesMap(P_ATTRIBUTES, attributes));
    entity.newEntity(document.getIdentity().isNew());
  }

  @Override
  protected void writeFields(final ODocument document, final T entity) {
    document.field(P_BUCKET, bucketEntityAdapter.recordIdentity(entity.bucketId()));
    document.field(P_FORMAT, entity.format());
    document.field(P_LAST_UPDATED, new Date());
    document.field(P_ATTRIBUTES, entity.attributes().backing());
  }

  Iterable<T> browseByBucket(final ODatabaseDocumentTx db, final Bucket bucket) {
    checkNotNull(bucket);
    checkState(EntityHelper.hasMetadata(bucket));

    Map<String, Object> parameters = ImmutableMap.<String, Object>of(
        "bucket", bucketEntityAdapter.recordIdentity(bucket)
    );
    String query = String.format(
        "select from %s where %s = :bucket",
        getTypeName(), P_BUCKET
    );
    Iterable<ODocument> docs = OrientAsyncHelper.asyncIterable(db, query, parameters);
    return transform(docs);
  }

  T findByProperty(final ODatabaseDocumentTx db,
                   final String propName, final Object propValue,
                   final Bucket bucket)
  {
    checkNotNull(propName);
    checkNotNull(propValue);
    checkNotNull(bucket);

    Map<String, Object> parameters = ImmutableMap.of(
        "bucket", bucketEntityAdapter.recordIdentity(bucket),
        "propValue", propValue
    );
    String query = String.format(
        "select from %s where %s = :bucket and %s = :propValue",
        getTypeName(), P_BUCKET, propName
    );
    Iterable<ODocument> docs = db.command(new OCommandSQL(query)).execute(parameters);
    ODocument first = Iterables.getFirst(docs, null);
    return first != null ? readEntity(first) : null;
  }

  Iterable<T> browseByQuery(final ODatabaseDocumentTx db,
                            @Nullable final String whereClause,
                            @Nullable final Map<String, Object> parameters,
                            @Nullable final Iterable<Bucket> buckets,
                            @Nullable final String querySuffix)
  {
    String query = buildQuery(false, whereClause, buckets, querySuffix);
    log.debug("Finding {}s with query: {}, parameters: {}", getTypeName(), query, parameters);
    Iterable<ODocument> docs = db.command(new OCommandSQL(query)).execute(parameters);
    return transform(docs);
  }

  long countByQuery(final ODatabaseDocumentTx db,
                    @Nullable final String whereClause,
                    @Nullable final Map<String, Object> parameters,
                    @Nullable final Iterable<Bucket> buckets,
                    @Nullable final String querySuffix)
  {
    String query = buildQuery(true, whereClause, buckets, querySuffix);
    log.debug("Counting {}s with query: {}, parameters: {}", getTypeName(), query, parameters);
    List<ODocument> results = db.command(new OCommandSQL(query)).execute(parameters);
    return results.get(0).field("count");
  }

  private String buildQuery(final boolean isCount,
                            @Nullable final String whereClause,
                            @Nullable final Iterable<Bucket> buckets,
                            @Nullable final String querySuffix)
  {
    StringBuilder query = new StringBuilder();
    query.append("select");
    if (isCount) {
      query.append(" count(*)");
    }
    query.append(" from ").append(getTypeName());
    if (whereClause != null) {
      query.append(" where (").append(whereClause).append(")");
    }

    addBucketConstraints(whereClause, buckets, query);

    if (querySuffix != null) {
      query.append(" ").append(querySuffix);
    }

    return query.toString();
  }

  /**
   * Constrain a query to certain buckets.
   */
  protected void addBucketConstraints(@Nullable final String whereClause,
                                      @Nullable final Iterable<Bucket> buckets,
                                      final StringBuilder query)
  {
    if (buckets != null) {
      List<String> bucketConstraints = Lists.newArrayList(
          Iterables.transform(buckets, new Function<Bucket, String>()
          {
            @Override
            public String apply(final Bucket bucket) {
              return String.format("%s=%s", P_BUCKET, bucketEntityAdapter.recordIdentity(bucket));
            }
          }).iterator());
      if (bucketConstraints.size() > 0) {
        if (whereClause == null) {
          query.append(" where");
        }
        else {
          query.append(" and");
        }
        query.append(" (");
        query.append(Joiner.on(" or ").join(bucketConstraints));
        query.append(")");
      }
    }
  }
}
