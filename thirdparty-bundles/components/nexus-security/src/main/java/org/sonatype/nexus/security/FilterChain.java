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
package org.sonatype.nexus.security;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A Shiro filter chain (mapping between a path pattern and a filter expression).
 *
 * @since 2.5
 */
public class FilterChain
{
  private final String pathPattern;

  private final String filterExpression;

  public FilterChain(final String pathPattern, final String filterExpression) {
    this.pathPattern = checkNotNull(pathPattern);
    this.filterExpression = checkNotNull(filterExpression);
  }

  public String getPathPattern() {
    return pathPattern;
  }

  public String getFilterExpression() {
    return filterExpression;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "pathPattern='" + pathPattern + '\'' +
        ", filterExpression='" + filterExpression + '\'' +
        '}';
  }
}
