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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.sonatype.nexus.bootstrap.validation.ValidationConfiguration;
import org.sonatype.nexus.testcommon.validation.ValidationExtension;
import org.sonatype.nexus.testcommon.validation.ValidationExtension.ValidationExecutor;
import org.sonatype.nexus.validation.group.Create;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorFactoryImpl;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ExtendWith(ValidationExtension.class)
class RepositoryXOTest
{
  @ValidationExecutor
  private final Validator validator =
      new ValidationConfiguration().validatorFactory(new ConstraintValidatorFactoryImpl()).getValidator();

  @Test
  void nameIsAlwaysRequired() {
    RepositoryXO repositoryXO = new RepositoryXO();
    repositoryXO.setAttributes(Map.of("any", Map.of("any", "any")));
    repositoryXO.setOnline(true);
    Set<ConstraintViolation<RepositoryXO>> violations = validator.validate(repositoryXO);
    assertThat(violations.size(), is(1));
    assertThat(violations.iterator().next().getPropertyPath().toString(), is("name"));
  }

  @ParameterizedTest
  @MethodSource("invalidAttributes")
  void attributesAreAlwaysRequiredAndCannotBeEmpty(final Map<String, Map<String, Object>> attributes) {
    RepositoryXO repositoryXO = new RepositoryXO();
    repositoryXO.setName("foo");
    repositoryXO.setOnline(true);
    repositoryXO.setAttributes(attributes);

    Set<ConstraintViolation<RepositoryXO>> violations = validator.validate(repositoryXO);
    assertThat(violations.size(), is(1));
    assertThat(violations.iterator().next().getPropertyPath().toString(), is("attributes"));
  }

  static Map<String, Object>[] invalidAttributes() {
    return new Map[]{Map.of(), null};
  }

  @ParameterizedTest
  @MethodSource("invalidNames")
  void nameShouldNotValidate(final String name) {
    RepositoryXO repositoryXO = new RepositoryXO();
    repositoryXO.setName(name);
    repositoryXO.setOnline(true);
    repositoryXO.setAttributes(Map.of("any", Map.of("any", "any")));
    Set<ConstraintViolation<RepositoryXO>> violations = validator.validate(repositoryXO);
    assertThat(violations.size(), is(1));
    assertThat(violations.iterator().next().getPropertyPath().toString(), is("name"));
  }

  static String[] invalidNames() {
    List<String> invalid = "#.,* #'\\/?<>| \r\n\t,+@&å©不βخ".chars()
        .mapToObj(c -> (char) c)
        .map(String::valueOf)
        .collect(Collectors.toList()); // NOSONAR
    invalid.add("_leadingUnderscore");
    invalid.add("..");
    return invalid.toArray(String[]::new);
  }

  @ParameterizedTest
  @ValueSource(strings = {"Foo_1.2-3", "foo.", "-0.", "a", "1"})
  void nameShouldBeValid(final String name) {
    RepositoryXO repositoryXO = new RepositoryXO();
    repositoryXO.setName(name);
    repositoryXO.setOnline(true);
    repositoryXO.setAttributes(Map.of("any", Map.of("any", "any")));
    Set<ConstraintViolation<RepositoryXO>> violations = validator.validate(repositoryXO);
    assertThat(violations.isEmpty(), is(true));
  }

  @Test
  void recipeFieldIsOnlyRequiredOnCreation() {
    RepositoryXO repositoryXO = new RepositoryXO();
    repositoryXO.setName("bob");
    repositoryXO.setAttributes(Map.of("any", Map.of("any", "any")));
    Set<ConstraintViolation<RepositoryXO>> violations = validator.validate(repositoryXO, Create.class);
    assertThat(violations.size(), is(1));
    assertThat(violations.iterator().next().getPropertyPath().toString(), is("recipe"));

    repositoryXO.setRecipe("any");
    violations = validator.validate(repositoryXO, Create.class);
    assertThat(violations.isEmpty(), is(true));
  }

  @Test
  @ValueSource(strings = {"Foo", "bAr", "baZ"})
  void nameShouldBeValidatedAsCaseInsensitivelyUniqueOnCreation(final String repoName) {
    RepositoryXO repositoryXO = new RepositoryXO();
    repositoryXO.setAttributes(Map.of("any", Map.of()));
    repositoryXO.setOnline(true);
    repositoryXO.setRecipe("any");
    repositoryXO.setName(repoName);

    Set<ConstraintViolation<RepositoryXO>> violations = validator.validate(repositoryXO, Create.class);
    assertThat(violations.size(), is(1));
    assertThat(violations.iterator().next().getPropertyPath().toString(), is("name"));
    assertThat(violations.iterator().next().getMessage(), is("Name is already used, must be unique (ignoring case)"));
  }
}
