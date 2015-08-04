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

import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.plexus.rest.representation.XStreamRepresentation;
import org.sonatype.security.rest.model.UserChangePasswordRequest;
import org.sonatype.security.rest.model.UserChangePasswordResource;

import com.thoughtworks.xstream.XStream;
import org.restlet.data.MediaType;
import org.restlet.data.Status;

public class ChangePasswordUtils
{

  private static XStream xstream;

  static {
    xstream = XStreamFactory.getXmlXStream();
  }

  public static Status changePassword(String username, String oldPassword, String newPassword)
      throws Exception
  {
    String serviceURI = "service/local/users_changepw";

    UserChangePasswordResource resource = new UserChangePasswordResource();
    resource.setUserId(username);
    resource.setOldPassword(oldPassword);
    resource.setNewPassword(newPassword);

    UserChangePasswordRequest request = new UserChangePasswordRequest();
    request.setData(resource);

    XStreamRepresentation representation = new XStreamRepresentation(xstream, "", MediaType.APPLICATION_XML);
    representation.setPayload(request);

    return RequestFacade.doPostForStatus(serviceURI, representation);

  }

  public static Status changePassword(String username, String newPassword)
      throws Exception
  {
    String serviceURI = "service/local/users_setpw";

    UserChangePasswordResource resource = new UserChangePasswordResource();
    resource.setUserId(username);
    resource.setNewPassword(newPassword);

    UserChangePasswordRequest request = new UserChangePasswordRequest();
    request.setData(resource);

    XStreamRepresentation representation = new XStreamRepresentation(xstream, "", MediaType.APPLICATION_XML);
    representation.setPayload(request);

    return RequestFacade.doPostForStatus(serviceURI, representation);
  }

}
