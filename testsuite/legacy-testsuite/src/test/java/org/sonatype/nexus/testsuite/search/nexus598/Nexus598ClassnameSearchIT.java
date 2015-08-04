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
package org.sonatype.nexus.testsuite.search.nexus598;

import java.util.List;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.rest.model.NexusArtifact;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test class name search functionality.
 */
public class Nexus598ClassnameSearchIT
    extends AbstractNexusIntegrationTest
{
  public Nexus598ClassnameSearchIT() {
    // TestContainer.getInstance().getTestContext().setSecureTest( true );
  }

  @Test
  public void searchDeployedArtifact()
      throws Exception
  {
    List<NexusArtifact> artifacts =
        getSearchMessageUtil().searchForClassname(
            "org.sonatype.nexus.test.classnamesearch.ClassnameSearchTestHelper");
    Assert.assertFalse("Nexus598 artifact was not found", artifacts.isEmpty());
  }

  @Test
  public void unqualifiedSearchDeployedArtifact()
      throws Exception
  {
    List<NexusArtifact> artifacts = getSearchMessageUtil().searchForClassname("ClassnameSearchTestHelper");
    Assert.assertFalse("Nexus598 artifact was not found", artifacts.isEmpty());
  }

  @Test
  public void searchUnexistentClass()
      throws Exception
  {
    // This test is meaningless, since it does use tokens that appear in other class ("class", "nexus", "test"), so
    // Index
    // _will_ return it
    // Fixed by removing the two problematic token, but this still makes this test meaningless and very UNSTABLE
    // List<NexusArtifact> artifacts =
    // SearchMessageUtil.searchForClassname(
    // "I.hope.this.class.name.is.not.available.at.nexus.repo.for.test.issue.Nexus598" );

    List<NexusArtifact> artifacts = getSearchMessageUtil().searchForClassname("I.hope.this.name.is.not.available");
    Assert.assertTrue("The search found something, but it shouldn't.", artifacts.isEmpty());
  }

}
