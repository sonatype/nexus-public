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
package org.sonatype.nexus.coreui.internal.branding;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import org.sonatype.nexus.capability.CapabilitySupport;
import org.sonatype.nexus.capability.Condition;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Branding capability.
 *
 * @since 3.0
 */
@Component(BrandingCapabilityDescriptor.TYPE_ID)
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class BrandingCapability
    extends CapabilitySupport<BrandingCapabilityConfiguration>
{

  private final Branding branding;

  @Autowired
  public BrandingCapability(final Branding branding) {
    this.branding = checkNotNull(branding);
  }

  @Override
  protected BrandingCapabilityConfiguration createConfig(final Map<String, String> properties) {
    return new BrandingCapabilityConfiguration(properties);
  }

  @Override
  protected void onActivate(final BrandingCapabilityConfiguration config) throws Exception {
    branding.set(config);
  }

  @Override
  protected void onPassivate(final BrandingCapabilityConfiguration config) throws Exception {
    branding.reset();
  }

  @Override
  protected void onRemove(final BrandingCapabilityConfiguration config) throws Exception {
    branding.reset();
  }

  @Override
  public Condition activationCondition() {
    return conditions().capabilities().passivateCapabilityDuringUpdate();
  }

}
