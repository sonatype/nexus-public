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
package org.sonatype.security.rest.users;

import java.util.HashMap;
import java.util.Map;

import org.sonatype.plexus.rest.resource.PlexusResource;
import org.sonatype.security.rest.model.PlexusComponentListResource;
import org.sonatype.security.rest.model.PlexusComponentListResourceResponse;

import com.thoughtworks.xstream.XStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

/**
 * Tests for UserLocatorComponentListPlexusResource.
 */
public class UserLocatorComponentListTest
    extends AbstractSecurityRestTest
{

  public void testGet()
      throws Exception
  {
    PlexusResource resource = this.lookup(PlexusResource.class, "UserLocatorComponentListPlexusResource");
    Object result = resource.get(null, null, null, null);
    assertThat(result, instanceOf(PlexusComponentListResourceResponse.class));

    PlexusComponentListResourceResponse response = (PlexusComponentListResourceResponse) result;

    assertThat("Result: " + new XStream().toXML(response), response.getData().size(), equalTo(3));

    Map<String, String> data = new HashMap<String, String>();
    for (PlexusComponentListResource item : response.getData()) {
      data.put(item.getRoleHint(), item.getDescription());
    }

    assertThat(data.keySet(), containsInAnyOrder("default", "allConfigured", "MockUserManager"));
    assertThat(data.get("default"), equalTo("Default"));
    assertThat(data.get("allConfigured"), equalTo("All Configured Users"));
    assertThat(data.get("MockUserManager"), equalTo("MockUserManager"));
  }

}
