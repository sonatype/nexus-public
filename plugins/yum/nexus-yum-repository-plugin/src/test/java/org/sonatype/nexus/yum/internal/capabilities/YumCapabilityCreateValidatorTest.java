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
package org.sonatype.nexus.yum.internal.capabilities;

import org.sonatype.nexus.plugins.capabilities.ValidationResult;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class YumCapabilityCreateValidatorTest
{
  private YumCapabilityCreateValidator underTest = new YumCapabilityCreateValidator();

  @Test
  public void null_createsViolation() {
    ValidationResult result = underTest.validate(ImmutableMap.of("createrepoPath", "createrepo"));
    assertFalse(result.isValid());
    assertEquals(1, result.violations().size());
    ValidationResult.Violation first = result.violations().iterator().next();
    assertEquals("mergerepoPath must be 'mergerepo', you can set it later in sonatype-work/nexus/conf/capabilities.xml", first.message());
  }

  @Test
  public void bothNull_createsTwoViolations() {
    ValidationResult result = underTest.validate(ImmutableMap.of());
    assertFalse(result.isValid());
    assertEquals(2, result.violations().size());
    ValidationResult.Violation first = result.violations().iterator().next();
    assertEquals("createrepoPath must be 'createrepo', you can set it later in sonatype-work/nexus/conf/capabilities.xml", first.message());
  }

  @Test
  public void emptyString_createsViolation() {
    ValidationResult result = underTest.validate(ImmutableMap.of("createrepoPath", "", "mergerepoPath", "mergerepo"));
    assertFalse(result.isValid());
    assertEquals(1, result.violations().size());
    ValidationResult.Violation first = result.violations().iterator().next();
    assertEquals("createrepoPath must be 'createrepo', you can set it later in sonatype-work/nexus/conf/capabilities.xml", first.message());
  }

  @Test
  public void nonDefaultValue_createsViolation() {
    ValidationResult result = underTest.validate(ImmutableMap.of("createrepoPath", "createrepo", "mergerepoPath", "/usr/sbin/mergerepo"));
    assertFalse(result.isValid());
    assertEquals(1, result.violations().size());
    ValidationResult.Violation first = result.violations().iterator().next();
    assertEquals("mergerepoPath must be 'mergerepo', you can set it later in sonatype-work/nexus/conf/capabilities.xml", first.message());
  }

  @Test
  public void bothDefaults_createsNoViolations() {
    ValidationResult result = underTest.validate(ImmutableMap.of("createrepoPath", "createrepo", "mergerepoPath", "mergerepo"));
    assertTrue(result.isValid());
    assertTrue(result.violations().isEmpty());
  }
}
