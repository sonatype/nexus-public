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
package org.sonatype.nexus.capability;

import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.security.privilege.ApplicationPermission;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.util.Objects.requireNonNull;

/**
 * Enforces the additional authorization required to create, update, or enable security-sensitive capability types.
 * <p/>
 * A capability type whose descriptor reports {@link CapabilityDescriptor#requiresSettingsUpdatePermission()} activates
 * or configures a security realm (authentication/authorization power). Managing such a capability must therefore
 * require realm-administration authority ({@code nexus:settings:update}) in addition to the ordinary
 * {@code nexus:capabilities:create}/{@code :update} permission, so the weaker capabilities permission alone cannot be
 * used to escalate privileges (CVE-2026-14645 / NEXUS-54726).
 * <p/>
 * This is enforced at the user-facing endpoint layer (REST + ExtDirect, self-hosted + Cloud), where an authenticated
 * subject exists. It is deliberately NOT enforced inside {@code DefaultCapabilityRegistry}, whose write methods are
 * also reached by cross-node replication, boot-time loading, and non-exposed system capabilities that have no
 * user subject.
 */
@Component
public class CapabilitySecurityPermissionHelper
{
  private final SecurityHelper securityHelper;

  private final CapabilityDescriptorRegistry capabilityDescriptorRegistry;

  @Autowired
  public CapabilitySecurityPermissionHelper(
      final SecurityHelper securityHelper,
      final CapabilityDescriptorRegistry capabilityDescriptorRegistry)
  {
    this.securityHelper = requireNonNull(securityHelper);
    this.capabilityDescriptorRegistry = requireNonNull(capabilityDescriptorRegistry);
  }

  /**
   * Ensures the current subject may manage a capability of the given type, throwing
   * {@link org.apache.shiro.authz.AuthorizationException} when the type is security-sensitive and the subject lacks
   * {@code nexus:settings:update}.
   *
   * @param type the capability type being created, updated, or enabled
   */
  public void ensurePermittedForType(final CapabilityType type) {
    if (type == null) {
      return;
    }
    ensurePermittedForDescriptor(capabilityDescriptorRegistry.get(type));
  }

  /**
   * Ensures the current subject may manage a capability described by the given descriptor, throwing
   * {@link org.apache.shiro.authz.AuthorizationException} when the descriptor is security-sensitive and the subject
   * lacks {@code nexus:settings:update}.
   *
   * @param descriptor the descriptor of the capability being created, updated, or enabled
   */
  public void ensurePermittedForDescriptor(final CapabilityDescriptor descriptor) {
    if (descriptor != null && descriptor.requiresSettingsUpdatePermission()) {
      String domain = descriptor.settingsUpdatePermissionDomain();
      // A sensitive type that does not declare its own realm-admin domain falls back to the global
      // nexus:settings:update authority.
      if (domain == null) {
        domain = "settings";
      }
      securityHelper.ensurePermitted(new ApplicationPermission(domain, "update"));
    }
  }
}
