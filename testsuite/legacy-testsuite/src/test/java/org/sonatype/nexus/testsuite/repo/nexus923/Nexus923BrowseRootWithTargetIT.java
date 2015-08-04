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
package org.sonatype.nexus.testsuite.repo.nexus923;

import java.util.List;

import org.sonatype.nexus.integrationtests.AbstractPrivilegeTest;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.jsecurity.realms.TargetPrivilegeDescriptor;
import org.sonatype.nexus.rest.model.ContentListResource;
import org.sonatype.nexus.rest.model.PrivilegeResource;
import org.sonatype.nexus.rest.model.RepositoryTargetResource;
import org.sonatype.nexus.test.utils.ContentListMessageUtil;
import org.sonatype.security.rest.model.PrivilegeStatusResource;

import org.junit.Assert;
import org.junit.Test;
import org.restlet.data.MediaType;

public class Nexus923BrowseRootWithTargetIT
    extends AbstractPrivilegeTest
{
  @Test
  public void browseRootTest()
      throws Exception
  {
    if (this.printKnownErrorButDoNotFail(Nexus923BrowseRootWithTargetIT.class, "browseRootTest")) {
      return;
    }

    // create repo target
    RepositoryTargetResource target = new RepositoryTargetResource();
    target.setName("browseRootTest");
    target.setContentClass("maven2");
    target.addPattern("/nexus923/group/*");

    target = this.targetUtil.createTarget(target);

    // now create a priv
    PrivilegeResource browseRootTestPriv = new PrivilegeResource();
    browseRootTestPriv.addMethod("create");
    browseRootTestPriv.addMethod("read");
    browseRootTestPriv.addMethod("update");
    browseRootTestPriv.addMethod("delete");
    browseRootTestPriv.setName("browseRootTestPriv");
    browseRootTestPriv.setType(TargetPrivilegeDescriptor.TYPE);
    browseRootTestPriv.setRepositoryTargetId(target.getId());
    browseRootTestPriv.setRepositoryId(this.getTestRepositoryId());
    // get the Resource object
    List<PrivilegeStatusResource> browseRootTestPrivs = this.privUtil.createPrivileges(browseRootTestPriv);
    String[] pivsStrings = new String[browseRootTestPrivs.size()];
    for (int ii = 0; ii < browseRootTestPrivs.size(); ii++) {
      PrivilegeStatusResource priv = browseRootTestPrivs.get(ii);
      pivsStrings[ii] = priv.getId();
    }

    this.overwriteUserRole(TEST_USER_NAME, "browseRootTestRole", pivsStrings);
    this.giveUserPrivilege(TEST_USER_NAME, "repository-all");

    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    ContentListMessageUtil contentUtil = new ContentListMessageUtil(
        this.getXMLXStream(),
        MediaType.APPLICATION_XML);

    List<ContentListResource> items = contentUtil.getContentListResource(this.getTestRepositoryId(), "/", false);
    Assert.assertEquals("Expected to have only one entry: " + items, items.size(), 1);

    items = contentUtil.getContentListResource(this.getTestRepositoryId(), "/nexus923", false);
    Assert.assertEquals("Expected to have only one entry: " + items, items.size(), 1);

    items = contentUtil.getContentListResource(this.getTestRepositoryId(), "/nexus923/group/", false);
    Assert.assertEquals("Expected to have only one entry: " + items, items.size(), 1);
  }

}
