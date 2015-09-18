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

/**
 * {@link CapabilityRegistry} related events.
 *
 * @since capabilities 2.0
 */
public class CapabilityRegistryEvent
{
  private final CapabilityRegistry capabilityRegistry;

  public CapabilityRegistryEvent(final CapabilityRegistry capabilityRegistry) {
    this.capabilityRegistry = capabilityRegistry;
  }

  @Override
  public String toString() {
    return capabilityRegistry.toString();
  }

  /**
   * Event fired after capabilities were loaded loaded from persistence store.
   *
   * @since capabilities 2.0
   */
  public static class AfterLoad
      extends CapabilityRegistryEvent
  {
    public AfterLoad(final CapabilityRegistry capabilityRegistry) {
      super(capabilityRegistry);
    }

    @Override
    public String toString() {
      return "Loaded " + super.toString();
    }
  }

  /**
   * Event fired once the registry is ready on boot.
   *
   * @since 3.0
   */
  public static class Ready
      extends CapabilityRegistryEvent
  {
    private final CapabilityRegistry capabilityRegistry;

    public Ready(final CapabilityRegistry capabilityRegistry) {
      super(capabilityRegistry);
      this.capabilityRegistry = capabilityRegistry;
    }

    public CapabilityRegistry getCapabilityRegistry() {
      return capabilityRegistry;
    }

    @Override
    public String toString() {
      return "Ready " + super.toString();
    }
  }
}