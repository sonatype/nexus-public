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
package org.sonatype.nexus.web;

import javax.servlet.http.HttpServletRequest;

import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.RequestContext;
import org.sonatype.nexus.proxy.router.RepositoryRouter;

import org.apache.shiro.subject.Subject;

/**
 * Common HTTP attribute and parameter keys, used to communicate between filters and servlets and similar.
 *
 * @since 2.7.0
 */
public interface Constants
{
  /**
   * Key of {@link HttpServletRequest} attribute to mark that a request was rejected by some component (like
   * {@link RepositoryRouter} throwing {@link AccessDeniedException} for example) due to lack of authorization of
   * current Shiro {@link Subject}. To mark this state, request attribute should contain {@link Boolean#TRUE} mapped
   * with this key.
   *
   * @since 2.7.0
   */
  String ATTR_KEY_REQUEST_IS_AUTHZ_REJECTED = "request.is.authz.rejected";
}
