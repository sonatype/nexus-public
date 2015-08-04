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

import java.util.Map;

import org.sonatype.nexus.client.core.subsystem.Entity;

/**
 * A Nexus capability.
 *
 * @since capabilities 2.2
 */
public interface Capability<T extends Capability>
    extends Entity<T>
{

  /**
   * @return capability type id
   */
  String type();

  /**
   * @return capability notes
   */
  String notes();

  /**
   * @return true if capability is enabled
   */
  boolean isEnabled();

  /**
   * @return true if capability is active
   */
  boolean isActive();

  /**
   * @return true if capability transition between states resulted in an unhandled exception
   * @since capabilities 2.4
   */
  boolean hasErrors();

  /**
   * @return capabilities properties
   */
  Map<String, String> properties();

  /**
   * @param key of property (cannot be null)
   * @return property value (can be null)
   */
  String property(String key);

  /**
   * @param key of property (cannot be null)
   * @return true if a property with specified key exists
   */
  boolean hasProperty(String key);

  /**
   * @return status of capability
   */
  String status();

  /**
   * @return description of capability state
   * @since capabilities 2.4
   */
  String stateDescription();

  /**
   * Sets capability notes.
   *
   * @param notes to set
   * @return itself
   */
  T withNotes(String notes);

  /**
   * Directly enables the capability (no save required).
   *
   * @return itself
   */
  T enable();

  /**
   * Directly disables the capability (no save required).
   *
   * @return itself
   */
  T disable();

  /**
   * Enables/disables the capability
   *
   * @param enabled to set
   * @return itself
   */
  T withEnabled(boolean enabled);

  /**
   * Sets value of a property.
   *
   * @param key   of property (cannot be null)
   * @param value of property (can be null)
   * @return itself
   */
  T withProperty(String key, String value);

  /**
   * Removes a property.
   *
   * @param key of property to be removed
   * @return itself
   */
  T removeProperty(String key);

  T refresh();

  T save();

  T remove();

}
