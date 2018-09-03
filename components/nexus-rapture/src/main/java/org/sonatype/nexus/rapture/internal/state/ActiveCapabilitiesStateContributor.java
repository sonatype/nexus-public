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
package org.sonatype.nexus.rapture.internal.state;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.capability.CapabilityContext;
import org.sonatype.nexus.capability.CapabilityReference;
import org.sonatype.nexus.capability.CapabilityRegistry;
import org.sonatype.nexus.capability.CapabilityType;
import org.sonatype.nexus.rapture.StateContributor;

import com.google.common.collect.ImmutableMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;

/**
 * Contributes {@code activeCapabilities} state.
 *
 * @since 3.next
 */
@Named
@Singleton
public class ActiveCapabilitiesStateContributor
    extends ComponentSupport
    implements StateContributor
{
  private static final String STATE_ID = "activeCapabilities";

  private CapabilityRegistry capabilityRegistry;

  @Inject
  public ActiveCapabilitiesStateContributor(final CapabilityRegistry capabilityRegistry) {
    this.capabilityRegistry = checkNotNull(capabilityRegistry);
  }

  @Override
  public Map<String, Object> getState() {
    return ImmutableMap.of(STATE_ID, getActiveCapabilities());
  }

  private List<String> getActiveCapabilities() {
    return capabilityRegistry.getAll().stream()
        .map(CapabilityReference::context)
        .filter(CapabilityContext::isActive)
        .map(CapabilityContext::type)
        .map(CapabilityType::toString)
        .collect(toList());
  }
}
