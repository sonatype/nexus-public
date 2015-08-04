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
package org.sonatype.nexus.proxy.repository;

import java.net.URL;
import java.util.Set;

/**
 * Remote proxy settings.
 */
public interface RemoteProxySettings
{

  /**
   * @return HTTP proxy settings. When null, no HTTP proxy should be used
   * @since 2.6
   */
  RemoteHttpProxySettings getHttpProxySettings();

  /**
   * @param settings HTTP proxy settings. When null, will no proxy will be used for HTTP
   * @since 2.6
   */
  void setHttpProxySettings(RemoteHttpProxySettings settings);

  /**
   * @return HTTPS proxy settings. When null, HTTP proxy settings should be used
   * @since 2.6
   */
  RemoteHttpProxySettings getHttpsProxySettings();

  /**
   * @param settings HTTPS proxy settings. When null, will default to HTTP proxy settings
   * @since 2.6
   */
  void setHttpsProxySettings(RemoteHttpProxySettings settings);

  /**
   * @return set of hosts for which proxy should not be used
   */
  Set<String> getNonProxyHosts();

  /**
   * @param nonProxyHosts set of hosts for which proxy should not be used
   */
  void setNonProxyHosts(Set<String> nonProxyHosts);

  /**
   * Determines the {@link RemoteHttpProxySettings} to be used for provided URL.
   *
   * @param url for which the {@link RemoteHttpProxySettings} should be determined
   * @return {@link RemoteHttpProxySettings} to be used or null if http proxy is not configured/enabled
   */
  RemoteHttpProxySettings getRemoteHttpProxySettingsFor(final URL url);

}
