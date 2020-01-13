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
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.common.entity.EntityEvent;
import org.sonatype.nexus.common.entity.EntityHelper;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.EntityMetadata;
import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.OIndexBuilder;
import org.sonatype.nexus.orient.OIndexNameBuilder;
import org.sonatype.nexus.orient.entity.AttachedEntityId;
import org.sonatype.nexus.orient.entity.AttachedEntityMetadata;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.isEmpty;
import static java.lang.String.format;
import static org.sonatype.nexus.common.entity.EntityHelper.id;
import static org.sonatype.nexus.repository.storage.BucketEntityAdapter.P_REPOSITORY_NAME;

/**
 * {@link Asset} entity-adapter.
 *
 * @since 3.0
 */
@Named
@Singleton
public class AssetEntityAdapter
    extends MetadataNodeEntityAdapter<Asset>
{
  private static final String DB_CLASS = new OClassNameBuilder()
      .type("asset")
      .build();

  /**
   * Applied (optionally) to asset only, holds a value from format describing what current asset is.
   */
  public static final String P_ASSET_KIND = "asset_kind";

  /**
   * Key of {@link Asset} blob ref attribute (if asset has backing content).
   *
   * @see StorageTx#attachBlob(Asset, AssetBlob)
   */
  public static final String P_BLOB_REF = "blob_ref";

  /**
   * Key of {@link Asset} component reference attribute (if asset belongs to a component).
   */
  public static final String P_COMPONENT = "component";

  /**
   * Key of {@link Asset} for content type attribute (if asset has backing content).
   *
   * @see StorageTx#attachBlob(Asset, AssetBlob)
   */
  public static final String P_CONTENT_TYPE = "content_type";

  /**
   * Key of {@link Asset} attribute denoting when it was last downloaded.
   */
  public static final String P_LAST_DOWNLOADED = "last_downloaded";

  /**
   * Key of {@link Asset} size attribute (if asset has backing content).
   *
   * @see StorageTx#attachBlob(Asset, AssetBlob)
   */
  public static final String P_SIZE = "size";

  /**
   * Key of {@link Asset} created by attribute (if asset has backing content).
   *
   * @see StorageTx#attachBlob(Asset, AssetBlob)
   */
  public static final String P_CREATED_BY = "created_by";

  /**
   * Key of {@link Asset} created by ip attribute (if asset has backing content).
   *
   * @see StorageTx#attachBlob(Asset, AssetBlob)
   */
  public static final String P_CREATED_BY_IP = "created_by_ip";

  /**
   * Key of {@link Asset} attribute indicating when a blob was first attached to this asset.
   */
  public static final String P_BLOB_CREATED = "blob_created";

  /**
   * Key of {@link Asset} attribute indicating when a blob was updated (on creation or attaching a different blob).
   */
  public static final String P_BLOB_UPDATED = "blob_updated";

  public static final String I_BUCKET_COMPONENT_NAME = new OIndexNameBuilder()
      .type(DB_CLASS)
      .property(P_BUCKET)
      .property(P_COMPONENT)
      .property(P_NAME)
      .build();

  private static final String I_BUCKET_NAME = new OIndexNameBuilder()
      .type(DB_CLASS)
      .property(P_BUCKET)
      .property(P_NAME)
      .build();

  public static final String I_COMPONENT = new OIndexNameBuilder()
      .type(DB_CLASS)
      .property(P_COMPONENT)
      .build();

  public static final String I_NAME_CASEINSENSITIVE = new OIndexNameBuilder()
      .type(DB_CLASS)
      .property(P_NAME)
      .caseInsensitive()
      .build();

  private static final String EXISTS_QUERY_STRING = format("select from index:%1$s where key = [:%2$s, :%3$s]",
      I_BUCKET_NAME, P_BUCKET, P_NAME);

  private final ComponentEntityAdapter componentEntityAdapter;

  @Inject
  public AssetEntityAdapter(final BucketEntityAdapter bucketEntityAdapter,
                            final ComponentEntityAdapter componentEntityAdapter)
  {
    super(DB_CLASS, bucketEntityAdapter);
    this.componentEntityAdapter = componentEntityAdapter;
  }

  @Override
  protected void defineType(final ODatabaseDocumentTx db, final OClass type) {
    super.defineType(type);
    type.createProperty(P_COMPONENT, OType.LINK, componentEntityAdapter.getSchemaType());
    type.createProperty(P_NAME, OType.STRING)
        .setMandatory(true)
        .setNotNull(true);
    type.createProperty(P_SIZE, OType.LONG);
    type.createProperty(P_CONTENT_TYPE, OType.STRING);
    type.createProperty(P_BLOB_REF, OType.STRING);
    type.createProperty(P_LAST_DOWNLOADED, OType.DATETIME);
    type.createProperty(P_BLOB_CREATED, OType.DATETIME);
    type.createProperty(P_BLOB_UPDATED, OType.DATETIME);
    type.createProperty(P_CREATED_BY, OType.STRING);
    type.createProperty(P_CREATED_BY_IP, OType.STRING);

    ODocument metadata = db.newInstance()
        .field("ignoreNullValues", false)
        .field("mergeKeys", false);
    type.createIndex(I_BUCKET_COMPONENT_NAME, INDEX_TYPE.UNIQUE.name(), null, metadata,
        new String[]{P_BUCKET, P_COMPONENT, P_NAME}
    );
    type.createIndex(I_BUCKET_NAME, INDEX_TYPE.NOTUNIQUE, P_BUCKET, P_NAME);
    type.createIndex(I_COMPONENT, INDEX_TYPE.NOTUNIQUE, P_COMPONENT);

    new OIndexBuilder(type, I_NAME_CASEINSENSITIVE, INDEX_TYPE.NOTUNIQUE)
        .property(P_NAME, OType.STRING)
        .caseInsensitive()
        .build(db);
  }

  @Override
  protected Asset newEntity() {
    return new Asset();
  }

  @Override
  protected void readFields(final ODocument document, final Asset entity) {
    super.readFields(document, entity);

    ORID componentId = document.field(P_COMPONENT, ORID.class);
    String name = document.field(P_NAME, OType.STRING);
    Long size = document.field(P_SIZE, OType.LONG);
    String contentType = document.field(P_CONTENT_TYPE, OType.STRING);
    String blobRef = document.field(P_BLOB_REF, OType.STRING);
    Date lastDownloaded = document.field(P_LAST_DOWNLOADED, OType.DATETIME);
    Date blobCreated = document.field(P_BLOB_CREATED, OType.DATETIME);
    Date blobUpdated = document.field(P_BLOB_UPDATED, OType.DATETIME);
    String createdBy = document.field(P_CREATED_BY, OType.STRING);
    String createdByIp = document.field(P_CREATED_BY_IP, OType.STRING);

    if (componentId != null) {
      entity.componentId(new AttachedEntityId(componentEntityAdapter, componentId));
    }
    entity.name(name);
    entity.size(size);
    entity.contentType(contentType);
    entity.createdBy(createdBy);
    entity.createdByIp(createdByIp);
    if (blobRef != null) {
      entity.blobRef(BlobRef.parse(blobRef));
    }
    if (lastDownloaded != null) {
      entity.lastDownloaded(new DateTime(lastDownloaded));
    }
    if (blobCreated != null) {
      entity.blobCreated(new DateTime(blobCreated));
    }
    if (blobUpdated != null) {
      entity.blobUpdated(new DateTime(blobUpdated));
    }
  }

  @Override
  protected void writeFields(final ODocument document, final Asset entity) {
    super.writeFields(document, entity);

    EntityId componentId = entity.componentId();
    document.field(P_COMPONENT, componentId != null ? componentEntityAdapter.recordIdentity(componentId) : null);
    document.field(P_NAME, entity.name());
    document.field(P_SIZE, entity.size());
    document.field(P_CONTENT_TYPE, entity.contentType());
    document.field(P_CREATED_BY, entity.createdBy());
    document.field(P_CREATED_BY_IP, entity.createdByIp());
    BlobRef blobRef = entity.blobRef();
    document.field(P_BLOB_REF, blobRef != null ? blobRef.toString() : null);
    DateTime lastDownloaded = entity.lastDownloaded();
    document.field(P_LAST_DOWNLOADED, lastDownloaded != null ? lastDownloaded.toDate() : null);
    DateTime blobCreated = entity.blobCreated();
    document.field(P_BLOB_CREATED, blobCreated != null ? blobCreated.toDate() : null);
    DateTime blobUpdated = entity.blobUpdated();
    document.field(P_BLOB_UPDATED, blobUpdated != null ? blobUpdated.toDate() : null);
  }

  Asset findByProperty(final ODatabaseDocumentTx db,
                       final String propName,
                       final Object propValue,
                       final Component component)
  {
    checkNotNull(propName);
    checkNotNull(propValue);
    checkNotNull(component);

    Map<String, Object> parameters = ImmutableMap.of(
        "bucket", bucketEntityAdapter.recordIdentity(component.bucketId()),
        "component", componentEntityAdapter.recordIdentity(component),
        "propValue", propValue
    );
    String query = String.format(
        "select from %s where %s = :bucket and %s = :component and %s = :propValue",
        DB_CLASS, P_BUCKET, P_COMPONENT, propName
    );
    Iterable<ODocument> docs = db.command(new OCommandSQL(query)).execute(parameters);
    ODocument first = Iterables.getFirst(docs, null);
    return first != null ? readEntity(first) : null;
  }

  Iterable<Asset> browseByComponent(final ODatabaseDocumentTx db, final Component component) {
    checkNotNull(component);
    checkState(EntityHelper.hasMetadata(component));

    Map<String, Object> parameters = ImmutableMap.of(
        "bucket", bucketEntityAdapter.recordIdentity(component.bucketId()),
        "component", componentEntityAdapter.recordIdentity(component)
    );
    String query = String.format(
        "select from %s where %s = :bucket and %s = :component",
        DB_CLASS, P_BUCKET, P_COMPONENT
    );
    Iterable<ODocument> docs = db.command(new OCommandSQL(query)).execute(parameters);
    return transform(docs);
  }

  public Asset findByBucketComponentName(final ODatabaseDocumentTx db,
                                         final ORID bucketId,
                                         @Nullable final ORID componentId,
                                         final String name)
  {
    String query = "select from " + DB_CLASS + " where " + P_BUCKET + " = :bucket ";
    query += "and " + P_COMPONENT + (componentId == null ? " is null " : " = :component ");
    query +=  "and " + P_NAME + " = :name";
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(P_BUCKET, bucketId);
    parameters.put(P_NAME, name);
    if (componentId != null) {
      parameters.put(P_COMPONENT, componentId);
    }
    Iterable<ODocument> docs = db.command(new OCommandSQL(query)).execute(parameters);
    ODocument first = Iterables.getFirst(docs, null);
    return first != null ? readEntity(first) : null;
  }

  @Override
  public boolean sendEvents() {
    return true;
  }

  @Override
  public EntityEvent newEvent(final ODocument document, final EventKind eventKind) {
    EntityMetadata metadata = new AttachedEntityMetadata(this, document);

    String repositoryName = ((ODocument) document.field(P_BUCKET)).field(P_REPOSITORY_NAME);

    ORID rid = document.field(P_COMPONENT, ORID.class);
    EntityId componentId = rid != null ? new AttachedEntityId(componentEntityAdapter, rid) : null;

    switch (eventKind) {
      case CREATE:
        return new AssetCreatedEvent(metadata, repositoryName, componentId);
      case UPDATE:
        return new AssetUpdatedEvent(metadata, repositoryName, componentId);
      case DELETE:
        return new AssetDeletedEvent(metadata, repositoryName, componentId);
      default:
        return null;
    }
  }

  /**
   * Custom affinity based on name, so recreated assets will have the same affinity.
   * Attempts to give asset events the same affinity as events from their component.
   */
  @Override
  public String eventAffinity(final ODocument document) {
    ORID bucketId = document.field(P_BUCKET, ORID.class);
    // this either returns a pre-fetched document or an ORID needing fetching
    OIdentifiable component = document.field(P_COMPONENT, OIdentifiable.class);
    ODocument node = null;
    if (component != null) {
      node = component.getRecord(); // fetch document (no-op if it's already there)
    }
    if (node == null) {
      node = document; // no component (or it's AWOL) so fall back to asset node
    }
    return bucketId + "@" + node.field(P_NAME, OType.STRING);
  }

  public boolean exists(final ODatabaseDocumentTx db, final String name, final Bucket bucket) {
    Map<String, Object> params = ImmutableMap.of(
        P_NAME, checkNotNull(name),
        P_BUCKET, recordIdentity(id(checkNotNull(bucket)))
    );

    OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<>(EXISTS_QUERY_STRING, 1);
    return !isEmpty(db.command(query).<Iterable<ODocument>>execute(params));
  }

  /**
   * Enables deconfliction of asset metadata.
   */
  @Override
  public boolean resolveConflicts() {
    return true;
  }
}
