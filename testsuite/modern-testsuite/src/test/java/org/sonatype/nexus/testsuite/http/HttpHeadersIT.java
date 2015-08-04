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

import org.sonatype.nexus.client.core.NexusStatus;
import org.sonatype.nexus.testsuite.support.NexusRunningParametrizedITSupport;
import org.sonatype.nexus.testsuite.support.NexusStartAndStopStrategy;

import com.google.common.net.HttpHeaders;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.equalTo;

@NexusStartAndStopStrategy(NexusStartAndStopStrategy.Strategy.EACH_TEST)
public class HttpHeadersIT
    extends NexusRunningParametrizedITSupport
{

  public HttpHeadersIT(final String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  @Test
  public void ifRestletResourceRequestedThenResponseShouldContainSingleDateHeader() throws Exception {
    final HttpGet get = new HttpGet(nexus().getUrl() + "service/local/authentication/logout");
    try (CloseableHttpClient client = HttpClients.createMinimal()) {
      try (CloseableHttpResponse response = client.execute(get)) {
        Header[] headers = response.getHeaders(HttpHeaders.DATE);
        assertThat(headers, arrayWithSize(1));
      }
    }
  }

  @Test
  public void ifRestletResourceRequestedThenResponseShouldContainSingleCombinedServerHeader() throws Exception {
    final NexusStatus status = createNexusClientForAdmin(nexus()).getNexusStatus();
    final HttpGet get = new HttpGet(nexus().getUrl() + "service/local/authentication/logout");
    try (CloseableHttpClient client = HttpClients.createMinimal()) {
      try (CloseableHttpResponse response = client.execute(get)) {
        Header[] headers = response.getHeaders(HttpHeaders.SERVER);
        assertThat(headers, arrayWithSize(1));
        assertThat(headers[0].toString(), equalTo("Server: Nexus/" + status.getVersion() + " Noelios-Restlet-Engine/1.1.6-SONATYPE-5348-V8"));
      }
    }
  }

}
