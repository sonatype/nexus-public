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
package org.sonatype.nexus.quartz.internal.orient;

import java.util.function.Predicate;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.entity.EntityEvent;
import org.sonatype.nexus.common.entity.EntityMetadata;
import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.OIndexNameBuilder;
import org.sonatype.nexus.orient.entity.AttachedEntityMetadata;
import org.sonatype.nexus.orient.entity.action.BrowseEntitiesWithPredicateAction;
import org.sonatype.nexus.orient.entity.action.DeleteEntitiesAction;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.quartz.JobKey;

/**
 * {@link JobDetailEntity} adapter.
 *
 * @since 3.0
 */
@Named
@Singleton
public class JobDetailEntityAdapter
    extends MarshalledEntityAdapter<JobDetailEntity>
{
  private static final String DB_CLASS = new OClassNameBuilder()
      .prefix("quartz")
      .type("job_detail")
      .build();

  private static final String P_NAME = "name";

  private static final String P_GROUP = "group";

  private static final String P_JOB_TYPE = "job_type";

  private static final String I_NAME_GROUP = new OIndexNameBuilder()
      .type(DB_CLASS)
      .property(P_NAME)
      .property(P_GROUP)
      .build();

  private final ReadEntityByKeyAction<JobDetailEntity> readByKey =
      new ReadEntityByKeyAction<>(this, P_NAME, P_GROUP);

  private final ExistsByKeyAction existsByKey = new ExistsByKeyAction(this, P_NAME, P_GROUP);

  private final BrowseEntitiesWithPredicateAction<JobDetailEntity> browseWithPredicate =
      new BrowseEntitiesWithPredicateAction<>(this);

  private final DeleteEntityByKeyAction deleteByKey = new DeleteEntityByKeyAction(this, P_NAME, P_GROUP);

  private final DeleteEntitiesAction deleteAll = new DeleteEntitiesAction(this);

  public JobDetailEntityAdapter() {
    super(DB_CLASS, createMarshaller(), JobDetailEntity.class.getClassLoader());
  }

  private static Marshaller createMarshaller() {
    return new JacksonMarshaller(new FieldObjectMapper());
  }

  @Override
  protected void defineType(final OClass type) {
    super.defineType(type);

    type.createProperty(P_NAME, OType.STRING)
        .setMandatory(true)
        .setNotNull(true);
    type.createProperty(P_GROUP, OType.STRING)
        .setMandatory(true)
        .setNotNull(false); // nullable
    type.createProperty(P_JOB_TYPE, OType.STRING)
        .setMandatory(true)
        .setNotNull(true);

    type.createIndex(I_NAME_GROUP, INDEX_TYPE.UNIQUE, P_NAME, P_GROUP);
  }

  @Override
  protected JobDetailEntity newEntity() {
    return new JobDetailEntity();
  }

  @Override
  protected void readFields(final ODocument document, final JobDetailEntity entity) throws Exception {
    super.readFields(document, entity);

    entity.setName(document.field(P_NAME));
    entity.setGroup(document.field(P_GROUP));
    entity.setJobType(document.field(P_JOB_TYPE));
  }

  @Override
  protected void writeFields(final ODocument document, final JobDetailEntity entity) throws Exception {
    super.writeFields(document, entity);

    document.field(P_NAME, entity.getName());
    document.field(P_GROUP, entity.getGroup());
    document.field(P_JOB_TYPE, entity.getJobType());
  }

  //
  // Actions
  //

  /**
   * Read a single entity by a {@link JobKey}.
   * 
   * @since 3.1
   */
  @Nullable
  public JobDetailEntity readByKey(final ODatabaseDocumentTx db, final JobKey key) {
    return readByKey.execute(db, key);
  }

  /**
   * Check if an entity exists for a {@link JobKey}.
   * 
   * @since 3.1
   */
  public boolean existsByKey(final ODatabaseDocumentTx db, final JobKey key) {
    return existsByKey.execute(db, key);
  }

  /**
   * Browse all entities matching given {@link Predicate}.
   * 
   * @since 3.1
   */
  public Iterable<JobDetailEntity> browseWithPredicate(final ODatabaseDocumentTx db,
                                                       final Predicate<JobDetailEntity> predicate)
  {
    return browseWithPredicate.execute(db, predicate);
  }

  /**
   * Delete a single entity matching {@link JobKey}.
   * 
   * @since 3.1
   */
  public boolean deleteByKey(final ODatabaseDocumentTx db, final JobKey key) {
    return deleteByKey.execute(db, key);
  }

  /**
   * Delete all entities.
   * 
   * @since 3.1
   */
  public void deleteAll(final ODatabaseDocumentTx db) {
    deleteAll.execute(db);
  }

  @Override
  public boolean sendEvents() {
    return true;
  }

  @Override
  public EntityEvent newEvent(final ODocument document, final EventKind eventKind) {
    EntityMetadata metadata = new AttachedEntityMetadata(this, document);

    switch (eventKind) {
      case CREATE:
        return new JobCreatedEvent(metadata);
      case UPDATE:
        return new JobUpdatedEvent(metadata);
      case DELETE:
        return new JobDeletedEvent(metadata);
      default:
        return null;
    }
  }
}
