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
package org.sonatype.nexus.internal.capability.storage;

import java.util.Map;

import org.sonatype.goodies.lifecycle.Lifecycle;
import org.sonatype.nexus.capability.CapabilityIdentity;

/**
 * Capability storage.
 */
public interface CapabilityStorage
    extends Lifecycle
{
  /**
   * Adds a capability
   *
   * @param item to be added
   */
  CapabilityIdentity add(CapabilityStorageItem item);

  // FIXME: update() and remove() return values are never used, probably should return void instead

  /**
   * Updates stored capability if exists.
   *
   * @param item to be updated
   * @return false if capability to be updated does not exist in storage, true otherwise
   */
  boolean update(CapabilityIdentity id, CapabilityStorageItem item);

  /**
   * Deletes stored capability if exists.
   *
   * @param id of capability to be deleted
   * @return false if capability to be deleted does not exist in storage, true otherwise
   */
  boolean remove(CapabilityIdentity id);

  /**
   * Retrieves stored capabilities.
   *
   * @return capabilities (never null)
   */
  Map<CapabilityIdentity, CapabilityStorageItem> getAll();
}
