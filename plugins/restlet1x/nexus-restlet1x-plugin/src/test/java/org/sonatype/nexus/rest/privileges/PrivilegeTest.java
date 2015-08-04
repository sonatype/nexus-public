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
package org.sonatype.nexus.rest.privileges;

import org.sonatype.nexus.jsecurity.realms.TargetPrivilegeDescriptor;
import org.sonatype.nexus.rest.AbstractRestTestCase;
import org.sonatype.nexus.rest.model.PrivilegeResourceRequest;
import org.sonatype.plexus.rest.representation.XStreamRepresentation;

import org.junit.Test;
import org.restlet.data.MediaType;

public class PrivilegeTest
    extends AbstractRestTestCase
{

  @Test
  public void testTargetRequest()
      throws Exception
  {
    String jsonString =
        "{\"data\":{\"name\":\"Test Priv\",\"type\":\"target\",\"method\":[\"read\",\"create\"],"
            + "\"repositoryTargetId\":\"targetId\",\"repositoryId\":\"repoId\",\"repositoryGroupId\":\"groupId\"}}";
    XStreamRepresentation representation =
        new XStreamRepresentation(xstream, jsonString, MediaType.APPLICATION_JSON);

    PrivilegeResourceRequest request =
        (PrivilegeResourceRequest) representation.getPayload(new PrivilegeResourceRequest());

    assert request.getData().getName().equals("Test Priv");
    assert request.getData().getType().equals(TargetPrivilegeDescriptor.TYPE);
    assert request.getData().getMethod().size() == 2;
    assert request.getData().getMethod().contains("read");
    assert request.getData().getMethod().contains("create");
    assert request.getData().getRepositoryTargetId().equals("targetId");
    assert request.getData().getRepositoryId().equals("repoId");
    assert request.getData().getRepositoryGroupId().equals("groupId");
  }
}
