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
package org.sonatype.nexus.cleanup.storage;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;

public class CleanupPolicyPreviewXOTest
{
  private final CleanupPolicyPreviewXO underTest = new CleanupPolicyPreviewXO();

  @Test
  public void testDefaultsAreNull() {
    assertThat(underTest.getRepositoryName(), is(nullValue()));
    assertThat(underTest.getCriteria(), is(nullValue()));
  }

  @Test
  public void testGetSetRepositoryName() {
    underTest.setRepositoryName("my-repo");
    assertThat(underTest.getRepositoryName(), is("my-repo"));
  }

  @Test
  public void testGetSetCriteria() {
    CleanupPolicyCriteria criteria = newCriteria();
    underTest.setCriteria(criteria);
    assertThat(underTest.getCriteria(), is(sameInstance(criteria)));
  }

  @Test
  public void testToString() {
    CleanupPolicyCriteria criteria = newCriteria();
    underTest.setRepositoryName("my-repo");
    underTest.setCriteria(criteria);

    String result = underTest.toString();

    assertThat(result, is("CleanupPolicyPreviewXO(repositoryName:my-repo, criteria:" + criteria + ")"));
    assertThat(result, containsString("CleanupPolicyPreviewXO("));
    assertThat(result, containsString("repositoryName:my-repo"));
    assertThat(result, containsString(", criteria:" + criteria));
  }

  @Test
  public void testToStringWithNullFields() {
    assertThat(underTest.toString(), is("CleanupPolicyPreviewXO(repositoryName:null, criteria:null)"));
  }

  private static CleanupPolicyCriteria newCriteria() {
    return new CleanupPolicyCriteria(10, 20, CleanupPolicyReleaseType.RELEASES, "*.tmp", 5, "version");
  }
}
