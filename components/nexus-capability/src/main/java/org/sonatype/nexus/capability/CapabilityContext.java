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

import java.util.Map;

/**
 * Provides access to capability context.
 *
 * @since capabilities 2.0
 */
public interface CapabilityContext
{

  /**
   * Returns an unique capability identifier.
   *
   * @return identifier
   */
  CapabilityIdentity id();

  /**
   * Returns type of capability.
   *
   * @return capability type (never null)
   */
  CapabilityType type();

  /**
   * Returns descriptor of capability.
   *
   * @return capability descriptor (never null)
   */
  CapabilityDescriptor descriptor();

  /**
   * Returns capability notes.
   *
   * @return capability notes (can be null)
   */
  String notes();

  /**
   * Current capability properties.
   *
   * @return properties.
   */
  Map<String, String> properties();

  /**
   * Whether the capability is enabled.
   *
   * @return true, if capability is enabled
   */
  boolean isEnabled();

  /**
   * Whether the capability is active.
   *
   * @return true, if capability was activated and not yet passivated
   */
  boolean isActive();

  /**
   * Whether the capability had failed a lifecycle callback method (create/load/update/activate/passivate).
   *
   * @return true, if capability had failed a callback method
   */
  boolean hasFailure();

  /**
   * Last exception thrown during a lifecycle callback method (create/load/update/activate/passivate).
   *
   * @return last exception thrown during a lifecycle callback method or null if it not failed
   */
  Exception failure();

  /**
   * Returns name of last lifecycle callback method (create/load/update/activate/passivate) that failed.
   *
   * @since 2.7
   */
  String failingAction();

  /**
   * Describe current state.
   *
   * @return state description
   */
  String stateDescription();

}
