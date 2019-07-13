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
package org.sonatype.nexus.testsuite.testsupport.dispatch;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

/**
 * A chain of {@link RequestMatcher}, matching iff all matchers do.
 */
public class ChainedRequestMatcher
    implements RequestMatcher
{
  private List<RequestMatcher> requestMatchers = new ArrayList<>();

  public static ChainedRequestMatcher forOperation(final String operation) {
    final ChainedRequestMatcher chainedRequestMatcher = new ChainedRequestMatcher();
    return chainedRequestMatcher.operation(operation);
  }

  public ChainedRequestMatcher operation(final String operation) {
    requestMatchers.add(new PathEndsWith(operation));
    return this;
  }

  public ChainedRequestMatcher hasParam(final String paramName) {
    requestMatchers.add(new QueryParamMatcher(paramName));
    return this;
  }

  public ChainedRequestMatcher hasParam(final String paramName, final String value) {
    requestMatchers.add(new QueryParamMatcher(paramName, value));
    return this;
  }

  @Override
  public boolean matches(final HttpServletRequest request) throws Exception {
    for (RequestMatcher matcher : requestMatchers) {
      if (!matcher.matches(request)) {
        return false;
      }
    }
    return true;
  }
}
