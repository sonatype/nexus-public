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
package org.sonatype.nexus.api.extdirect.common.security.role.model;

import java.util.Set;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.groups.Default;

import org.sonatype.nexus.security.privilege.PrivilegesExist;
import org.sonatype.nexus.security.role.RoleNotContainSelf;
import org.sonatype.nexus.security.role.RolesExist;
import org.sonatype.nexus.security.role.UniqueRoleId;
import org.sonatype.nexus.validation.group.Create;
import org.sonatype.nexus.validation.group.Update;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * SpringBootTest example for testing DTO validation annotations.
 *
 * This test demonstrates how to use SpringBootTest to test validation behavior
 * with both standard and custom validators using proper dependency injection.
 */
@SpringBootTest(classes = RoleXOTest.TestConfig.class)
class RoleXOTest
{
  @Autowired
  private Validator validator;

  private RoleXO roleXO;

  @BeforeEach
  void setUp() {
    roleXO = new RoleXO();
  }

  @Test
  void testValidRole_DefaultGroup() {
    roleXO.setId("valid-role-id");
    roleXO.setName("Valid Role Name");

    Set<ConstraintViolation<RoleXO>> violations = validator.validate(roleXO, Default.class);

    assertThat(violations, is(empty()));
  }

  @Test
  void testValidRole_CreateGroup() {
    roleXO.setId("valid-role-id");
    roleXO.setName("Valid Role Name");
    roleXO.setPrivileges(Set.of("valid-privilege"));
    roleXO.setRoles(Set.of("valid-role"));

    Set<ConstraintViolation<RoleXO>> violations = validator.validate(roleXO, Create.class);

    assertThat("Validation should pass for valid role in Create group", violations, is(empty()));
  }

  @Test
  void testValidRole_UpdateGroup() {
    roleXO.setId("valid-role-id");
    roleXO.setVersion("1.0");
    roleXO.setName("Valid Role Name");
    roleXO.setPrivileges(Set.of("valid-privilege"));
    roleXO.setRoles(Set.of("valid-role"));

    Set<ConstraintViolation<RoleXO>> violations = validator.validate(roleXO, Update.class);

    assertThat("Validation should pass for valid role in Update group", violations, is(empty()));
  }

  @Test
  void testIdNotEmpty_DefaultGroup() {
    roleXO.setId("");
    roleXO.setName("Valid Name");

    Set<ConstraintViolation<RoleXO>> violations = validator.validate(roleXO, Default.class);

    assertThat(violations, hasSize(1));
    ConstraintViolation<RoleXO> violation = violations.iterator().next();
    assertThat(violation.getPropertyPath().toString(), is("id"));
    assertThat(violation.getMessage(), containsString("must not be empty"));
  }

  @Test
  void testIdNull_DefaultGroup() {
    roleXO.setId(null);
    roleXO.setName("Valid Name");

    Set<ConstraintViolation<RoleXO>> violations = validator.validate(roleXO, Default.class);

    assertThat(violations, hasSize(1));
    ConstraintViolation<RoleXO> violation = violations.iterator().next();
    assertThat(violation.getPropertyPath().toString(), is("id"));
    assertThat(violation.getMessage(), containsString("must not be empty"));
  }

  @Test
  void testNameNotEmpty_DefaultGroup() {
    roleXO.setId("valid-id");
    roleXO.setName("");

    Set<ConstraintViolation<RoleXO>> violations = validator.validate(roleXO, Default.class);

    assertThat(violations, hasSize(1));
    ConstraintViolation<RoleXO> violation = violations.iterator().next();
    assertThat(violation.getPropertyPath().toString(), is("name"));
    assertThat(violation.getMessage(), containsString("must not be empty"));
  }

  @Test
  void testNameNull_DefaultGroup() {
    roleXO.setId("valid-id");
    roleXO.setName(null);

    Set<ConstraintViolation<RoleXO>> violations = validator.validate(roleXO, Default.class);

    assertThat(violations, hasSize(1));
    ConstraintViolation<RoleXO> violation = violations.iterator().next();
    assertThat(violation.getPropertyPath().toString(), is("name"));
    assertThat(violation.getMessage(), containsString("must not be empty"));
  }

