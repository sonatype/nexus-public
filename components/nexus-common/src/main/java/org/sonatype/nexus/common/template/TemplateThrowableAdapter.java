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
package org.sonatype.nexus.common.template;

import com.google.common.base.Throwables;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Helper to deal with {@link Throwable} instances in a template.
 *
 * @since 3.0
 */
@TemplateAccessible
public class TemplateThrowableAdapter
{
  private final Throwable cause;

  public TemplateThrowableAdapter(final Throwable cause) {
    this.cause = checkNotNull(cause);
  }

  public Throwable getCause() {
    return cause;
  }

  public String getType() {
    return cause.getClass().getName();
  }

  public String getSimpleType() {
    return cause.getClass().getSimpleName();
  }

  public String getMessage() {
    return cause.getMessage();
  }

  public String getTrace() {
    return Throwables.getStackTraceAsString(cause);
  }

  public String toString() {
    return cause.toString();
  }
}