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
package org.sonatype.nexus.siesta.internal.resteasy;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.CacheControl;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.rest.NotCacheable;

import static javax.ws.rs.core.HttpHeaders.CACHE_CONTROL;
import static javax.ws.rs.core.HttpHeaders.EXPIRES;

/**
 * @since 3.36
 */
@NotCacheable
public class NotCacheableResponseFilter
    extends ComponentSupport
    implements ContainerResponseFilter
{
  private static final String PRAGMA = "Pragma";

  private static final String NO_CACHE = "no-cache";

  @Override
  public void filter(final ContainerRequestContext requestContext, final ContainerResponseContext responseContext)
      throws IOException
  {
    CacheControl cacheControl = new CacheControl();
    cacheControl.setNoStore(true);
    cacheControl.setNoCache(true);
    cacheControl.setMustRevalidate(true);
    cacheControl.setProxyRevalidate(true);
    cacheControl.setMaxAge(0);
    cacheControl.setSMaxAge(0);
    responseContext.getHeaders().add(CACHE_CONTROL, cacheControl.toString());
    responseContext.getHeaders().add(EXPIRES, 0);
    responseContext.getHeaders().add(PRAGMA, NO_CACHE);
  }
}
