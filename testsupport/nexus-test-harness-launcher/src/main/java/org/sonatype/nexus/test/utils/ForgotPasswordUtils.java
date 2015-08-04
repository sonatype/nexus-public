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

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.plexus.rest.representation.XStreamRepresentation;
import org.sonatype.security.rest.model.UserForgotPasswordRequest;
import org.sonatype.security.rest.model.UserForgotPasswordResource;

import com.thoughtworks.xstream.XStream;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Response;

/**
 * Example of simple conversion to "fluent" api from static stuff. This opens the gates to paralell ITs too! Currently,
 * we cannot have them.
 *
 * @author cstamas
 */
public class ForgotPasswordUtils
    extends ITUtil
{
  private final XStream xstream;

  public static ForgotPasswordUtils get(AbstractNexusIntegrationTest test) {
    return new ForgotPasswordUtils(test);
  }

  public ForgotPasswordUtils(AbstractNexusIntegrationTest test) {
    super(test);

    this.xstream = XStreamFactory.getXmlXStream();
  }

  public Response recoverUserPassword(String username, String email)
      throws Exception
  {
    String serviceURI = "service/local/users_forgotpw";
    UserForgotPasswordResource resource = new UserForgotPasswordResource();
    resource.setUserId(username);
    resource.setEmail(email);

    UserForgotPasswordRequest request = new UserForgotPasswordRequest();
    request.setData(resource);

    XStreamRepresentation representation = new XStreamRepresentation(xstream, "", MediaType.APPLICATION_XML);
    representation.setPayload(request);

    return RequestFacade.sendMessage(serviceURI, Method.POST, representation);
  }
}
