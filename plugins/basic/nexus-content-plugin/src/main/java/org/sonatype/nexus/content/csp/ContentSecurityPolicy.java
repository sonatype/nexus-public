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
package org.sonatype.nexus.content.csp;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Will add the Content-Security-Policy header to responses unless an exclusion is in place
 */
@Singleton
@Named
public class ContentSecurityPolicy {
  private static final String
      SANDBOX =
      "sandbox allow-forms allow-modals allow-popups allow-presentation allow-scripts allow-top-navigation";

  private final List<ContentSecurityPolicyExclusion> contentSecurityPolicyExclusions;

  private final boolean sandboxEnabled;

  private Set<String> excludedPaths;

  @Inject
  public ContentSecurityPolicy(
      final List<ContentSecurityPolicyExclusion> contentSecurityPolicyExclusions,
      @Named("${nexus.repository.sandbox.enable:-true}") final boolean sandboxEnabled)
  {
    this.contentSecurityPolicyExclusions = contentSecurityPolicyExclusions;
    this.sandboxEnabled = sandboxEnabled;
  }

  public void apply(final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse) {
    if (excludedPaths == null) {
      excludedPaths = new HashSet<>();
      for (ContentSecurityPolicyExclusion exclusion : contentSecurityPolicyExclusions) {
        excludedPaths.addAll(exclusion.getExcludedPaths());
      }
    }
    if (sandboxEnabled && !excludePath(httpServletRequest)) {
      httpServletResponse.setHeader("Content-Security-Policy", SANDBOX);
      httpServletResponse.setHeader("X-Content-Security-Policy", SANDBOX);
    }
  }

  private boolean excludePath(final HttpServletRequest httpServletRequest) {
    String path = httpServletRequest.getRequestURI();
    return excludedPaths.stream().anyMatch(path::contains);
  }
}
