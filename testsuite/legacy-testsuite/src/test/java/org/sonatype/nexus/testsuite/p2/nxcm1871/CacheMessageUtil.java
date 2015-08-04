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
package org.sonatype.nexus.testsuite.p2.nxcm1871;

import java.io.IOException;

import org.sonatype.nexus.integrationtests.RequestFacade;

import org.restlet.data.Method;
import org.restlet.data.Response;
import org.restlet.data.Status;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.sonatype.nexus.test.utils.TaskScheduleUtil.waitForAllTasksToStop;

public class CacheMessageUtil
{

  public static void expireRepositoryCache(final String... repositories)
      throws Exception
  {
    reindex(false, repositories);
  }

  private static void reindex(final boolean group, final String... repositories)
      throws IOException, Exception
  {
    for (final String repo : repositories) {
      // http://localhost:51292/nexus/service/local/data_cache/repo_groups/p2g/content
      String serviceURI;
      if (group) {
        serviceURI = "service/local/data_cache/repo_groups/" + repo + "/content";
      }
      else {
        serviceURI = "service/local/data_cache/repositories/" + repo + "/content";
      }

      final Response response = RequestFacade.sendMessage(serviceURI, Method.DELETE);
      final Status status = response.getStatus();
      assertThat("Fail to update " + repo + " repository index " + status, status.isSuccess(), is(true));
    }

    // let w8 a few time for indexes
    waitForAllTasksToStop();
  }

  public static void expireGroupCache(final String... groups)
      throws Exception
  {
    reindex(true, groups);
  }

}