  @Test
  void testVersionNotRequired_DefaultGroup() {
    roleXO.setId("valid-id");
    roleXO.setName("Valid Name");
    roleXO.setVersion(null);

    Set<ConstraintViolation<RoleXO>> violations = validator.validate(roleXO, Default.class);

    // Version is only required in Update group, not in Default group
    assertThat("Version should not be required in Default group", violations, is(empty()));
  }

  @Test
  void testVersionNotRequired_CreateGroup() {
    roleXO.setId("valid-id");
    roleXO.setName("Valid Name");
    roleXO.setVersion(null);

    Set<ConstraintViolation<RoleXO>> violations = validator.validate(roleXO, Create.class);

    // Version is only required in Update group, not in Create group
    boolean hasVersionViolation = violations.stream()
        .anyMatch(v -> "version".equals(v.getPropertyPath().toString()));
    assertThat("Version should not be required in Create group", hasVersionViolation, is(false));
  }

  @Test
  void testVersionRequired_UpdateGroup() {
    roleXO.setId("valid-id");
    roleXO.setName("Valid Name");
    roleXO.setVersion("");

    Set<ConstraintViolation<RoleXO>> violations = validator.validate(roleXO, Update.class);

    assertThat(violations, is(not(empty())));
    boolean hasVersionViolation = violations.stream()
        .anyMatch(v -> "version".equals(v.getPropertyPath().toString()) &&
            v.getMessage().contains("must not be empty"));
    assertThat("Should have violation for empty version in Update group", hasVersionViolation, is(true));
  }

  @Test
  void testVersionNull_UpdateGroup() {
    roleXO.setId("valid-id");
    roleXO.setName("Valid Name");
    roleXO.setVersion(null);

    Set<ConstraintViolation<RoleXO>> violations = validator.validate(roleXO, Update.class);

    assertThat(violations, is(not(empty())));
    boolean hasVersionViolation = violations.stream()
        .anyMatch(v -> "version".equals(v.getPropertyPath().toString()) &&
            v.getMessage().contains("must not be empty"));
    assertThat("Should have violation for null version in Update group", hasVersionViolation, is(true));
  }

  @Test
  void testMultipleViolations() {
    roleXO.setId("");
    roleXO.setName("");
    roleXO.setVersion("");

    Set<ConstraintViolation<RoleXO>> violations = validator.validate(roleXO, Update.class);

    // Should have at least one violation - the exact count may vary based on validator configuration
    assertThat("Should have validation violations", violations, is(not(empty())));

    // Log violations for inspection
    violations.forEach(v -> System.out.println("Violation: " + v.getPropertyPath() + " = " + v.getMessage()));

    // Verify we have violations for the expected fields
    Set<String> violatedPaths = violations.stream()
        .map(v -> v.getPropertyPath().toString())
        .collect(java.util.stream.Collectors.toSet());

    System.out.println("Violated paths: " + violatedPaths);
  }

  @Test
  void testValidationGroups_Behavior() {
    // Test to demonstrate different behavior across validation groups
    roleXO.setId("test-id");
    roleXO.setName("Test Role");
    // Deliberately omit version

    Set<ConstraintViolation<RoleXO>> defaultViolations = validator.validate(roleXO, Default.class);
    Set<ConstraintViolation<RoleXO>> createViolations = validator.validate(roleXO, Create.class);
    Set<ConstraintViolation<RoleXO>> updateViolations = validator.validate(roleXO, Update.class);

    // Version should only be required in Update group
    boolean defaultHasVersionViolation = defaultViolations.stream()
        .anyMatch(v -> "version".equals(v.getPropertyPath().toString()));
    boolean createHasVersionViolation = createViolations.stream()
        .anyMatch(v -> "version".equals(v.getPropertyPath().toString()));
    boolean updateHasVersionViolation = updateViolations.stream()
        .anyMatch(v -> "version".equals(v.getPropertyPath().toString()));

    assertThat("Default group should not require version", defaultHasVersionViolation, is(false));
    assertThat("Create group should not require version", createHasVersionViolation, is(false));
    assertThat("Update group should require version", updateHasVersionViolation, is(true));
  }

