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
package org.sonatype.nexus.security.token;

import java.util.List;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.sonatype.nexus.security.authc.apikey.ApiKeyExtractor;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.net.HttpHeaders.AUTHORIZATION;

/**
 * Extracts a token from a HttpServletRequest
 *
 * @since 3.6
 */
public class BearerToken
    implements ApiKeyExtractor
{
  private final String format;

  public BearerToken (final String format) {
    this.format = checkNotNull(format);
  }

  @Nullable
  @Override
  public String extract(final HttpServletRequest request) {
    final String headerValue = request.getHeader(AUTHORIZATION);
    if (headerValue != null && headerValue.startsWith("Bearer ")) {
      List<String> parts = Lists.newArrayList(Splitter.on(' ').split(headerValue));
      if (parts.size() == 2 && "Bearer".equals(parts.get(0)) && matchesFormat(parts)) {
        return parts.get(1).replaceAll(format + ".", "");
      }
    }
    return null;
  }

  protected boolean matchesFormat(final List<String> parts) {
    return parts.get(1).startsWith(format);
  }
}
