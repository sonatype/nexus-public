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
package org.sonatype.nexus.repository.security;

import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.view.Request;

import org.apache.shiro.authz.AuthorizationException;

/**
 * Security facet.
 *
 * @since 3.0
 */
@Facet.Exposed
public interface SecurityFacet
  extends Facet
{
  /**
   * Ensure the given request is permitted on the repository.
   *
   * @throws AuthorizationException
   */
  void ensurePermitted(Request request);
}
