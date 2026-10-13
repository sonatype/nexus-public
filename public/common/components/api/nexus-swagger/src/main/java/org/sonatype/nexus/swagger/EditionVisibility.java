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
 * Hides fields based on the <em>licensed edition</em>, independently of the deployment the instance runs as.
 * <p>
 * This is the REST-layer counterpart of {@code @ConditionalOnEdition}. The deployment axis lives in the separate
 * {@link DistributionVisibility} annotation; the two compose because hiding is one-way, so a field is visible only
 * where both annotations allow it. A field that only cares about one axis carries only that annotation.
 * <p>
 * Allows hiding fields from the OpenAPI model, to hide from JSON output use in conjunction with
 * {@code @JsonFilter(EditionVisibility.NAME)} on the containing object. Jackson permits a single filter id per class,
 * so that one filter reads both annotations and this id covers the deployment axis too.
 * <p>
 * All flags default to {@code true}. A field with no annotation is visible everywhere; a field carrying this
 * annotation when the edition cannot be resolved is hidden.
 */
@Target({FIELD})
@Retention(RUNTIME)
public @interface EditionVisibility
{
  public static final String NAME = "EditionVisibility";

  boolean community() default true;

  boolean enterprise() default true;

  boolean pro() default true;

  boolean proPlus() default true;

  /**
   * Specify a note about why this field is hidden. Not used at runtime
   */
  String note() default "";
}
