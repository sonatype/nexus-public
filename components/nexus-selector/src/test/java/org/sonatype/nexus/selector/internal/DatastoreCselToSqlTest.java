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
package org.sonatype.nexus.selector.internal;

import java.util.HashMap;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.datastore.api.DataStore;
import org.sonatype.nexus.datastore.api.DataStoreManager;
import org.sonatype.nexus.selector.SelectorSqlBuilder;

import org.apache.commons.jexl3.parser.ASTJexlScript;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Optional.of;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DatastoreCselToSqlTest
    extends TestSupport
{
  @Mock
  private DataStoreManager dataStoreManager;

  @Mock
  private DataStore dataStore;

  @Mock
  private ASTJexlScript script;

  @Mock
  private SelectorSqlBuilder builder;

  @Mock
  private DatastoreSqlTransformer transformerA;

  @Mock
  private DatastoreSqlTransformer transformerB;

  private Map<String, DatastoreSqlTransformer> transformerMap;

  private DatastoreCselToSql datastoreCselToSql;

  @Before
  public void setup() {
    transformerMap = new HashMap<>();
    transformerMap.put("A", transformerA);
    transformerMap.put("B", transformerB);

    when(dataStoreManager.get(any())).thenReturn(of(dataStore));
  }

  @Test
  public void picksCorrectTransformer() throws Exception {
    when(dataStore.getDatabaseId()).thenReturn("A");
    datastoreCselToSql = new DatastoreCselToSql(dataStoreManager, transformerMap);

    datastoreCselToSql.doStart();

    datastoreCselToSql.transformCselToSql(script, builder);
    verify(script).childrenAccept(eq(transformerA), eq(builder));
  }

  @Test(expected = IllegalStateException.class)
  public void missingTransformerYieldsException() throws Exception {
    when(dataStore.getDatabaseId()).thenReturn("C");
    datastoreCselToSql = new DatastoreCselToSql(dataStoreManager, transformerMap);

    datastoreCselToSql.doStart();

    datastoreCselToSql.transformCselToSql(script, builder);
  }
}
