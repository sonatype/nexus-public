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
 * Hides fields from the OpenAPI model unless a boolean feature flag is enabled.
 * <p>
 * Documentation only: it does not affect JSON serialization or deserialization, so code that reads or writes the field
 * must honour the flag itself. Independent of {@link EditionVisibility} and {@link DistributionVisibility}.
 * <p>
 * A flag that is unset is treated as {@code false}, so the field is hidden.
 */
@Target({FIELD})
@Retention(RUNTIME)
public @interface FeatureFlagVisibility
{
  /**
   * The property name of the boolean feature flag, e.g. {@code FeatureFlags.SOME_FEATURE_ENABLED}.
   */
  String value();

  /**
   * Specify a note about why this field is hidden. Not used at runtime
   */
  String note() default "";
}
