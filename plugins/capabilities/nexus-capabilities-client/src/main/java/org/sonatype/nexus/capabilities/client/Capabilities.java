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
package org.sonatype.nexus.capabilities.client;

import java.util.Collection;

import org.sonatype.nexus.capabilities.client.exceptions.CapabilityFactoryNotAvailableException;
import org.sonatype.nexus.capabilities.client.exceptions.MultipleCapabilitiesFoundException;
import org.sonatype.nexus.capabilities.client.internal.CapabilitiesImpl;
import org.sonatype.nexus.client.core.exception.NexusClientNotFoundException;

import com.google.inject.ImplementedBy;

/**
 * Capabilities Nexus Client Subsystem.
 *
 * @since capabilities 2.1
 */
@ImplementedBy(CapabilitiesImpl.class)
public interface Capabilities
{

  /**
   * Creates a new capability of specified type.
   *
   * @param type capability type id (cannot be null)
   * @return created capability (never null)
   */
  Capability create(String type);

  /**
   * Retrieves a capability by id.
   *
   * @param id of capability to be retrieved (cannot be null)
   * @return capability with specified id (never null)
   * @throws NexusClientNotFoundException If a capability with specified id does not exist
   */
  Capability get(String id)
      throws NexusClientNotFoundException;

  /**
   * Retrieves all existing capabilities, except those that are not exposed.
   *
   * @return capabilities (never null, can be empty)
   */
  Collection<Capability> get();

  /**
   * Retrieves all capabilities that matches the filter.
   *
   * @param filter matching filter (cannot be null)
   * @return matching capabilities (never null, can be empty)
   */
  Collection<Capability> get(Filter filter);

  /**
   * Retrieves a unique capability that matches the filter.
   *
   * @param filter matching filter (cannot be null)
   * @return matching capability (never null)
   * @throws MultipleCapabilitiesFoundException
   *                                      If more then one capability is matching the filter
   * @throws NexusClientNotFoundException If a capability with filter does not exist
   */
  Capability getUnique(Filter filter)
      throws MultipleCapabilitiesFoundException, NexusClientNotFoundException;

  /**
   * Creates a new capability of specified type.
   *
   * @param type capability type (cannot be null)
   * @return created capability (never null)
   * @throws CapabilityFactoryNotAvailableException
   *          If there is no capability factory configured for specified type
   */
  <C extends Capability> C create(Class<C> type)
      throws CapabilityFactoryNotAvailableException;

  /**
   * Retrieves a capability by id.
   *
   * @param type expected capability type (cannot be null)
   * @param id   of capability to be retrieved (cannot be null)
   * @return capability with specified id (never null)
   * @throws NexusClientNotFoundException If a capability with specified id does not exist
   * @throws ClassCastException           If capability with specified id is of a different type then expected
   */
  <C extends Capability> C get(Class<C> type, String id)
      throws NexusClientNotFoundException, ClassCastException;

  /**
   * Retrieves all capabilities that matches the filter.
   *
   * @param type   expected capabilities type (cannot be null)
   * @param filter matching filter (cannot be null)
   * @return matching capabilities (never null, can be empty)
   * @throws ClassCastException If any of matching capabilities is of a different type then expected
   */
  <C extends Capability> Collection<C> get(Class<C> type, Filter filter)
      throws ClassCastException;

  /**
   * Retrieves a unique capability that matches the filter.
   *
   * @param type   expected capabilities type (cannot be null)
   * @param filter matching filter (cannot be null)
   * @return matching capability (never null)
   * @throws MultipleCapabilitiesFoundException
   *                                      If more then one capability is matching the filter
   * @throws NexusClientNotFoundException If a capability with filter does not exist
   * @throws ClassCastException           If found capability is of a different type then expected
   */
  <C extends Capability> C getUnique(Class<C> type, Filter filter)
      throws MultipleCapabilitiesFoundException, NexusClientNotFoundException, ClassCastException;

}
