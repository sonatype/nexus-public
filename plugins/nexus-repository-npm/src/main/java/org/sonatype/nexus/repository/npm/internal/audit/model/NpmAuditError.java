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
package org.sonatype.nexus.repository.npm.internal.audit.model;

/**
 * Enum of error codes and error messages for npm audit
 *
 * @since 3.24
 */
public enum NpmAuditError
{
  ABSENT_PARSING_FILE("Neither npm-shrinkwrap.json nor package-lock.json found"),
  PARSING_ISSUE("Can't parse npm-shrinkwrap.json or package-lock.json"),
  COMPONENT_NOT_FOUND("Component wasn't found in package-lock.json"),
  SERVER_INTERNAL_ERROR("Server error"),
  TIMEOUT_ERROR("Waiting for results too long"),
  INTERRUPT_ERROR("Interrupt error");

  private final String message;

  NpmAuditError(final String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }
}
