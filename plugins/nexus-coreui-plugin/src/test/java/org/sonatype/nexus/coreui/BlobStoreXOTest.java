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
import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import com.google.common.base.Strings;
import org.junit.Before;
import org.junit.Test;

import static java.lang.String.format;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

public class BlobStoreXOTest
{
  public static final String NULL_VIOLATES_RULE = "'null' violates rule";

  private static Validator validator;

  @Before
  public void setUp() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  public void checkNullValues() {
    BlobStoreXO blobStoreXOAllNullValues = new BlobStoreXO();
    Set<ConstraintViolation<BlobStoreXO>> violationsAllNullValues = validator.validate(blobStoreXOAllNullValues);
    assertThat(NULL_VIOLATES_RULE, violationsAllNullValues, hasSize(3));

    BlobStoreXO blobStoreXOJustName = new BlobStoreXO();
    blobStoreXOJustName.setName("someName");
    Set<ConstraintViolation<BlobStoreXO>> violationsJustName = validator.validate(blobStoreXOJustName);
    assertThat(NULL_VIOLATES_RULE, violationsJustName, hasSize(2));

    BlobStoreXO blobStoreXONoAttibutes = new BlobStoreXO();
    blobStoreXONoAttibutes.setName("someName");
    blobStoreXONoAttibutes.setType("someType");
    Set<ConstraintViolation<BlobStoreXO>> violationsNoAttributes = validator.validate(blobStoreXONoAttibutes);
    assertThat(NULL_VIOLATES_RULE, violationsNoAttributes, hasSize(1));
  }

  @Test
  public void checkNameRestrictions() {
    BlobStoreXO blobStoreEmptyName = createBlobStoreXO("");
    Set<ConstraintViolation<BlobStoreXO>> violationsEmptytName = validator.validate(blobStoreEmptyName);
    assertThat(NULL_VIOLATES_RULE, violationsEmptytName, hasSize(2));

    BlobStoreXO blobStoreMinName = createBlobStoreXO("a");
    Set<ConstraintViolation<BlobStoreXO>> violationsMinName = validator.validate(blobStoreMinName);
    assertThat(NULL_VIOLATES_RULE, violationsMinName, hasSize(0));

    String maxCharsName = Strings.padEnd("", 255, 'x');
    BlobStoreXO blobStoreMaxName = createBlobStoreXO(maxCharsName);
    Set<ConstraintViolation<BlobStoreXO>> violationsMaxName = validator.validate(blobStoreMaxName);
    assertThat(NULL_VIOLATES_RULE, violationsMaxName, hasSize(0));

    BlobStoreXO blobStoreLongerName = createBlobStoreXO(maxCharsName + "x");
    Set<ConstraintViolation<BlobStoreXO>> violationsLongerName = validator.validate(blobStoreLongerName);
    assertThat(NULL_VIOLATES_RULE, violationsLongerName, hasSize(1));
  }

  private BlobStoreXO createBlobStoreXO(final String name) {
    Map<String, Map<String, Object>> attributes = new HashMap<>();
    Map<String, Object> attribute = new HashMap<>();
    attribute.put("testAttribute", null);
    attributes.put("attibute1", attribute);

    BlobStoreXO blobStore = new BlobStoreXO();
    blobStore.setType("someType");
    blobStore.setAttributes(attributes);
    blobStore.setName(name);
    return(blobStore);
  }
}
