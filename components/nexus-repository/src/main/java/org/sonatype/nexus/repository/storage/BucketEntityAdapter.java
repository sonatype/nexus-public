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

import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.OIndexNameBuilder;
import org.sonatype.nexus.orient.entity.IterableEntityAdapter;
import org.sonatype.nexus.orient.entity.action.ReadEntityByPropertyAction;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;

import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_ATTRIBUTES;

/**
 * {@link Bucket} entity-adapter.
 *
 * @since 3.0
 */
@Named
@Singleton
public class BucketEntityAdapter
    extends IterableEntityAdapter<Bucket>
{
  private static final String DB_CLASS = new OClassNameBuilder()
      .type("bucket")
      .build();

  /**
   * Key of {@link Bucket} repository name attribute.
   */
  public static final String P_REPOSITORY_NAME = "repository_name";

  private static final String I_REPOSITORY_NAME = new OIndexNameBuilder()
      .type(DB_CLASS)
      .property(P_REPOSITORY_NAME)
      .build();

  private final ReadEntityByPropertyAction<Bucket> read = new ReadEntityByPropertyAction<>(this, P_REPOSITORY_NAME);

  @Inject
  public BucketEntityAdapter() {
    super(DB_CLASS);
  }

  @Override
  protected void defineType(final OClass type) {
    type.createProperty(P_REPOSITORY_NAME, OType.STRING)
        .setMandatory(true)
        .setNotNull(true);
    type.createProperty(P_ATTRIBUTES, OType.EMBEDDEDMAP)
        .setNotNull(true);

    type.createIndex(I_REPOSITORY_NAME, INDEX_TYPE.UNIQUE, P_REPOSITORY_NAME);
  }

  @Override
  protected Bucket newEntity() {
    return new Bucket();
  }

  @Override
  protected void readFields(final ODocument document, final Bucket entity) {
    String repositoryName = document.field(P_REPOSITORY_NAME, OType.STRING);
    Map<String, Object> attributes = document.field(P_ATTRIBUTES, OType.EMBEDDEDMAP);

    entity.setRepositoryName(repositoryName);
    entity.attributes(new NestedAttributesMap(P_ATTRIBUTES, attributes));
  }

  @Override
  protected void writeFields(final ODocument document, final Bucket entity) {
    document.field(P_REPOSITORY_NAME, entity.getRepositoryName());
    document.field(P_ATTRIBUTES, entity.attributes().backing());
  }

  //
  // Actions
  //

  /**
   * @since 3.1
   */
  @Nullable
  public Bucket read(final ODatabaseDocumentTx db, final String name) {
    return read.execute(db, name);
  }
}
