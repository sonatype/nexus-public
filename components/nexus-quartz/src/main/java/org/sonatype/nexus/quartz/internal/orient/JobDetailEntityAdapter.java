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

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.OIndexNameBuilder;
import org.sonatype.nexus.orient.entity.action.BrowseEntitiesWithPredicateAction;
import org.sonatype.nexus.orient.entity.action.DeleteEntitiesAction;
import org.sonatype.nexus.orient.entity.marshal.FieldObjectMapper;
import org.sonatype.nexus.orient.entity.marshal.JacksonMarshaller;
import org.sonatype.nexus.orient.entity.marshal.MarshalledEntityAdapter;
import org.sonatype.nexus.orient.entity.marshal.Marshaller;

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
   */
  public final ReadEntityByKeyAction<JobDetailEntity> readyByKey =
      new ReadEntityByKeyAction<>(this, P_NAME, P_GROUP);

  /**
   * Check if an entity exists for a {@link JobKey}.
   */
  public final ExistsByKeyAction existsByKey = new ExistsByKeyAction(this, P_NAME, P_GROUP);

  /**
   * Browse all entities matching given {@link Predicate}.
   */
  public final BrowseEntitiesWithPredicateAction<JobDetailEntity> browseWithPredicate =
      new BrowseEntitiesWithPredicateAction<>(this);

  /**
   * Delete a single entity matching {@link JobKey}.
   */
  public final DeleteEntityByKeyAction deleteByKey = new DeleteEntityByKeyAction(this, P_NAME, P_GROUP);

  /**
   * Delete all entities.
   */
  public final DeleteEntitiesAction deleteAll = new DeleteEntitiesAction(this);
}
