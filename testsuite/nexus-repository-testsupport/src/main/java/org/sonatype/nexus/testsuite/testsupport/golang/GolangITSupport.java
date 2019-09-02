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
package org.sonatype.nexus.testsuite.testsupport.golang;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import org.sonatype.goodies.httpfixture.server.fluent.Server;
import org.sonatype.nexus.common.log.LogManager;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.testsuite.testsupport.RepositoryITSupport;

import com.google.common.base.Joiner;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.junit.experimental.categories.Category;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.hash;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.sonatype.nexus.repository.http.HttpStatus.OK;
import static org.sonatype.nexus.testsuite.testsupport.FormatClientSupport.status;


/**
 * Go IT support.
 */
@Category(GolangTestGroup.class)
public class GolangITSupport
    extends RepositoryITSupport
{
  protected Server remote;

  private static final Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).create();

  @Inject
  protected LogManager logManager;

  public GolangITSupport() {
    testData.addDirectory(resolveBaseFile("target/it-resources/golang"));
  }

  protected Repository createGolangProxyRepository(final String name, final String remoteUrl) {
    return repos.createGolangProxy(name, remoteUrl);
  }

  protected Repository createGolangHostedRepository(final String name) {
    return repos.createGolangHosted(name);
  }

  protected Repository createGolangGroupRepository(final String name, final String ...members) {
    return repos.createGolangGroup(name, members);
  }

  protected void assertResponseMatches(final HttpResponse response, final String expectedFile) throws IOException {
    assertThat(status(response), is(OK));
    byte[] fetchedContent = EntityUtils.toByteArray(response.getEntity());
    try (FileInputStream file = new FileInputStream(resolveTestFile(expectedFile))) {
      byte[] expectedContent = IOUtils.toByteArray(file);
      assertThat(Arrays.equals(fetchedContent, expectedContent), is(true));
    }
  }

  protected void assertInfoResponseMatches(final HttpResponse response,
                                           final String expectedFile) throws Exception
  {
    PackageInfo packageInfo = fetchPackageInfo(response);

    try (JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(resolveTestFile(expectedFile)),
        UTF_8))) {
      PackageInfo expectedPackageInfo = gson.fromJson(reader, PackageInfo.class);

      assertThat(packageInfo, equalTo(expectedPackageInfo));
    }
  }

  protected PackageInfo fetchPackageInfo(final HttpResponse response) throws Exception {
    assertThat(status(response), is(OK));

    String responseString = EntityUtils.toString(response.getEntity());

     return gson.fromJson(responseString.trim(), PackageInfo.class);
  }

  protected void assertResponseMatches(final HttpResponse response, final byte[] contents) throws IOException {
    assertThat(status(response), is(OK));
    byte[] fetchedContent = EntityUtils.toByteArray(response.getEntity());
    assertThat(Arrays.equals(fetchedContent, contents), is(true));
  }

  protected void assertFetchWhenOffline(final GolangClient client, final String path) throws Exception {
    client.fetch(path);
    remote.stop();
    assertThat(client.fetch(path).getStatusLine().getStatusCode(), is(OK));
  }

  protected void assertLastDownloadedForPath(final GolangClient golangClient,
                                             final Repository repository,
                                             final String path) throws Exception
  {
    assertThat(getLastDownloadedTime(repository, path), equalTo(null));

    assertThat(status(golangClient.fetchAndClose(path)), is(OK));

    assertThat(getLastDownloadedTime(repository, path).isBeforeNow(), is(true));
  }

  protected byte[] getExpectedListResponseBytes(final List<?> versions) {
    return Joiner.on("\n").join(versions).getBytes(UTF_8);
  }

  /**
   * Simple class for use with the package info
   */
  public static class PackageInfo {
    private String version;

    private String time;

    public String getVersion() {
      return version;
    }

    public String getTime() {
      return time;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }

      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      PackageInfo that = (PackageInfo) o;
      return Objects.equals(version, that.version) &&
          Objects.equals(time, that.time);
    }

    @Override
    public int hashCode() {
      return hash(version, time);
    }
  }
}
