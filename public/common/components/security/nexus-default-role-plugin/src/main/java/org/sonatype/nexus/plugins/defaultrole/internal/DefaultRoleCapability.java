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
package org.sonatype.nexus.plugins.defaultrole.internal;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import org.sonatype.nexus.common.i18n.I18N;
import org.sonatype.nexus.common.i18n.MessageBundle;
import org.sonatype.nexus.capability.CapabilitySupport;
import org.sonatype.nexus.capability.Condition;
import org.sonatype.nexus.plugins.defaultrole.DefaultRoleRealm;
import org.sonatype.nexus.security.realm.RealmManager;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Capability that allows selecting a role to apply to all authorized users
 *
 * @since 3.22
 */
@Component(DefaultRoleCapabilityDescriptor.TYPE_ID)
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DefaultRoleCapability
    extends CapabilitySupport<DefaultRoleCapabilityConfiguration>
{
  private interface Messages
      extends MessageBundle
  {
    @DefaultMessage("%s")
    String description(String role);
  }

  private static final Messages messages = I18N.create(Messages.class);

  private final RealmManager realmManager;

  private final DefaultRoleRealm defaultRoleRealm;

  @Autowired
  public DefaultRoleCapability(final RealmManager realmManager, final DefaultRoleRealm defaultRoleRealm) {
    this.realmManager = checkNotNull(realmManager);
    this.defaultRoleRealm = checkNotNull(defaultRoleRealm);
  }

  @Override
  protected DefaultRoleCapabilityConfiguration createConfig(final Map<String, String> properties) {
    return new DefaultRoleCapabilityConfiguration(properties);
  }

  @Override
  protected String renderDescription() {
    String role = defaultRoleRealm.getRole();
    return role != null ? messages.description(role) : null;
  }

  @Override
  public Condition activationCondition() {
    return conditions().capabilities().passivateCapabilityDuringUpdate();
  }

  @Override
  protected void onActivate(final DefaultRoleCapabilityConfiguration defaultRoleCapabilityConfiguration) {
    defaultRoleRealm.setRole(defaultRoleCapabilityConfiguration.getRole());

    // install realm if needed
    realmManager.enableRealm(DefaultRoleRealm.NAME);
  }

  @Override
  protected void onPassivate(final DefaultRoleCapabilityConfiguration defaultRoleCapabilityConfiguration) {
    if (isShuttingDown()) {
      log.info("Skipping DefaultRole realm disable during shutdown");
    }
    else {
      disableDefaultRoleRealm();
      defaultRoleRealm.setRole(null);
    }
  }

  private void disableDefaultRoleRealm() {
    try {
      log.info("Attempting to disable DefaultRole realm");
      realmManager.disableRealm(DefaultRoleRealm.NAME);
    }
    catch (Exception e) {
      log.warn("Failed to disable DefaultRole realm", e);
    }
  }

  private boolean isShuttingDown() {
    return Thread.currentThread().getName().contains("FelixStartLevel");
  }
}
