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
package org.sonatype.nexus.configuration.model;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.configuration.CoreConfiguration;
import org.sonatype.nexus.configuration.ExternalConfiguration;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;

/**
 * A superclass class that holds an Xpp3Dom and maintains it.
 *
 * @author cstamas
 */
public class DefaultExternalConfiguration<T extends AbstractXpp3DomExternalConfigurationHolder>
    implements ExternalConfiguration<T>
{
  private final ApplicationConfiguration applicationConfiguration;

  private final CoreConfiguration coreConfiguration;

  private final T configuration;

  private T changedConfiguration;

  public DefaultExternalConfiguration(ApplicationConfiguration applicationConfiguration,
                                      CoreConfiguration coreConfiguration, T configuration)
  {
    this.applicationConfiguration = applicationConfiguration;

    this.coreConfiguration = coreConfiguration;

    this.configuration = configuration;

    this.changedConfiguration = null;
  }

  public boolean isDirty() {
    return this.changedConfiguration != null;
  }

  public void validateChanges()
      throws ConfigurationException
  {
    if (changedConfiguration != null) {
      changedConfiguration.validate(getApplicationConfiguration(), coreConfiguration);
    }
  }

  public void commitChanges()
      throws ConfigurationException
  {
    if (changedConfiguration != null) {
      changedConfiguration.validate(getApplicationConfiguration(), coreConfiguration);

      configuration.apply(changedConfiguration);

      changedConfiguration = null;
    }
  }

  public void rollbackChanges() {
    changedConfiguration = null;
  }

  @SuppressWarnings("unchecked")
  public T getConfiguration(boolean forModification) {
    if (forModification) {
      // copy configuration if needed
      if (changedConfiguration == null) {
        changedConfiguration = (T) configuration.clone();
      }

      return changedConfiguration;
    }
    else {
      return configuration;
    }
  }

  // ==

  protected ApplicationConfiguration getApplicationConfiguration() {
    return applicationConfiguration;
  }
}
