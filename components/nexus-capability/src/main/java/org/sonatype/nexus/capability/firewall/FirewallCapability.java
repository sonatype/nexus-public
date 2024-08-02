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
package org.sonatype.nexus.capability.firewall;

import java.util.Collection;
import java.util.Optional;

import org.sonatype.nexus.capability.CapabilityContext;
import org.sonatype.nexus.capability.CapabilityReference;
import org.sonatype.nexus.capability.CapabilityRegistry;
import org.sonatype.nexus.capability.CapabilityType;

import static org.sonatype.nexus.capability.CapabilityReferenceFilterBuilder.capabilities;

public class FirewallCapability
{
  private static final String CLM_CAPABILITY_ID = "clm";

  private static final String AUDIT_QUARANTINE_CAPABILITY_ID = "firewall.audit";

  private FirewallCapability() {
    throw new IllegalStateException("Utility class");
  }

  public static Optional<CapabilityContext> findFirewallCapability(final CapabilityRegistry capabilities) {
    Collection<? extends CapabilityReference> references = capabilities
        .get(capabilities().withType(CapabilityType.capabilityType(CLM_CAPABILITY_ID)).includeNotExposed());

    if (references.isEmpty()) {
      return Optional.empty();
    }

    CapabilityContext context = references.iterator().next().context();
    return Optional.of(context);
  }

  public static boolean auditAndQuarantineCapabilityExists(final CapabilityRegistry capabilities) {
    return capabilities.get(capabilities().withType(CapabilityType.capabilityType(AUDIT_QUARANTINE_CAPABILITY_ID))).
        stream().
        anyMatch(reference -> reference.context().isEnabled());
  }
}
