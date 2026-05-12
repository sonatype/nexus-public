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
package org.sonatype.nexus.repository.raw.rest;

import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.rest.api.ProxyRepositoryApiRequestToConfigurationConverter;
import org.sonatype.nexus.repository.rest.api.ContentDispositionHelper;
import org.sonatype.nexus.repository.routing.RoutingRuleStore;

import static org.sonatype.nexus.repository.raw.rest.RawAttributes.CONTENT_DISPOSITION;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @since 3.25
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class RawProxyRepositoryApiRequestToConfigurationConverter
    extends ProxyRepositoryApiRequestToConfigurationConverter<RawProxyRepositoryApiRequest>
{
  private RepositoryManager repositoryManager;

  @Autowired
  public RawProxyRepositoryApiRequestToConfigurationConverter(final RoutingRuleStore routingRuleStore) {
    super(routingRuleStore);
  }

  @Autowired
  public void setRepositoryManager(final RepositoryManager repositoryManager) {
    this.repositoryManager = repositoryManager;
  }

  @Override
  public Configuration convert(final RawProxyRepositoryApiRequest request) {
    Configuration configuration = super.convert(request);

    String requestedDisposition = null;
    if (request.getRaw() != null && request.getRaw().getContentDisposition() != null) {
      requestedDisposition = request.getRaw().getContentDisposition().name();
    }

    String contentDisposition = ContentDispositionHelper.resolveContentDisposition(
        requestedDisposition,
        repositoryManager,
        request.getName(),
        "raw");

    configuration.attributes("raw").set(CONTENT_DISPOSITION, contentDisposition);
    return configuration;
  }
}
