/*
 * Copyright (c) 2010-2014 Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package org.sonatype.nexus.test.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.ImmutableMap;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.B64Code;
import org.eclipse.jetty.util.StringUtil;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A real HTTP proxy servlet with ability to gather
 * accessed URIs accessed via this proxy.
 */
public class MonitorableProxyServlet
    extends org.eclipse.jetty.proxy.ProxyServlet
{
  private final List<String> accessedUris;

  private final Map<String, String> authentications;

  public MonitorableProxyServlet() {
    this(new ArrayList<String>(), null);
  }

  public MonitorableProxyServlet(final List<String> accessedUris, @Nullable final Map<String, String> authentications) {
    this.accessedUris = checkNotNull(accessedUris);
    if (authentications != null) {
      this.authentications = ImmutableMap.copyOf(authentications);
    }
    else {
      this.authentications = null;
    }
  }

  public List<String> getAccessedUris() {
    return accessedUris;
  }

  @Override
  public void service(final ServletRequest req, final ServletResponse res)
      throws ServletException,
             IOException
  {
    final HttpServletRequest request = (HttpServletRequest) req;
    final HttpServletResponse response = (HttpServletResponse) res;

    if (authentications != null) {
      String proxyAuthorization = request.getHeader("Proxy-Authorization");
      if (proxyAuthorization != null && proxyAuthorization.startsWith("Basic ")) {
        String proxyAuth = proxyAuthorization.substring(6);
        String authorization = B64Code.decode(proxyAuth, StringUtil.__ISO_8859_1);
        String[] authTokens = authorization.split(":");
        String user = authTokens[0];
        String password = authTokens[1];

        String authPass = authentications.get(user);
        if (!password.equals(authPass)) {
          // Proxy-Authenticate Basic realm="CCProxy Authorization"
          response.addHeader("Proxy-Authenticate", "Basic realm=\"Jetty Proxy Authorization\"");
          response.setStatus(HttpServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED);
          return;
        }
      }
    }
    final HttpURI uri = ((Request) req).getHttpURI();
    accessedUris.add(uri.getHost() + ":" + uri.getPort());
    super.service(req, res);
  }
}
