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
package org.sonatype.nexus.coreui;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Validates that Docker repository names are lowercase to support path-based routing.
 * Docker specification requires repository names to be lowercase.
 *
 * <p>
 * <strong>Security Rationale:</strong> This constraint is scoped to the {@code Create} validation group.
 * The lowercase requirement is enforced on creation to prevent ambiguous routing paths
 * that could be exploited for path traversal or URL confusion attacks. Existing mixed-case repositories
 * (created before this rule) are exempt on update to maintain backward compatibility; these cannot
 * enable path-based routing without renaming (enforced separately in DockerConnectorFacetImpl).
 */
@Target({TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = DockerRepositoryNameValidator.class)
@Documented
public @interface DockerRepositoryNameConstraint
{
  String message() default "Docker repository names must be lowercase to support path-based routing";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
