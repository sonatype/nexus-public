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
package org.sonatype.nexus.api.rest.selfhosted.blobstore.file;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import org.sonatype.nexus.api.rest.common.blobstore.file.model.FileBlobStoreApiCreateRequest;
import org.sonatype.nexus.bootstrap.validation.ValidationConfiguration;
import org.sonatype.nexus.testcommon.validation.ValidationExtension;
import org.sonatype.nexus.testcommon.validation.ValidationExtension.ValidationExecutor;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorFactoryImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;

/**
 * Tests for validating FileBlobStoreResource endpoint validation.
 */
@ExtendWith(ValidationExtension.class)
class FileBlobStoreResourceValidationTest
{
  @ValidationExecutor
  private final Validator validator =
      new ValidationConfiguration().validatorFactory(new ConstraintValidatorFactoryImpl()).getValidator();

  @Test
  void testCreateRequestRejectsNamesStartingWithUnderscore() {
    FileBlobStoreApiCreateRequest request = new FileBlobStoreApiCreateRequest();
    request.setName("_invalidName");
    request.setPath("/tmp/blobstore");

    Set<ConstraintViolation<FileBlobStoreApiCreateRequest>> violations = validator.validate(request);

    assertThat(violations.size(), is(greaterThanOrEqualTo(1)));
    boolean hasNameViolation = violations.stream()
        .anyMatch(v -> "name".equals(v.getPropertyPath().toString()));
    assertThat("Should have a name validation violation", hasNameViolation, is(true));
  }

  @Test
  void testCreateRequestRejectsNamesStartingWithDot() {
    FileBlobStoreApiCreateRequest request = new FileBlobStoreApiCreateRequest();
    request.setName(".invalidName");
    request.setPath("/tmp/blobstore");

    Set<ConstraintViolation<FileBlobStoreApiCreateRequest>> violations = validator.validate(request);

    assertThat(violations.size(), is(greaterThanOrEqualTo(1)));
    boolean hasNameViolation = violations.stream()
        .anyMatch(v -> "name".equals(v.getPropertyPath().toString()));
    assertThat("Should have a name validation violation", hasNameViolation, is(true));
  }

  @Test
  void testCreateRequestRejectsNamesWithSpecialCharacters() {
    FileBlobStoreApiCreateRequest request = new FileBlobStoreApiCreateRequest();
    request.setName("invalid@name");
    request.setPath("/tmp/blobstore");

    Set<ConstraintViolation<FileBlobStoreApiCreateRequest>> violations = validator.validate(request);

    assertThat(violations.size(), is(greaterThanOrEqualTo(1)));
    boolean hasNameViolation = violations.stream()
        .anyMatch(v -> "name".equals(v.getPropertyPath().toString()));
    assertThat("Should have a name validation violation", hasNameViolation, is(true));
  }

  @Test
  void testCreateRequestAcceptsValidNames() {
    FileBlobStoreApiCreateRequest request = new FileBlobStoreApiCreateRequest();
    request.setName("valid-name");
    request.setPath("/tmp/blobstore");

    Set<ConstraintViolation<FileBlobStoreApiCreateRequest>> violations = validator.validate(request);

    assertThat("Valid name should not have violations", violations.isEmpty(), is(true));
  }

  @Test
  void testCreateRequestAcceptsNamesWithUnderscoresInMiddle() {
    FileBlobStoreApiCreateRequest request = new FileBlobStoreApiCreateRequest();
    request.setName("valid_name");
    request.setPath("/tmp/blobstore");

    Set<ConstraintViolation<FileBlobStoreApiCreateRequest>> violations = validator.validate(request);

    assertThat("Name with underscores in middle should be valid", violations.isEmpty(), is(true));
  }

  @Test
  void testCreateRequestAcceptsNamesWithDotsInMiddle() {
    FileBlobStoreApiCreateRequest request = new FileBlobStoreApiCreateRequest();
    request.setName("valid.name");
    request.setPath("/tmp/blobstore");

    Set<ConstraintViolation<FileBlobStoreApiCreateRequest>> violations = validator.validate(request);

    assertThat("Name with dots in middle should be valid", violations.isEmpty(), is(true));
  }
}
