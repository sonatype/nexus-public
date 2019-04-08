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
package org.sonatype.nexus.orient.entity;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.collect.DetachingList;
import org.sonatype.nexus.common.collect.DetachingMap;
import org.sonatype.nexus.common.collect.DetachingSet;
import org.sonatype.nexus.common.entity.Entity;
import org.sonatype.nexus.common.entity.EntityCreatedEvent;
import org.sonatype.nexus.common.entity.EntityDeletedEvent;
import org.sonatype.nexus.common.entity.EntityEvent;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.EntityMetadata;
import org.sonatype.nexus.common.entity.EntityUpdatedEvent;
import org.sonatype.nexus.orient.RecordIdObfuscator;
import org.sonatype.nexus.orient.internal.PbeCompression;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import com.orientechnologies.orient.core.db.ODatabase;
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.db.record.OTrackedList;
import com.orientechnologies.orient.core.db.record.OTrackedMap;
import com.orientechnologies.orient.core.db.record.OTrackedSet;
import com.orientechnologies.orient.core.exception.OSecurityException;
import com.orientechnologies.orient.core.exception.OStorageException;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.storage.OCluster;
import com.orientechnologies.orient.core.storage.OStorage;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Streams.stream;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toSet;
import static org.sonatype.nexus.common.entity.EntityHelper.id;
import static org.sonatype.nexus.orient.entity.ConflictState.DENY;
import static org.sonatype.nexus.orient.entity.ConflictState.IGNORE;

/**
 * Support for entity-adapter implementations.
 *
 * @since 3.0
 */
