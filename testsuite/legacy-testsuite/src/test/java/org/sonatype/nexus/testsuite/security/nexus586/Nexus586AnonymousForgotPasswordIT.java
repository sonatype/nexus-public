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
package org.sonatype.nexus.testsuite.security.nexus586;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.test.utils.ForgotPasswordUtils;

import org.junit.Assert;
import org.junit.Test;
import org.restlet.data.Response;

/**
 * Saving the Nexus config needs to validate the anonymous user information
 */
public class Nexus586AnonymousForgotPasswordIT
    extends AbstractNexusIntegrationTest
{

  @Test
  public void forgotPassword()
      throws Exception
  {
    String username = "anonymous";
    Response response = ForgotPasswordUtils.get(this).recoverUserPassword(username, "changeme2@yourcompany.com");
    Assert.assertEquals(400, response.getStatus().getCode());
  }
}
