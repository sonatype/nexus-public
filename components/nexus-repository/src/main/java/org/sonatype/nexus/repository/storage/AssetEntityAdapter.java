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
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.common.entity.EntityEvent;
import org.sonatype.nexus.common.entity.EntityHelper;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.EntityMetadata;
import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.OIndexNameBuilder;
import org.sonatype.nexus.orient.entity.AttachedEntityId;
import org.sonatype.nexus.orient.entity.AttachedEntityMetadata;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
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
   * Key of {@link Asset} attribute denoting when it was last accessed.
   */
  public static final String P_LAST_ACCESSED = "last_accessed";

  /**
   * Key of {@link Asset} size attribute (if asset has backing content).
   *
   * @see StorageTx#attachBlob(Asset, AssetBlob)
   */
  public static final String P_SIZE = "size";

  private static final String I_BUCKET_COMPONENT_NAME = new OIndexNameBuilder()
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
    type.createProperty(P_LAST_ACCESSED, OType.DATETIME);

    ODocument metadata = db.newInstance()
        .field("ignoreNullValues", false)
        .field("mergeKeys", false);
    type.createIndex(I_BUCKET_COMPONENT_NAME, INDEX_TYPE.UNIQUE.name(), null, metadata,
        new String[]{P_BUCKET, P_COMPONENT, P_NAME}
    );
    type.createIndex(I_BUCKET_NAME, INDEX_TYPE.NOTUNIQUE, P_BUCKET, P_NAME);
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
    Date lastAccessed = document.field(P_LAST_ACCESSED, OType.DATETIME);

    if (componentId != null) {
      entity.componentId(new AttachedEntityId(componentEntityAdapter, componentId));
    }
    entity.name(name);
    entity.size(size);
    entity.contentType(contentType);
    if (blobRef != null) {
      entity.blobRef(BlobRef.parse(blobRef));
    }
    if (lastAccessed != null) {
      entity.lastAccessed(new DateTime(lastAccessed));
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
    BlobRef blobRef = entity.blobRef();
    document.field(P_BLOB_REF, blobRef != null ? blobRef.toString() : null);
    DateTime lastAccessed = entity.lastAccessed();
    document.field(P_LAST_ACCESSED, lastAccessed != null ? lastAccessed.toDate() : null);
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

    Map<String, Object> parameters = ImmutableMap.<String, Object>of(
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
}