public abstract class EntityAdapter<T extends Entity>
    extends ComponentSupport
{
  /**
   * @since 3.1
   */
  public enum EventKind
  {
    CREATE, UPDATE, DELETE
  }

  private final String typeName;

  private final Class<T> entityType;

  private RecordIdObfuscator recordIdObfuscator;

  private EntityHook entityHook;

  private ConflictHook conflictHook;

  private List<? extends DeconflictStep<T>> deconflictSteps = ImmutableList.of();

  private String dbName;

  private OClass schemaType;

  private boolean partialLoading;

  @SuppressWarnings("unchecked")
  public EntityAdapter(final String typeName) {
    this.typeName = checkNotNull(typeName);

    // this extracts the concrete entity type from the generic signature of any sub-classes
    final TypeToken<?> superType = TypeToken.of(getClass()).getSupertype(EntityAdapter.class);
    entityType = (Class<T>) ((ParameterizedType) superType.getType()).getActualTypeArguments()[0];
  }

  public String getTypeName() {
    return typeName;
  }

  public Class<T> getEntityType() {
    return entityType;
  }

  @Inject
  public void enableObfuscation(final RecordIdObfuscator recordIdObfuscator) {
    this.recordIdObfuscator = checkNotNull(recordIdObfuscator);
  }

  @Inject
  public void enableEntityHook(final EntityHook entityHook) {
    this.entityHook = checkNotNull(entityHook);
  }

  @Inject
  public void enableConflictHook(final ConflictHook conflictHook) {
    this.conflictHook = checkNotNull(conflictHook);
  }

  @Inject
  public void setDeconflictSteps(final List<? extends DeconflictStep<T>> deconflictSteps) {
    this.deconflictSteps = checkNotNull(deconflictSteps);
  }

  /**
   * Declares that this adapter may partially load a subset of the declared fields.
   *
   * @since 3.6
   */
  protected void enablePartialLoading() {
    this.partialLoading = true;
  }

  protected RecordIdObfuscator getRecordIdObfuscator() {
    checkState(recordIdObfuscator != null);
    return recordIdObfuscator;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "typeName='" + typeName + '\'' +
        '}';
  }

  //
  // Schema
  //

  public void register(final ODatabaseDocumentTx db, @Nullable final Runnable initializer) {
    checkNotNull(db);

    OSchema schema = db.getMetadata().getSchema();
    OClass type = schema.getClass(typeName);
    if (type == null) {
      int clusters = getMinimumClusters();
      type = clusters > 0 ? schema.createClass(typeName, clusters) : schema.createClass(typeName);
      defineType(db, type);

      log.debug("Created schema type '{}': properties={}, indexes={}",
          type,
          type.properties(),
          type.getIndexes()
      );

      if (initializer != null) {
        log.debug("Running initializer: {}", initializer);
        initializer.run();
      }
    }

    this.dbName = db.getName();
    this.schemaType = type;

    if (sendEvents() && entityHook != null) {
      entityHook.enableEvents(this);
    }

    if (resolveConflicts() && conflictHook != null) {
      conflictHook.enableConflictResolution(this);
      if (!conflictHook.equals(db.getConflictStrategy())) {
        db.setConflictStrategy(conflictHook);
      }
    }
  }

  public void register(final ODatabaseDocumentTx db) {
    register(db, null);
  }

  /**
   * Indicates the number of clusters to initially create for the type. The default implementation returns {@code 0} to
   * consult the corresponding database configuration for the number.
   * 
   * @since 3.7
   */
  protected int getMinimumClusters() {
    return 0;
  }

  protected void defineType(final ODatabaseDocumentTx db, final OClass type) {
    defineType(type);
  }

  protected abstract void defineType(final OClass type);

  /**
   * Enables password-based-encryption for records of the given type.
   *
   * Can only be called when creating the schema.
   *
   * @since 3.1
   */
  protected void enableRecordEncryption(final ODatabaseDocumentTx db, final OClass type) {
    OStorage storage = db.getStorage();
    for (int clusterId : type.getClusterIds()) {
      OCluster cluster = storage.getClusterById(clusterId);
      try {
        log.debug("Enabling PBE compression for cluster: {}", cluster.getName());
        cluster.set(OCluster.ATTRIBUTES.COMPRESSION, PbeCompression.NAME);
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
      catch (IllegalArgumentException | OStorageException | OSecurityException e) {
        log.warn("Cannot enable PBE compression for cluster: {}", cluster.getName(), e);
      }
    }
  }

  public String getDbName() {
    checkState(dbName != null, "Not registered");
    return dbName;
  }

  public OClass getSchemaType() {
    checkState(schemaType != null, "Not registered");
    return schemaType;
  }

  //
  // BREAD operations
  //

  protected abstract T newEntity();

  protected abstract void readFields(final ODocument document, final T entity) throws Exception;

  protected abstract void writeFields(final ODocument document, final T entity) throws Exception;

  /**
   * Browse all documents.
   */
  public Iterable<ODocument> browseDocuments(final ODatabaseDocumentTx db) {
    checkNotNull(db);
    return db.browseClass(typeName);
  }

  /**
   * Read entity from document.
   */
  public T readEntity(final OIdentifiable identifiable) {
    ODocument document = identifiable.getRecord(); // no-op if it's already a document
    checkNotNull(document);

    T entity = newEntity();
    if (!partialLoading) {
      document.deserializeFields();
    }
    try {
      readFields(document, entity);
    }
    catch (Exception e) {
      Throwables.throwIfUnchecked(e);
      throw new RuntimeException(e);
    }
    attachMetadata(entity, document);

    return entity;
  }

  /**
   * Write document from entity.
   */
  public ODocument writeEntity(final ODocument document, final T entity) {
    checkNotNull(document);
    checkNotNull(entity);

    // TODO: MVCC

    try {
      writeFields(document, entity);
    }
    catch (Exception e) {
      Throwables.throwIfUnchecked(e);
      throw new RuntimeException(e);
    }
    attachMetadata(entity, document);

    return document.save();
  }

  /**
   * Edit entity.
   */
  public ODocument editEntity(final ODatabaseDocumentTx db, final T entity) {
    checkNotNull(db);
    checkNotNull(entity);

    ORID rid = recordIdentity(entity);
    ODocument document = db.getRecord(rid);
    checkState(document != null);

    return writeEntity(document, entity);
  }

  /**
   * Add new entity.
   */
  public ODocument addEntity(final ODatabaseDocumentTx db, final T entity) {
    checkNotNull(db);
    checkNotNull(entity);

    // new entity must either have no metadata or it should be a new record
    EntityMetadata metadata = entity.getEntityMetadata();
    checkState(metadata == null || recordIdentity(metadata.getId()).isNew());

    ODocument doc = db.newInstance(typeName);
    return writeEntity(doc, entity);
  }

  /**
   * Delete an entity.
   */
  public void deleteEntity(final ODatabaseDocumentTx db, final T entity) {
    checkNotNull(db);
    checkNotNull(entity);

    // TODO: MVCC

    ORID rid = recordIdentity(entity);
    db.delete(rid);

    entity.setEntityMetadata(null);
  }

  //
  // Metadata support
  //

  /**
   * Attach metadata to entity.
   */
  protected void attachMetadata(final T entity, final ODocument doc) {
    entity.setEntityMetadata(new AttachedEntityMetadata(this, doc));
  }

  /**
   * Return the document for given {@link EntityId}.
   */
  public ODocument document(final ODatabaseDocumentTx db, final EntityId id) {
    ORID rid;
    if (id instanceof AttachedEntityId) {
      rid = ((AttachedEntityId)id).getIdentity();
    }
    else {
      rid = getRecordIdObfuscator().decode(getSchemaType(), id.getValue());
    }
    return db.getRecord(rid);
  }

  public Iterable<ODocument> documents(final ODatabaseDocumentTx db, final Iterable<EntityId> ids) {
    Set<ORID> rids = stream(ids).map(id -> {
      if (id instanceof AttachedEntityId) {
        return ((AttachedEntityId) id).getIdentity();
      }
      return getRecordIdObfuscator().decode(getSchemaType(), id.getValue());
    }).collect(toSet());

    return db.command(new OCommandSQL("select from :rids")).execute(singletonMap("rids", rids));
  }

  /**
   * Return record identity of entity.
   */
  public ORID recordIdentity(final T entity) {
    return recordIdentity(id(entity));
  }

  public ORID recordIdentity(final EntityId id) {
    checkNotNull(id);
    if (id instanceof AttachedEntityId) {
      return ((AttachedEntityId) id).getIdentity();
    }
    return getRecordIdObfuscator().decode(getSchemaType(), id.getValue());
  }

  /**
   * Makes the given (tracked) entity attributes detachable by using a lazy copy technique.
   * Detaching only occurs outside of transactions, see {@link #allowDetach()} for details.
   *
   * Without this the tracked Orient collections will attempt to call back into the current
   * transaction when their content is changed, which can lead to exceptions if there is no
   * surrounding transaction. Note: this is only an issue if you want to modify attributes
   * from an attached entity outside of a transaction.
   *
   * @since 3.5
   */
  protected <K, V> Map<K, V> detachable(final Map<K, V> attributes) {
    return new DetachingMap<>(attributes, this::allowDetach, this::detach);
  }

  /**
   * Only allow detaching when we have no DB context or it is from a different DB.
   * If we have a valid context from the same DB then we don't need to detach yet.
   */
  private boolean allowDetach() {
    ODatabase<?> db = ODatabaseRecordThreadLocal.INSTANCE.getIfDefined();
    return db == null || !getDbName().equals(db.getName());
  }

  /**
   * Lazily detaches the value; tracked collections are detached as their content is touched.
   *
   * @since 3.11
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  protected <V> V detach(final V value) {
    Object untracked = value;
    if (value instanceof OTrackedMap) {
      untracked = new DetachingMap((Map) value, this::allowDetach, this::detach);
    }
    else if (value instanceof OTrackedList) {
      untracked = new DetachingList((List) value, this::allowDetach, this::detach);
    }
    else if (value instanceof OTrackedSet) {
      untracked = new DetachingSet((Set) value, this::allowDetach, this::detach);
    }
    return (V) untracked;
  }

  //
  // Event support
  //

  /**
   * Override this method to enable {@link EntityEvent}s for this adapter.
   */
  public boolean sendEvents() {
    return false;
  }

  /**
   * Override this method to customize {@link EntityEvent}s for this adapter.
   */
  @Nullable
  public EntityEvent newEvent(final ODocument document, final EventKind eventKind)  {
    EntityMetadata metadata = new AttachedEntityMetadata(this, document);

    switch (eventKind) {
      case CREATE:
        return new EntityCreatedEvent(metadata);
      case UPDATE:
        return new EntityUpdatedEvent(metadata);
      case DELETE:
        return new EntityDeletedEvent(metadata);
      default:
        return null;
    }
  }

  /**
   * Override this method to declare a custom affinity for {@link EntityEvent}s.
   *
   * @since 3.11
   */
  public String eventAffinity(final ODocument document) {
    return document.getIdentity().toString();
  }

  //
  // Conflict resolution
  //

  /**
   * Override this method to enable conflict resolution for entities managed by this adapter.
   *
   * @since 3.14
   */
  public boolean resolveConflicts() {
    return false;
  }

  /**
   * Attempts to resolve conflicts between the stored and incoming records by deconfliction.
   *
   * @return {@link ConflictState#MERGE} if this results in further changes to {@code changeRecord}
   *
   * @since 3.14
   */
  public ConflictState resolve(final ODocument storedRecord, final ODocument changeRecord) {
    return deconflictSteps.stream()
        .reduce(IGNORE, (decision, step) -> decision.andThen(
            () -> step.deconflict(storedRecord, changeRecord)),
            ConflictState::max)
        .andThen(() -> compare(storedRecord, changeRecord));
  }

  /**
   * Does one last round of comparison between the (hopefully) deconflicted records.
   *
   * @since 3.14
   */
  protected ConflictState compare(final ODocument storedRecord, final ODocument changeRecord) {
    for (Entry<String, Object> property : storedRecord) {
      Object changeValue = changeRecord.rawField(property.getKey());
      if (!Objects.equals(property.getValue(), changeValue)) {
        log.trace("Conflict detected in {}: {} vs {}", property.getKey(), property.getValue(), changeValue);
        return DENY;
      }
    }
    return IGNORE; // identical content, update can be ignored or allowed
  }
}
