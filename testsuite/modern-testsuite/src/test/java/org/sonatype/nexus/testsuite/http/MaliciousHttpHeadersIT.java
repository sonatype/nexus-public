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
package org.sonatype.nexus.testsuite.http;

import java.io.IOException;

import org.sonatype.nexus.testsuite.support.NexusRunningParametrizedITSupport;
import org.sonatype.nexus.testsuite.support.NexusStartAndStopStrategy;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;

import static org.apache.http.HttpHeaders.HOST;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

@NexusStartAndStopStrategy(NexusStartAndStopStrategy.Strategy.EACH_TEST)
public class MaliciousHttpHeadersIT
    extends NexusRunningParametrizedITSupport
{
  private static final String X_FORWARDED_PROTO = "X-Forwarded-Proto";

  public MaliciousHttpHeadersIT(final String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  @Test
  public void badHeaderShouldReturn400() throws Exception {
    final String badHeaderValue = "\"><script>alert(document.domain)</script>";
    final String target = nexus().getUrl() + "service/local/authentication/logout";

    testIt(new HttpGet(target), null, null, 200);

    testIt(new HttpGet(target), HOST, badHeaderValue, 400);
    testIt(new HttpPut(target), HOST, badHeaderValue, 400);
    testIt(new HttpPost(target), HOST, badHeaderValue, 400);
    testIt(new HttpHead(target), HOST, badHeaderValue, 400);
    testIt(new HttpDelete(target), HOST, badHeaderValue, 400);

    testIt(new HttpGet(target), X_FORWARDED_PROTO, badHeaderValue, 400);
    testIt(new HttpPut(target), X_FORWARDED_PROTO, badHeaderValue, 400);
    testIt(new HttpPost(target), X_FORWARDED_PROTO, badHeaderValue, 400);
    testIt(new HttpHead(target), X_FORWARDED_PROTO, badHeaderValue, 400);
    testIt(new HttpDelete(target), X_FORWARDED_PROTO, badHeaderValue, 400);
  }

  private void testIt(HttpRequestBase request, String header, String headerValue, int expectedStatus)
      throws IOException
  {
    if (header != null && headerValue != null) {
      request.setHeader(header, headerValue);
    }
    try (CloseableHttpClient client = HttpClients.createMinimal()) {
      try (CloseableHttpResponse response = client.execute(request)) {
        assertThat(response.getStatusLine().getStatusCode(), equalTo(expectedStatus));
      }
    }
  }
}
