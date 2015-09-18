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

public interface Capability
{

  /**
   * Initializes the capability after it has been created by factory.
   *
   * @param context capability context
   */
  void init(CapabilityContext context);

  /**
   * Returns description of capability.
   *
   * @return description. Can be null.
   */
  String description();

  /**
   * Returns status of capability.
   *
   * @return status. Can be null. Can be an html chunk.
   */
  String status();

  /**
   * Callback when a new capability is created.
   * <p/>
   * If an exception occurs, during invocation of this method,  the exception will be ignored and capability will be
   * in an invalid state.
   * Any further interaction with this capability will result in an {@link IllegalStateException}.
   *
   * @throws Exception If capability cannot be create
   */
  void onCreate()
      throws Exception;

  /**
   * Callback when a capability configuration is loaded from persisted store (configuration file).
   * <p/>
   * If an exception occurs, during invocation of this method,  the exception will be ignored and capability will be
   * in an invalid state.
   * Any further interaction with this capability will result in an {@link IllegalStateException}.
   *
   * @throws Exception If capability cannot be loaded
   */
  void onLoad()
      throws Exception;

  /**
   * Callback when a capability configuration is updated.
   * <p/>
   * If an exception occurs, during invocation of this method, the exception will be ignored and capability, if
   * active, will be automatically passivated.
   *
   * @throws Exception If capability cannot be updated
   */
  void onUpdate()
      throws Exception;

  /**
   * Callback when a capability is removed.
   * <p/>
   * If an exception occurs, during invocation of this method, the exception will be ignored and capability will be
   * in
   * a removed state.
   *
   * @throws Exception If capability cannot be removed
   */
  void onRemove()
      throws Exception;

  /**
   * Callback when capability is activated. Activation is triggered on create/load (if capability is not disabled),
   * or when capability is re-enabled.
   * <p/>
   * If an exception occurs, during invocation of this method, the exception will be ignored and capability will be
   * in
   * an non active state.
   *
   * @throws Exception If capability cannot be activated
   */
  void onActivate()
      throws Exception;

  /**
   * Callback when capability is passivated. Passivation will be triggered before a capability is removed, on
   * Nexus shutdown or when capability is disabled.
   * <p/>
   * If an exception occurs, during invocation of this method, the exception will be ignored.
   *
   * @throws Exception If capability cannot be passivated
   */
  void onPassivate()
      throws Exception;

  /**
   * Returns the condition that should be satisfied in order for this capability to be active.
   * <p/>
   * If an exception occurs, during invocation of this method, the capability is considered as not activatable.
   *
   * @return activation condition. If null, it considers that condition is always activatable.
   */
  Condition activationCondition();

  /**
   * Returns the condition that should be satisfied in order for this capability to be valid. When this condition
   * becomes unsatisfied, the capability will be automatically removed.
   * <p/>
   * Example of such a condition will be a capability that applies to a repository should be automatically be removed
   * when repository is removed.
   * <p/>
   * If an exception occurs, during invocation of this method, the capability is considered as always valid.
   *
   * @return activation condition. If null, it considers that condition is always valid.
   */
  Condition validityCondition();

}
