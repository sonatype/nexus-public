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
package org.sonatype.nexus.testsuite.security.nexus408;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.test.utils.ChangePasswordUtils;

import org.junit.Assert;
import org.junit.Test;
import org.restlet.data.Status;

/**
 * Test change password service.
 */
public class Nexus408ChangePasswordIT
    extends AbstractNexusIntegrationTest
{

  @Test
  public void changeUserPassword()
      throws Exception
  {
    Status status = ChangePasswordUtils.changePassword("test-user", "admin123", "123admin");
    Assert.assertEquals(Status.SUCCESS_ACCEPTED.getCode(), status.getCode());
  }

}
