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
package org.sonatype.nexus.extdirect.model;

import com.google.common.collect.Lists;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.subject.Subject;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Ext.Direct error response.
 *
 * @since 3.0
 */
public class ErrorResponse
    extends Response<Object>
{
  private String message;

  private boolean authenticationRequired;

  public ErrorResponse(final Throwable cause) {
    this(checkNotNull(cause).getMessage() == null ? cause.getClass().getName() : cause.getMessage());
    authenticationRequired = cause instanceof UnauthenticatedException;
    if (authenticationRequired) {
      Subject subject = SecurityUtils.getSubject();
      if (subject == null || !(subject.isRemembered() || subject.isAuthenticated())) {
        message = "Access denied (authentication required)";
      }
    }
  }

  public ErrorResponse(final String message) {
    super(false, Lists.newArrayList());
    this.message = checkNotNull(message);
  }

}
