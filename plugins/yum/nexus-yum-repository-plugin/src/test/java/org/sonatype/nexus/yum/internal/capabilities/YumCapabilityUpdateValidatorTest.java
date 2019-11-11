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

import java.util.HashMap;
import java.util.Map;

import org.sonatype.nexus.plugins.capabilities.ValidationResult;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class YumCapabilityUpdateValidatorTest
{
  @Test
  public void valid() {
    assertValidResult(null, null, null, null);
    assertValidResult(null, "", null, "");
    assertValidResult("createrepo", "mergerepo", "createrepo", "mergerepo");
    assertValidResult("/sbin/crepo", "/sbin/mrepo", "/sbin/crepo", "/sbin/mrepo");
  }

  @Test
  public void invalid() {
    assertInvalidResult(null, "", "", "", 1, "createrepoPath can not be modified, you can change it in sonatype-work/nexus/conf/capabilities.xml");
    assertInvalidResult("", "", null, null, 2, "createrepoPath can not be modified, you can change it in sonatype-work/nexus/conf/capabilities.xml");
    assertInvalidResult("createrepo", "/sbin/mergerepo", "createrepo", "mergerepo", 1, "mergerepoPath can not be modified, you can change it in sonatype-work/nexus/conf/capabilities.xml");
    assertInvalidResult("/sbin/createrepo", "/sbin/mergerepo", "createrepo", "mergerepo", 2, "createrepoPath can not be modified, you can change it in sonatype-work/nexus/conf/capabilities.xml");
  }

  private void assertValidResult(String createrepoUpdate, String mergerepoUpdate, String createrepoExisting,
                                String mergerepoExisting) {
    ValidationResult result = getResult(createrepoUpdate, mergerepoUpdate, createrepoExisting, mergerepoExisting);
    assertTrue(result.isValid());
  }

  private void assertInvalidResult(
      String createrepoUpdate, String mergerepoUpdate, String createrepoExisting,
      String mergerepoExisting, int numViolations, String firstViolationMessage)
  {
    ValidationResult result = getResult(createrepoUpdate, mergerepoUpdate, createrepoExisting, mergerepoExisting);

    assertFalse(result.isValid());
    assertEquals(numViolations, result.violations().size());
    ValidationResult.Violation first = result.violations().iterator().next();
    assertEquals(firstViolationMessage, first.message());
  }

  private ValidationResult getResult(
      final String createrepoUpdate,
      final String mergerepoUpdate,
      final String createrepoExisting,
      final String mergerepoExisting)
  {
    Map<String, String> existingProperties = new HashMap<>();
    existingProperties.put("createrepoPath", createrepoExisting);
    existingProperties.put("mergerepoPath", mergerepoExisting);

    Map<String, String> updateProperties = new HashMap<>();
    updateProperties.put("createrepoPath", createrepoUpdate);
    updateProperties.put("mergerepoPath", mergerepoUpdate);

    YumCapabilityUpdateValidator underTest = new YumCapabilityUpdateValidator(existingProperties);

    return underTest.validate(updateProperties);
  }
}
