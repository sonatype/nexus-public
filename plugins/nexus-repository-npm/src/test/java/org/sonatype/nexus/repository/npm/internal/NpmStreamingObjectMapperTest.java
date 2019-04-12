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
package org.sonatype.nexus.repository.npm.internal;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.app.BaseUrlHolder;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.fail;
import static org.sonatype.nexus.repository.npm.internal.NpmFieldFactory.REMOVE_DEFAULT_FIELDS_MATCHERS;
import static org.sonatype.nexus.repository.npm.internal.NpmFieldFactory.rewriteTarballUrlMatcher;

public class NpmStreamingObjectMapperTest
    extends TestSupport
{
  private final static String REPO_NAME = "npm-repository";

  private final static String ID_FIELD_NAME = "\"_id\"";

  private final static String REV_FIELD_NAME = "\"_rev\"";

  private final static String PACKAGE_REV = "1234567890";

  private final static String PACKAGE_ID = "array-first";

  private final static String PACKAGE_ID_JSON = ID_FIELD_NAME + ":\"" + PACKAGE_ID + "\"";

  private final static String PACKAGE_REV_JSON = REV_FIELD_NAME + ":\"" + PACKAGE_REV + "\"";

  @Before
  public void setUp() {
    BaseUrlHolder.unset();
  }

  @Test
  public void verify_MultiType_Json_StreamsOut_Same_JSON() throws IOException {
    try (InputStream packageRoot = getResource("streaming-payload.json");
         InputStream packageRoot2 = getResource("streaming-payload.json")) {

      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      new NpmStreamingObjectMapper().readAndWrite(packageRoot, byteArrayOutputStream);

      assertThat(byteArrayOutputStream.toString(), equalTo(IOUtils.toString(packageRoot2)));
    }
  }

  @Test
  public void verify_Large_Json_StreamsOut_Same_JSON() throws IOException {
    try (InputStream packageRoot = getResource("streaming-payload-large.json");
         InputStream packageRoot2 = getResource("streaming-payload-large.json")) {

      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      new NpmStreamingObjectMapper().readAndWrite(packageRoot, byteArrayOutputStream);

      assertThat(byteArrayOutputStream.toString(), equalTo(IOUtils.toString(packageRoot2)));
    }
  }

  @Test
  public void verify_Large_Json_StreamsOut_Within_Reasonable_Performance() throws IOException {
    assertThat(BaseUrlHolder.isSet(), is(false));
    BaseUrlHolder.set("http://localhost:8080/");

    double maxTimesSlowerAllowed = 2.5;
    double streamTotalDuration = 0;
    double memoryTotalDuration = 0;
    long counter = 100;
    int tenMb = 10485760; // 8361219 bytes in the json file use 10485760 bytes = 10240kb = 10mb

    for (int i = 0; i < counter; i++) {
      try (InputStream packageRoot = getResource("streaming-payload-large.json");
           InputStream packageRoot2 = getResource("streaming-payload-large.json")) {

        long start = currentTimeMillis();

        NpmJsonUtils.parse(() -> packageRoot);

        long memoryDuration = currentTimeMillis() - start;

        start = currentTimeMillis();

        new NpmStreamingObjectMapper().readAndWrite(packageRoot2, new BufferedOutputStream(new ByteArrayOutputStream(), tenMb));

        long streamDuration = currentTimeMillis() - start;

        memoryTotalDuration += memoryDuration;
        streamTotalDuration += streamDuration;
      }
    }

    double averageTimeSlower = (streamTotalDuration / memoryTotalDuration);

    if (averageTimeSlower > maxTimesSlowerAllowed) {
      fail(format("Streaming %s slower then using in memory parsing, while only %s times slower is allowed",
          averageTimeSlower, maxTimesSlowerAllowed));
    }
  }

  @Test
  public void verify_TarballUrl_Manipulation_Of_Json_While_Streaming_Out() throws IOException {
    String nxrmUrl = "http://localhost:8080/";
    String remoteUrl = "https://registry.npmjs.org";
    String distTarballPath = "\"dist\":{\"tarball\":\"";
    String packageId = "array-first";
    String packagePath = "/" + packageId + "/-/" + packageId;

    // these are not the complete tarball urls but are unique enough to identify that it was changed
    String normDistTarball = distTarballPath + nxrmUrl + "repository/" + REPO_NAME + packagePath;
    String remoteDistTarball = distTarballPath + remoteUrl + packagePath;

    assertThat(BaseUrlHolder.isSet(), is(false));

    BaseUrlHolder.set(nxrmUrl);

    try (InputStream packageRoot = getResource("streaming-payload-manipulate-while-streaming-out.json");
         InputStream packageRoot2 = getResource("streaming-payload-manipulate-while-streaming-out.json")) {

      String original = IOUtils.toString(packageRoot);

      assertThat(original, containsString(remoteDistTarball));
      assertThat(original, not(containsString(normDistTarball)));

      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      new NpmStreamingObjectMapper(singletonList(rewriteTarballUrlMatcher(REPO_NAME, packageId)))
          .readAndWrite(packageRoot2, outputStream);

      String streamed = outputStream.toString();
      assertThat(streamed, not(containsString(remoteDistTarball)));
      assertThat(streamed, containsString(normDistTarball));
    }
  }

  @Test
  public void verify_Remove_Id_and_Rev_Manipulation_Of_Json_While_Streaming_Out() throws IOException {
    try (InputStream packageRoot = getResource("streaming-payload-manipulate-while-streaming-out.json");
         InputStream packageRoot2 = getResource("streaming-payload-manipulate-while-streaming-out.json")) {

      String original = IOUtils.toString(packageRoot);

      assertThat(original, containsString(PACKAGE_ID_JSON));
      assertThat(original, containsString(PACKAGE_REV));

      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      new NpmStreamingObjectMapper(REMOVE_DEFAULT_FIELDS_MATCHERS).readAndWrite(packageRoot2, outputStream);

      String streamed = outputStream.toString();
      assertThat(streamed, not(containsString(PACKAGE_ID_JSON)));
      assertThat(streamed, not(containsString(PACKAGE_REV_JSON)));
    }
  }

  @Test
  public void verify_Add_Id_and_Rev_Manipulation_Of_Json_While_Streaming_Out() throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    new NpmStreamingObjectMapper(PACKAGE_ID, null, emptyList()).readAndWrite(toInputStream("{}"), outputStream);

    String streamed = outputStream.toString();
    assertThat(streamed, containsString(PACKAGE_ID_JSON));
    assertThat(streamed, not(containsString(REV_FIELD_NAME)));

    outputStream = new ByteArrayOutputStream();
    new NpmStreamingObjectMapper(null, PACKAGE_REV, emptyList()).readAndWrite(toInputStream("{}"), outputStream);

    streamed = outputStream.toString();
    assertThat(streamed, not(containsString(ID_FIELD_NAME)));
    assertThat(streamed, containsString(PACKAGE_REV_JSON));

    outputStream = new ByteArrayOutputStream();
    new NpmStreamingObjectMapper(PACKAGE_ID, PACKAGE_REV, emptyList())
        .readAndWrite(toInputStream("{}"), outputStream);

    streamed = outputStream.toString();
    assertThat(streamed, containsString(PACKAGE_ID_JSON));
    assertThat(streamed, containsString(PACKAGE_REV_JSON));

    outputStream = new ByteArrayOutputStream();
    new NpmStreamingObjectMapper().readAndWrite(toInputStream("{}"), outputStream);

    streamed = outputStream.toString();
    assertThat(streamed, not(containsString(ID_FIELD_NAME)));
    assertThat(streamed, not(containsString(REV_FIELD_NAME)));

    outputStream = new ByteArrayOutputStream();
    new NpmStreamingObjectMapper(REMOVE_DEFAULT_FIELDS_MATCHERS)
        .readAndWrite(toInputStream("{" + PACKAGE_ID_JSON + "," + PACKAGE_REV_JSON + "}"), outputStream);

    streamed = outputStream.toString();
    assertThat(streamed, not(containsString(ID_FIELD_NAME)));
    assertThat(streamed, not(containsString(REV_FIELD_NAME)));
  }

  private InputStream getResource(final String fileName) {
    return getClass().getResourceAsStream(fileName);
  }
}
