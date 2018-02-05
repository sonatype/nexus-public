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
package org.sonatype.nexus.orient.entity.action;

import javax.annotation.Nullable;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.entity.Entity;
import org.sonatype.nexus.orient.entity.EntityAdapter;
import org.sonatype.nexus.orient.entity.EntityAdapter.EventKind;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.dictionary.ODictionary;
import com.orientechnologies.orient.core.record.ORecord;
import com.orientechnologies.orient.core.record.impl.ODocument;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Singleton actions.
 *
 * @since 3.0
 */
public class SingletonActions<T extends Entity>
    extends ComponentSupport
{
  private final EntityAdapter<T> adapter;

  private final String key;

  public SingletonActions(final EntityAdapter<T> adapter) {
    this.adapter = checkNotNull(adapter);
    this.key = String.format("%s.SINGLETON", adapter.getTypeName());
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "key='" + key + '\'' +
        '}';
  }

  // TODO: Add support to verify there is only 1 entity (or none) when registering type
  // TODO: Do we need to use some form of database-lock to prevent race on instance creation?

  /**
   * Get singleton entity or {@code null} if entity was not found.
   */
  @Nullable
  public T get(final ODatabaseDocumentTx db) {
    checkNotNull(db);

    ODictionary<ORecord> dictionary = db.getDictionary();
    ODocument document = dictionary.get(key);
    if (document != null) {
      return adapter.readEntity(document);
    }
    return null;
  }

  /**
   * Set singleton entity.  Returns {@code true} if entity was replaced.
   */
  public boolean set(final ODatabaseDocumentTx db, final T entity) {
    checkNotNull(db);
    checkNotNull(entity);

    ODictionary<ORecord> dictionary = db.getDictionary();
    ODocument document = dictionary.get(key);
    if (document == null) {
      document = adapter.addEntity(db, entity);
      dictionary.put(key, document);
      return false;
    }
    else {
      adapter.writeEntity(document, entity);
      return true;
    }
  }

  /**
   * Delete singleton entity.  Returns {@code true} if entity was deleted.
   */
  public boolean delete(final ODatabaseDocumentTx db) {
    checkNotNull(db);

    ODictionary<ORecord> dictionary = db.getDictionary();
    ODocument document = dictionary.get(key);
    if (document != null) {
      db.delete(document);
      dictionary.remove(key);
      return true;
    }
    return false;
  }

  /**
   * TEMP: workaround OrientDB 2.1 issue where in-TX dictionary updates are not replicated.
   *
   * @since 3.1
   */
  public void replicate(final ODocument document, final EventKind eventKind) {
    ODictionary<ORecord> dictionary = document.getDatabase().getDictionary();
    switch (eventKind) {
      case CREATE:
      case UPDATE:
        dictionary.put(key, document);
        break;
      case DELETE:
        dictionary.remove(key);
        break;
      default:
        break;
    }
  }
}
