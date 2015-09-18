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
import org.sonatype.nexus.orient.OIndexNameBuilder;
import org.sonatype.nexus.orient.entity.AttachedEntityId;
import org.sonatype.nexus.orient.entity.IterableEntityAdapter;
import org.sonatype.nexus.repository.Repository;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.sonatype.nexus.repository.storage.StorageFacet.P_ATTRIBUTES;
import static org.sonatype.nexus.repository.storage.StorageFacet.P_BUCKET;
import static org.sonatype.nexus.repository.storage.StorageFacet.P_FORMAT;
import static org.sonatype.nexus.repository.storage.StorageFacet.P_LAST_UPDATED;
import static org.sonatype.nexus.repository.storage.StorageFacet.P_REPOSITORY_NAME;

/**
 * {@link MetadataNode} entity-adapter.
 *
 * @since 3.0
 */
public abstract class MetadataNodeEntityAdapter<T extends MetadataNode<?>>
    extends IterableEntityAdapter<T>
{
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

    String indexName = new OIndexNameBuilder().type(getTypeName()).property(P_BUCKET).build();
    type.createIndex(indexName, INDEX_TYPE.NOTUNIQUE, P_BUCKET);
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
    return readEntities(docs);
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
                            final @Nullable String whereClause,
                            final @Nullable Map<String, Object> parameters,
                            final @Nullable Iterable<Repository> repositories,
                            final @Nullable String querySuffix)
  {
    String query = buildQuery(false, whereClause, repositories, querySuffix);
    log.debug("Finding {}s with query: {}, parameters: {}", getTypeName(), query, parameters);
    Iterable<ODocument> docs = db.command(new OCommandSQL(query)).execute(parameters);
    return readEntities(docs);
  }

  long countByQuery(final ODatabaseDocumentTx db,
                    final @Nullable String whereClause,
                    final @Nullable Map<String, Object> parameters,
                    final @Nullable Iterable<Repository> repositories,
                    final @Nullable String querySuffix)
  {
    String query = buildQuery(true, whereClause, repositories, querySuffix);
    log.debug("Counting {}s with query: {}, parameters: {}", getTypeName(), query, parameters);
    List<ODocument> results = db.command(new OCommandSQL(query)).execute(parameters);
    return results.get(0).field("count");
  }

  private String buildQuery(final boolean isCount,
                            final @Nullable String whereClause,
                            final @Nullable Iterable<Repository> repositories,
                            final @Nullable String querySuffix)
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

    if (repositories != null) {
      List<String> bucketConstraints = Lists.newArrayList(
          Iterables.transform(repositories, new Function<Repository, String>()
          {
            @Override
            public String apply(final Repository repository) {
              return String.format("%s.%s = '%s'", P_BUCKET, P_REPOSITORY_NAME, repository.getName());
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

    if (querySuffix != null) {
      query.append(" ").append(querySuffix);
    }

    return query.toString();
  }

  protected Iterable<T> readEntities(final Iterable<ODocument> documents) {
    return Iterables.transform(
        documents,
        new Function<ODocument, T>()
        {
          @Override
          public T apply(final ODocument doc) {
            return readEntity(doc);
          }
        });
  }

}
