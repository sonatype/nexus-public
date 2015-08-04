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
package org.sonatype.nexus.testsuite.group.nexus977;

import org.sonatype.nexus.rest.model.RepositoryGroupMemberRepository;
import org.sonatype.nexus.rest.model.RepositoryGroupResource;
import org.sonatype.nexus.testsuite.group.nexus532.Nexus532GroupsCrudXmlIT;

import org.junit.Assert;
import org.junit.Test;
import org.restlet.data.Method;
import org.restlet.data.Response;

public class Nexus977GroupOfGroupsCrudIT
    extends Nexus532GroupsCrudXmlIT
{

  @Override
  protected void createMembers(RepositoryGroupResource resource) {
    RepositoryGroupMemberRepository member = new RepositoryGroupMemberRepository();
    member.setId(REPO_TEST_HARNESS_REPO);
    resource.addRepository(member);

    member = new RepositoryGroupMemberRepository();
    member.setId(REPO_NEXUS_TEST_HARNESS_RELEASE_GROUP);
    resource.addRepository(member);
  }

  @Test
  public void cyclic()
      throws Exception
  {
    RepositoryGroupResource groupA = new RepositoryGroupResource();

    groupA.setId("groupA");
    groupA.setName("groupA");
    groupA.setFormat("maven2");
    groupA.setProvider("maven2");

    createMembers(groupA);

    this.messageUtil.createGroup(groupA);

    RepositoryGroupResource groupB = new RepositoryGroupResource();

    groupB.setId("groupB");
    groupB.setName("groupB");
    groupB.setFormat("maven2");
    groupB.setProvider("maven2");

    RepositoryGroupMemberRepository member = new RepositoryGroupMemberRepository();
    member.setId(groupA.getId());
    groupB.addRepository(member);

    this.messageUtil.createGroup(groupB);

    // introduces cyclic referece between repos
    member = new RepositoryGroupMemberRepository();
    member.setId(groupB.getId());
    groupA.addRepository(member);
    Response resp = this.messageUtil.sendMessage(Method.PUT, groupA);
    Assert.assertFalse(resp.getStatus().isSuccess());
  }
}
