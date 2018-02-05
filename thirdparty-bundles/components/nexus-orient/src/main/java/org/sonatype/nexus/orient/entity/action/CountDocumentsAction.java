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

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.orient.entity.EntityAdapter;

import com.google.common.primitives.Ints;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Count all documents.
 *
 * @since 3.0
 */
public class CountDocumentsAction
    extends ComponentSupport
{
  private final String type;

  public CountDocumentsAction(final EntityAdapter<?> adapter) {
    checkNotNull(adapter);
    this.type = adapter.getTypeName();
  }

  public long execute(final ODatabaseDocumentTx db) {
    checkNotNull(db);
    return db.countClass(type);
  }

  /**
   * Same as {@link #execute(ODatabaseDocumentTx)} but safely casts result to integer.
   */
  public int executeI(final ODatabaseDocumentTx db) {
    long result = execute(db);
    return Ints.checkedCast(result);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "type='" + type + '\'' +
        '}';
  }
}
