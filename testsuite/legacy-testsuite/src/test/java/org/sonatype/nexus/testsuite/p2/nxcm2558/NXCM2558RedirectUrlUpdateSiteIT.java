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
package org.sonatype.nexus.testsuite.p2.nxcm2558;

import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.plugins.p2.repository.updatesite.UpdateSiteMirrorTask;
import org.sonatype.nexus.plugins.p2.repository.updatesite.UpdateSiteMirrorTaskDescriptor;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.test.utils.ResponseMatchers;
import org.sonatype.nexus.testsuite.p2.AbstractNexusProxyP2IT;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.restlet.data.Response;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.sonatype.nexus.test.utils.TaskScheduleUtil.runTask;
import static org.sonatype.nexus.test.utils.TaskScheduleUtil.waitForAllTasksToStop;

public class NXCM2558RedirectUrlUpdateSiteIT
    extends AbstractNexusProxyP2IT
{

  private void doTest(String repoId, Matcher<Response> matcher, boolean setRepo, boolean install)
      throws Exception
  {
    if (setRepo) {
      final ScheduledServicePropertyResource repo = new ScheduledServicePropertyResource();
      repo.setKey(UpdateSiteMirrorTaskDescriptor.REPO_OR_GROUP_FIELD_ID);
      repo.setValue(repoId);

      runTask(UpdateSiteMirrorTask.ROLE_HINT + System.nanoTime(), UpdateSiteMirrorTask.ROLE_HINT, repo);
    }
    else {
      runTask(UpdateSiteMirrorTask.ROLE_HINT + System.nanoTime(), UpdateSiteMirrorTask.ROLE_HINT);
    }
    waitForAllTasksToStop();

    final Response response = RequestFacade.doGetRequest("content/repositories/" + repoId + "/features/");
    assertThat(response, matcher);

    if (install) {
      installAndVerifyP2Feature(repoId);
    }
  }

  @Test
  public void repository()
      throws Exception
  {
    doTest("nxcm2558", ResponseMatchers.isSuccessful(), true, true);
  }

  @Test
  public void group()
      throws Exception
  {
    doTest("nxcm3654", ResponseMatchers.isSuccessful(), true, false);
  }

  @Test
  public void all()
      throws Exception
  {
    doTest("all-repos", ResponseMatchers.isSuccessful(), false, false);
  }

  @Test
  public void invalid()
      throws Exception
  {
    doTest("invalid", ResponseMatchers.inError(), true, false);
  }

}
