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

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.sql.OCommandSQL;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Delete entity based on one or more properties.
 *
 * @since 3.0
 */
public class DeleteEntityByPropertyAction
    extends ComponentSupport
{
  private final String query;

  public DeleteEntityByPropertyAction(final EntityAdapter<?> adapter, final String... properties) {
    checkNotNull(adapter);
    this.query = String.format("DELETE FROM %s WHERE %s", adapter.getTypeName(), QueryUtils.buildPredicate(properties));
  }

  public boolean execute(final ODatabaseDocumentTx db, final Object... values) {
    checkNotNull(db);
    checkArgument(values.length > 0);

    int records = db.command(new OCommandSQL(query))
        .execute(values);

    return records == 1;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "query='" + query + '\'' +
        '}';
  }
}
