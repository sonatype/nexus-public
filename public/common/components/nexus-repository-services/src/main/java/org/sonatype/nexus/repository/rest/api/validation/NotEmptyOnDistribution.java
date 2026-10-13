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
package org.sonatype.nexus.repository.rest.api.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.sonatype.nexus.bootstrap.entrypoint.distribution.NexusDistributionType;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Validate that a value is not empty, on the deployments that genuinely require it.
 * <p>
 * A plain {@code @NotEmpty} applies everywhere, which is wrong for a field one deployment requires
 * and another supplies for itself. Requiring a value the server then discards forces callers to
 * send a placeholder, and if the field is also hidden from that deployment's OpenAPI document the
 * result is a request the document describes but the API rejects.
 * <p>
 * Name the deployments where the value is required:
 *
 * <pre>
 * &#64;NotEmptyOnDistribution(SELF_HOSTED)
 * private String blobStoreName;
 * </pre>
 * <p>
 * This is deliberately separate from {@code @DistributionVisibility} rather than a {@code required}
 * flag on it. The two answer different questions — what a caller may <em>see</em> versus what a
 * caller must <em>send</em> — and they can diverge, so a field may be visible yet optional. Folding
 * them together would forbid that, and would put validation into a shared annotation two other
 * squads consume, pulling {@code jakarta.validation} and {@code nexus-configuration} into
 * {@code nexus-swagger}. Keeping it here changes nothing shared. Where both apply, state both.
 * <p>
 * A distribution that cannot be resolved enforces the constraint: for validation the safe default is
 * the stricter one. If every deployment requires the value, use {@code @NotEmpty} instead.
 */
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = NotEmptyOnDistributionValidator.class)
@Documented
public @interface NotEmptyOnDistribution
{
  /**
   * The deployments on which the value is required. Deliberately has no default, so every use states
   * its intent.
   */
  NexusDistributionType[] value();

  String message() default "must not be empty";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
