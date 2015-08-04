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
package org.sonatype.nexus.testsuite.repo.nexus2996;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.rest.model.RepositoryTargetResource;
import org.sonatype.nexus.test.utils.PrivilegesMessageUtil;
import org.sonatype.nexus.test.utils.TargetMessageUtil;
import org.sonatype.nexus.test.utils.XStreamFactory;

import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.restlet.data.MediaType;

import static org.hamcrest.Matchers.hasItem;

public class Nexus2996DeleteRepoTargetIT
    extends AbstractNexusIntegrationTest
{

  private PrivilegesMessageUtil privUtil =
      new PrivilegesMessageUtil(this, XStreamFactory.getXmlXStream(), MediaType.APPLICATION_XML);

  private static final String TARGET_ID = "1c1fd83a2fd9";

  private static final String READ_PRIV_ID = "1c26537599f6";

  private static final String CREATE_PRIV_ID = "1c2652734258";

  private static final String UPDATE_PRIV_ID = "1c2653b9a119";

  private static final String DELETE_PRIV_ID = "1c2653f5a3e2";

  @Test
  public void deleteRepoTarget()
      throws Exception
  {
    RepositoryTargetResource target = TargetMessageUtil.get(TARGET_ID);
    MatcherAssert.assertThat(target.getPatterns(), hasItem(".*"));

    privUtil.assertExists(READ_PRIV_ID, CREATE_PRIV_ID, UPDATE_PRIV_ID, DELETE_PRIV_ID);

    TargetMessageUtil.delete(TARGET_ID);

    privUtil.assertNotExists(READ_PRIV_ID, CREATE_PRIV_ID, UPDATE_PRIV_ID, DELETE_PRIV_ID);
  }
}
