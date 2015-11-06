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
package org.sonatype.nexus.security.authc.apikey;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.sonatype.nexus.security.authc.NexusApiKeyAuthenticationToken;

/**
 * API-Key extractor component. It extracts the format specific API-Key if possible. Can use multiple headers to
 * extract key as needed. Extracted key will end up as {@link NexusApiKeyAuthenticationToken} credential and with this
 * named component' name as token principal. This implies that a realm implementation must exist that handles {@link
 * NexusApiKeyAuthenticationToken}s that has {@link NexusApiKeyAuthenticationToken#getPrincipal()} equal as name of
 * named component implementing this interface.
 */
public interface ApiKeyExtractor
{
  /**
   * Attempts to extract API key as string, whatever part (or parts) of the {@link HttpServletRequest}
   * it needs and returns the extracted key, or returns {@code null}.
   */
  @Nullable
  String extract(HttpServletRequest request);
}
