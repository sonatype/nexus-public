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
package org.sonatype.security.web;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.shiro.web.filter.mgt.FilterChainManager;
import org.apache.shiro.web.filter.mgt.FilterChainResolver;
import org.apache.shiro.web.filter.mgt.PathMatchingFilterChainResolver;

/**
 * The default implementation requires a FilterChainManager, so the configuration can be passed to it.
 *
 * @author Brian Demers
 */
@Singleton
@Typed(ProtectedPathManager.class)
@Named("default")
public class DefaultProtectedPathManager
    implements ProtectedPathManager, FilterChainManagerAware
{
  private FilterChainManager filterChainManager;

  protected Map<String, String> pseudoChains = new LinkedHashMap<String, String>();

  @Inject
  public DefaultProtectedPathManager(@Nullable FilterChainResolver filterChainResolver) {
    if (filterChainResolver instanceof PathMatchingFilterChainResolver) {
      setFilterChainManager(((PathMatchingFilterChainResolver) filterChainResolver).getFilterChainManager());
    }
  }

  public DefaultProtectedPathManager() {
    // legacy constructor
  }

  public void addProtectedResource(String pathPattern, String filterExpression) {
    // Only save the pathPattern and filterExpression in the pseudoChains, does not put real filters into the real
    // chain.
    // We can not get the real filters because this method is invoked when the application is starting, when
    // ShiroSecurityFilter
    // might not be located.

    if (this.filterChainManager != null) {
      this.filterChainManager.createChain(pathPattern, filterExpression);
    }
    else {
      this.pseudoChains.put(pathPattern, filterExpression);
    }
  }

  public void setFilterChainManager(FilterChainManager filterChainManager) {
    if (filterChainManager != null && filterChainManager != this.filterChainManager) {
      this.filterChainManager = filterChainManager;

      // lazy load: see https://issues.sonatype.org/browse/NEXUS-3111
      // which to me seems like a glassfish bug...
      for (Entry<String, String> entry : this.pseudoChains.entrySet()) {
        this.filterChainManager.createChain(entry.getKey(), entry.getValue());
      }
    }
  }
}
