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
package org.sonatype.nexus.rest.component;

import java.util.List;

import org.sonatype.nexus.NexusAppTestSupport;
import org.sonatype.nexus.rest.model.PlexusComponentListResource;
import org.sonatype.nexus.rest.model.PlexusComponentListResourceResponse;
import org.sonatype.plexus.rest.resource.PlexusResource;

import junit.framework.Assert;
import org.junit.Test;
import org.restlet.data.Request;
import org.restlet.resource.ResourceException;

// This is an IT just because it runs longer then 15 seconds
public class ComponentPlexusResourceIT
    extends NexusAppTestSupport
{
  private AbstractComponentListPlexusResource getComponentPlexusResource()
      throws Exception
  {
    return (AbstractComponentListPlexusResource) this.lookup(PlexusResource.class, "ComponentPlexusResource");
  }

  private PlexusComponentListResourceResponse runGetForRole(String role)
      throws Exception
  {
    AbstractComponentListPlexusResource componentPlexusResource = this.getComponentPlexusResource();

    Request request = new Request();
    request.getAttributes().put(AbstractComponentListPlexusResource.ROLE_ID, role);

    return (PlexusComponentListResourceResponse) componentPlexusResource.get(null, request, null, null);
  }

  @Test
  public void testInvalidRole()
      throws Exception
  {
    try {
      runGetForRole("JUNK-FOO_BAR-JUNK");
      Assert.fail("expected error thrown");
    }
    catch (ResourceException e) {
      Assert.assertEquals("Expected a 404 error message.", 404, e.getStatus().getCode());
    }
  }

  @Test
  public void testValidRoleMultipleResults()
      throws Exception
  {
    PlexusComponentListResourceResponse result = runGetForRole(PlexusResource.class.getName());

    Assert.assertTrue(result.getData().size() > 1); // expected a bunch of these thing, with new ones being
    // added all the time.

    // now for a more controled test
    result = runGetForRole("MULTI_TEST");
    Assert.assertEquals(2, result.getData().size());

    // the order is undefined
    PlexusComponentListResource resource1 = null;
    PlexusComponentListResource resource2 = null;

    for (PlexusComponentListResource resource : (List<PlexusComponentListResource>) result.getData()) {
      if (resource.getRoleHint().endsWith("1")) {
        resource1 = resource;
      }
      else {
        resource2 = resource;
      }
    }

    // make sure we found both
    Assert.assertNotNull(resource1);
    Assert.assertNotNull(resource2);

    Assert.assertEquals("Description-1", resource1.getDescription());
    Assert.assertEquals("hint-1", resource1.getRoleHint());

    Assert.assertEquals("Description-2", resource2.getDescription());
    Assert.assertEquals("hint-2", resource2.getRoleHint());

  }

  @Test
  public void testValidRoleSingleResult()
      throws Exception
  {
    PlexusComponentListResourceResponse result = runGetForRole("TEST_ROLE");

    Assert.assertTrue(result.getData().size() == 1);

    PlexusComponentListResource resource = (PlexusComponentListResource) result.getData().get(0);

    Assert.assertEquals("Test Description.", resource.getDescription());
    Assert.assertEquals("test-hint", resource.getRoleHint());
  }

  @Test
  public void testNullDescriptionAndHint()
      throws Exception
  {
    PlexusComponentListResourceResponse result = runGetForRole("TEST_NULL");

    Assert.assertTrue(result.getData().size() == 1);

    PlexusComponentListResource resource = (PlexusComponentListResource) result.getData().get(0);

    Assert.assertEquals("default", resource.getDescription());
    Assert.assertEquals("default", resource.getRoleHint());
  }

  @Test
  public void testEmptyDescriptionAndHint()
      throws Exception
  {
    PlexusComponentListResourceResponse result = runGetForRole("TEST_EMPTY");

    Assert.assertTrue(result.getData().size() == 1);

    PlexusComponentListResource resource = (PlexusComponentListResource) result.getData().get(0);

    Assert.assertEquals("default", resource.getDescription());
    Assert.assertEquals("default", resource.getRoleHint());
  }
}
