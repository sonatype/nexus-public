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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.proxy.ConnectHandler;
import org.eclipse.jetty.server.Request;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * HTTP CONNECT Handler that recods hosts it connects to.
 */
public class MonitorableConnectHandler
    extends ConnectHandler
{
  private final List<String> accessedUris;

  public MonitorableConnectHandler() {
    this(new ArrayList<String>());
  }

  public MonitorableConnectHandler(final List<String> accessedUris) {
    this.accessedUris = checkNotNull(accessedUris);
  }

  public List<String> getAccessedUris() {
    return accessedUris;
  }

  @Override
  public void handle(final String target,
                     final Request baseRequest,
                     final HttpServletRequest request,
                     final HttpServletResponse response)
      throws ServletException, IOException
  {
    accessedUris.add(target);
    super.handle(target, baseRequest, request, response);
  }
}
