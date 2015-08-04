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
import org.sonatype.configuration.validation.ValidationResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class that encapsulates Modello model loading and saving with interpolation.
 *
 * @author cstamas
 */
public abstract class AbstractStreamConfigurationSource<E extends Configuration>
    implements ConfigurationSource<E>
{
  /**
   * The configuration.
   */
  private E configuration;

  /**
   * Flag to mark update.
   */
  private boolean configurationUpgraded;

  /**
   * The validation response
   */
  private ValidationResponse validationResponse;

  private Logger logger = LoggerFactory.getLogger(this.getClass());

  public E getConfiguration() {
    return configuration;
  }

  public void setConfiguration(E configuration) {
    this.configuration = configuration;
  }

  /**
   * Called by subclasses when loaded configuration is rejected for some reason.
   */
  protected void rejectConfiguration(String message) {
    this.configuration = null;

    if (message != null) {
      getLogger().warn(message);
    }
  }

  /**
   * Load configuration.
   *
   * @param file the file
   * @return the configuration
   * @throws IOException Signals that an I/O exception has occurred.
   */
  protected abstract void loadConfiguration(InputStream is)
      throws IOException;

  public ValidationResponse getValidationResponse() {
    return validationResponse;
  }

  protected void setValidationResponse(ValidationResponse validationResponse) {
    this.validationResponse = validationResponse;
  }

  /**
   * Is configuration updated?
   */
  public boolean isConfigurationUpgraded() {
    return configurationUpgraded;
  }

  /**
   * Setter for configuration upgraded.
   */
  public void setConfigurationUpgraded(boolean configurationUpgraded) {
    this.configurationUpgraded = configurationUpgraded;
  }

  /**
   * Returns the default source of ConfigurationSource. May be null.
   */
  public ConfigurationSource<?> getDefaultsSource() {
    return null;
  }

  public Logger getLogger() {
    return this.logger;
  }
}
