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
package org.sonatype.nexus.test.utils;

import java.io.IOException;

import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.rest.model.UserAccount;
import org.sonatype.nexus.rest.model.UserAccountRequestResponseWrapper;
import org.sonatype.plexus.rest.representation.XStreamRepresentation;

import com.thoughtworks.xstream.XStream;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserAccountMessageUtil
{
  private static final String BASE_URL = "service/local/user_account/";

  private static final Logger LOGGER = LoggerFactory.getLogger(UserAccountMessageUtil.class);

  private static XStream xmlXstream;

  static {
    xmlXstream = XStreamFactory.getXmlXStream();
  }

  private UserAccount readAccount(Response response)
      throws IOException
  {
    if (response.getStatus().isSuccess()) {
      String responseText = response.getEntity().getText();

      LOGGER.debug("Response Text: \n" + responseText);

      XStreamRepresentation representation = new XStreamRepresentation(
          xmlXstream,
          responseText,
          MediaType.APPLICATION_XML);

      UserAccountRequestResponseWrapper responseDTO = (UserAccountRequestResponseWrapper) representation
          .getPayload(new UserAccountRequestResponseWrapper());

      return responseDTO.getData();
    }
    else {
      LOGGER.warn("HTTP Error: '" + response.getStatus().getCode() + "'");

      LOGGER.warn(response.getEntity().getText());

      return null;
    }
  }

  public UserAccount readAccount(String id)
      throws IOException
  {
    String serviceURI = BASE_URL + id;

    LOGGER.info("HTTP GET: '" + serviceURI + "'");

    Response response = RequestFacade.sendMessage(serviceURI, Method.GET);

    return readAccount(response);
  }

  public UserAccount updateAccount(UserAccount dto)
      throws IOException
  {
    String serviceURI = BASE_URL + dto.getUserId();

    XStreamRepresentation representation = new XStreamRepresentation(xmlXstream, "", MediaType.APPLICATION_XML);

    UserAccountRequestResponseWrapper requestDTO = new UserAccountRequestResponseWrapper();

    requestDTO.setData(dto);

    representation.setPayload(requestDTO);

    LOGGER.info("HTTP PUT: '" + serviceURI + "'");

    Response response = RequestFacade.sendMessage(serviceURI, Method.PUT, representation);

    return readAccount(response);
  }

}
