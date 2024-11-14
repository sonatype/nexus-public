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

import java.util.List;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

/**
 * Default impl that will add any values from a comma seperated list in nexus.properties to the excluded list
 */
@Named
@Singleton
public class DefaultContentSecurityPolicyExclusion implements ContentSecurityPolicyExclusion
{
  private final List<String> excludedPaths;

  @Inject
  public DefaultContentSecurityPolicyExclusion(@Named("${nexus.rest.csp.exclusions}") @Nullable String excludedPaths) {
    if (excludedPaths != null && !excludedPaths.isEmpty()) {
      this.excludedPaths = asList(excludedPaths.split(","));
    }
    else {
      this.excludedPaths = emptyList();
    }
  }
  @Override
  public List<String> getExcludedPaths() {
    return excludedPaths;
  }
}
