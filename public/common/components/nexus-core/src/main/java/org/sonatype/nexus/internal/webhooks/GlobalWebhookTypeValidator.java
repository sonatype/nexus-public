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

import javax.validation.ConstraintValidatorContext;

import org.sonatype.nexus.capability.CapabilityRegistry;
import org.sonatype.nexus.validation.ConstraintValidatorSupport;
import org.sonatype.nexus.webhooks.GlobalWebhook;

import jakarta.inject.Inject;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

@Component
@Scope(SCOPE_PROTOTYPE)
public class GlobalWebhookTypeValidator
    extends ConstraintValidatorSupport<GlobalWebhookType, List<String>>
{
  private static final String FIREWALL_QUARANTINE_WEBHOOK = "firewall_quarantine";

  private static final String CLM_CAPABILITY_TYPE = "clm";

  private static final String FIREWALL_AUDIT_CAPABILITY_TYPE = "firewall.audit";

  private final List<GlobalWebhook> globalWebhooks;

  private final CapabilityRegistry capabilityRegistry;

  @Inject
  public GlobalWebhookTypeValidator(
      final List<GlobalWebhook> globalWebhooks,
      final CapabilityRegistry capabilityRegistry)
  {
    this.globalWebhooks = checkNotNull(globalWebhooks);
    this.capabilityRegistry = checkNotNull(capabilityRegistry);
  }

  @Override
  public boolean isValid(final List<String> names, final ConstraintValidatorContext constraintValidatorContext) {
    if (names == null || names.isEmpty()) {
      return true; // empty list is valid
    }

    Set<String> webhookNames =
        globalWebhooks.stream().map(GlobalWebhook::getName).collect(Collectors.toUnmodifiableSet());

    // Check if all names in the provided list are valid webhook names
    if (!webhookNames.containsAll(names)) {
      return false;
    }

    // Additional validation for firewall_quarantine webhook
    if (names.contains(FIREWALL_QUARANTINE_WEBHOOK)) {
      return validateFirewallQuarantinePrerequisites(constraintValidatorContext);
    }

    return true;
  }

  /**
   * Validates that prerequisites for firewall_quarantine webhook are met:
   * 1. IQ Server capability must be configured
   * 2. At least one Firewall Audit capability must exist and be enabled
   */
  private boolean validateFirewallQuarantinePrerequisites(final ConstraintValidatorContext context) {
    // Check 1: IQ Server capability must be configured and enabled
    boolean clmConfigured = capabilityRegistry.getAll()
        .stream()
        .anyMatch(cap -> CLM_CAPABILITY_TYPE.equals(cap.context().type().toString())
            && cap.context().isEnabled());

    if (!clmConfigured) {
      context.disableDefaultConstraintViolation();
      context.buildConstraintViolationWithTemplate(
          "The 'firewall_quarantine' webhook event requires IQ Server to be configured. " +
              "Please configure IQ Server first.")
          .addConstraintViolation();
      return false;
    }

    // Check 2: At least one Firewall Audit capability must exist and be enabled
    boolean firewallEnabled = capabilityRegistry.getAll()
        .stream()
        .anyMatch(cap -> FIREWALL_AUDIT_CAPABILITY_TYPE.equals(cap.context().type().toString())
            && cap.context().isEnabled());

    if (!firewallEnabled) {
      context.disableDefaultConstraintViolation();
      context.buildConstraintViolationWithTemplate(
          "The 'firewall_quarantine' webhook event requires Repository Firewall to be enabled. " +
              "Please create a 'Firewall Audit and Quarantine' capability first with at least one repository configured")
          .addConstraintViolation();
      return false;
    }

    // Check 3: At least one enabled Firewall Audit capability must have quarantine enabled
    boolean quarantineEnabled = capabilityRegistry.getAll()
        .stream()
        .anyMatch(cap -> FIREWALL_AUDIT_CAPABILITY_TYPE.equals(cap.context().type().toString())
            && cap.context().isEnabled()
            && "true".equalsIgnoreCase(cap.context().properties().get("quarantine")));

    if (!quarantineEnabled) {
      context.disableDefaultConstraintViolation();
      context.buildConstraintViolationWithTemplate(
          "The 'firewall_quarantine' webhook event requires quarantine to be enabled in at least one " +
              "'Firewall Audit and Quarantine' capability. Please enable the 'Quarantine components' option")
          .addConstraintViolation();
      return false;
    }

    return true;
  }
}
