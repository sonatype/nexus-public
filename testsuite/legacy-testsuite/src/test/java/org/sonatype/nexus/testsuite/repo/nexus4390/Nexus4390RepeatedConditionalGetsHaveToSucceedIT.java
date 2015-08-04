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
package org.sonatype.nexus.testsuite.repo.nexus4390;

import java.io.IOException;
import java.util.Date;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.RequestFacade;

import org.junit.Assert;
import org.junit.Test;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.data.Response;

/**
 * This test tests NEXUS-4390 and Nexus' capability to properly respond to repeated conditionalGET requests. We simply
 * request for a resource, "remember" it's timestamp, and repeatedly ask newer version for the same resource using it's
 * timestamp. In related issue, it is seen that in this case, Nexus eventually responds with 404 because of bug
 * handling
 * the conditional GET.
 *
 * @author cstamas
 */
public class Nexus4390RepeatedConditionalGetsHaveToSucceedIT
    extends AbstractNexusIntegrationTest
{
  @Test
  public void testRepeatedConditionalGets()
      throws IOException
  {
    // we can use any resource, since bug stands for all content served by Nexus
    // here, for simplicity, we will use the archetype-catalog.xml which is empty, but that does not
    // matter, since even the empty catalog will respond with 404 when asked for it with NEXUS-4390 being true.
    final String servicePath = "content/repositories/fake-central/archetype-catalog.xml";

    // 1st, we do an unconditional GET to get it's timestamp. Unconditional GET is not affected by
    // this bug, it will succeed.
    Response response = null;
    Date lastModified;
    try {
      response = RequestFacade.sendMessage(servicePath, Method.GET);
      Assert.assertTrue(response.getStatus().isSuccess());
      lastModified = response.getEntity().getModificationDate();
    }
    finally {
      RequestFacade.releaseResponse(response);
    }

    // now, we construct and repeat conditional gets
    final String fullUrl = RequestFacade.toNexusURL(servicePath).toString();
    Request req = null;
    Response res = null;
    for (int i = 0; i < 10; i++) {
      try {
        req = new Request(Method.GET, fullUrl);
        req.getConditions().setModifiedSince(lastModified);
        res = RequestFacade.sendMessage(req, null);
        // we are fine with 200 OK, 303 Not Modified or whatever, but not with any server side error or 404
        Assert.assertTrue(res.getStatus().getCode() != 404 && !res.getStatus().isError());
      }
      finally {
        RequestFacade.releaseResponse(res);
      }
    }

    // good, we are here, we are fine
  }
}
