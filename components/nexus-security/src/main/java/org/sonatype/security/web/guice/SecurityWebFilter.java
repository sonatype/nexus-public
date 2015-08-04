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
package org.sonatype.security.web.guice;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.sonatype.security.SecuritySystem;

import org.apache.shiro.web.filter.mgt.FilterChainResolver;
import org.apache.shiro.web.mgt.WebSecurityManager;
import org.apache.shiro.web.servlet.AbstractShiroFilter;
import org.apache.shiro.web.servlet.ShiroFilter;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Injected {@link ShiroFilter}.
 *
 * @deprecated To be removed, replaced by nexus-core SecurityFilter.
 */
@Singleton
@Deprecated
public class SecurityWebFilter
    extends AbstractShiroFilter
{
  @Inject
  protected SecurityWebFilter(SecuritySystem securitySystem, FilterChainResolver filterChainResolver) {
    this.setSecurityManager((WebSecurityManager) checkNotNull(securitySystem.getSecurityManager(), "securityManager"));
    this.setFilterChainResolver(checkNotNull(filterChainResolver, "filterChainResolver"));
  }

}
