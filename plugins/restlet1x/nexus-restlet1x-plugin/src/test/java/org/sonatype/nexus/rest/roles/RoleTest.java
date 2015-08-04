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
package org.sonatype.nexus.rest.roles;

import org.sonatype.nexus.rest.AbstractRestTestCase;
import org.sonatype.plexus.rest.representation.XStreamRepresentation;
import org.sonatype.security.rest.model.RoleResourceRequest;

import org.junit.Test;
import org.restlet.data.MediaType;

public class RoleTest
    extends AbstractRestTestCase
{

  @Test
  public void testRequest()
      throws Exception
  {
    String jsonString =
        "{\"data\":{\"id\":null,\"name\":\"Test Role\",\"description\":\"This is a test role\",\"sessionTimeout\":50,"
            + "\"roles\":[\"roleid\"],\"privileges\":[\"privid\"]}}}";
    XStreamRepresentation representation =
        new XStreamRepresentation(xstream, jsonString, MediaType.APPLICATION_JSON);

    RoleResourceRequest request = (RoleResourceRequest) representation.getPayload(new RoleResourceRequest());

    assert request.getData().getId() == null;
    assert request.getData().getName().equals("Test Role");
    assert request.getData().getDescription().equals("This is a test role");
    assert request.getData().getSessionTimeout() == 50;
    assert request.getData().getRoles().size() == 1;
    assert ((String) request.getData().getRoles().get(0)).equals("roleid");
    assert request.getData().getPrivileges().size() == 1;
    assert ((String) request.getData().getPrivileges().get(0)).equals("privid");
  }
}
