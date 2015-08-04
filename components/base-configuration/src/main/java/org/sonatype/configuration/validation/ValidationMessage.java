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

import java.io.PrintWriter;
import java.io.StringWriter;

public class ValidationMessage
{
  /**
   * Message key.
   */
  private String key;

  /**
   * Message body.
   */
  private String message;

  /**
   * The short message, used to send back to UI for validation display
   */
  private String shortMessage;

  /**
   * The cause of validation problem, if any.
   */
  private Throwable cause;

  /**
   * Creates a validation message without a cause.
   */
  public ValidationMessage(String key, String message) {
    this(key, message, (Throwable) null);
  }

  /**
   * Creates a validation message without a cause.
   */
  public ValidationMessage(String key, String message, String shortMessage) {
    this(key, message, shortMessage, null);
  }

  /**
   * Creates a validation message with cause.
   */
  public ValidationMessage(String key, String message, Throwable cause) {
    this(key, message, message, cause);
  }

  /**
   * Creates a validation message with cause.
   */
  public ValidationMessage(String key, String message, String shortMessage, Throwable cause) {
    this.key = key;

    this.message = message;

    this.shortMessage = shortMessage;

    this.cause = cause;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getShortMessage() {
    return shortMessage;
  }

  public void setShortMessage(String shortMessage) {
    this.shortMessage = shortMessage;
  }

  public Throwable getCause() {
    return cause;
  }

  public void setCause(Throwable cause) {
    this.cause = cause;
  }

  public String toString() {
    StringWriter sw = new StringWriter();

    sw.append(" o ").append(getKey()).append(" - ").append(getMessage());

    if (getCause() != null) {
      sw.append("\n");

      sw.append("   Cause:\n");

      getCause().printStackTrace(new PrintWriter(sw));
    }

    return sw.toString();
  }
}
