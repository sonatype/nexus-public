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
package org.sonatype.nexus.swagger;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Allows hiding fields from the Swagger model, to hide from JSON output use in conjunction with
 * {@code @JsonFilter(SwaggerEditionVisibility.NAME)} on the containing object
 */
@Target({FIELD})
@Retention(RUNTIME)
public @interface SwaggerEditionVisibility
{
  public static final String NAME = "EditionVisibility";

  boolean cloud() default true;

  boolean community() default true;

  boolean pro() default true;

  /**
   * Specify a note about why this field is hidden. Not used at runtime
   */
  String note() default "";
}
