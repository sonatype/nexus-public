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

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link Capability} related events.
 *
 * @since capabilities 2.0
 */
public class CapabilityEvent
{

  private final CapabilityReference reference;

  public CapabilityEvent(final CapabilityRegistry capabilityRegistry,
                         final CapabilityReference reference)
  {
    this.reference = checkNotNull(reference);
  }

  public CapabilityReference getReference() {
    return reference;
  }

  @Override
  public String toString() {
    return getReference().toString();
  }

  /**
   * Event fired after a capability was activated.
   *
   * @since capabilities 2.0
   */
  public static class AfterActivated
      extends CapabilityEvent
  {

    public AfterActivated(final CapabilityRegistry capabilityRegistry,
                          final CapabilityReference reference)
    {
      super(capabilityRegistry, reference);
    }

    @Override
    public String toString() {
      return "Activated " + super.toString();
    }

  }

  /**
   * Event fired before a capability is passivated.
   *
   * @since capabilities 2.0
   */
  public static class BeforePassivated
      extends CapabilityEvent
  {

    public BeforePassivated(final CapabilityRegistry capabilityRegistry,
                            final CapabilityReference reference)
    {
      super(capabilityRegistry, reference);
    }

    @Override
    public String toString() {
      return "Passivated " + super.toString();
    }

  }

  /**
   * Event fired before a capability is updated.
   *
   * @since capabilities 2.0
   */
  public static class BeforeUpdate
      extends CapabilityEvent
  {

    private final Map<String, String> properties;

    private final Map<String, String> previousProperties;

    public BeforeUpdate(final CapabilityRegistry capabilityRegistry,
                        final CapabilityReference reference,
                        final Map<String, String> properties,
                        final Map<String, String> previousProperties)
    {
      super(capabilityRegistry, reference);
      this.properties = checkNotNull(properties);
      this.previousProperties = checkNotNull(previousProperties);
    }

    @Override
    public String toString() {
      return "Before update of " + super.toString();
    }

    public Map<String, String> properties() {
      return properties;
    }

    public Map<String, String> previousProperties() {
      return previousProperties;
    }

  }

  /**
   * Event fired after a capability was updated.
   *
   * @since capabilities 2.0
   */
  public static class AfterUpdate
      extends CapabilityEvent
  {

    private final Map<String, String> properties;

    private final Map<String, String> previousProperties;

    public AfterUpdate(final CapabilityRegistry capabilityRegistry,
                       final CapabilityReference reference,
                       final Map<String, String> properties,
                       final Map<String, String> previousProperties)
    {
      super(capabilityRegistry, reference);
      this.properties = checkNotNull(properties);
      this.previousProperties = checkNotNull(previousProperties);
    }

    @Override
    public String toString() {
      return "After update of " + super.toString();
    }

    public Map<String, String> properties() {
      return properties;
    }

    public Map<String, String> previousProperties() {
      return previousProperties;
    }

  }

  /**
   * Event fired when a capability is created (added to registry).
   * <p/>
   * Called before {@link Capability#onCreate(java.util.Map)} / {@link Capability#onLoad(java.util.Map)} are called.
   *
   * @since capabilities 2.0
   */
  public static class Created
      extends CapabilityEvent
  {

    public Created(final CapabilityRegistry capabilityRegistry,
                   final CapabilityReference reference)
    {
      super(capabilityRegistry, reference);
    }

    @Override
    public String toString() {
      return "Created " + super.toString();
    }

  }

  /**
   * Event fired when a capability is removed from registry.
   * <p/>
   * Called after {@link Capability#onRemove()} is called.
   *
   * @since capabilities 2.0
   */
  public static class AfterRemove
      extends CapabilityEvent
  {

    public AfterRemove(final CapabilityRegistry capabilityRegistry,
                       final CapabilityReference reference)
    {
      super(capabilityRegistry, reference);
    }

    @Override
    public String toString() {
      return "After remove of " + super.toString();
    }

  }

  /**
   * Event fired when an exception occurred during a lifecycle callback method (create/load/update/activate/passivate).
   *
   * @since 2.7
   */
  public static class CallbackFailure
      extends CapabilityEvent
  {

    private String failingAction;

    private Exception failure;

    public CallbackFailure(final CapabilityRegistry capabilityRegistry,
                           final CapabilityReference reference,
                           final String failingAction,
                           final Exception failure)
    {
      super(capabilityRegistry, reference);
      this.failingAction = checkNotNull(failingAction);
      this.failure = checkNotNull(failure);
    }

    @Override
    public String toString() {
      return failingAction + " Failed " + super.toString();
    }

    public Exception failure() {
      return failure;
    }

    public String failingAction() {
      return failingAction;
    }
  }

  /**
   * Event fired when an capability failure has been cleared.
   *
   * @since 2.7
   */
  public static class CallbackFailureCleared
      extends CapabilityEvent
  {

    public CallbackFailureCleared(final CapabilityRegistry capabilityRegistry,
                                  final CapabilityReference reference)
    {
      super(capabilityRegistry, reference);
    }

    @Override
    public String toString() {
      return "Error cleared " + super.toString();
    }

  }

}