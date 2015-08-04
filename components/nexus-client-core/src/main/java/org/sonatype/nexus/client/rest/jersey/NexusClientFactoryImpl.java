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
package org.sonatype.nexus.client.rest.jersey;

import java.net.URI;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.sonatype.nexus.client.core.Condition;
import org.sonatype.nexus.client.core.NexusClient;
import org.sonatype.nexus.client.core.condition.NexusStatusConditions;
import org.sonatype.nexus.client.core.spi.SubsystemProvider;
import org.sonatype.nexus.client.internal.rest.NexusXStreamFactory;
import org.sonatype.nexus.client.internal.rest.XStreamXmlProvider;
import org.sonatype.nexus.client.internal.util.Version;
import org.sonatype.nexus.client.rest.AuthenticationInfo;
import org.sonatype.nexus.client.rest.BaseUrl;
import org.sonatype.nexus.client.rest.ConnectionInfo;
import org.sonatype.nexus.client.rest.NexusClientFactory;
import org.sonatype.nexus.client.rest.ProxyInfo;
import org.sonatype.nexus.client.rest.UsernamePasswordAuthenticationInfo;
import org.sonatype.sisu.siesta.client.filters.RequestFilters;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.filter.LoggingFilter;
import com.sun.jersey.client.apache4.ApacheHttpClient4;
import com.sun.jersey.client.apache4.config.ApacheHttpClient4Config;
import com.sun.jersey.client.apache4.config.DefaultApacheHttpClient4Config;
import com.thoughtworks.xstream.XStream;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.BrowserCompatHostnameVerifier;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.StrictHostnameVerifier;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.impl.conn.SchemeRegistryFactory;
import org.apache.http.params.CoreProtocolPNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sun.jersey.api.client.config.ClientConfig.PROPERTY_FOLLOW_REDIRECTS;
import static com.sun.jersey.client.apache4.config.ApacheHttpClient4Config.PROPERTY_CREDENTIALS_PROVIDER;
import static com.sun.jersey.client.apache4.config.ApacheHttpClient4Config.PROPERTY_PREEMPTIVE_BASIC_AUTHENTICATION;
import static com.sun.jersey.client.apache4.config.ApacheHttpClient4Config.PROPERTY_PROXY_PASSWORD;
import static com.sun.jersey.client.apache4.config.ApacheHttpClient4Config.PROPERTY_PROXY_URI;
import static com.sun.jersey.client.apache4.config.ApacheHttpClient4Config.PROPERTY_PROXY_USERNAME;
import static org.apache.http.conn.params.ConnRoutePNames.DEFAULT_PROXY;

/**
 * Jersey based {@link NexusClientFactory}.
 *
 * @since 2.7
 */
