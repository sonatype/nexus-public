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
package org.sonatype.nexus.internal.capability.storage.datastore;

import org.sonatype.nexus.capability.CapabilityIdentity;
import org.sonatype.nexus.common.event.EventWithSource;
import org.sonatype.nexus.internal.capability.storage.CapabilityStorageImpl;
import org.sonatype.nexus.internal.capability.storage.CapabilityStorageItemData;
import org.sonatype.nexus.internal.capability.storage.CapabilityStorageItemEvent;

import static com.google.common.base.Preconditions.checkNotNull;

public class CapabilityStorageItemEventSupport
    extends EventWithSource
    implements CapabilityStorageItemEvent
{
  private CapabilityIdentity capabilityId;

  protected CapabilityStorageItemEventSupport() {
    // deserialization
  }

  protected CapabilityStorageItemEventSupport(final CapabilityStorageItemData item) {
    this.capabilityId = CapabilityStorageImpl.capabilityIdentity(checkNotNull(item));
  }

  @Override
  public CapabilityIdentity getCapabilityId() {
    return capabilityId;
  }

  public void setCapabilityId(final CapabilityIdentity capabilityId) {
    this.capabilityId = capabilityId;
  }
}
