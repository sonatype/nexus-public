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
package org.sonatype.nexus.repository.maven.api;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.repository.maven.rest.HttpClientAttributesWithPreemptiveAuth;
import org.sonatype.nexus.repository.maven.rest.HttpClientConnectionAuthenticationAttributesWithPreemptive;
import org.sonatype.nexus.repository.rest.api.SimpleApiRepositoryAdapter;
import org.sonatype.nexus.repository.rest.api.model.AbstractApiRepository;
import org.sonatype.nexus.repository.rest.api.model.HttpClientAttributes;
import org.sonatype.nexus.repository.routing.RoutingRuleStore;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;

/**
 * Adapter to expose maven specific repository configuration for the repositories REST API.
 *
 * @since 3.20
 */
@Named(Maven2Format.NAME)
public class MavenApiRepositoryAdapter
    extends SimpleApiRepositoryAdapter
{
  private static final String MAVEN = "maven";

  @Inject
  public MavenApiRepositoryAdapter(final RoutingRuleStore routingRuleStore) {
    super(routingRuleStore);
  }

  @Override
  public AbstractApiRepository adapt(final Repository repository) {
    boolean online = repository.getConfiguration().isOnline();
    String name = repository.getName();
    String url = repository.getUrl();

    switch (repository.getType().toString()) {
      case HostedType.NAME:
        return new MavenHostedApiRepository(
            name,
            url,
            online,
            getHostedStorageAttributes(repository),
            getCleanupPolicyAttributes(repository),
            createMavenAttributes(repository),
            getComponentAttributes(repository));
      case ProxyType.NAME:
        return new MavenProxyApiRepository(name, url, online,
            getHostedStorageAttributes(repository),
            getCleanupPolicyAttributes(repository),
            getProxyAttributes(repository),
            getNegativeCacheAttributes(repository),
            getHttpClientAttributes(repository),
            getRoutingRuleName(repository),
            createMavenAttributes(repository),
            getReplicationAttributes(repository));
      default:
        return super.adapt(repository);
    }
  }

  private MavenAttributes createMavenAttributes(final Repository repository) {
    String versionPolicy = repository.getConfiguration().attributes(MAVEN).get("versionPolicy", String.class);
    String layoutPolicy = repository.getConfiguration().attributes(MAVEN).get("layoutPolicy", String.class);
    String contentDisposition = repository.getConfiguration().attributes(MAVEN).get("contentDisposition", String.class);
    return new MavenAttributes(versionPolicy, layoutPolicy, contentDisposition);
  }

  @Override
  protected HttpClientAttributesWithPreemptiveAuth getHttpClientAttributes(final Repository repository) {
    HttpClientAttributes httpClientAttributes = super.getHttpClientAttributes(repository);
    HttpClientConnectionAuthenticationAttributesWithPreemptive authentication = null;

    Configuration configuration = repository.getConfiguration();
    NestedAttributesMap httpclient = configuration.attributes("httpclient");
    if (httpclient.contains("authentication")) {
      NestedAttributesMap authenticationMap = httpclient.child("authentication");
      Boolean preemptive = authenticationMap.get("preemptive", Boolean.class);

      authentication = new HttpClientConnectionAuthenticationAttributesWithPreemptive(httpClientAttributes.getAuthentication(),
          preemptive);
    }

    return new HttpClientAttributesWithPreemptiveAuth(httpClientAttributes, authentication);
  }
}
