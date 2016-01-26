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

import java.util.ArrayList;
import java.util.List;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.orient.entity.EntityAdapter;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Action which returns specific property value for all entities.
 *
 * @since 3.0
 */
public class BrowsePropertyAction<T>
    extends ComponentSupport
{
  private final EntityAdapter<?> adapter;

  private final String property;

  public BrowsePropertyAction(final EntityAdapter<?> adapter, final String property) {
    this.adapter = checkNotNull(adapter);
    this.property = checkNotNull(property);
  }

  public List<T> execute(final ODatabaseDocumentTx db) {
    checkNotNull(db);
    List<T> result = new ArrayList<>();
    for (ODocument doc : adapter.browseDocuments(db)) {
      result.add(doc.field(property));
    }
    return result;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "property='" + property + '\'' +
        '}';
  }
}
