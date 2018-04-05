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

import org.sonatype.nexus.orient.entity.EntityAdapter.EventKind;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.record.impl.ODocument;

/**
 * Extension point for the {@link ComponentEntityAdapter} to define/read/write additional fields to Orient on a {@link
 * Component}  instance
 *
 * @since 3.8
 */
public interface ComponentEntityAdapterExtension
{
  /**
   * @see ComponentEntityAdapter#defineType(ODatabaseDocumentTx, OClass)
   */
  void defineType(ODatabaseDocumentTx db, OClass type);

  /**
   * @see ComponentEntityAdapter#readFields(ODocument, Component)
   */
  void readFields(ODocument document, Component component);

  /**
   * @see ComponentEntityAdapter#writeFields(ODocument, Component)
   */
  void writeFields(ODocument document, Component component);

  /**
   * Override this to capture lazy/linked content for component events.
   *
   * @see ComponentEntityAdapter#newEvent(ODocument, EventKind)
   */
  default void prefetchFields(ODocument document) {/* no-op */}
}
