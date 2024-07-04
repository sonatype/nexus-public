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
package org.sonatype.nexus.testsuite.testsupport.fixtures;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.inject.Provider;

import org.sonatype.nexus.capability.CapabilityContext;
import org.sonatype.nexus.capability.CapabilityIdentity;
import org.sonatype.nexus.capability.CapabilityReference;
import org.sonatype.nexus.capability.CapabilityRegistry;
import org.sonatype.nexus.capability.CapabilityType;

import org.junit.rules.ExternalResource;

public class CapabilitiesRule
    extends ExternalResource
{
  private static final String OUTREACH = "OutreachManagementCapability";

  private final Provider<CapabilityRegistry> capabilityRegistryProvider;

  private final Collection<String> capabilitiesToDisable = new ArrayList<>();

  private final Collection<String> capabilitiesToRemove = new ArrayList<>();

  private final Map<String, Map<String, String>> originalProperties = new HashMap<>();

  public CapabilitiesRule(final Provider<CapabilityRegistry> capabilityRegistryProvider) {
    this.capabilityRegistryProvider = capabilityRegistryProvider;
  }

  public void disableOutreach() {
    disable(OUTREACH);
  }

  public Collection<CapabilityReference> getAll() {
    //noinspection unchecked
    return (Collection<CapabilityReference>) capabilityRegistryProvider.get().getAll();
  }

  public void removeById(final CapabilityIdentity id) {
    capabilityRegistryProvider.get().remove(id);
  }

  @Override
  public void after() {
    capabilitiesToRemove.stream()
        .map(this::find)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(CapabilityReference::context)
        .map(CapabilityContext::id)
        .forEach(capabilityRegistryProvider.get()::remove);

    capabilitiesToDisable.stream()
        .map(this::find)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(CapabilityReference::context)
        .map(CapabilityContext::id)
        .forEach(capabilityRegistryProvider.get()::disable);

    for (Entry<String, Map<String, String>> entry : originalProperties.entrySet()) {
      find(entry.getKey()).ifPresent(ref -> {
        capabilityRegistryProvider.get().update(ref.context().id(), ref.context().isEnabled(), null, entry.getValue());
      });
    }
  }

  /**
   * Creates a capability if not present, otherwise enable existing capability.
   *
   * @param capabilityType the capability type
   * @param properties the properties
   */
  protected void enableAndSetProperties(final String capabilityType, final Map<String, String> properties) {
    Optional<? extends CapabilityReference>  capabilityReference = find(capabilityType);
    if (capabilityReference.isPresent()) {
      CapabilityContext context = capabilityReference.get().context();
      if (!context.isEnabled()) {
        capabilitiesToDisable.add(capabilityType);
      }
      originalProperties.put(capabilityType, context.properties());

      capabilityRegistryProvider.get().update(context.id(), true, null, properties);
    }
    else {
      createCapability(capabilityType, properties);
    }
  }

  /**
   * Create a capability.
   *
   * @param capabilityType
   * @param properties
   */
  protected void createCapability(final String capabilityType, final Map<String, String> properties) {
    capabilityRegistryProvider.get().add(CapabilityType.capabilityType(capabilityType), true, null, properties);
    capabilitiesToRemove.add(capabilityType);
  }

  /**
   * Disables a capability, please note that original state will not be restored.
   */
  protected void disable(final String capabilityType) {
    // We don't handle missing capabilities here intentionally
    Optional<? extends CapabilityReference> capabilityReference = find(capabilityType);
    capabilityReference.ifPresent(capability -> capabilityRegistryProvider.get().disable(capability.context().id()));
  }

  /**
   * Removes a capability, please note that state will not be restored.
   */
  protected void remove(final String capabilityType) {
    find(capabilityType)
        .map(CapabilityReference::context)
        .map(CapabilityContext::id)
        .ifPresent(capabilityRegistryProvider.get()::remove);
  }

  protected boolean isCapabilityInstalledAndEnabled(final String capabilityType) {
    return find(capabilityType)
        .map(CapabilityReference::context)
        .map(CapabilityContext::isEnabled)
        .orElse(false);
  }

  private Optional<? extends CapabilityReference> find(final String capabilityType) {
    CapabilityType type = CapabilityType.capabilityType(capabilityType);
    return capabilityRegistryProvider.get().getAll().stream().filter(ref -> ref.context().type().equals(type))
        .findFirst();
  }
}
