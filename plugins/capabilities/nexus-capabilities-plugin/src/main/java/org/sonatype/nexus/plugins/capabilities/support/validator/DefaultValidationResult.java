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
package org.sonatype.nexus.plugins.capabilities.support.validator;

import java.util.Set;

import org.sonatype.nexus.plugins.capabilities.ValidationResult;

import com.google.common.collect.Sets;

/**
 * Default {@link ValidationResult} implementation.
 *
 * @since capabilities 2.0
 */
public class DefaultValidationResult
    implements ValidationResult
{

  private Set<Violation> violations;

  public DefaultValidationResult() {
    violations = Sets.newHashSet();
  }

  @Override
  public boolean isValid() {
    return violations.isEmpty();
  }

  @Override
  public Set<Violation> violations() {
    return violations;
  }

  public DefaultValidationResult add(final Violation violation) {
    violations().add(violation);

    return this;
  }

  public void add(final Set<Violation> violations) {
    violations().addAll(violations);
  }

  public DefaultValidationResult add(final String key, final String message) {
    return add(new DefaultViolation(key, message));
  }

  public DefaultValidationResult add(final String message) {
    return add(new DefaultViolation(message));
  }

}
