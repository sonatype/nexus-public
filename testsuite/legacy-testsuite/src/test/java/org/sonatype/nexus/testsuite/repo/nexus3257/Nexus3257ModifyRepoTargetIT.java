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
package org.sonatype.nexus.testsuite.repo.nexus3257;

import java.util.Arrays;
import java.util.List;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.jsecurity.realms.TargetPrivilegeDescriptor;
import org.sonatype.nexus.rest.model.PrivilegeResource;
import org.sonatype.nexus.rest.model.RepositoryTargetResource;
import org.sonatype.nexus.test.utils.PrivilegesMessageUtil;
import org.sonatype.nexus.test.utils.TargetMessageUtil;
import org.sonatype.security.rest.model.PrivilegeStatusResource;

import org.junit.Test;
import org.restlet.data.MediaType;

public class Nexus3257ModifyRepoTargetIT
    extends AbstractNexusIntegrationTest
{
  TargetMessageUtil targetUtil;

  PrivilegesMessageUtil privUtil;

  public Nexus3257ModifyRepoTargetIT() {
    targetUtil = new TargetMessageUtil(this, getXMLXStream(), MediaType.APPLICATION_XML);
    privUtil = new PrivilegesMessageUtil(this, getXMLXStream(), MediaType.APPLICATION_XML);
  }

  @Test
  public void testChangeTarget()
      throws Exception
  {
    RepositoryTargetResource target = new RepositoryTargetResource();
    target.setContentClass("maven2");
    target.setName("nexus3257-target");
    target.addPattern("/some-pattern");

    target = targetUtil.createTarget(target);

    // now add some privs
    PrivilegeResource privReq = new PrivilegeResource();
    privReq.setDescription("nexus3257-target repo-target privilege");
    privReq.setMethod(Arrays.asList("create", "read", "update", "delete"));
    privReq.setName("nexus-3257-priv");
    privReq.setRepositoryTargetId(target.getId());
    privReq.setType(TargetPrivilegeDescriptor.TYPE);

    List<PrivilegeStatusResource> privs = privUtil.createPrivileges(privReq);

    // now make sure the privs exist
    checkPrivs(privs);

    // now lets change the target and add a new path
    target.addPattern("/other-pattern");
    targetUtil.saveTarget(target, true);

    // now make sure the privs still exist
    checkPrivs(privs);
  }

  private void checkPrivs(List<PrivilegeStatusResource> privs)
      throws Exception
  {
    for (PrivilegeStatusResource priv : privs) {
      privUtil.getPrivilegeResource(priv.getId());
    }
  }
}
