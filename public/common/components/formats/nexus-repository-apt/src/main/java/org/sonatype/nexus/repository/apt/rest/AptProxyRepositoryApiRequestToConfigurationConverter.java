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
package org.sonatype.nexus.repository.apt.rest;

import jakarta.inject.Inject;

import org.sonatype.nexus.repository.apt.internal.gpg.AptSigningFacet;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.rest.api.ProxyRepositoryApiRequestToConfigurationConverter;
import org.sonatype.nexus.repository.routing.RoutingRuleStore;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @since 3.20
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class AptProxyRepositoryApiRequestToConfigurationConverter
    extends ProxyRepositoryApiRequestToConfigurationConverter<AptProxyRepositoryApiRequest>
{
  @Inject
  public AptProxyRepositoryApiRequestToConfigurationConverter(final RoutingRuleStore routingRuleStore) {
    super(routingRuleStore);
  }

  @Override
  public Configuration convert(final AptProxyRepositoryApiRequest request) {
    Configuration configuration = super.convert(request);
    configuration.attributes("apt").set("distribution", request.getApt().getDistribution());
    configuration.attributes("apt").set("flat", request.getApt().getFlat());
    configuration.attributes("apt")
        .set("enforceDistribution",
            Boolean.TRUE.equals(request.getApt().getEnforceDistribution()));

    // Add signing configuration if provided (optional for proxy repos)
    if (request.getAptSigning() != null) {
      configuration.attributes(AptSigningFacet.CONFIG_KEY).set("keypair", request.getAptSigning().getKeypair());
      configuration.attributes(AptSigningFacet.CONFIG_KEY).set("passphrase", request.getAptSigning().getPassphrase());
    }

    return configuration;
  }
}
