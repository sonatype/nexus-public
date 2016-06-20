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
package org.sonatype.nexus.coreui

import javax.validation.constraints.Max
import javax.validation.constraints.Min

import org.sonatype.nexus.httpclient.config.NonProxyHosts
import org.sonatype.nexus.validation.constraint.PortNumber

import groovy.transform.ToString

/**
 * HTTP System Settings exchange object.
 *
 * @since 3.0
 */
@ToString(includePackage = false, includeNames = true)
class HttpSettingsXO
{
  String userAgentSuffix

  /**
   * Timeout seconds.
   */
  @Min(1L)
  @Max(3600L) // 1 hour
  Integer timeout

  @Min(0L)
  @Max(10L)
  Integer retries

  // HTTP proxy

  Boolean httpEnabled

  String httpHost

  @PortNumber
  Integer httpPort

  // HTTP proxy-authentication

  Boolean httpAuthEnabled

  String httpAuthUsername

  String httpAuthPassword

  String httpAuthNtlmHost

  String httpAuthNtlmDomain

  // HTTPS proxy

  Boolean httpsEnabled

  String httpsHost

  @PortNumber
  Integer httpsPort

  // HTTPS proxy-authentication

  Boolean httpsAuthEnabled

  String httpsAuthUsername

  String httpsAuthPassword

  String httpsAuthNtlmHost

  String httpsAuthNtlmDomain

  // HTTP[S] non-proxy hosts

  @NonProxyHosts
  Set<String> nonProxyHosts
}
