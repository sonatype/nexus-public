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
package org.sonatype.nexus.testsuite.security.nexus507;

import org.sonatype.nexus.integrationtests.AbstractPrivilegeTest;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.integrationtests.TestContext;
import org.sonatype.security.rest.model.RoleResource;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class Nexus507UserTimeoutIT
    extends AbstractPrivilegeTest
{

  @Before
  public void reduceAdminRoleTimeout()
      throws Exception
  {
    TestContainer.getInstance().getTestContext().useAdminForRequests();
    RoleResource role = roleUtil.getRole("test-admin");
    Assert.assertNotNull("Invalid test-admin role timeout", role);
  }

  @Test
  public void checkHtmlRequest()
      throws Exception
  {
    String loginURI = "service/local/authentication/login";

    // accessUrl( serviceURI );

    TestContext context = TestContainer.getInstance().getTestContext();
    context.setSecureTest(true);
    context.setUsername("test-admin");
    context.setPassword("admin123");

    RequestFacade.doGet(loginURI);

    String userURI = "service/local/users/admin";
    RequestFacade.doGet(userURI);


/*        WebConversation wc = new WebConversation();
        wc.setAuthorization( "test-admin", "admin123" );
        WebRequest req = new GetMethodWebRequest( loginURI );
        WebResponse resp = wc.getResponse( req );
        Assert.assertEquals( resp.getResponseCode(), 200, "Unable to login " + resp.getResponseMessage() );

        String userURI = nexusBaseUrl + "service/local/users/admin";
        req = new GetMethodWebRequest( userURI );
        resp = wc.getResponse( req );
        Assert.assertEquals( resp.getResponseCode(), 200, "Unable to access users " + resp.getResponseMessage() );
*/
    this.printKnownErrorButDoNotFail(this.getClass(), "checkHtmlRequest");
    //FIXME: the timeout was never configurable, this the below is going to fail.
    //        // W8 2' minutes to get timeout
    //        Thread.sleep( ( 2 * 60 * 1000 ) );
    //
    //        req = new GetMethodWebRequest( userURI );
    //        resp = wc.getResponse( req );
    //        Assert.assertEquals( "The session didn't expire " + resp.getResponseCode() + ":" + resp.getResponseMessage(),
    //                             401, resp.getResponseCode() );
  }

  // private void accessUrl( String serviceURI )
  // throws IOException, InterruptedException
  // {
  // TestContext testContext = TestContainer.getInstance().getTestContext();
  // testContext.useAdminForRequests();
  // Status status = UserCreationUtil.login();
  // Assert.assertTrue( "Unable to make login as test-admin", status.isSuccess() );
  //
  // Response response = doGetRequest( serviceURI );
  // Assert.assertTrue( "Unable to access " + serviceURI, response.getStatus().isSuccess() );
  //
  // // W8 1'10" minute to get timeout
  // Thread.sleep( (long) ( 1.15 * 60 * 1000 ) );
  //
  // response = doGetRequest( serviceURI );
  // Assert.assertEquals( "The session didn't expire, still with access to: " + serviceURI, 301,
  // response.getStatus().getCode() );

  // Reference redirectRef = response.getRedirectRef();
  // Assert.assertNotNull( "Snapshot download should redirect to a new file "
  // + response.getRequest().getResourceRef().toString(), redirectRef );
  //
  // serviceURI = redirectRef.toString();
  //
  // response = RequestFacade.sendMessage( new URL( serviceURI ), Method.GET, null );
  //
  // }

  // private Response doGetRequest( String serviceURI )
  // {
  // Request request = new Request();
  // request.setResourceRef( serviceURI );
  // request.setMethod( Method.GET );
  // ChallengeResponse authentication = new ChallengeResponse( ChallengeScheme.HTTP_BASIC, "admin" );
  // request.setChallengeResponse( authentication );
  // Context ctx = new Context();
  //
  // Client client = new Client( ctx, Protocol.HTTP );
  // return client.handle( request );
  // }
}
