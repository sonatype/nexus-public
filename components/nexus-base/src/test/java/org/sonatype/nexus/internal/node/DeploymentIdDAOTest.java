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
package org.sonatype.nexus.internal.node;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.content.testsuite.groups.SQLTestGroup;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.testdb.DataSessionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

@Category(SQLTestGroup.class)
public class DeploymentIdDAOTest
    extends TestSupport
{
  private static final String DEPLOYMENT_ID = "aslfsd";

  @Rule
  public DataSessionRule sessionRule = new DataSessionRule().access(DeploymentIdDAO.class);

  private DataSession session;

  private DeploymentIdDAO dao;

  @Before
  public void setup() {
    session = sessionRule.openSession(DEFAULT_DATASTORE_NAME);
    dao = (DeploymentIdDAO) session.access(DeploymentIdDAO.class);
    dao.set(DEPLOYMENT_ID);
  }

  @After
  public void cleanup() {
    session.close();
  }

  @Test
  public void testGet() {
    String deploymentId = dao.get().orElse(null);

    assertThat(deploymentId, is(DEPLOYMENT_ID));
  }

  @Test
  public void testSet() {
    dao.set("DEPLOYMENT_ID");
    String deploymentId = dao.get().orElse(null);

    assertThat(deploymentId, is("DEPLOYMENT_ID"));
  }
}
