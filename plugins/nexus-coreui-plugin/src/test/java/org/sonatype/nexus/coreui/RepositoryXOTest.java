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

import com.google.inject.Guice;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import junitparams.JUnitParamsRunner;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.validation.ValidationModule;
import org.sonatype.nexus.validation.group.Create;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(JUnitParamsRunner.class)
public class RepositoryXOTest
    extends TestSupport
{
  private Validator validator;

  @Before
  public void setup() {
    validator =
        Guice.createInjector(new ValidationModule(), new TestRepositoryManagerModule())
            .getInstance(Validator.class);
  }

  @Test
  public void nameIsAlwaysRequired() {
    RepositoryXO repositoryXO = new RepositoryXO();
    repositoryXO.setAttributes(Map.of("any", Map.of("any", "any")));
    repositoryXO.setOnline(true);
    Set<ConstraintViolation<RepositoryXO>> violations = validator.validate(repositoryXO);
    assertThat(violations.size(), is(1));
    assertThat(violations.iterator().next().getPropertyPath().toString(), is("name"));
  }

  @Test
  @Parameters(method = "invalidAttributes")
  public void attributesAreAlwaysRequiredAndCannotBeEmpty(Map<String, Map<String, Object>> attributes) {
    RepositoryXO repositoryXO = new RepositoryXO();
    repositoryXO.setName("foo");
    repositoryXO.setOnline(true);
    repositoryXO.setAttributes(attributes);

    Set<ConstraintViolation<RepositoryXO>> violations = validator.validate(repositoryXO);
    assertThat(violations.size(), is(1));
    assertThat(violations.iterator().next().getPropertyPath().toString(), is("attributes"));
  }

  private Object[] invalidAttributes() {
    return new Object[]{null, Collections.emptyMap()};
  }

  @Test
  @Parameters(method = "invalidNames")
  public void nameShouldNotValidate(String name) {
    RepositoryXO repositoryXO = new RepositoryXO();
    repositoryXO.setName(name);
    repositoryXO.setOnline(true);
    repositoryXO.setAttributes(Map.of("any", Map.of("any", "any")));
    Set<ConstraintViolation<RepositoryXO>> violations = validator.validate(repositoryXO);
    assertThat(violations.size(), is(1));
    assertThat(violations.iterator().next().getPropertyPath().toString(), is("name"));
  }

  private Object[] invalidNames() {
    List<Object> noValid = new ArrayList<>("#.,* #'\\/?<>| \r\n\t,+@&å©不βخ".chars()
        .mapToObj(c -> (char) c)
        .collect(Collectors.toList())); // NOSONAR
    noValid.add("_leadingUnderscore");
    noValid.add("..");
    return noValid.toArray();
  }

  @Test
  @Parameters(method = "validNames")
  public void nameShouldBeValid(String name) {
    RepositoryXO repositoryXO = new RepositoryXO();
    repositoryXO.setName(name);
    repositoryXO.setOnline(true);
    repositoryXO.setAttributes(Map.of("any", Map.of("any", "any")));
    Set<ConstraintViolation<RepositoryXO>> violations = validator.validate(repositoryXO);
    assertThat(violations.isEmpty(), is(true));
  }

  private Object[] validNames() {
    return new Object[]{"Foo_1.2-3", "foo.", "-0.", "a", "1"};
  }

  @Test
  public void recipeFieldIsOnlyRequiredOnCreation() {
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
  @Parameters(method = "nonUniqueNames")
  public void nameShouldBeValidatedAsCaseInsensitivelyUniqueOnCreation(String repoName) {
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

  private Object[] nonUniqueNames() {
    return new Object[]{"Foo", "bAr", "baZ"};
  }
}
