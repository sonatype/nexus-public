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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorFactory;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.sonatype.nexus.repository.config.UniqueRepositoryName;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.validation.group.Create;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Tests for Docker repository name validation in RepositoryXO.
 *
 * <p>
 * The {@code @DockerRepositoryNameConstraint} on {@link RepositoryXO} is scoped to {@link Create},
 * so the lowercase requirement only applies on creation. These tests validate with
 * the {@code Create} group explicitly to assert the constraint fires. Updates (the default group)
 * do not trigger the constraint, which is how existing mixed-case repositories remain editable.
 *
 * <p>
 * A custom {@link ConstraintValidatorFactory} supplies a no-op {@code @UniqueRepositoryName}
 * validator (its real validator requires a Spring-injected {@code RepositoryManager}); only Docker
 * name violations are asserted on. The {@code withMock} tests exercise the validator directly and
 * are independent of validation groups.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class DockerRepositoryNameValidatorTest
{
  private static final String LOWERCASE_MESSAGE =
      "Docker repository names must be lowercase to support path-based routing";

  private Validator validator;

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private ConstraintValidatorContext context;

  @Before
  public void setUp() {
    ValidatorFactory factory = Validation.byDefaultProvider()
        .configure()
        .constraintValidatorFactory(new TestConstraintValidatorFactory())
        .buildValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  public void testDockerHosted_lowercaseName_isValidOnCreate() {
    RepositoryXO xo = createRepository("my-docker-repo", "docker-hosted", "docker");
    assertThat("Lowercase Docker repository name should be valid on create",
        dockerNameMessages(validator.validate(xo, Create.class)), is(empty()));
  }

  @Test
  public void testDockerHosted_uppercaseName_isInvalidOnCreate() {
    RepositoryXO xo = createRepository("MY-DOCKER-REPO", "docker-hosted", "docker");
    assertThat("Uppercase Docker repository name should be invalid on create",
        dockerNameMessages(validator.validate(xo, Create.class)), contains(LOWERCASE_MESSAGE));
  }

  @Test
  public void testDockerHosted_mixedCaseName_isInvalidOnCreate() {
    RepositoryXO xo = createRepository("MyDockerRepo", "docker-hosted", "docker");
    assertThat("Mixed case Docker repository name should be invalid on create",
        dockerNameMessages(validator.validate(xo, Create.class)), contains(LOWERCASE_MESSAGE));
  }

  @Test
  public void testDockerHosted_mixedCaseName_isValidOnUpdate() {
    // The default (update) group must NOT trigger the lowercase constraint,
    // so existing mixed-case repositories remain editable.
    RepositoryXO xo = createRepository("MyDockerRepo", "docker-hosted", "docker");
    assertThat("Mixed case Docker repository name should be valid on update (default group)",
        dockerNameMessages(validator.validate(xo)), is(empty()));
  }

  @Test
  public void testDockerProxy_mixedCaseName_isInvalidOnCreate() {
    RepositoryXO xo = createRepository("MyDockerProxy", "docker-proxy", "docker");
    assertThat("Mixed case Docker proxy repository name should be invalid on create",
        dockerNameMessages(validator.validate(xo, Create.class)), contains(LOWERCASE_MESSAGE));
  }

  @Test
  public void testDockerGroup_mixedCaseName_isInvalidOnCreate() {
    RepositoryXO xo = createRepository("MyDockerGroup", "docker-group", "docker");
    assertThat("Mixed case Docker group repository name should be invalid on create",
        dockerNameMessages(validator.validate(xo, Create.class)), contains(LOWERCASE_MESSAGE));
  }

  @Test
  public void testNonDockerRepository_mixedCaseName_isValidOnCreate() {
    RepositoryXO xo = createRepository("MyMavenRepo", "maven2-hosted", "maven2");
    assertThat("Mixed case non-Docker repository name should be valid (validation only for Docker)",
        dockerNameMessages(validator.validate(xo, Create.class)), is(empty()));
  }

  @Test
  public void testDockerRepository_withFormatOnly_mixedCaseName_isInvalidOnCreate() {
    RepositoryXO xo = createRepository("MyDockerRepo", null, "docker");
    assertThat("Mixed case Docker repository (format=docker, no recipe) should be invalid on create",
        dockerNameMessages(validator.validate(xo, Create.class)), contains(LOWERCASE_MESSAGE));
  }

  @Test
  public void testDockerRepository_withRecipeOnly_mixedCaseName_isInvalidOnCreate() {
    RepositoryXO xo = createRepository("MyDockerRepo", "docker-hosted", null);
    assertThat("Mixed case Docker repository (recipe=docker-hosted, no format) should be invalid on create",
        dockerNameMessages(validator.validate(xo, Create.class)), contains(LOWERCASE_MESSAGE));
  }

  @Test
  public void testDockerRepository_withNumbersAndHyphens_lowercase_isValidOnCreate() {
    RepositoryXO xo = createRepository("my-docker-repo-123", "docker-hosted", "docker");
    assertThat("Lowercase Docker repository name with numbers and hyphens should be valid on create",
        dockerNameMessages(validator.validate(xo, Create.class)), is(empty()));
  }

  @Test
  public void testDockerRepository_withSingleUppercaseLetter_isInvalidOnCreate() {
    RepositoryXO xo = createRepository("myDockerrepo", "docker-hosted", "docker");
    assertThat("Docker repository name with single uppercase letter should be invalid on create",
        dockerNameMessages(validator.validate(xo, Create.class)), contains(LOWERCASE_MESSAGE));
  }

  @Test
  public void testNullName_isValidOnCreate() {
    RepositoryXO xo = createRepository(null, "docker-hosted", "docker");
    // Null name will be caught by other validators (NotEmpty), not by Docker name validator
    assertThat("Null name should not trigger Docker validation",
        dockerNameMessages(validator.validate(xo, Create.class)), is(empty()));
  }

  @Test
  public void testNullRepositoryXO_isValid() {
    // This test verifies the validator handles null gracefully
    DockerRepositoryNameValidator validator = new DockerRepositoryNameValidator();
    boolean result = validator.isValid(null, null);
    assertThat("Null RepositoryXO should be valid (handled gracefully)", result, is(true));
  }

  @Test
  public void testDockerRepository_caseInsensitiveFormatCheck() {
    RepositoryXO xo = createRepository("MyDockerRepo", "docker-hosted", "DOCKER");
    assertThat("Format check should be case-insensitive, mixed case name should be invalid on create",
        dockerNameMessages(validator.validate(xo, Create.class)), contains(LOWERCASE_MESSAGE));
  }

  @Test
  public void testDockerRepository_caseInsensitiveRecipeCheck() {
    RepositoryXO xo = createRepository("MyDockerRepo", "DOCKER-hosted", "maven2");
    assertThat("Recipe check should be case-insensitive, mixed case name should be invalid on create",
        dockerNameMessages(validator.validate(xo, Create.class)), contains(LOWERCASE_MESSAGE));
  }

  private static List<String> dockerNameMessages(final Set<ConstraintViolation<RepositoryXO>> violations) {
    return violations.stream()
        .map(ConstraintViolation::getMessage)
        .filter(LOWERCASE_MESSAGE::equals)
        .collect(Collectors.toList());
  }

  private RepositoryXO createRepository(String name, String recipe, String format) {
    RepositoryXO xo = new RepositoryXO();
    xo.setName(name);
    xo.setRecipe(recipe);
    xo.setFormat(format);
    xo.setOnline(true);

    // Attributes must not be empty - add a dummy entry
    HashMap<String, Map<String, Object>> attributes = new HashMap<>();
    attributes.put("storage", new HashMap<>());
    xo.setAttributes(attributes);
    return xo;
  }

  // Unit tests with mocked RepositoryManager for testing existing repository exemption

  @Test
  public void testMixedCaseName_newRepository_isInvalid_withMock() {
    DockerRepositoryNameValidator validatorWithMock = new DockerRepositoryNameValidator(repositoryManager);
    RepositoryXO xo = createRepository("MyDockerRepo", "docker-hosted", "docker");

    when(repositoryManager.exists("MyDockerRepo")).thenReturn(false);

    boolean result = validatorWithMock.isValid(xo, context);

    assertThat("Mixed case name for NEW repository should be invalid", result, is(false));
    verify(repositoryManager).exists("MyDockerRepo");
  }

  @Test
  public void testMixedCaseName_existingRepository_isValid_withMock() {
    DockerRepositoryNameValidator validatorWithMock = new DockerRepositoryNameValidator(repositoryManager);
    RepositoryXO xo = createRepository("MyDockerRepo", "docker-hosted", "docker");

    when(repositoryManager.exists("MyDockerRepo")).thenReturn(true);

    boolean result = validatorWithMock.isValid(xo, context);

    assertThat("Mixed case name for EXISTING repository should be valid (exempted)", result, is(true));
    verify(repositoryManager).exists("MyDockerRepo");
  }

  @Test
  public void testUppercaseName_existingRepository_isValid_withMock() {
    DockerRepositoryNameValidator validatorWithMock = new DockerRepositoryNameValidator(repositoryManager);
    RepositoryXO xo = createRepository("MYDOCKERREPO", "docker-hosted", "docker");

    when(repositoryManager.exists("MYDOCKERREPO")).thenReturn(true);

    boolean result = validatorWithMock.isValid(xo, context);

    assertThat("Uppercase name for EXISTING repository should be valid (exempted)", result, is(true));
    verify(repositoryManager).exists("MYDOCKERREPO");
  }

  @Test
  public void testRepositoryManagerException_failsSafeAndAllows_withMock() {
    DockerRepositoryNameValidator validatorWithMock = new DockerRepositoryNameValidator(repositoryManager);
    RepositoryXO xo = createRepository("MyDockerRepo", "docker-hosted", "docker");

    when(repositoryManager.exists("MyDockerRepo")).thenThrow(new RuntimeException("Database error"));

    boolean result = validatorWithMock.isValid(xo, context);

    assertThat("When RepositoryManager throws exception, validator should fail safe and allow", result, is(true));
    verify(repositoryManager).exists("MyDockerRepo");
  }

  @Test
  public void testLowercaseName_doesNotCheckRepository_withMock() {
    DockerRepositoryNameValidator validatorWithMock = new DockerRepositoryNameValidator(repositoryManager);
    RepositoryXO xo = createRepository("my-docker-repo", "docker-hosted", "docker");

    boolean result = validatorWithMock.isValid(xo, context);

    assertThat("Lowercase name should be valid without checking repository existence", result, is(true));
    verify(repositoryManager, never()).exists("my-docker-repo");
  }

  @Test
  public void testNonDockerRepository_doesNotCheckRepository_withMock() {
    DockerRepositoryNameValidator validatorWithMock = new DockerRepositoryNameValidator(repositoryManager);
    RepositoryXO xo = createRepository("MyMavenRepo", "maven2-hosted", "maven2");

    boolean result = validatorWithMock.isValid(xo, context);

    assertThat("Non-Docker repository should be valid without checking repository existence", result, is(true));
    verify(repositoryManager, never()).exists("MyMavenRepo");
  }

  /**
   * Supplies the real {@code @DockerRepositoryNameConstraint} validator and a permissive stub for
   * {@link UniqueRepositoryName} (whose real validator needs a Spring-injected RepositoryManager).
   */
  private static class TestConstraintValidatorFactory
      implements ConstraintValidatorFactory
  {
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ConstraintValidator<?, ?>> T getInstance(final Class<T> key) {
      if (isUniqueRepositoryNameValidator(key)) {
        return (T) new AlwaysValidValidator();
      }
      try {
        return key.getDeclaredConstructor().newInstance();
      }
      catch (ReflectiveOperationException e) {
        throw new IllegalStateException("Unable to instantiate validator: " + key, e);
      }
    }

    @Override
    public void releaseInstance(final ConstraintValidator<?, ?> instance) {
      // no-op
    }

    private static boolean isUniqueRepositoryNameValidator(final Class<?> key) {
      return key.getName().endsWith("UniqueRepositoryNameValidator");
    }
  }

  /**
   * Stub that approves any value, standing in for the Spring-managed UniqueRepositoryNameValidator.
   */
  private static class AlwaysValidValidator
      implements ConstraintValidator<UniqueRepositoryName, String>
  {
    @Override
    public boolean isValid(final String value, final ConstraintValidatorContext context) {
      return true;
    }
  }
}
