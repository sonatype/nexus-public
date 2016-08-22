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
package org.sonatype.nexus.repository.search;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.search.SearchSubjectHelper.SubjectRegistration;

import org.apache.shiro.subject.Subject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class SearchSubjectHelperTest
    extends TestSupport
{
  @Mock
  Subject subject;

  SearchSubjectHelper helper;

  @Before
  public void setup() {
    helper = new SearchSubjectHelper();
  }

  @Test
  public void testRegistration() {
    assertThat(helper.subjects.size(), is(0));
    try (SubjectRegistration registration = helper.register(subject)) {
      assertThat(helper.subjects.size(), is(1));
      assertThat(helper.getSubject(registration.getId()), is(subject));
    }
    assertThat(helper.subjects.size(), is(0));
  }

  @Test(expected = NullPointerException.class)
  public void testMissingSubject() {
    helper.getSubject("");
  }
}
