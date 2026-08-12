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
package org.sonatype.nexus.testdb;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Used to indicate that a test should be run with databases. See {@link DatabaseExtension} for a description of usage.
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@TestTemplate
@ExtendWith(DatabaseExtension.class)
public @interface DatabaseTest
{
  /**
   * Can be used to disable creating a test run of the annotated method using the H2 database. Both this and the
   * related {@link DataSessionConfiguration#h2()} must be true for the test to run with H2.
   */
  boolean h2() default true;

  /**
   * Can be used to enable creating a test run of the annotated method using the PostgreSQL database. Both this and
   * the related {@link DataSessionConfiguration#postgresql()} must be true for the test to run with PostgreSQL.
   */
  boolean postgresql() default false;
}
