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
package org.sonatype.nexus.upgrade.internal;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.entity.SingletonEntityAdapter;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.record.impl.ODocument;

/**
 * {@link ClusteredModelVersions} entity-adapter.
 * 
 * @since 3.1
 */
@Named
@Singleton
public class ClusteredModelVersionsEntityAdapter
    extends SingletonEntityAdapter<ClusteredModelVersions>
{
  private static final String DB_CLASS = new OClassNameBuilder().prefix("upgrade").type("model_versions").build();

  public ClusteredModelVersionsEntityAdapter() {
    super(DB_CLASS);
  }

  @Override
  protected void defineType(OClass type) {
    // no schema
  }

  @Override
  protected ClusteredModelVersions newEntity() {
    return new ClusteredModelVersions();
  }

  @Override
  protected void readFields(ODocument document, ClusteredModelVersions entity) throws Exception {
    document.forEach(entry -> entity.put(entry.getKey(), entry.getValue().toString()));
  }

  @Override
  protected void writeFields(ODocument document, ClusteredModelVersions entity) throws Exception {
    entity.forEach(entry -> document.field(entry.getKey(), entry.getValue()));
  }

  public ClusteredModelVersions get(ODatabaseDocumentTx db) {
    ClusteredModelVersions entity = singleton.get(db);
    if (entity == null) {
      entity = newEntity();
    }
    return entity;
  }

  public void set(ODatabaseDocumentTx db, ClusteredModelVersions entity) {
    singleton.set(db, entity);
  }
}
