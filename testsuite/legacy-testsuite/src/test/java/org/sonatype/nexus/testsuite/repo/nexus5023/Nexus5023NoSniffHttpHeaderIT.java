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
package org.sonatype.nexus.testsuite.repo.nexus5023;

import java.net.URL;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.RequestFacade;

import org.junit.Test;
import org.restlet.data.Form;
import org.restlet.data.Method;
import org.restlet.data.Parameter;
import org.restlet.data.Response;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Tests that Nexus will set an "X-Content-Type-Options: nosniff" response header.
 *
 * @since 2.1
 */
public class Nexus5023NoSniffHttpHeaderIT
    extends AbstractNexusIntegrationTest
{

  @Test
  public void downloadViaContentRepositories()
      throws Exception
  {
    downloadAndVerifyHeader(
        "content/repositories/" + getTestRepositoryId() + "/nexus5023/release-jar/1/release-jar-1.jar"
    );
  }

  @Test
  public void downloadViaServiceRepositories()
      throws Exception
  {
    downloadAndVerifyHeader(
        "service/local/repositories/" + getTestRepositoryId() + "/content/nexus5023/release-jar/1/release-jar-1.jar"
    );
  }

  @Test
  public void downloadViaServiceArtifact()
      throws Exception
  {
    downloadAndVerifyHeader(
        "service/local/artifact/maven/content?g=nexus5023&a=release-jar&v=1&e=jar&r=" + getTestRepositoryId()
    );
  }

  private void downloadAndVerifyHeader(final String path)
      throws Exception
  {
    final URL url = RequestFacade.toNexusURL(path);

    Response response = null;
    try {
      response = RequestFacade.sendMessage(url, Method.GET, null);
      assertThat(response.getStatus().isSuccess(), is(true));

      final Form form = (Form) response.getAttributes().get("org.restlet.http.headers");
      assertThat(form, is(notNullValue()));

      final Parameter xCTOHeader = form.getFirst("X-Content-Type-Options");
      assertThat(xCTOHeader, is(notNullValue()));
      assertThat(xCTOHeader.getValue(), is(equalTo("nosniff")));
    }
    finally {
      RequestFacade.releaseResponse(response);
    }
  }

}
