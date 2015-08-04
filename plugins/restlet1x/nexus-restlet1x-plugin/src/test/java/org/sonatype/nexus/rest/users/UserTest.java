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
package org.sonatype.nexus.rest.users;

import org.sonatype.nexus.rest.AbstractRestTestCase;
import org.sonatype.plexus.rest.representation.XStreamRepresentation;
import org.sonatype.security.rest.model.UserResourceRequest;

import org.junit.Test;
import org.restlet.data.MediaType;

public class UserTest
    extends AbstractRestTestCase
{

  @Test
  public void testRequest()
      throws Exception
  {
    String jsonString =
        "{\"data\":{\"userId\":\"myuser\",\"firstName\":\"johnny test\",\"email\":\"test@email.com\",\"status\":\"active\","
            + "\"roles\":[\"roleId\"]}}}";
    XStreamRepresentation representation =
        new XStreamRepresentation(xstream, jsonString, MediaType.APPLICATION_JSON);

    UserResourceRequest request = (UserResourceRequest) representation.getPayload(new UserResourceRequest());

    assert request.getData().getUserId().equals("myuser");
    assert request.getData().getFirstName().equals("johnny test");
    assert request.getData().getEmail().equals("test@email.com");
    assert request.getData().getStatus().equals("active");
    assert request.getData().getRoles().size() == 1;
    assert request.getData().getRoles().get(0).equals("roleId");
  }
}
