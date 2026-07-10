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

import java.util.Collection;
import java.util.Date;
import java.util.Map;

import javax.annotation.Nullable;

import org.sonatype.nexus.capability.CapabilityRegistryEvent.Ready;
import org.sonatype.nexus.common.db.DatabaseCheck;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.event.EventHelper;
import org.sonatype.nexus.common.upgrade.events.UpgradeEventSupport;

import com.google.common.base.Throwables;
import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Support for components which need to handle capability registration upon booting.
 *
 * @since capabilities 2.2
 */
public abstract class CapabilityBooterSupport
    implements EventAware
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private CapabilityRegistry registry;

  private DatabaseCheck dbCheck;

  private final String requiredVersion;

  public CapabilityBooterSupport() {
    this(null);
  }

  public CapabilityBooterSupport(final String version) {
    this.requiredVersion = version;
  }

  @Autowired
  public final void injectDependencies(final DatabaseCheck dbCheck, final CapabilityRegistry registry) {
    this.dbCheck = checkNotNull(dbCheck);
    this.registry = checkNotNull(registry);
  }

  @Subscribe
  public void handle(final Ready event) {
    doBoot();
  }

  @Subscribe
  public void handle(final UpgradeEventSupport event) {
    if (!EventHelper.isReplicating()) {
      doBoot();
    }
  }

  private void doBoot() {
    if (isRequiredVersion()) {
      try {
        boot(registry);
      }
      catch (Exception e) {
        Throwables.throwIfUnchecked(e);
        throw new RuntimeException(e);
      }
    }
  }

  private boolean isRequiredVersion() {
    return requiredVersion == null || dbCheck.isAtLeast(requiredVersion);
  }

  protected abstract void boot(final CapabilityRegistry registry) throws Exception;

  /**
   * This method is ONLY to be called when internally creating capabilities programmatically. Users should not be able
   * to utilize this method in any fashion.
   */
  protected void maybeAddCapability(
      final CapabilityRegistry capabilityRegistry,
      final CapabilityType type,
      final boolean enabled,
      @Nullable final String notes,
      @Nullable final Map<String, String> properties) throws Exception
  {
    CapabilityReference reference = findCapability(capabilityRegistry, type);
    if (reference == null) {
      log.debug("Automatically adding capability type: {}; enabled: {}", type, enabled);
      addCapability(capabilityRegistry, type, enabled, notes, properties);
    }
  }

  protected CapabilityReference findCapability(
      final CapabilityRegistry capabilityRegistry,
      final CapabilityType type)
  {
    return findCapability(capabilityRegistry, type, true);
  }

  protected CapabilityReference findCapability(
      final CapabilityRegistry capabilityRegistry,
      final CapabilityType type,
      final boolean includeNotExposed)
  {
    CapabilityReferenceFilterBuilder.CapabilityReferenceFilter filter =
        CapabilityReferenceFilterBuilder.capabilities().withType(type);

    if (includeNotExposed) {
      filter.includeNotExposed();
    }

    Collection<? extends CapabilityReference> capabilities = capabilityRegistry.get(filter);
    if (capabilities != null && !capabilities.isEmpty()) {
      return capabilities.iterator().next();
    }
    return null;
  }

  /**
   * This method is ONLY to be called when internally creating capabilities programmatically. Users should not be able
   * to utilize this method in any fashion.
   */
  protected void addCapability(
      final CapabilityRegistry capabilityRegistry,
      final CapabilityType type,
      final boolean enabled,
      @Nullable final String notes,
      @Nullable final Map<String, String> properties) throws Exception
  {
    capabilityRegistry.addNonExposed(type, enabled, notes == null ? "Automatically added on " + new Date() : notes,
        properties);
  }
}
