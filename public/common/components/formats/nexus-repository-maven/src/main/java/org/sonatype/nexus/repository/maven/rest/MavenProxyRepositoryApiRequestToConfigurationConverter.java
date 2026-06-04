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
package org.sonatype.nexus.repository.maven.rest;

import java.util.Objects;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.rest.api.ContentDispositionHelper;
import org.sonatype.nexus.repository.rest.api.ProxyRepositoryApiRequestToConfigurationConverter;
import org.sonatype.nexus.repository.routing.RoutingRuleStore;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @since 3.20
 */
@Component
public class MavenProxyRepositoryApiRequestToConfigurationConverter
    extends ProxyRepositoryApiRequestToConfigurationConverter<MavenProxyRepositoryApiRequest>
{
  private static final String MAVEN = "maven";

  private RepositoryManager repositoryManager;

  @Autowired
  public MavenProxyRepositoryApiRequestToConfigurationConverter(final RoutingRuleStore routingRuleStore) {
    super(routingRuleStore);
  }

  @Autowired
  public void setRepositoryManager(final RepositoryManager repositoryManager) {
    this.repositoryManager = repositoryManager;
  }

  @Override
  public Configuration convert(final MavenProxyRepositoryApiRequest request) {
    Configuration configuration = super.convert(request);
    configuration.attributes(MAVEN).set("versionPolicy", request.getMaven().getVersionPolicy());
    configuration.attributes(MAVEN).set("layoutPolicy", request.getMaven().getLayoutPolicy());

    String contentDisposition = ContentDispositionHelper.resolveContentDisposition(
        request.getMaven().getContentDisposition(),
        repositoryManager,
        request.getName(),
        MAVEN);

    configuration.attributes(MAVEN).set("contentDisposition", contentDisposition);

    NestedAttributesMap httpclient = configuration.attributes("httpclient");
    if (Objects.nonNull(httpclient.get("authentication"))) {
      httpclient.child("authentication").set("preemptive", request.getHttpClient().getAuthentication().isPreemptive());
    }
    return configuration;
  }
}
