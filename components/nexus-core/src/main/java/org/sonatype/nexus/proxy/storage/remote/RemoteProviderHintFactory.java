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
package org.sonatype.nexus.proxy.storage.remote;

/**
 * Component that drives the remote storage transport provider selection by telling the "hint" (former Plexus role
 * hint), of the RRS component to be used. It allows multiple way of configuration, either by setting the default
 * provider and even forceful overriding the hint (if configuration would say otherwise). This component is meant to
 * help smooth transition from Apache HttpClient3x RRS (for long time the one and only RRS implementation) to Ning's
 * AsyncHttpClient implementation.
 */
public interface RemoteProviderHintFactory
{
  /**
   * Returns the default provider role hint for provided remote URL.
   *
   * @return The default provider role hint as a string.
   */
  String getDefaultRoleHint(final String remoteUrl)
      throws IllegalArgumentException;

  /**
   * Returns the provider role hint to be used, based on passed in remote URL and hint.
   *
   * @return The provider role hint to be used, based on passed in remote URL and hint. If forceful override is in
   *         effect, it will return the forced, otherwise the passed in one (if it is valid, non-null, etc).
   */
  String getRoleHint(final String remoteUrl, final String hint)
      throws IllegalArgumentException;

  /**
   * Returns the default HTTP provider role hint.
   *
   * @return The default HTTP provider role hint as a string.
   */
  String getDefaultHttpRoleHint();

  /**
   * Returns the HTTP provider role hint to be used, based on passed in hint.
   *
   * @return The HTTP provider role hint to be used, based on passed in hint. If forceful override is in effect, it
   *         will return the forced, otherwise the passed in one (if it is valid, non-null, etc).
   */
  String getHttpRoleHint(final String hint);
}
