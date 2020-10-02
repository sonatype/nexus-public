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
package org.sonatype.nexus.testsuite.ldap;

import java.io.IOException;

import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.security.ldap.realms.api.dto.LdapConnectionInfoDTO;
import org.sonatype.nexus.security.ldap.realms.api.dto.LdapConnectionInfoResponse;
import org.sonatype.nexus.test.utils.GroupMessageUtil;
import org.sonatype.plexus.rest.representation.XStreamRepresentation;
import org.sonatype.security.ldap.realms.persist.model.CConnectionInfo;

import com.thoughtworks.xstream.XStream;
import org.hamcrest.Matcher;
import org.junit.Assert;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LdapConnMessageUtil
{

  private static final String SERVICE_PART = RequestFacade.SERVICE_LOCAL + "ldap/conn_info";

  private XStream xstream;

  private MediaType mediaType;

  private static final Logger LOG = LoggerFactory.getLogger(GroupMessageUtil.class);

  public LdapConnMessageUtil(XStream xstream, MediaType mediaType) {
    super();
    this.xstream = xstream;
    this.mediaType = mediaType;
  }

  public LdapConnectionInfoDTO getConnectionInfo()
      throws IOException
  {
    Response response = this.sendMessage(Method.GET, null);
    return this.getResourceFromResponse(response);
  }

  public LdapConnectionInfoDTO updateConnectionInfo(LdapConnectionInfoDTO connInfo)
      throws Exception
  {
    Response response = null;
    try {
      response = this.sendMessage(Method.PUT, connInfo);

      if (!response.getStatus().isSuccess()) {
        String responseText = response.getEntity().getText();
        Assert.fail("Could not update LDAP connection info: " + response.getStatus() + ":\n" + responseText);
      }
      LdapConnectionInfoDTO responseResource = this.getResourceFromResponse(response);
      this.validateResourceResponse(connInfo, responseResource);

      return responseResource;
    }
    finally {
      RequestFacade.releaseResponse(response);
    }

  }

  public Response sendMessage(Method method, LdapConnectionInfoDTO resource)
      throws IOException
  {

    XStreamRepresentation representation = new XStreamRepresentation(xstream, "", mediaType);

    String serviceURI = SERVICE_PART;

    LdapConnectionInfoResponse repoResponseRequest = new LdapConnectionInfoResponse();
    repoResponseRequest.setData(resource);

    // now set the payload
    representation.setPayload(repoResponseRequest);

    LOG.debug("sendMessage: " + representation.getText());

    return RequestFacade.sendMessage(serviceURI, method, representation);
  }

  public Response sendTestMessage(LdapConnectionInfoDTO resource)
      throws IOException
  {

    XStreamRepresentation representation = new XStreamRepresentation(xstream, "", mediaType);

    String serviceURI = RequestFacade.SERVICE_LOCAL + "ldap/test_auth";

    LdapConnectionInfoResponse repoResponseRequest = new LdapConnectionInfoResponse();
    repoResponseRequest.setData(resource);

    // now set the payload
    representation.setPayload(repoResponseRequest);

    LOG.debug("sendMessage: " + representation.getText());

    return RequestFacade.sendMessage(serviceURI, Method.PUT, representation);
  }

  public String testAuth(LdapConnectionInfoDTO resource, final Matcher<Response> matcher)
      throws IOException
  {

    XStreamRepresentation representation = new XStreamRepresentation(xstream, "", mediaType);

    String serviceURI = RequestFacade.SERVICE_LOCAL + "ldap/test_auth";

    LdapConnectionInfoResponse repoResponseRequest = new LdapConnectionInfoResponse();
    repoResponseRequest.setData(resource);

    // now set the payload
    representation.setPayload(repoResponseRequest);

    LOG.debug("sendMessage: " + representation.getText());

    return RequestFacade.doPutForText(serviceURI, representation, matcher);
  }

  public LdapConnectionInfoDTO getResourceFromResponse(Response response)
      throws IOException
  {
    String responseString = response.getEntity().getText();
    LOG.debug(" getResourceFromResponse: " + responseString);

    XStreamRepresentation representation = new XStreamRepresentation(xstream, responseString, mediaType);
    LdapConnectionInfoResponse resourceResponse =
        (LdapConnectionInfoResponse) representation.getPayload(new LdapConnectionInfoResponse());

    return resourceResponse.getData();
  }

  @SuppressWarnings("unchecked")
  public void validateLdapConfig(LdapConnectionInfoDTO connInfo)
      throws Exception
  {
    CConnectionInfo fileConfig = LdapConfigurationUtil.getConfiguration().getConnectionInfo();
    Assert.assertEquals(fileConfig.getAuthScheme(), connInfo.getAuthScheme());
    Assert.assertEquals(fileConfig.getHost(), connInfo.getHost());
    Assert.assertEquals(connInfo.getPort(), fileConfig.getPort());
    Assert.assertEquals(fileConfig.getProtocol(), connInfo.getProtocol());
    Assert.assertEquals(fileConfig.getRealm(), connInfo.getRealm());
    Assert.assertEquals(fileConfig.getSearchBase(), connInfo.getSearchBase());
    Assert.assertEquals(fileConfig.getSystemUsername(), connInfo.getSystemUsername());

    // if the expectedPassword == null then the actual should be null
    if (connInfo.getSystemPassword() == null) {
      Assert.assertNull(fileConfig.getSystemPassword());
    }
    else {
      // make sure its not clear text
      Assert.assertNotSame(connInfo.getSystemPassword(), fileConfig.getSystemPassword());
      Assert.assertTrue(fileConfig.getSystemPassword().length() > 0);
    }
  }

  public void validateResourceResponse(LdapConnectionInfoDTO expected, LdapConnectionInfoDTO actual)
      throws Exception
  {
    Assert.assertEquals(actual.getAuthScheme(), expected.getAuthScheme());
    Assert.assertEquals(actual.getHost(), expected.getHost());
    Assert.assertEquals(expected.getPort(), actual.getPort());
    Assert.assertEquals(actual.getProtocol(), expected.getProtocol());
    Assert.assertEquals(actual.getRealm(), expected.getRealm());
    Assert.assertEquals(actual.getSearchBase(), expected.getSearchBase());
    Assert.assertEquals(actual.getSystemUsername(), expected.getSystemUsername());
    Assert.assertNull(actual.getSystemPassword());

    // also validate the file config
    this.validateLdapConfig(expected);
  }

}
