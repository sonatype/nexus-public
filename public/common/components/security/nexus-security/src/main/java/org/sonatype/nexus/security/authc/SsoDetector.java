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
package org.sonatype.nexus.security.authc;

/**
 * Abstraction that allows filters in {@code nexus-rapture} to detect whether SSO is active
 * without creating a direct dependency on private SAML or OAuth2 modules.
 *
 * <p>
 * Implementations are registered as Spring beans. If no implementation is present
 * (e.g. in open-source builds), callers should treat SSO as disabled.
 * </p>
 */
public interface SsoDetector
{

  /**
   * Returns {@code true} if at least one SSO provider (SAML, OIDC/OAuth2, etc.) is currently
   * enabled and should receive the authentication traffic instead of local credentials.
   */
  boolean isSsoEnabled();
}
