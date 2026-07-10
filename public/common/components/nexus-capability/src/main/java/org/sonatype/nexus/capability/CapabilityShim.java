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

/**
 * Decouples the capabilities REST resource from concrete capability-shim implementations, allowing
 * the resource to delegate capability operations to any registered shim without depending on
 * shim-specific types (e.g. firewall, IQ-server-connection-info).
 * <p>
 * A shim presents a synthetic capability API for state that has been migrated out of the
 * capability registry into another storage layer (typically repository configuration). The shim is
 * responsible for translating the legacy capability-shaped request into the new model and back.
 */
public interface CapabilityShim
{
  /**
   * Returns {@code true} if this shim handles capabilities of the given type ID.
   */
  boolean handlesType(String typeId);

  /**
   * Returns {@code true} if this shim handles the capability with the given ID.
   */
  boolean handlesId(String capabilityId);

  /**
   * Returns the synthetic {@link CapabilityTypeDTO} exposed by this shim.
   */
  CapabilityTypeDTO getCapabilityTypeDTO();

  /**
   * Returns all synthetic capability DTOs managed by this shim.
   */
  Collection<CapabilityDTO> list();

  /**
   * Creates a capability from the given DTO and returns the resulting synthetic DTO.
   */
  CapabilityDTO create(CapabilityDTO dto);

  /**
   * Updates the capability identified by {@code capabilityId} using the given DTO.
   */
  void update(String capabilityId, CapabilityDTO dto);

  /**
   * Deletes the capability identified by {@code capabilityId}.
   */
  void delete(String capabilityId);
}
