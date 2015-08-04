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
package org.sonatype.nexus.rutauth.internal.capability;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.capability.support.CapabilitySupport;
import org.sonatype.nexus.plugins.capabilities.Condition;
import org.sonatype.nexus.rutauth.internal.RutAuthAuthenticationTokenFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Rut Auth capability.
 *
 * @since 2.7
 */
@Named(RutAuthCapabilityDescriptor.TYPE_ID)
public class RutAuthCapability
    extends CapabilitySupport<RutAuthCapabilityConfiguration>
{
  private final RutAuthAuthenticationTokenFactory authenticationTokenFactory;

  @Inject
  public RutAuthCapability(final RutAuthAuthenticationTokenFactory authenticationTokenFactory) {
    this.authenticationTokenFactory = checkNotNull(authenticationTokenFactory, "authenticationTokenFactory");
  }

  @Override
  protected RutAuthCapabilityConfiguration createConfig(final Map<String, String> properties) {
    return new RutAuthCapabilityConfiguration(properties);
  }

  @Override
  protected void onActivate(final RutAuthCapabilityConfiguration config) throws Exception {
    authenticationTokenFactory.setHeaderName(config.getHttpHeader());
  }

  @Override
  protected void onPassivate(final RutAuthCapabilityConfiguration config) throws Exception {
    authenticationTokenFactory.setHeaderName(null);
  }

  @Override
  public Condition activationCondition() {
    return conditions().capabilities().passivateCapabilityDuringUpdate();
  }

}
