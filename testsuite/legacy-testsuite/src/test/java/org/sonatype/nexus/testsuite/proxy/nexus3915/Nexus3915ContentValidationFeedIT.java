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
package org.sonatype.nexus.testsuite.proxy.nexus3915;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import org.sonatype.nexus.integrationtests.AbstractNexusProxyIntegrationTest;
import org.sonatype.nexus.rest.model.RepositoryProxyResource;
import org.sonatype.nexus.test.utils.FeedUtil;
import org.sonatype.nexus.test.utils.GavUtil;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import org.apache.maven.index.artifact.Gav;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.restlet.data.MediaType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class Nexus3915ContentValidationFeedIT
    extends AbstractNexusProxyIntegrationTest
{

  private Gav gav;

  @Before
  public void createGAV() {
    gav = GavUtil.newGav("nexus3915", "artifact", "1.0.0");
  }

  @Test
  public void contentValidationFeed()
      throws Exception
  {

    // make sure it is validating the content!
    RepositoryMessageUtil repoUtil = new RepositoryMessageUtil(this, getXMLXStream(), MediaType.APPLICATION_XML);
    RepositoryProxyResource repo = (RepositoryProxyResource) repoUtil.getRepository(REPO_RELEASE_PROXY_REPO1);
    repo.setFileTypeValidation(true);
    repoUtil.updateRepo(repo);

    String msg = null;

    try {
      this.downloadArtifactFromRepository(REPO_RELEASE_PROXY_REPO1, gav, "target/downloads");
      Assert.fail("Should fail to download artifact");
    }
    catch (FileNotFoundException e) {
      // ok!
      msg = e.getMessage();
    }

    File file = new File(nexusWorkDir, "storage/release-proxy-repo-1/nexus2922/artifact/1.0.0/artifact-1.0.0.jar");
    Assert.assertFalse(file.toString(), file.exists());

    Assert.assertTrue(msg, msg.contains("404"));

    // brokenFiles feed is a asynchronous event, so need to wait async event to finish running
    getEventInspectorsUtil().waitForCalmPeriod();

    SyndFeed feed = FeedUtil.getFeed("brokenFiles");

    @SuppressWarnings("unchecked")
    List<SyndEntry> entries = feed.getEntries();

    Assert.assertTrue("Expected more then 1 entries, but got " + entries.size() + " - "
        + entries, entries.size() >= 1);

    validateContent(entries);

  }

  private void validateContent(List<SyndEntry> entries) {
    StringBuilder titles = new StringBuilder();

    String contentName = gav.getArtifactId() + "-" + gav.getVersion() + "." + gav.getExtension();

    for (SyndEntry entry : entries) {
      // check if the title contains the file name (pom or jar)
      String title = entry.getDescription().getValue();
      titles.append(title);
      titles.append(',');

      assertThat(title, containsString(contentName));
      return;
    }

    Assert.fail(titles.toString());
  }
}
