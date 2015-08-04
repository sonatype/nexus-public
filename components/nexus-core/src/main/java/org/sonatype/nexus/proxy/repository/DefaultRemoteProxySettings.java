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

import com.google.common.collect.Sets;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default {@link RemoteProxySettings} implementation.
 */
public class DefaultRemoteProxySettings
    implements RemoteProxySettings
{

  private RemoteHttpProxySettings httpProxySettings;

  private RemoteHttpProxySettings httpsProxySettings;

  private Set<String> nonProxyHosts = Sets.newHashSet();

  public RemoteHttpProxySettings getHttpProxySettings() {
    return httpProxySettings;
  }

  public void setHttpProxySettings(final RemoteHttpProxySettings httpProxySettings) {
    this.httpProxySettings = httpProxySettings;
  }

  public RemoteHttpProxySettings getHttpsProxySettings() {
    return httpsProxySettings;
  }

  public void setHttpsProxySettings(final RemoteHttpProxySettings httpsProxySettings) {
    this.httpsProxySettings = httpsProxySettings;
  }

  public Set<String> getNonProxyHosts() {
    return nonProxyHosts;
  }

  public void setNonProxyHosts(final Set<String> nonProxyHosts) {
    this.nonProxyHosts.clear();
    if (nonProxyHosts != null && !nonProxyHosts.isEmpty()) {
      this.nonProxyHosts.addAll(nonProxyHosts);
    }
  }

  @Override
  public RemoteHttpProxySettings getRemoteHttpProxySettingsFor(final URL url) {
    return getRemoteHttpProxySettingsFor(url, this);
  }

  public static RemoteHttpProxySettings getRemoteHttpProxySettingsFor(final URL url,
                                                                      final RemoteProxySettings settings)
  {
    checkNotNull(url);
    checkNotNull(settings);

    // if we have a secure url, try to use a secure proxy
    if ("https".equals(url.getProtocol())) {
      RemoteHttpProxySettings httpsProxySettings = settings.getHttpsProxySettings();
      if (httpsProxySettings != null && httpsProxySettings.isEnabled()) {
        return httpsProxySettings;
      }
    }

    if ("http".equals(url.getProtocol()) || "https".equals(url.getProtocol())) {
      // if no secure proxy is available, or the url is not secure, use normal proxy
      RemoteHttpProxySettings httpProxySettings = settings.getHttpProxySettings();
      if (httpProxySettings != null && httpProxySettings.isEnabled()) {
        return httpProxySettings;
      }
    }

    return null; // no proxies are enabled
  }

}
