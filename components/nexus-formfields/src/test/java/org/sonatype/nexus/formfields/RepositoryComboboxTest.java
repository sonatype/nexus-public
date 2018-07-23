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
package org.sonatype.nexus.formfields;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class RepositoryComboboxTest
    extends TestSupport

{
  RepositoryCombobox underTest;

  @Before
  public void setUp() throws Exception {
    underTest = new RepositoryCombobox("test");
  }

  @Test
  public void includeAnEntryForAllRepositories() {
    underTest.includeAnEntryForAllRepositories();

    assertThat(underTest.getStoreFilters(), nullValue());
    assertThat(underTest.getStoreApi(), is("coreui_Repository.readReferencesAddingEntryForAll"));
  }

  @Test
  public void testFormatFilters() {
    underTest.excludingAnyOfFormats("nuget", "npm");
    underTest.includingAnyOfFormats("maven", "docker");

    assertThat(underTest.getStoreFilters().get("format"), is("maven,docker,!nuget,!npm"));
  }

  @Test
  public void testVersionPolicyFilters() {
    underTest.excludingAnyOfVersionPolicies("RELEASE");
    underTest.includingAnyOfVersionPolicies("MIXED", "SNAPSHOT");

    assertThat(underTest.getStoreFilters().get("versionPolicies"), is("MIXED,SNAPSHOT,!RELEASE"));
  }

  @Test
  public void testVersionPolicyFilters_onlyExclude() {
    underTest.excludingAnyOfVersionPolicies("RELEASE", "MIXED");

    assertThat(underTest.getStoreFilters().get("versionPolicies"), is("!RELEASE,!MIXED"));
  }

  @Test
  public void testVersionPolicyFilters_onlyInclude() {
    underTest.includingAnyOfVersionPolicies("RELEASE", "MIXED");

    assertThat(underTest.getStoreFilters().get("versionPolicies"), is("RELEASE,MIXED"));
  }

  @Test
  public void testFormatFilters_noVersionPolicies() {
    underTest.excludingAnyOfFormats("nuget", "npm");
    underTest.includingAnyOfFormats("maven", "docker");

    assertThat(underTest.getStoreFilters().containsKey("versionPolicies"), is(false));
  }

}
