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
package org.sonatype.nexus.testsuite.site;

import java.io.File;
import java.util.Collection;

import javax.annotation.Nullable;
import javax.ws.rs.core.MediaType;

import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.nexus.repository.site.client.SiteRepository;
import org.sonatype.nexus.rest.model.ContentListResourceResponse;
import org.sonatype.nexus.rest.model.NexusRepositoryTypeListResource;
import org.sonatype.nexus.rest.model.NexusRepositoryTypeListResourceResponse;
import org.sonatype.nexus.testsuite.support.NexusStartAndStopStrategy;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.sun.jersey.api.client.ClientResponse;
import org.junit.Test;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.sonatype.sisu.goodies.testsupport.hamcrest.FileMatchers.matchSha1;

@NexusStartAndStopStrategy(NexusStartAndStopStrategy.Strategy.EACH_TEST)
public class SiteRepositoryIT
    extends SiteRepositoryITSupport
{

  public SiteRepositoryIT(final String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  /**
   * Verify that maven site repository is available in list of repository types.
   */
  @Test
  public void repoType() {
    final JerseyNexusClient client = (JerseyNexusClient) client();

    final NexusRepositoryTypeListResourceResponse types = client.serviceResource("components/repo_types")
        .get(NexusRepositoryTypeListResourceResponse.class);

    final Collection<String> typeFormats =
        Collections2.transform(types.getData(), new Function<NexusRepositoryTypeListResource, String>()
        {
          @Override
          public String apply(@Nullable final NexusRepositoryTypeListResource input) {
            return input.getFormat();
          }
        });

    assertThat(typeFormats, hasItem("site"));
  }

  /**
   * Verify that a "site site:deploy" maven build can deploy to a maven site repository.
   *
   * @throws Exception unexpected
   */
  @Test
  public void siteDeployViaMaven()
      throws Exception
  {
    final String repositoryId = repositoryIdForTest();

    repositories().create(SiteRepository.class, repositoryId).save();

    final File builtProjectHome = executeMaven("site-1", repositoryId, "site", "site:deploy");
    final File indexHtml = downloadFromSite(repositoryId, "site-1/index.html");

    assertThat(indexHtml, matchSha1(new File(builtProjectHome, "target/site/index.html")));
  }

  /**
   * Verify that a build with dotted site path can deploy to a maven site repository.
   *
   * @throws Exception unexpected
   */
  @Test
  public void dottedSiteDeployViaMaven()
      throws Exception
  {
    final String repositoryId = repositoryIdForTest();

    repositories().create(SiteRepository.class, repositoryId).save();

    final File builtProjectHome = executeMaven("site-2", repositoryId, "site", "site:deploy");
    final File indexHtml = downloadFromSite(repositoryId, "site-2/index.html");

    assertThat(indexHtml, matchSha1(new File(builtProjectHome, "target/site/index.html")));
  }

  /**
   * Verify that accessing a css file via a maven site repository will respond with "text/css" mime type.
   */
  @Test
  public void cssMimeType() {
    final String repositoryId = repositoryIdForTest();

    repositories().create(SiteRepository.class, repositoryId).save();
    copySiteContentToRepository("site-content", repositoryId);

    ClientResponse clientResponse = null;
    try {
      clientResponse = getStatusOf(
          format("service/local/repositories/%s/content/css/site.css", repositoryId)
      );

      assertThat(clientResponse.getStatus(), is(200));
      assertThat(clientResponse.getType(), is(MediaType.valueOf("text/css")));
    }
    finally {
      if (clientResponse != null) {
        clientResponse.close();
      }
    }
  }

  /**
   * Verify that directory listing of the root of a maven site repository will return an xml collection of contained
   * items.
   */
  @Test
  public void directoryListing() {
    final String repositoryId = repositoryIdForTest();

    repositories().create(SiteRepository.class, repositoryId).save();

    copySiteContentToRepository("site-content", repositoryId);

    ClientResponse clientResponse = null;
    try {
      clientResponse = getStatusOf(
          format("service/local/repositories/%s/content/", repositoryId)
      );

      assertThat(clientResponse.getStatus(), is(200));
      assertThat(clientResponse.getType().toString(), startsWith("application/xml"));
      assertThat(clientResponse.getEntity(ContentListResourceResponse.class), is(notNullValue()));
    }
    finally {
      if (clientResponse != null) {
        clientResponse.close();
      }
    }
  }

  /**
   * Verify that accessing the root of a maven site repository via exposed URL will return the index.html.
   */
  @Test
  public void indexHtml() {
    final String repositoryId = repositoryIdForTest();

    repositories().create(SiteRepository.class, repositoryId).save();
    copySiteContentToRepository("site-content", repositoryId);

    ClientResponse clientResponse = null;
    try {
      clientResponse = getStatusOf(
          format("content/sites/%s/", repositoryId)
      );

      assertThat(clientResponse.getStatus(), is(200));
      assertThat(clientResponse.getType(), is(MediaType.TEXT_HTML_TYPE));
      assertThat(clientResponse.getEntity(String.class), containsString("<html"));

      String xFrameOptions = clientResponse.getHeaders().getFirst("X-Frame-Options");
      assertThat(xFrameOptions, equalTo("SAMEORIGIN"));
    }
    finally {
      if (clientResponse != null) {
        clientResponse.close();
      }
    }
  }

}
