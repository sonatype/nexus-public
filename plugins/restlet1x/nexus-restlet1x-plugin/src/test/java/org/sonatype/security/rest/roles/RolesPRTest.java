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
package org.sonatype.security.rest.roles;

import org.sonatype.plexus.rest.resource.PlexusResource;
import org.sonatype.security.rest.model.RoleResource;
import org.sonatype.security.rest.model.RoleResourceRequest;
import org.sonatype.security.rest.model.RoleResourceResponse;
import org.sonatype.security.rest.users.AbstractSecurityRestTest;

import junit.framework.Assert;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.data.Response;

public class RolesPRTest
    extends AbstractSecurityRestTest
{

  public void testPostWithSpace()
      throws Exception
  {
    PlexusResource resource = this.lookup(PlexusResource.class, "RoleListPlexusResource");

    RoleResourceRequest roleRequest = new RoleResourceRequest();
    roleRequest.setData(new RoleResource());

    roleRequest.getData().setId("with spaces");
    roleRequest.getData().setDescription("foo");
    roleRequest.getData().setName("Foo Bar");
    roleRequest.getData().setSessionTimeout(60);
    roleRequest.getData().addPrivilege("1001");

    Request request = new Request();
    Reference ref = new Reference("http://localhost:12345/");
    request.setRootRef(ref);
    request.setResourceRef(new Reference(ref, "roles"));

    Response response = new Response(request);

    RoleResourceResponse roleResponse = (RoleResourceResponse) resource.post(null, request, response, roleRequest);

    Assert.assertEquals("with spaces", roleResponse.getData().getId());

    // ok now we try the gets
    resource = this.lookup(PlexusResource.class, "RolePlexusResource");

    // first with +
    request.getAttributes().put("roleId", "with+spaces");
    roleResponse = (RoleResourceResponse) resource.get(null, request, response, null);
    Assert.assertEquals("with spaces", roleResponse.getData().getId());

    // then with %20
    request.getAttributes().put("roleId", "with%20spaces");
    roleResponse = (RoleResourceResponse) resource.get(null, request, response, null);
    Assert.assertEquals("with spaces", roleResponse.getData().getId());

  }

}
