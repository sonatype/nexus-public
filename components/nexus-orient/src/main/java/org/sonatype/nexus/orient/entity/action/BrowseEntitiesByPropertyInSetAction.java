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

import java.util.Collection;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.entity.Entity;
import org.sonatype.nexus.orient.entity.IterableEntityAdapter;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.joining;

/**
 * Browse entities based on one or more properties.
 *
 * @since 3.8
 */
public class BrowseEntitiesByPropertyInSetAction<T extends Entity>
    extends ComponentSupport
{
  private final IterableEntityAdapter<T> adapter;

  private final String query;

  public BrowseEntitiesByPropertyInSetAction(final IterableEntityAdapter<T> adapter, final String property) {
    this.adapter = checkNotNull(adapter);
    this.query = String.format("SELECT * FROM %s WHERE %s IN [ %%s ]", adapter.getTypeName(), checkNotNull(property));
  }

  public Iterable<T> execute(final ODatabaseDocumentTx db, final Collection<? extends Object> values) {
    checkNotNull(db);
    checkArgument(values != null && values.size() > 0);

    String valueFields = values.stream().map(value -> "?").collect(joining(","));
    String adjustedQuery = String.format(query, valueFields);

    Iterable<ODocument> results = db.command(new OSQLSynchQuery<>(adjustedQuery)).execute(values.toArray());

    return adapter.transform(results);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "query='" + query + '\'' +
        '}';
  }
}
