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
package org.sonatype.nexus.quartz.internal.orient;

import java.util.List;

import javax.annotation.Nullable;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.entity.Entity;
import org.sonatype.nexus.orient.entity.EntityAdapter;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import org.quartz.utils.Key;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Read entity by {@link Key}.
 *
 * @since 3.0
 */
public class ReadEntityByKeyAction<EntityT extends Entity>
  extends ComponentSupport
{
  private final EntityAdapter<EntityT> adapter;

  private final String query;

  public ReadEntityByKeyAction(final EntityAdapter<EntityT> adapter,
                               final String namePropery,
                               final String groupProperty)
  {
    this.adapter = checkNotNull(adapter);
    checkNotNull(namePropery);
    checkNotNull(groupProperty);

    this.query = String.format("SELECT FROM %s WHERE %s = ? AND %s = ?",
        adapter.getTypeName(), namePropery, groupProperty);
  }

  @Nullable
  public EntityT execute(final ODatabaseDocumentTx db, final Key key) {
    checkNotNull(db);
    checkNotNull(key);

    List<ODocument> results = db.command(new OSQLSynchQuery<>(query))
        .execute(key.getName(), key.getGroup());

    if (results.isEmpty()) {
      return null;
    }
    return adapter.readEntity(results.get(0));
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "query='" + query + '\'' +
        '}';
  }
}
