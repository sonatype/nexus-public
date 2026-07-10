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
package org.sonatype.nexus.self.hosted.internal.capability.proxy;

import java.util.Map;

import javax.annotation.Nullable;

import org.sonatype.nexus.capability.CapabilitySupport;
import org.sonatype.nexus.capability.CapabilityType;
import org.sonatype.nexus.capability.Condition;
import org.sonatype.nexus.common.i18n.I18N;
import org.sonatype.nexus.common.i18n.MessageBundle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.capability.CapabilityType.capabilityType;

/**
 * Capability to control forwarded header processing at runtime.
 *
 * <p>
 * This capability allows enabling/disabling the processing of forwarded headers
 * (X-Forwarded-For, X-Forwarded-Proto, etc.) without restarting the server.
 * </p>
 */
@Component(ForwardedRequestCapability.TYPE_ID)
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ForwardedRequestCapability
    extends CapabilitySupport<ForwardedRequestCapabilityConfiguration>
{
  public static final String TYPE_ID = "http.forwarded";

  public static final CapabilityType TYPE = capabilityType(TYPE_ID);

  interface Messages
      extends MessageBundle
  {
    @DefaultMessage("HTTP Forwarded Headers")
    String name();

    @DefaultMessage("HTTP")
    String category();

    @DefaultMessage("Disabled")
    String disabled();

    @DefaultMessage("Enabled")
    String enabled();
  }

  static final Messages messages = I18N.create(Messages.class);

  private final ForwardedRequestCustomizerManager manager;

  @Autowired
  public ForwardedRequestCapability(final ForwardedRequestCustomizerManager manager) {
    this.manager = checkNotNull(manager);
  }

  @Override
  protected ForwardedRequestCapabilityConfiguration createConfig(final Map<String, String> properties) {
    return new ForwardedRequestCapabilityConfiguration(properties);
  }

  @Override
  @Nullable
  protected String renderDescription() {
    if (context().isActive()) {
      return messages.enabled();
    }
    return messages.disabled();
  }

  @Override
  public Condition activationCondition() {
    return conditions().capabilities().passivateCapabilityDuringUpdate();
  }

  @Override
  protected void onActivate(final ForwardedRequestCapabilityConfiguration config) {
    manager.setEnabled(config.isEnabled());
  }

  @Override
  protected void onPassivate(final ForwardedRequestCapabilityConfiguration config) {
    manager.setEnabled(false);
  }

  @Override
  protected void onUpdate(final ForwardedRequestCapabilityConfiguration config) {
    manager.setEnabled(config.isEnabled());
  }
}
