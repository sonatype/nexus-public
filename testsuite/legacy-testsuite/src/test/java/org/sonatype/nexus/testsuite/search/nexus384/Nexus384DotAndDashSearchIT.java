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
package org.sonatype.nexus.testsuite.search.nexus384;

import java.util.List;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.rest.model.NexusArtifact;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Searches for artifact that has a '.' and a '-' in the artifact name.
 */
public class Nexus384DotAndDashSearchIT
    extends AbstractNexusIntegrationTest
{

  @BeforeClass
  public static void setSecureTest() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @Test
  public void searchAll()
      throws Exception
  {
    // groupId
    List<NexusArtifact> results = getSearchMessageUtil().searchFor("nexus384");
    Assert.assertEquals(9, results.size());
  }

  // look on artifactId and groupId
  @Test
  public void searchDash()
      throws Exception
  { // with dash

    if (printKnownErrorButDoNotFail(this.getClass(), "searchDash")) {
      return;
    }

    List<NexusArtifact> results = getSearchMessageUtil().searchFor("dash");
    Assert.assertEquals(5, results.size());
  }

  @Test
  public void searchDot()
      throws Exception
  { // with dot

    if (printKnownErrorButDoNotFail(this.getClass(), "searchDot")) {
      return;
    }

    List<NexusArtifact> results = getSearchMessageUtil().searchFor("dot");
    Assert.assertEquals(5, results.size());
  }

  @Test
  public void searchDashAndDot()
      throws Exception
  { // with both

    if (printKnownErrorButDoNotFail(this.getClass(), "searchDashAndDot")) {
      return;
    }

    List<NexusArtifact> results = getSearchMessageUtil().searchFor("dot dash");
    Assert.assertEquals(3, results.size());
  } // look on groupId

  @Test
  public void searchGroudDashed()
      throws Exception
  { // dashed

    if (printKnownErrorButDoNotFail(this.getClass(), "searchGroudDashed")) {
      return;
    }

    List<NexusArtifact> results = getSearchMessageUtil().searchFor("dashed");
    Assert.assertEquals(2, results.size());
  }

  @Test
  public void searchGroudDoted()
      throws Exception
  { // doted

    if (printKnownErrorButDoNotFail(this.getClass(), "searchGroudDoted")) {
      return;
    }

    List<NexusArtifact> results = getSearchMessageUtil().searchFor("doted");
    Assert.assertEquals(2, results.size());
  }

  @Test
  public void searchGroudDashedAndDoted()
      throws Exception
  { // both

    List<NexusArtifact> results = getSearchMessageUtil().searchFor("dashed.doted");
    Assert.assertEquals(2, results.size());
  }

  @Test
  public void searchMixed()
      throws Exception
  { // mixed

    if (printKnownErrorButDoNotFail(this.getClass(), "searchMixed")) {
      return;
    }

    List<NexusArtifact> results = getSearchMessageUtil().searchFor("mixed");
    Assert.assertEquals(2, results.size());
  }

  @Test
  public void searchMixedNexus83()
      throws Exception
  { // based on nexus-83

    if (printKnownErrorButDoNotFail(this.getClass(), "searchMixedNexus83")) {
      return;
    }

    List<NexusArtifact> results = getSearchMessageUtil().searchFor("mixed-");
    Assert.assertEquals(2, results.size());
  }

}
