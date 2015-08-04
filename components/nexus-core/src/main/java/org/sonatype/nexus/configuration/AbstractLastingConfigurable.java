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

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.sisu.goodies.eventbus.EventBus;

import com.google.common.base.Throwables;
import com.google.common.base.Strings;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * A lasting component, participant of configuration framework.
 * 
 * @since 2.7.0
 */
public abstract class AbstractLastingConfigurable<C>
    extends AbstractConfigurable<C>
    implements Configurable<C>
{
  private final String name;

  public AbstractLastingConfigurable(final String name, final EventBus eventBus,
      final ApplicationConfiguration applicationConfiguration)
  {
    super(eventBus, applicationConfiguration);
    checkArgument(!Strings.isNullOrEmpty(name));
    this.name = name;
  }
  
  /**
   * This method mimics "post construct"! This will work as long we don't have optional dependencies!
   * TODO: this needs better solution.
   */
  @Inject
  public void init()
  {
    try {
      initializeConfiguration();
    }
    catch (ConfigurationException e) {
      Throwables.propagate(e);
    }
  }

  @Override
  public String getName() {
    return name;
  }

}
