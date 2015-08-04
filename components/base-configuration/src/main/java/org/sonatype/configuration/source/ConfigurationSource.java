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
package org.sonatype.configuration.source;

import java.io.IOException;
import java.io.InputStream;

import org.sonatype.configuration.Configuration;
import org.sonatype.configuration.ConfigurationException;
import org.sonatype.configuration.validation.ValidationResponse;

/**
 * The Interface ConfigurationSource.
 *
 * @author cstamas
 */
public interface ConfigurationSource<E extends Configuration>
{
  /**
   * Returns the validation response, if any. It is created on the loading of the user configuration.
   *
   * @return the response or null if not applicable or config was still not loaded.
   */
  ValidationResponse getValidationResponse();

  /**
   * Persists the current configuration.
   */
  void storeConfiguration()
      throws IOException;

  /**
   * Gets the current configuration.
   *
   * @return the configuration, null if not loaded
   */
  E getConfiguration();

  void setConfiguration(E configuration);

  /**
   * Forces reloading the user configuration.
   *
   * @return the configuration
   */
  E loadConfiguration()
      throws ConfigurationException, IOException;

  /**
   * Returns the actual content of configuration as stream.
   */
  InputStream getConfigurationAsStream()
      throws IOException;

  /**
   * Returns whether the configuration was upgraded.
   *
   * @return true if the user configuration was upgraded, false otherwise
   */
  boolean isConfigurationUpgraded();

  /**
   * Returns true if the configuration was got from defaults.
   */
  boolean isConfigurationDefaulted();
}
