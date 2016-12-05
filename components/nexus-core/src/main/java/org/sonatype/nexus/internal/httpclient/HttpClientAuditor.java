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
package org.sonatype.nexus.internal.httpclient;

import java.util.Arrays;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.audit.AuditData;
import org.sonatype.nexus.audit.AuditorSupport;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.httpclient.config.AuthenticationConfiguration;
import org.sonatype.nexus.httpclient.config.ConnectionConfiguration;
import org.sonatype.nexus.httpclient.config.HttpClientConfiguration;
import org.sonatype.nexus.httpclient.config.HttpClientConfigurationChangedEvent;
import org.sonatype.nexus.httpclient.config.NtlmAuthenticationConfiguration;
import org.sonatype.nexus.httpclient.config.ProxyConfiguration;
import org.sonatype.nexus.httpclient.config.ProxyServerConfiguration;
import org.sonatype.nexus.httpclient.config.UsernameAuthenticationConfiguration;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

/**
 * HttpClient auditor.
 *
 * @since 3.1
 */
@Named
@Singleton
public class HttpClientAuditor
    extends AuditorSupport
    implements EventAware
{
  public static final String DOMAIN = "httpclient";

  @Subscribe
  @AllowConcurrentEvents
  public void on(final HttpClientConfigurationChangedEvent event) {
    if (isRecording()) {
      HttpClientConfiguration configuration = event.getConfiguration();

      AuditData data = new AuditData();
      data.setDomain(DOMAIN);
      data.setType(CHANGED_TYPE);
      data.setContext(SYSTEM_CONTEXT);

      Map<String, String> attributes = data.getAttributes();

      ConnectionConfiguration connection = configuration.getConnection();
      if (connection != null) {
        attributes.put("connection.timeout", string(connection.getTimeout()));
        attributes.put("connection.maximumRetries", string(connection.getMaximumRetries()));
        attributes.put("connection.userAgentSuffix", string(connection.getUserAgentSuffix()));
        attributes.put("connection.useTrustStore", string(connection.getUseTrustStore()));
      }

      ProxyConfiguration proxy = configuration.getProxy();
      if (proxy != null) {
        proxy(attributes, "proxy.http", proxy.getHttp());
        proxy(attributes, "proxy.https", proxy.getHttps());
        if (proxy.getNonProxyHosts() != null) {
          attributes.put("proxy.nonProxyHosts", string(Arrays.asList(proxy.getNonProxyHosts())));
        }
      }

      record(data);
    }
  }

  private static String key(final String prefix, final String suffix) {
    return prefix + "." + suffix;
  }

  private static void proxy(final Map<String,String> attributes,
                            final String prefix,
                            final ProxyServerConfiguration server)
  {
    if (server == null) {
      return;
    }

    attributes.put(key(prefix, "enabled"), string(server.isEnabled()));
    attributes.put(key(prefix, "host"), server.getHost());
    attributes.put(key(prefix, "port"), string(server.getPort()));

    AuthenticationConfiguration auth = server.getAuthentication();
    if (auth != null) {
      attributes.put(key(prefix, "authentication.type"), auth.getType());
      if (auth instanceof UsernameAuthenticationConfiguration) {
        UsernameAuthenticationConfiguration username = (UsernameAuthenticationConfiguration)auth;
        attributes.put(key(prefix, "authentication.username"), username.getUsername());
        // omit password
      }
      else if (auth instanceof NtlmAuthenticationConfiguration) {
        NtlmAuthenticationConfiguration nrlm = (NtlmAuthenticationConfiguration)auth;
        attributes.put(key(prefix, "authentication.username"), nrlm.getUsername());
        attributes.put(key(prefix, "authentication.host"), nrlm.getHost());
        attributes.put(key(prefix, "authentication.domain"), nrlm.getDomain());
        // omit password
      }
    }
  }
}
