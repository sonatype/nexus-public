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
package org.sonatype.nexus.internal.webhooks;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintValidatorContext;

import org.sonatype.nexus.validation.ConstraintValidatorSupport;
import org.sonatype.nexus.webhooks.GlobalWebhook;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

/**
 * Validates that every entry in the configured list of Global Webhook event names corresponds to a
 * discovered {@link GlobalWebhook} bean.
 * <p>
 * Historically this validator also gated the {@code firewall_quarantine} event on the presence of
 * an enabled {@code clm} capability and an enabled {@code firewall.audit} capability with
 * {@code quarantine=true}. Those capability-based prerequisites were removed as part of NEXUS-53667:
 * firewall configuration is now stored on each repository (see
 * {@code FirewallCapabilityToRepositoryConfigMigrationStep_2_143}) and the {@code firewall.audit}
 * capability rows no longer exist post-migration, so that check always failed. The event pipeline
 * (see {@code FirewallContributedHandler}) already gates on
 * {@code FirewallConfigurationHelper.isFirewallEnabled(repository)} and
 * {@code clmConnector.isEnabled()} at fire time, so a webhook subscribed to a
 * {@code firewall_quarantine} event that no repository will ever raise is harmless.
 */
@Component
@Scope(SCOPE_PROTOTYPE)
public class GlobalWebhookTypeValidator
    extends ConstraintValidatorSupport<GlobalWebhookType, List<String>>
{
  private final List<GlobalWebhook> globalWebhooks;

  @Autowired
  public GlobalWebhookTypeValidator(final List<GlobalWebhook> globalWebhooks) {
    this.globalWebhooks = checkNotNull(globalWebhooks);
  }

  @Override
  public boolean isValid(final List<String> names, final ConstraintValidatorContext constraintValidatorContext) {
    if (names == null || names.isEmpty()) {
      return true; // empty list is valid
    }

    Set<String> webhookNames =
        globalWebhooks.stream().map(GlobalWebhook::getName).collect(Collectors.toUnmodifiableSet());

    return webhookNames.containsAll(names);
  }
}
