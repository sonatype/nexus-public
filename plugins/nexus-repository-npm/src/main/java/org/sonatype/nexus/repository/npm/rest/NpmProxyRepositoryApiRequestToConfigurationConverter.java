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
package org.sonatype.nexus.repository.npm.rest;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.rest.api.ProxyRepositoryApiRequestToConfigurationConverter;
import org.sonatype.nexus.repository.routing.RoutingRuleStore;

import static org.sonatype.nexus.repository.npm.rest.NpmAttributes.REMOVE_NON_CATALOGED;

/**
 * @since 3.next
 */
@Named
public class NpmProxyRepositoryApiRequestToConfigurationConverter
    extends ProxyRepositoryApiRequestToConfigurationConverter<NpmProxyRepositoryApiRequest>
{
  @Inject
  public NpmProxyRepositoryApiRequestToConfigurationConverter(final RoutingRuleStore routingRuleStore) {
    super(routingRuleStore);
  }

  @Override
  public Configuration convert(final NpmProxyRepositoryApiRequest request) {
    Configuration convert = super.convert(request);
    NpmAttributes npm = request.getNpm();
    if (npm != null) {
      convert.attributes("npm").set(REMOVE_NON_CATALOGED, npm.getRemoveNonCataloged());
    }
    return convert;
  }
}
