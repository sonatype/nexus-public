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
 * Registry of capability factories.
 *
 * @since capabilities 2.0
 */
public interface CapabilityFactoryRegistry
{

  /**
   * Registers a factory.
   *
   * @param type    type of capabilities created by factory
   * @param factory to be added
   * @return itself, for fluent api usage
   * @throws IllegalArgumentException if another factory for same type was already registered
   */
  CapabilityFactoryRegistry register(CapabilityType type, CapabilityFactory factory);

  /**
   * Unregisters factory with specified type. If a factory with specified type was not registered before it returns
   * silently.
   *
   * @param type of factory to be removed
   * @return itself, for fluent api usage
   */
  CapabilityFactoryRegistry unregister(CapabilityType type);

  /**
   * Returns the factory bounded to specified type.
   *
   * @param type of factory
   * @return bounded factory or null if none was bounded to specified type
   */
  CapabilityFactory get(CapabilityType type);

}
