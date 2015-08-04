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
package org.sonatype.nexus.testsuite.security.nexus448;

import java.io.IOException;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.jsecurity.realms.TargetPrivilegeDescriptor;
import org.sonatype.nexus.test.utils.PrivilegesMessageUtil;
import org.sonatype.security.realms.privileges.application.ApplicationPrivilegeDescriptor;
import org.sonatype.security.rest.model.PrivilegeStatusResource;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.data.MediaType;

/**
 * GETS for application privileges where returning an error, so this is a really simple test to make sure the GET will
 * work.
 */
public class Nexus448PrivilegeUrlIT
    extends AbstractNexusIntegrationTest
{

  private PrivilegesMessageUtil messageUtil;

  public Nexus448PrivilegeUrlIT() {
    this.messageUtil = new PrivilegesMessageUtil(this, this.getXMLXStream(), MediaType.APPLICATION_XML);
  }

  @BeforeClass
  public static void setSecureTest() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @Test
  public void testUrls()
      throws IOException
  {

    PrivilegeStatusResource resource = this.messageUtil.getPrivilegeResource("T2");
    Assert.assertEquals("Type", resource.getType(), TargetPrivilegeDescriptor.TYPE);

    resource = this.messageUtil.getPrivilegeResource("1");
    Assert.assertEquals("Type", resource.getType(), ApplicationPrivilegeDescriptor.TYPE);

  }

}
