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
 * Hides fields based on the <em>deployment</em> the instance runs as, independently of its licensed edition.
 * <p>
 * This is the REST-layer counterpart of {@code @ConditionalOnDistribution}, and is deliberately a separate annotation
 * from {@link EditionVisibility} so that the two axes compose without multiplying: a field that only cares where it
 * runs carries only this annotation, and a field that only cares what is licensed carries only the edition one. Where
 * both apply, both are present and either can hide the field.
 * <p>
 * To hide from JSON output as well as the OpenAPI model, the containing class needs
 * {@code @JsonFilter(EditionVisibility.NAME)} — Jackson permits a single filter id per class, so one filter reads both
 * annotations and that id covers this axis too.
 * <p>
 * Both flags default to {@code true}. A field with no annotation is visible everywhere; a field carrying this
 * annotation when the distribution cannot be resolved is hidden.
 */
@Target({FIELD})
@Retention(RUNTIME)
public @interface DistributionVisibility
{
  boolean cloud() default true;

  boolean selfhosted() default true;

  /**
   * Specify a note about why this field is hidden. Not used at runtime
   */
  String note() default "";
}
