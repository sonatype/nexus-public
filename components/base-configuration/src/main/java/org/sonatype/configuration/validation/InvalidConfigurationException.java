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
package org.sonatype.configuration.validation;

import java.io.StringWriter;

import org.sonatype.configuration.ConfigurationException;


/**
 * Thrown when some semantical error is detected in validated configuration.
 *
 * @author cstamas
 */
public class InvalidConfigurationException
    extends ConfigurationException
{

  private static final long serialVersionUID = -7524456367570093185L;

  /**
   * The validation response.
   */
  private ValidationResponse validationResponse;

  public InvalidConfigurationException() {
    this("Configuration is invalid!");
  }

  public InvalidConfigurationException(String msg) {
    super(msg);
  }

  public InvalidConfigurationException(String msg, Throwable t) {
    super(msg, t);
  }

  public InvalidConfigurationException(ValidationResponse validationResponse) {
    this();

    this.validationResponse = validationResponse;
  }

  public ValidationResponse getValidationResponse() {
    return validationResponse;
  }

  public String getMessage() {
    StringWriter sw = new StringWriter();

    sw.append(super.getMessage());

    if (getValidationResponse() != null) {
      if (getValidationResponse().getValidationErrors().size() > 0) {
        sw.append("\nValidation errors follows:\n");

        for (ValidationMessage error : getValidationResponse().getValidationErrors()) {
          sw.append(error.toString());
        }
        sw.append("\n");
      }

      if (getValidationResponse().getValidationWarnings().size() > 0) {
        sw.append("\nValidation warnings follows:\n");

        for (ValidationMessage warning : getValidationResponse().getValidationWarnings()) {
          sw.append(warning.toString());
        }
        sw.append("\n");
      }
    }

    return sw.toString();
  }

}
