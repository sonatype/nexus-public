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
package org.sonatype.nexus.validation;

import javax.validation.ConstraintViolation;
import javax.validation.Valid;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.NotNull;

import org.junit.Before;
import org.junit.Test;
import org.sonatype.goodies.testsupport.TestSupport;

import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

public class BeanValidationTrialTest
    extends TestSupport
{

  private ValidatorFactory factory;

  @Before
  public void setUp() {
    factory = Validation.buildDefaultValidatorFactory();
  }

  private static class Parent
  {
    @Valid
    private Child child;

    public Parent(Child child) {
      this.child = child;
    }
  }

  private static class Child
  {
    @Valid
    private GrandChild grandChild;

    public Child(GrandChild grandChild) {
      this.grandChild = grandChild;
    }
  }

  private static class GrandChild
  {
    @NotNull
    private String name;

    public GrandChild(String name) {
      this.name = name;
    }
  }

  @Test
  public void shouldValidateHierarchy() {
    Parent parent = new Parent(new Child(new GrandChild(null)));
    Set<ConstraintViolation<Parent>> violations = factory.getValidator().validate(parent);
    assertThat(violations, hasSize(1));
    ConstraintViolation<Parent> violation = violations.iterator().next();
    assertThat(violation.getPropertyPath().toString(), equalTo("child.grandChild.name"));
    assertThat(violation.getMessage(), equalTo("must not be null"));
  }

  private static class WithMap
  {
    @Valid
    private Map<String, Object> contents;

    public WithMap(Map<String, Object> contents) {
      this.contents = contents;
    }
  }

  @Test
  public void shouldValidateWithMap() {
    Parent parent = new Parent(new Child(new GrandChild(null)));
    WithMap withMap = new WithMap(Map.of("foo", parent));
    Set<ConstraintViolation<WithMap>> violations = factory.getValidator().validate(withMap);
    assertThat(violations, hasSize(1));
    ConstraintViolation<WithMap> violation = violations.iterator().next();
    assertThat(violation.getPropertyPath().toString(), equalTo("contents[foo].child.grandChild.name"));
    assertThat(violation.getMessage(), equalTo("must not be null"));
  }
}