public class NexusClientFactoryImpl
    implements NexusClientFactory
{

  private static final Logger LOG = LoggerFactory.getLogger(NexusClientFactoryImpl.class);

  /**
   * Modified "content-type" used by Nexus Client: it enforces body encoding too for UTF8.
   */
  private static final MediaType APPLICATION_XML_UTF8_TYPE = MediaType.valueOf("application/xml; charset=UTF-8");

  private final Condition connectionCondition;

  private final List<SubsystemProvider> subsystemProviders;

  public NexusClientFactoryImpl(final List<SubsystemProvider> subsystemProviders) {
    this(NexusStatusConditions.anyModern(), subsystemProviders);
  }

  public NexusClientFactoryImpl(final Condition connectionCondition,
                                final List<SubsystemProvider> subsystemProviders)
  {
    this.connectionCondition = Preconditions.checkNotNull(connectionCondition, "connectionCondition");
    this.subsystemProviders = Preconditions.checkNotNull(subsystemProviders, "subsystemProviders");
  }

  @Override
  public final NexusClient createFor(final BaseUrl baseUrl) {
    return createFor(baseUrl, null);
  }

  @Override
  public final NexusClient createFor(final BaseUrl baseUrl, final AuthenticationInfo authenticationInfo) {
    return createFor(new ConnectionInfo(baseUrl, authenticationInfo, null));
  }

  @Override
  public final NexusClient createFor(final ConnectionInfo connectionInfo) {
    // we are java2java client, so we use XML instead of JSON, as
    // some current Nexus are one way only! So, we fix for XML
    final XStream xstream = new NexusXStreamFactory().createAndConfigureForXml();

    // we use XML for communication (unlike web browsers do, for which JSON makes more sense)
    return new JerseyNexusClient(
        connectionCondition,
        subsystemProviders,
        connectionInfo,
        xstream,
        doCreateHttpClientFor(connectionInfo, xstream),
        APPLICATION_XML_UTF8_TYPE
    );
  }

  protected ApacheHttpClient4 doCreateHttpClientFor(final ConnectionInfo connectionInfo, final XStream xstream) {
    final ApacheHttpClient4Config config = new DefaultApacheHttpClient4Config();
    config.getSingletons().add(new XStreamXmlProvider(xstream, APPLICATION_XML_UTF8_TYPE));
    // set _real_ URL for baseUrl, and not a redirection (typically http instead of https)
    config.getProperties().put(PROPERTY_FOLLOW_REDIRECTS, Boolean.FALSE);

    applyAuthenticationIfAny(connectionInfo, config);
    applyProxyIfAny(connectionInfo, config);

    // obey JSSE defined system properties
    config.getProperties().put(ApacheHttpClient4Config.PROPERTY_CONNECTION_MANAGER,
        new PoolingClientConnectionManager(SchemeRegistryFactory.createSystemDefault()));

    final ApacheHttpClient4 client = ApacheHttpClient4.create(config);

    // set UA
    client.getClientHandler().getHttpClient().getParams().setParameter(
        CoreProtocolPNames.USER_AGENT, "Nexus-Client/" + discoverClientVersion()
    );

    // "tweak" HTTPS scheme as requested
    final TrustStrategy trustStrategy;
    switch (connectionInfo.getSslCertificateValidation()) {
      case NONE:
        trustStrategy = new TrustStrategy()
        {
          @Override
          public boolean isTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
            return true;
          }
        };
        break;
      case LAX:
        trustStrategy = new TrustSelfSignedStrategy();
        break;
      default:
        trustStrategy = null;
    }

    final X509HostnameVerifier hostnameVerifier;
    switch (connectionInfo.getSslCertificateHostnameValidation()) {
      case NONE:
        hostnameVerifier = new AllowAllHostnameVerifier();
        break;
      case STRICT:
        hostnameVerifier = new StrictHostnameVerifier();
        break;
      default:
        hostnameVerifier = new BrowserCompatHostnameVerifier();
    }

    try {
      final SSLSocketFactory ssf = new SSLSocketFactory(trustStrategy, hostnameVerifier);
      final Scheme tweakedHttpsScheme = new Scheme("https", 443, ssf);
      client.getClientHandler().getHttpClient().getConnectionManager().getSchemeRegistry().register(tweakedHttpsScheme);
    }
    catch (Exception e) {
      Throwables.propagate(e);
    }

    // NXCM-4547 JERSEY-1293 Enforce proxy setting on httpclient
    enforceProxyUri(config, client);

    if (LOG.isDebugEnabled()) {
      client.addFilter(new LoggingFilter());
    }

    client.addFilter(new RequestFilters());

    return client;
  }

  protected String discoverClientVersion() {
    return Version.readVersion("META-INF/maven/org.sonatype.nexus/nexus-client-core/pom.properties", "unknown");
  }

  // ==

  /**
   * NXCM-4547 JERSEY-1293 Enforce proxy setting on httpclient
   * <p/>
   * Revisit for jersey 1.13.
   */
  private void enforceProxyUri(final ApacheHttpClient4Config config, final ApacheHttpClient4 client) {
    final Object proxyProperty = config.getProperties().get(PROPERTY_PROXY_URI);
    if (proxyProperty != null) {
      final URI uri = getProxyUri(proxyProperty);
      final HttpHost proxy = new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme());
      client.getClientHandler().getHttpClient().getParams().setParameter(DEFAULT_PROXY, proxy);
    }
  }

  private static URI getProxyUri(final Object proxy) {
    if (proxy instanceof URI) {
      return (URI) proxy;
    }
    else if (proxy instanceof String) {
      return URI.create((String) proxy);
    }
    else {
      throw new ClientHandlerException("The proxy URI (" + PROPERTY_PROXY_URI +
          ") property MUST be an instance of String or URI");
    }
  }

  // ==

  protected void applyAuthenticationIfAny(final ConnectionInfo connectionInfo, ApacheHttpClient4Config config) {
    if (connectionInfo.getAuthenticationInfo() != null) {
      if (connectionInfo.getAuthenticationInfo() instanceof UsernamePasswordAuthenticationInfo) {
        final UsernamePasswordAuthenticationInfo upinfo =
            (UsernamePasswordAuthenticationInfo) connectionInfo.getAuthenticationInfo();
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
            AuthScope.ANY,
            new UsernamePasswordCredentials(upinfo.getUsername(), upinfo.getPassword())
        );
        config.getProperties().put(PROPERTY_CREDENTIALS_PROVIDER, credentialsProvider);
        config.getProperties().put(PROPERTY_PREEMPTIVE_BASIC_AUTHENTICATION, true);
      }
      else {
        throw new IllegalArgumentException(String.format("AuthenticationInfo of type %s is not supported!",
            connectionInfo.getAuthenticationInfo().getClass().getName()));
      }
    }
  }

  protected void applyProxyIfAny(final ConnectionInfo connectionInfo, ApacheHttpClient4Config config) {
    if (connectionInfo.getProxyInfos().size() > 0) {
      final ProxyInfo proxyInfo = connectionInfo.getProxyInfos().get(connectionInfo.getBaseUrl().getProtocol());
      if (proxyInfo != null) {
        config.getProperties().put(
            PROPERTY_PROXY_URI, "http://" + proxyInfo.getProxyHost() + ":" + proxyInfo.getProxyPort()
        );

        if (proxyInfo.getProxyAuthentication() != null) {
          if (proxyInfo.getProxyAuthentication() instanceof UsernamePasswordAuthenticationInfo) {
            final UsernamePasswordAuthenticationInfo upinfo =
                (UsernamePasswordAuthenticationInfo) connectionInfo.getAuthenticationInfo();
            config.getProperties().put(PROPERTY_PROXY_USERNAME, upinfo.getUsername());
            config.getProperties().put(PROPERTY_PROXY_PASSWORD, upinfo.getPassword());
          }
          else {
            throw new IllegalArgumentException(String.format(
                "AuthenticationInfo of type %s is not supported!",
                connectionInfo.getAuthenticationInfo().getClass().getName()));
          }
        }
      }
      else {
        throw new IllegalArgumentException("ProxyInfo and BaseUrl protocols does not align!");
      }
    }
  }
}
