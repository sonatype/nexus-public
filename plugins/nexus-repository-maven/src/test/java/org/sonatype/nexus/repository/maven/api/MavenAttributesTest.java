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
package org.sonatype.nexus.repository.maven.api;

import java.util.Arrays;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.sonatype.nexus.repository.maven.ContentDisposition;
import org.sonatype.nexus.repository.maven.LayoutPolicy;
import org.sonatype.nexus.repository.maven.VersionPolicy;

public class MavenAttributesTest
{
  private Validator validator;

  private static final String[] VALID_VERSION_POLICIES = Arrays.stream(VersionPolicy.values())
      .map(Enum::toString)
      .toArray(String[]::new);

  private static final String[] VALID_LAYOUT_POLICIES = Arrays.stream(LayoutPolicy.values())
      .map(Enum::toString)
      .toArray(String[]::new);

  private static final String[] VALID_CONTENT_DISPOSITIONS = Arrays.stream(ContentDisposition.values())
      .map(Enum::toString)
      .toArray(String[]::new);

  private static final String VERSION_POLICY_ERROR_MSG = "must be one of RELEASE, SNAPSHOT, MIXED";

  private static final String LAYOUT_POLICY_ERROR_MSG = "must be one of STRICT, PERMISSIVE";

  private static final String CONTENT_DISPOSITION_ERROR_MSG = "must be one of INLINE, ATTACHMENT";

  private static final String EMPTY_ERROR_MSG = "must not be empty";

  @Before
  public void setUp() {
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      validator = factory.getValidator();
    }
  }

  @Test
  public void testConstructorAndGetters() {
    String versionPolicy = VALID_VERSION_POLICIES[0];
    String layoutPolicy = VALID_LAYOUT_POLICIES[0];
    String contentDisposition = VALID_CONTENT_DISPOSITIONS[0];

    MavenAttributes mavenAttributes = new MavenAttributes(versionPolicy, layoutPolicy, contentDisposition);

    assertEquals(versionPolicy, mavenAttributes.getVersionPolicy());
    assertEquals(layoutPolicy, mavenAttributes.getLayoutPolicy());
    assertEquals(contentDisposition, mavenAttributes.getContentDisposition());
  }

  @Test
  public void testValidVersionPolicy() {
    for (String validVersionPolicy : VALID_VERSION_POLICIES) {
      MavenAttributes attributes =
          new MavenAttributes(validVersionPolicy, VALID_LAYOUT_POLICIES[0], VALID_CONTENT_DISPOSITIONS[0]);
      assertTrue(isValid(attributes));
    }
  }

  @Test
  public void testValidLayoutPolicy() {
    for (String validLayoutPolicy : VALID_LAYOUT_POLICIES) {
      MavenAttributes attributes =
          new MavenAttributes(VALID_VERSION_POLICIES[0], validLayoutPolicy, VALID_CONTENT_DISPOSITIONS[0]);
      assertTrue(isValid(attributes));
    }
  }

  @Test
  public void testValidContentDisposition() {
    for (String validContentDisposition : VALID_CONTENT_DISPOSITIONS) {
      MavenAttributes attributes =
          new MavenAttributes(VALID_VERSION_POLICIES[0], VALID_LAYOUT_POLICIES[0], validContentDisposition);
      assertTrue(isValid(attributes));
    }
  }

  @Test
  public void testInvalidVersionPolicy() {
    MavenAttributes attributes =
        new MavenAttributes("invalid", VALID_LAYOUT_POLICIES[0], VALID_CONTENT_DISPOSITIONS[0]);
    assertFalse(isValid(attributes));
    assertTrue(hasMessage(attributes, VERSION_POLICY_ERROR_MSG));
  }

  @Test
  public void testInvalidLayoutPolicy() {
    MavenAttributes attributes =
        new MavenAttributes(VALID_VERSION_POLICIES[0], "invalid", VALID_CONTENT_DISPOSITIONS[0]);
    assertFalse(isValid(attributes));
    assertTrue(hasMessage(attributes, LAYOUT_POLICY_ERROR_MSG));
  }

  @Test
  public void testInvalidContentDisposition() {
    MavenAttributes attributes =
        new MavenAttributes(VALID_VERSION_POLICIES[0], VALID_LAYOUT_POLICIES[0], "invalid");
    assertFalse(isValid(attributes));
    assertTrue(hasMessage(attributes, CONTENT_DISPOSITION_ERROR_MSG));
  }

  @Test
  public void testEmptyVersionPolicy() {
    MavenAttributes attributes = new MavenAttributes("", VALID_LAYOUT_POLICIES[0], VALID_CONTENT_DISPOSITIONS[0]);
    assertFalse(isValid(attributes));
    assertTrue(hasMessage(attributes, EMPTY_ERROR_MSG));
  }

  @Test
  public void testEmptyLayoutPolicy() {
    MavenAttributes attributes = new MavenAttributes(VALID_VERSION_POLICIES[0], "", VALID_CONTENT_DISPOSITIONS[0]);
    assertFalse(isValid(attributes));
    assertTrue(hasMessage(attributes, EMPTY_ERROR_MSG));
  }

  @Test
  public void testEmptyContentDisposition() {
    MavenAttributes attributes = new MavenAttributes(VALID_VERSION_POLICIES[0], VALID_LAYOUT_POLICIES[0], "");
    assertFalse(isValid(attributes));
  }

  @Test
  public void testNullVersionPolicy() {
    MavenAttributes attributes = new MavenAttributes(null, VALID_LAYOUT_POLICIES[0], VALID_CONTENT_DISPOSITIONS[0]);
    assertFalse(isValid(attributes));
    assertTrue(hasMessage(attributes, EMPTY_ERROR_MSG));
  }

  @Test
  public void testNullLayoutPolicy() {
    MavenAttributes attributes = new MavenAttributes(VALID_VERSION_POLICIES[0], null, VALID_CONTENT_DISPOSITIONS[0]);
    assertFalse(isValid(attributes));
    assertTrue(hasMessage(attributes, EMPTY_ERROR_MSG));
  }

  @Test
  public void testNullContentDisposition() {
    MavenAttributes attributes = new MavenAttributes(VALID_VERSION_POLICIES[0], VALID_LAYOUT_POLICIES[0], null);
    assertTrue(isValid(attributes));
  }

  private boolean isValid(final MavenAttributes attributes) {
    Set<ConstraintViolation<MavenAttributes>> violations = validator.validate(attributes);
    return violations.isEmpty();
  }

  private boolean hasMessage(final MavenAttributes attributes, String errorMessage) {
    Set<ConstraintViolation<MavenAttributes>> violations = validator.validate(attributes);
    for (ConstraintViolation<MavenAttributes> violation : violations) {
      if (violation.getMessage().equals(errorMessage)) {
        return true;
      }
    }
    return false;
  }
}
