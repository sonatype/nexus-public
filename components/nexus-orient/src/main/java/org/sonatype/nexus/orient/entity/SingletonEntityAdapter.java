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
import org.sonatype.nexus.orient.entity.action.SingletonActions;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;

/**
 * Singleton record entity-adapter.
 *
 * @since 3.1
 */
public abstract class SingletonEntityAdapter<T extends Entity>
    extends EntityAdapter<T>
{
  protected final SingletonActions<T> singleton = new SingletonActions<>(this);

  public SingletonEntityAdapter(final String typeName) {
    super(typeName);
  }

  @Override
  public boolean sendEvents() {
    return true; // enable replication workaround for all singleton entities
  }

  //
  // Actions
  //

  /**
   * Get singleton entity or {@code null} if entity was not found.
   */
  @Nullable
  public T get(final ODatabaseDocumentTx db) {
    return singleton.get(db);
  }

  /**
   * Set singleton entity. Returns {@code true} if entity was replaced.
   */
  public boolean set(final ODatabaseDocumentTx db, final T entity) {
    return singleton.set(db, entity);
  }

  /**
   * Delete singleton entity. Returns {@code true} if entity was deleted.
   */
  public boolean delete(final ODatabaseDocumentTx db) {
    return singleton.delete(db);
  }

  /**
   * TEMP: workaround OrientDB 2.1 issue where in-TX dictionary updates are not replicated.
   */
  public void replicate(final ODocument document, final EventKind eventKind) {
    singleton.replicate(document, eventKind);
  }
}
