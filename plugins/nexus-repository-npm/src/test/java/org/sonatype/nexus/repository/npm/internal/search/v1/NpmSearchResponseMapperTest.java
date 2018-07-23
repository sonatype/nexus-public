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
package org.sonatype.nexus.repository.npm.internal.search.v1;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import org.sonatype.nexus.repository.npm.internal.search.v1.NpmSearchResponse;
import org.sonatype.nexus.repository.npm.internal.search.v1.NpmSearchResponseMapper;
import org.sonatype.nexus.repository.npm.internal.search.v1.NpmSearchResponseObject;
import org.sonatype.nexus.repository.npm.internal.search.v1.NpmSearchResponsePackage;
import org.sonatype.nexus.repository.npm.internal.search.v1.NpmSearchResponsePackageLinks;
import org.sonatype.nexus.repository.npm.internal.search.v1.NpmSearchResponsePerson;
import org.sonatype.nexus.repository.npm.internal.search.v1.NpmSearchResponseScore;
import org.sonatype.nexus.repository.npm.internal.search.v1.NpmSearchResponseScoreDetail;

import org.sonatype.goodies.testsupport.TestSupport;

import com.google.common.io.CharStreams;
import net.javacrumbs.jsonunit.JsonMatchers;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class NpmSearchResponseMapperTest
    extends TestSupport
{
  /**
   * Tests that a string generated from populated JSON data carriers produces JSON equivalent to the sample search
   * response in the npm V1 search API documentation.
   *
   * @see <a href="https://github.com/npm/registry/blob/master/docs/REGISTRY-API.md#get-v1search">GET·/-/v1/search</a>
   */
  @Test
  public void testWriteString() throws Exception {
    NpmSearchResponseMapper underTest = new NpmSearchResponseMapper();
    NpmSearchResponse response = buildSearchResponse();
    String result = underTest.writeString(response);

    try (InputStream in = getClass().getResourceAsStream("sample-search-response.json")) {
      String expected = CharStreams.toString(new InputStreamReader(in, StandardCharsets.UTF_8));
      assertThat(result, JsonMatchers.jsonEquals(expected));
    }
  }

  private NpmSearchResponse buildSearchResponse() {
    NpmSearchResponseScoreDetail detail = new NpmSearchResponseScoreDetail();
    detail.setQuality(0.9270640902288084);
    detail.setPopularity(0.8484861649808381);
    detail.setMaintenance(0.9962706951777409);

    NpmSearchResponseScore score = new NpmSearchResponseScore();
    score.setFinalScore(0.9237841281241451);
    score.setDetail(detail);

    NpmSearchResponsePackageLinks links = new NpmSearchResponsePackageLinks();
    links.setNpm("https://www.npmjs.com/package/yargs");
    links.setHomepage("http://yargs.js.org/");
    links.setRepository("https://github.com/yargs/yargs");
    links.setBugs("https://github.com/yargs/yargs/issues");

    NpmSearchResponsePerson publisher = new NpmSearchResponsePerson();
    publisher.setUsername("bcoe");
    publisher.setEmail("ben@npmjs.com");

    NpmSearchResponsePerson maintainer1 = new NpmSearchResponsePerson();
    maintainer1.setUsername("bcoe");
    maintainer1.setEmail("ben@npmjs.com");

    NpmSearchResponsePerson maintainer2 = new NpmSearchResponsePerson();
    maintainer2.setUsername("chevex");
    maintainer2.setEmail("alex.ford@codetunnel.com");

    NpmSearchResponsePerson maintainer3 = new NpmSearchResponsePerson();
    maintainer3.setUsername("nexdrew");
    maintainer3.setEmail("andrew@npmjs.com");

    NpmSearchResponsePerson maintainer4 = new NpmSearchResponsePerson();
    maintainer4.setUsername("nylen");
    maintainer4.setEmail("jnylen@gmail.com");

    NpmSearchResponsePackage packageEntry = new NpmSearchResponsePackage();
    packageEntry.setName("yargs");
    packageEntry.setVersion("6.6.0");
    packageEntry.setDescription("yargs the modern, pirate-themed, successor to optimist.");
    packageEntry.setKeywords(asList("argument", "args", "option", "parser", "parsing", "cli", "command"));
    packageEntry.setDate("2016-12-30T16:53:16.023Z");
    packageEntry.setLinks(links);
    packageEntry.setPublisher(publisher);
    packageEntry.setMaintainers(asList(maintainer1, maintainer2, maintainer3, maintainer4));

    NpmSearchResponseObject responseObject = new NpmSearchResponseObject();
    responseObject.setPackageEntry(packageEntry);
    responseObject.setScore(score);
    responseObject.setSearchScore(100000.914);

    NpmSearchResponse response = new NpmSearchResponse();
    response.setObjects(Collections.singletonList(responseObject));
    response.setTotal(1);
    response.setTime("Wed Jan 25 2017 19:23:35 GMT+0000 (UTC)");
    return response;
  }

  /**
   * Tests that reading the sample search response in the npm V1 search API documentation produces semantically correct
   * Java objects containing the information.
   *
   * @see <a href="https://github.com/npm/registry/blob/master/docs/REGISTRY-API.md#get-v1search">GET·/-/v1/search</a>
   */
  @Test
  public void testReadInputStream() throws Exception {
    try (InputStream in = getClass().getResourceAsStream("sample-search-response.json")) {

      NpmSearchResponseMapper underTest = new NpmSearchResponseMapper();
      NpmSearchResponse response = underTest.readFromInputStream(in);

      assertThat(response.getObjects(), hasSize(1));
      assertThat(response.getTotal(), is(1));
      assertThat(response.getTime(), is("Wed Jan 25 2017 19:23:35 GMT+0000 (UTC)"));

      NpmSearchResponseObject object = response.getObjects().get(0);
      assertThat(object.getSearchScore(), is(closeTo(100000.914, 0.01)));

      NpmSearchResponseScore score = object.getScore();
      assertThat(score.getFinalScore(), is(closeTo(0.9237841281241451, 0.01)));

      NpmSearchResponseScoreDetail detail = score.getDetail();
      assertThat(detail.getQuality(), is(closeTo(0.9270640902288084, 0.01)));
      assertThat(detail.getPopularity(), is(closeTo(0.8484861649808381, 0.01)));
      assertThat(detail.getMaintenance(), is(closeTo(0.9962706951777409, 0.01)));

      NpmSearchResponsePackage packageEntry = object.getPackageEntry();
      assertThat(packageEntry.getName(), is("yargs"));
      assertThat(packageEntry.getVersion(), is("6.6.0"));
      assertThat(packageEntry.getDescription(), is("yargs the modern, pirate-themed, successor to optimist."));
      assertThat(packageEntry.getKeywords(),
          containsInAnyOrder("argument", "args", "option", "parser", "parsing", "cli", "command"));
      assertThat(packageEntry.getDate(), is("2016-12-30T16:53:16.023Z"));

      NpmSearchResponsePackageLinks links = packageEntry.getLinks();
      assertThat(links.getNpm(), is("https://www.npmjs.com/package/yargs"));
      assertThat(links.getHomepage(), is("http://yargs.js.org/"));
      assertThat(links.getRepository(), is("https://github.com/yargs/yargs"));
      assertThat(links.getBugs(), is("https://github.com/yargs/yargs/issues"));

      NpmSearchResponsePerson publisher = packageEntry.getPublisher();
      assertThat(publisher.getUsername(), is("bcoe"));
      assertThat(publisher.getEmail(), is("ben@npmjs.com"));

      List<NpmSearchResponsePerson> maintainers = packageEntry.getMaintainers();
      assertThat(maintainers, hasSize(4));
      assertThat(maintainers.get(0).getUsername(), is("bcoe"));
      assertThat(maintainers.get(0).getEmail(), is("ben@npmjs.com"));
      assertThat(maintainers.get(1).getUsername(), is("chevex"));
      assertThat(maintainers.get(1).getEmail(), is("alex.ford@codetunnel.com"));
      assertThat(maintainers.get(2).getUsername(), is("nexdrew"));
      assertThat(maintainers.get(2).getEmail(), is("andrew@npmjs.com"));
      assertThat(maintainers.get(3).getUsername(), is("nylen"));
      assertThat(maintainers.get(3).getEmail(), is("jnylen@gmail.com"));
    }
  }
}
