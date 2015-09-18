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

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.dictionary.ODictionary;
import com.orientechnologies.orient.core.record.ORecord;
import com.orientechnologies.orient.core.record.impl.ODocument;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Single record entity-adapter.
 *
 * @since 3.0
 */
public abstract class SingletonEntityAdapter<T extends Entity>
  extends EntityAdapter<T>
{
  /**
   * Singleton record dictionary-index key.
   */
  private final String key;

  public SingletonEntityAdapter(final String typeName) {
    super(typeName);
    this.key = String.format("%s.SINGLETON", typeName);
    log.debug("Singleton key: {}", key);
  }

  // TODO: Add support to verify there is only 1 entity (or none) when registering type
  // TODO: Do we need to use some form of database-lock to prevent race on instance creation?

  /**
   * Get singleton entity or null if not set.
   */
  @Nullable
  public T get(final ODatabaseDocumentTx db) {
    checkNotNull(db);

    ODocument document = db.getDictionary().get(key);
    if (document != null) {
      return readEntity(document);
    }
    return null;
  }

  /**
   * Set singleton entity.
   */
  public void set(final ODatabaseDocumentTx db, final T entity) {
    checkNotNull(db);
    checkNotNull(entity);

    ODictionary<ORecord> dictionary = db.getDictionary();
    ODocument document = dictionary.get(key);
    if (document == null) {
      document = addEntity(db, entity);
      dictionary.put(key, document);
    }
    else {
      writeEntity(document, entity);
    }
  }

  /**
   * Remove singleton entity.
   */
  public void remove(final ODatabaseDocumentTx db) {
    checkNotNull(db);

    ODictionary<ORecord> dictionary = db.getDictionary();
    ODocument document = dictionary.get(key);
    if (document != null) {
      db.delete(document);
      dictionary.remove(key);
    }
  }
}
