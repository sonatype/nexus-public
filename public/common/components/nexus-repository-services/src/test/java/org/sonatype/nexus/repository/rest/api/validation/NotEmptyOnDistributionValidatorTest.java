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

import java.lang.annotation.Annotation;

import org.sonatype.nexus.bootstrap.entrypoint.distribution.NexusDistribution;
import org.sonatype.nexus.bootstrap.entrypoint.distribution.NexusDistributionType;

import jakarta.validation.Payload;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.sonatype.nexus.bootstrap.entrypoint.distribution.NexusDistributionType.CLOUD;
import static org.sonatype.nexus.bootstrap.entrypoint.distribution.NexusDistributionType.SELF_HOSTED;

class NotEmptyOnDistributionValidatorTest
{
  @AfterEach
  void tearDown() {
    NexusDistribution.clearTestOverride();
  }

  @Test
  void testRequiredOnANamedDistribution() {
    NotEmptyOnDistributionValidator underTest = validatorFor(SELF_HOSTED, SELF_HOSTED);

    assertTrue(underTest.isValid("default", null));
    assertFalse(underTest.isValid(null, null));
    assertFalse(underTest.isValid("", null));
    assertFalse(underTest.isValid("   ", null));
  }

  @Test
  void testNotRequiredOnAnUnnamedDistribution() {
    NotEmptyOnDistributionValidator underTest = validatorFor(CLOUD, SELF_HOSTED);

    assertTrue(underTest.isValid(null, null));
    assertTrue(underTest.isValid("", null));
    assertTrue(underTest.isValid("default", null));
  }

  @Test
  void testSeveralDistributionsCanBeNamed() {
    for (NexusDistributionType running : NexusDistributionType.values()) {
      NotEmptyOnDistributionValidator underTest = validatorFor(running, SELF_HOSTED, CLOUD);

      assertTrue(underTest.isValid("default", null), "value should be accepted on " + running);
      assertFalse(underTest.isValid(null, null), "null should be rejected on " + running);
      assertFalse(underTest.isValid("", null), "empty should be rejected on " + running);
    }
  }

  /**
   * An un-evaluable gate must not silently relax a constraint, so an unresolvable distribution enforces it even though
   * no distribution is named.
   */
  @Test
  void testUnresolvableDistributionEnforces() {
    NexusDistribution.clearTestOverride();
    NotEmptyOnDistributionValidator underTest = new NotEmptyOnDistributionValidator();
    underTest.initialize(annotation());

    assertFalse(underTest.isValid(null, null));
    assertTrue(underTest.isValid("default", null));
  }

  private static NotEmptyOnDistributionValidator validatorFor(
      final NexusDistributionType running,
      final NexusDistributionType... requiredOn)
  {
    NexusDistribution.setTestOverride(running);
    NotEmptyOnDistributionValidator validator = new NotEmptyOnDistributionValidator();
    validator.initialize(annotation(requiredOn));
    return validator;
  }

  private static NotEmptyOnDistribution annotation(final NexusDistributionType... requiredOn) {
    return new NotEmptyOnDistribution()
    {
      @Override
      public Class<? extends Annotation> annotationType() {
        return NotEmptyOnDistribution.class;
      }

      @Override
      public NexusDistributionType[] value() {
        return requiredOn;
      }

      @Override
      public String message() {
        return "must not be empty";
      }

      @Override
      public Class<?>[] groups() {
        return new Class<?>[0];
      }

      @Override
      public Class<? extends Payload>[] payload() {
        return newPayloadArray();
      }
    };
  }

  @SuppressWarnings("unchecked")
  private static Class<? extends Payload>[] newPayloadArray() {
    return new Class[0];
  }
}
