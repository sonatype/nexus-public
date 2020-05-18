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
package org.sonatype.nexus.repository.rest.internal.resources;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Path;

import org.sonatype.nexus.repository.routing.RoutingRuleHelper;
import org.sonatype.nexus.repository.routing.RoutingRuleStore;

import io.swagger.annotations.Api;

import static org.sonatype.nexus.repository.rest.internal.resources.RoutingRulesApiResourceBeta.RESOURCE_URI;
import static org.sonatype.nexus.rest.APIConstants.BETA_API_PREFIX;

@Api(hidden = true)
@Named
@Singleton
@Path(RESOURCE_URI)
@Deprecated
public class RoutingRulesApiResourceBeta
  extends RoutingRulesApiResource
{
  static final String RESOURCE_URI = BETA_API_PREFIX + "/routing-rules";

  @Inject
  public RoutingRulesApiResourceBeta(final RoutingRuleStore routingRuleStore,
                                     final RoutingRuleHelper routingRuleHelper)
  {
    super(routingRuleStore, routingRuleHelper);
  }
}
