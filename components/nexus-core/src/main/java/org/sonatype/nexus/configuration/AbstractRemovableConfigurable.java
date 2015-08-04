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
package org.sonatype.nexus.configuration;

import javax.inject.Inject;

import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.sisu.goodies.eventbus.EventBus;

/**
 * A removable or late created component, participant of configuration framework.
 *
 * @since 2.7.0
 */
public abstract class AbstractRemovableConfigurable<C>
    extends AbstractConfigurable<C>
    implements Configurable<C>
{
  /**
   * Just an empty constructor, to mark that ctor injection is explicitly disabled. All the classes extending this one should populate/inject all the fields on their own, most probably 
   */
  public AbstractRemovableConfigurable() {
  }

  @Override
  @Inject
  public void setEventBus(final EventBus eventBus) {
    super.setEventBus(eventBus);
    registerWithEventBus();
  }
  
  @Override
  @Inject
  public void setApplicationConfiguration(final ApplicationConfiguration applicationConfiguration) {
    super.setApplicationConfiguration(applicationConfiguration);
  }

  public void dispose() {
    unregisterFromEventBus();
  }
}