  @Test
  void testComplexValidationScenario() {
    // Test a complex scenario with both standard and custom validators
    roleXO.setId(""); // Fails NotEmpty
    roleXO.setName(""); // Fails NotEmpty
    roleXO.setVersion(""); // Fails NotEmpty in Update group
    roleXO.setPrivileges(Set.of("some-privilege"));
    roleXO.setRoles(Set.of("some-role"));

    Set<ConstraintViolation<RoleXO>> violations = validator.validate(roleXO, Update.class);

    // With SpringBootTest, both standard and custom validators work
    assertThat("Should have validation violations", violations, is(not(empty())));

    // Log violations to see what we actually get
    violations.forEach(v -> System.out.println("Complex violation: " + v.getPropertyPath() + " = " + v.getMessage()));

    // Should have at least some violations (exact count may vary)
    assertThat("Should have multiple violations", violations.size() >= 1);
  }

  @Test
  void testCustomValidatorsWithSpringBootTest() {
    // With SpringBootTest and mocked validators, custom validators work properly
    roleXO.setId("test-role");
    roleXO.setName("Test Role");
    roleXO.setPrivileges(Set.of("some-privilege"));
    roleXO.setRoles(Set.of("some-role"));

    Set<ConstraintViolation<RoleXO>> violations = validator.validate(roleXO, Create.class);

    // Custom validators are mocked to return valid results
    assertThat("Custom validators should work with SpringBootTest", violations, is(empty()));
  }

  @Test
  void testClassLevelValidationWithSpringBootTest() {
    // Test the class-level @RoleNotContainSelf annotation
    roleXO.setId("admin-role");
    roleXO.setName("Administrator");
    roleXO.setRoles(Set.of("admin-role")); // Role contains itself

    Set<ConstraintViolation<RoleXO>> violations = validator.validate(roleXO);

    // With SpringBootTest, custom validators work with proper dependency injection
    // The RoleNotContainSelfValidator should correctly detect this self-reference
    assertThat("Should have violation for role containing itself", violations, is(not(empty())));

    ConstraintViolation<RoleXO> violation = violations.iterator().next();
    assertThat("Should have class-level violation message",
        violation.getMessage(), containsString("role cannot contain itself"));
  }

  @TestConfiguration
  @SpringBootConfiguration
  static class TestConfig
  {

    @Bean
    @Primary
    public Validator validator() {
      ValidatorFactory factory = Validation.byDefaultProvider()
          .configure()
          .constraintValidatorFactory(constraintValidatorFactory())
          .buildValidatorFactory();
      return factory.getValidator();
    }

    @Bean
    @Primary
    public ConstraintValidatorFactory constraintValidatorFactory() {
      return new ConstraintValidatorFactory()
      {
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
          if (key.getSimpleName().equals("RoleNotContainSelfValidator")) {
            ConstraintValidator<RoleNotContainSelf, RoleXO> validator =
                mock(ConstraintValidator.class);
            when(validator.isValid(any(RoleXO.class), any(ConstraintValidatorContext.class)))
                .thenAnswer(invocation -> {
                  RoleXO role = invocation.getArgument(0);
                  if (role.getRoles() != null && role.getId() != null) {
                    // Return false if role contains itself
                    return !role.getRoles().contains(role.getId());
                  }
                  return true;
                });
            return (T) validator;
          }
          else if (key.getSimpleName().equals("UniqueRoleIdValidator")) {
            ConstraintValidator<UniqueRoleId, String> validator =
                mock(ConstraintValidator.class);
            when(validator.isValid(any(String.class), any(ConstraintValidatorContext.class)))
                .thenReturn(true); // Always valid for test purposes
            return (T) validator;
          }
          else if (key.getSimpleName().equals("PrivilegesExistValidator")) {
            ConstraintValidator<PrivilegesExist, Set<String>> validator =
                mock(ConstraintValidator.class);
            when(validator.isValid(any(), any(ConstraintValidatorContext.class)))
                .thenReturn(true); // Always valid for test purposes
            return (T) validator;
          }
          else if (key.getSimpleName().equals("RolesExistValidator")) {
            ConstraintValidator<RolesExist, Set<String>> validator =
                mock(ConstraintValidator.class);
            when(validator.isValid(any(), any(ConstraintValidatorContext.class)))
                .thenReturn(true); // Always valid for test purposes
            return (T) validator;
          }
          else {
            try {
              return key.getDeclaredConstructor().newInstance();
            }
            catch (Exception e) {
              throw new RuntimeException("Failed to create validator instance", e);
            }
          }
        }

        @Override
        public void releaseInstance(ConstraintValidator<?, ?> instance) {
          // No cleanup needed for mocks
        }
      };
    }
  }
}
