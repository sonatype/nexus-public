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
package org.sonatype.nexus.repository.httpclient

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.repository.http.HttpMethods

import org.apache.http.Header
import org.apache.http.HttpHeaders
import org.apache.http.HttpRequest
import org.apache.http.HttpResponse
import org.apache.http.HttpVersion
import org.apache.http.message.BasicHeader
import org.apache.http.message.BasicHttpRequest
import org.apache.http.message.BasicHttpResponse
import org.apache.http.message.BasicRequestLine
import org.apache.http.message.BasicStatusLine
import org.apache.http.protocol.HttpContext
import org.junit.Assert
import org.junit.Test
import org.mockito.Mock
import org.mockito.Spy

import static org.mockito.Mockito.doReturn

public class RequestHeaderAuthenticationStrategyTest
    extends TestSupport
{
  private static final String REQUEST_ATTRIBUTE = "http.request";

  @Spy
  RequestHeaderAuthenticationStrategy under;

  @Mock
  HttpContext httpContext;

  @Test
  void testGetChallenges() {
    String bearerHeader = "Bearer " +
        "realm=\"https://registry.connect.redhat.com/auth/realms/rhc4tp/protocol/redhat-docker-v2/auth\"," +
        "service=\"docker-registry\""
    Header requestBearerHeader = new BasicHeader(HttpHeaders.AUTHORIZATION, bearerHeader)
    Header responseBearerHeader = new BasicHeader(HttpHeaders.WWW_AUTHENTICATE, bearerHeader)
    Header responseBasicHeader = new BasicHeader(HttpHeaders.WWW_AUTHENTICATE,
        "Basic realm=openshift,error=\"access denied\"")

    Map<String, Header> expected = new HashMap<>()
    expected.put("basic", responseBasicHeader)
    expected.put("bearer", responseBearerHeader)

    HttpRequest request = new BasicHttpRequest(
        new BasicRequestLine(HttpMethods.POST, "https://localhost:8081/", HttpVersion.HTTP_1_1));
    HttpResponse response = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, null));
    request.addHeader(requestBearerHeader);
    response.addHeader(responseBasicHeader);
    doReturn(request).when(httpContext).getAttribute(REQUEST_ATTRIBUTE)

    Assert.assertEquals(expected.keySet(), under.getChallenges(null, response, httpContext).keySet());
  }
}
