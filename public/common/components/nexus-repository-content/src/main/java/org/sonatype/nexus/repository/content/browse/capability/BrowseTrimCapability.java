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
package org.sonatype.nexus.repository.content.browse.capability;

import java.util.Map;

import org.sonatype.nexus.capability.CapabilitySupport;
import org.sonatype.nexus.capability.Condition;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static java.util.Objects.requireNonNull;

@Component(BrowseTrimCapabilityDescriptor.TYPE_ID)
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class BrowseTrimCapability
    extends CapabilitySupport<BrowseTrimCapabilityConfiguration>
{
  private final BrowseTrimService browseTrimService;

  @Autowired
  public BrowseTrimCapability(final BrowseTrimService browseTrimService) {
    this.browseTrimService = requireNonNull(browseTrimService);
  }

  @Override
  protected BrowseTrimCapabilityConfiguration createConfig(final Map<String, String> properties) {
    return new BrowseTrimCapabilityConfiguration(properties);
  }

  @Override
  protected void onActivate(final BrowseTrimCapabilityConfiguration config) throws Exception {
    browseTrimService.setPostgresqlTrimEnabled(config.isPostgresqlTrimEnabled());
    browseTrimService.setBatchTrimEnabled(config.isBatchTrimEnabled());
  }

  @Override
  protected void onPassivate(final BrowseTrimCapabilityConfiguration config) throws Exception {
    browseTrimService.setPostgresqlTrimEnabled(false);
    browseTrimService.setBatchTrimEnabled(false);
  }

  @Override
  protected void onRemove(final BrowseTrimCapabilityConfiguration config) throws Exception {
    browseTrimService.setPostgresqlTrimEnabled(false);
    browseTrimService.setBatchTrimEnabled(false);
  }

  @Override
  public Condition activationCondition() {
    return conditions().capabilities().passivateCapabilityDuringUpdate();
  }
}
