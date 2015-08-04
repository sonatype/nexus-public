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
package org.sonatype.nexus.testsuite.ldap.nxcm58;

import java.io.IOException;

import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.testsuite.ldap.AbstractLdapIntegrationIT;

import org.junit.Assert;
import org.junit.Test;
import org.restlet.data.Response;
import org.restlet.data.Status;


public class Nxcm58NexusStatusIT
    extends AbstractLdapIntegrationIT
{

  @Test
  public void getStatus()
      throws IOException
  {
    Response response = RequestFacade.doGetRequest("service/local/status");
    Status status = response.getStatus();
    Assert.assertTrue("Unable to get nexus status" + status, status.isSuccess());
  }

  @Test
  public void getLdapInfo()
      throws IOException
  {
    Response response = RequestFacade.doGetRequest("service/local/ldap/conn_info");
    Status status = response.getStatus();
    Assert.assertTrue("Unable to reach ldap services\n" + status + "\n"
        + response.getEntity().getText(), status.isSuccess());
  }

}
