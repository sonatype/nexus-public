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

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

/**
 * Matches requests as long as they have a particular query string parameter.
 */
public class QueryParamMatcher
    implements RequestMatcher
{
  private final String paramName;

  private final String value;

  public QueryParamMatcher(final String paramName) {
    this(paramName, null);
  }

  public QueryParamMatcher(final String paramName, final String value) {
    this.paramName = paramName;
    this.value = value;
  }

  @Override
  public boolean matches(final HttpServletRequest request) throws Exception {
    final URI uri = new URI("http://placeholder?" + request.getQueryString());
    List<NameValuePair> params = URLEncodedUtils.parse(uri, StandardCharsets.UTF_8);

    for (NameValuePair param : params) {
      if (param.getName().equals(paramName)) {
        return value == null ? true : value.equals(param.getValue());
      }
    }
    return false;
  }
}
