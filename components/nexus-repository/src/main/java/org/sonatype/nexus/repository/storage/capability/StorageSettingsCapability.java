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
package org.sonatype.nexus.repository.storage.capability;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.capability.CapabilitySupport;
import org.sonatype.nexus.repository.storage.AssetManager;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Integer.parseInt;
import static org.sonatype.nexus.repository.storage.AssetManager.ONE_HOUR_IN_SECONDS;
import static org.sonatype.nexus.repository.storage.capability.StorageSettingsCapabilityConfiguration.DEFAULT_LAST_DOWNLOADED;

/**
 * Storage settings capability.
 *
 * @since 3.next
 */
@Named(StorageSettingsCapabilityDescriptor.TYPE_ID)
public class StorageSettingsCapability
    extends CapabilitySupport<StorageSettingsCapabilityConfiguration>
{
  private final AssetManager assetManager;

  @Inject
  public StorageSettingsCapability(final AssetManager assetManager) {
    this.assetManager = checkNotNull(assetManager);
  }

  @Override
  protected StorageSettingsCapabilityConfiguration createConfig(final Map<String, String> properties) {
    return new StorageSettingsCapabilityConfiguration(properties);
  }

  @Override
  protected void onUpdate(final StorageSettingsCapabilityConfiguration config) throws Exception {
    if (!context().isActive()) {
      return;
    }

    log.info("Using configured value of {} hours for lastDownloaded update frequency", config.getLastDownloaded());
    assetManager.setExpireInSeconds(convertToSeconds(parseInt(config.getLastDownloaded())));
  }

  @Override
  protected void onActivate(final StorageSettingsCapabilityConfiguration config) throws Exception {
    log.info("Using configured value of {} hours for lastDownloaded update frequency", config.getLastDownloaded());
    assetManager.setExpireInSeconds(convertToSeconds(parseInt(config.getLastDownloaded())));
  }

  @Override
  protected void onPassivate(final StorageSettingsCapabilityConfiguration config) throws Exception {
    log.info("Reverting back to {} hours for lastDownloaded update frequency", DEFAULT_LAST_DOWNLOADED);
    assetManager.setExpireInSeconds(convertToSeconds(DEFAULT_LAST_DOWNLOADED));
  }

  private int convertToSeconds(final int configuredValue) {
    return configuredValue * ONE_HOUR_IN_SECONDS;
  }
}
