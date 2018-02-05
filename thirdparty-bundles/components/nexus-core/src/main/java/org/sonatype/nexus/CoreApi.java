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
package org.sonatype.nexus;

import org.sonatype.nexus.common.script.ScriptApi;

/**
 * Core provisioning capabilities of the repository manager.
 *
 * @since 3.0
 */
public interface CoreApi
    extends ScriptApi
{
  default String getName() {
    return "core";
  }

  /**
   * Set the base url of the repository manager.
   */
  void baseUrl(String url);

  /**
   * Remove any existing base url capability.
   */
  void removeBaseUrl();

  /**
   * Customize the User-Agent header in outgoing HTTP requests by appending this value.
   * Can be removed by calling with an empty string.
   */
  void userAgentCustomization(String userAgentSuffix);

  /**
   * Set the connection timeout between 1 and 3600 seconds.
   */
  void connectionTimeout(int timeout);

  /**
   * Set the number of connection retries between 1 and 10.
   */
  void connectionRetryAttempts(int retries);

  /**
   * Create an unauthenticated http proxy.
   */
  void httpProxy(String host, int port);

  /**
   * Create an http proxy using username/password authentication.
   */
  void httpProxyWithBasicAuth(String host, int port, String username, String password);

  /**
   * Create an http proxy using Windows NTLM authentication.
   */
  void httpProxyWithNTLMAuth(String host, int port, String username, String password, String ntlmHost,
                             String domain);

  /**
   * Remove any existing http proxy configuration.
   */
  void removeHTTPProxy();

  /**
   * Create an unauthenticated https proxy.
   */
  void httpsProxy(String host, int port);

  /**
   * Create an https proxy using username/password authentication.
   */
  void httpsProxyWithBasicAuth(String host, int port, String username, String password);

  /**
   * Create an https proxy using Windows NTLM authentication.
   */
  void httpsProxyWithNTLMAuth(String host, int port, String username, String password, String ntlmHost,
                              String domain);

  /**
   * Remove any existing https proxy configuration.
   */
  void removeHTTPSProxy();
  
  /**
   * Set hosts that should not be proxied. Accepts Java "http.nonProxyHosts" wildcard patterns (one per line, no '|'
   * hostname delimiters).
   * Previously configured values can be removed by calling this with no parameters.
   */
  void nonProxyHosts(String... nonProxyHosts);
}
