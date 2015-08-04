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
package org.sonatype.nexus.testsuite.security.nxcm897;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.rest.model.UserAccount;
import org.sonatype.nexus.test.utils.UserAccountMessageUtil;
import org.sonatype.nexus.test.utils.UserMessageUtil;
import org.sonatype.nexus.test.utils.XStreamFactory;
import org.sonatype.security.rest.model.UserResource;

import org.junit.Assert;
import org.junit.Test;
import org.restlet.data.MediaType;

/**
 * Copied from the nexus-user-account-plugin to test the migrated UserAccountManager.
 */
public class NXCM897UserAccountIT
    extends AbstractNexusIntegrationTest
{
  protected UserAccountMessageUtil accountMsgUtil = new UserAccountMessageUtil();

  protected UserMessageUtil userMsgUtil =
      new UserMessageUtil(this, XStreamFactory.getXmlXStream(), MediaType.APPLICATION_XML.APPLICATION_XML);

  @Test
  public void userAccountCRU()
      throws Exception
  {
    UserResource resource = new UserResource();
    resource.setUserId("nxcm897");
    resource.setFirstName("NXCM 897");
    resource.setEmail("nxcm897@changeme.com");
    resource.setPassword("admin123");
    resource.addRole("ui-basic");
    resource.setStatus("active");

    this.userMsgUtil.createUser(resource);

    UserAccount dto = accountMsgUtil.readAccount("nxcm897");
    Assert.assertEquals("NXCM 897", dto.getFirstName());
    Assert.assertEquals("nxcm897@changeme.com", dto.getEmail());

    dto.setEmail("nxcm897@changeme2.com");
    dto = accountMsgUtil.updateAccount(dto);
    Assert.assertEquals("nxcm897@changeme2.com", dto.getEmail());
  }
}
