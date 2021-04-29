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
package org.sonatype.nexus.internal.security.realm;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.content.testsuite.groups.SQLTestGroup;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.testdb.DataSessionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsNot.not;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

@Category(SQLTestGroup.class)
public class RealmConfigurationDAOTest
    extends TestSupport
{
  @Rule
  public DataSessionRule sessionRule = new DataSessionRule().access(RealmConfigurationDAO.class);

  private DataSession session;

  private RealmConfigurationDAO dao;

  @Before
  public void setup() throws Exception {
    session = sessionRule.openSession(DEFAULT_DATASTORE_NAME);
    dao = (RealmConfigurationDAO) session.access(RealmConfigurationDAO.class);
  }

  @After
  public void cleaup() {
    session.close();
  }

  @Test
  public void testSetAndGet() {
    RealmConfigurationData config = new RealmConfigurationData();
    config.setRealmNames(asList("hello", "world"));
    dao.set(config);
    RealmConfigurationData readData = dao.get().orElse(null);

    assertThat(readData, not(nullValue()));
    assertThat(readData.getRealmNames(), containsInAnyOrder("hello", "world"));

    config.setRealmNames(asList("foo", "bar"));
    dao.set(config);
    readData = dao.get().orElse(null);
    assertThat(readData, not(nullValue()));
    assertThat(readData.getRealmNames(), containsInAnyOrder("foo", "bar"));
  }
}
