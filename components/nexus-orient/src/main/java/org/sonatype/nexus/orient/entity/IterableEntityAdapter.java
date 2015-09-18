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

import javax.annotation.Nullable;

import org.sonatype.nexus.common.entity.Entity;
import org.sonatype.nexus.common.entity.EntityId;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Iterable records entity-adapter.
 *
 * @since 3.0
 */
public abstract class IterableEntityAdapter<T extends Entity>
    extends EntityAdapter<T>
{
  public IterableEntityAdapter(final String typeName) {
    super(typeName);
  }

  /**
   * Return number of entities.
   */
  public long count(final ODatabaseDocumentTx db) {
    checkNotNull(db);
    return db.countClass(getTypeName());
  }

  /**
   * Helper to iterate over set of documents and transform into entities.
   */
  protected Iterable<T> transform(final Iterable<ODocument> documents) {
    return Iterables.transform(documents, new Function<ODocument, T>()
    {
      @Nullable
      @Override
      public T apply(@Nullable final ODocument input) {
        return input != null ? readEntity(input) : null;
      }
    });
  }

  /**
   * Browse all entities.
   */
  public Iterable<T> browse(final ODatabaseDocumentTx db) {
    return transform(browseDocuments(db));
  }

  /**
   * Read entity.
   */
  @Nullable
  public T read(final ODatabaseDocumentTx db, final EntityId id) {
    checkNotNull(db);
    checkNotNull(id);
    ODocument doc = document(db, id);
    return doc != null ? readEntity(doc) : null;
  }

  /**
   * Edit entity.
   */
  public void edit(final ODatabaseDocumentTx db, final T entity) {
    super.editEntity(db, entity);
  }

  /**
   * Add entity.
   */
  public void add(final ODatabaseDocumentTx db, final T entity) {
    super.addEntity(db, entity);
  }

  /**
   * Delete entity.
   */
  public void delete(final ODatabaseDocumentTx db, final T entity) {
    super.deleteEntity(db, entity);
  }
}
