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
package org.sonatype.nexus.testsuite.raw;

import java.io.File;
import java.nio.file.Files;
import java.util.Optional;

import javax.ws.rs.core.Response.Status;

import org.sonatype.nexus.repository.http.HttpStatus;
import org.sonatype.nexus.testsuite.testsupport.FormatClientSupport;
import org.sonatype.nexus.testsuite.testsupport.raw.RawClient;
import org.sonatype.nexus.testsuite.testsupport.raw.RawITSupport;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.ContentType;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.sonatype.nexus.testsuite.testsupport.FormatClientSupport.status;

/**
 * IT for group raw repositories
 */
public class RawGroupIT
    extends RawITSupport
{
  public static final String TEST_PATH = "alphabet.txt";

  public static final String TEST_CONTENT = "alphabet.txt";

  public static final String TEST_CONTENT2 = "alphabet2.txt";

  private RawClient hosted1;

  private RawClient hosted2;

  private RawClient groupClient;

  @Before
  public void setUpRepositories() throws Exception {
    hosted1 = rawClient(repos.createRawHosted("raw-hosted-test1"));
    hosted2 = rawClient(repos.createRawHosted("raw-hosted-test2"));

    groupClient = rawClient(repos.createRawGroup("raw-group", "raw-hosted-test1", "raw-hosted-test2"));
  }

  /**
   * When the membersdon't contain any content, requests to the group return 404.
   */
  @Test
  public void emptyMembersReturn404() throws Exception {
    HttpResponse httpResponse = groupClient.get(TEST_PATH);
    assertThat(status(httpResponse), is(HttpStatus.NOT_FOUND));
  }

  /**
   * When a member does contain content, that's returned.
   */
  @Test
  public void memberContentIsFound() throws Exception {
    File testFile = resolveTestFile(TEST_CONTENT);
    hosted1.put(TEST_PATH, ContentType.TEXT_PLAIN, testFile);

    assertThat(string(groupClient.get(TEST_PATH)),  is(new String(Files.readAllBytes(testFile.toPath()))));
  }

  /**
   * The group consults members in order, returning the first success.
   */
  @Test
  public void firstSuccessfulResponseWins() throws Exception {
    File testFile = resolveTestFile(TEST_CONTENT);
    hosted1.put(TEST_PATH, ContentType.TEXT_PLAIN, testFile);
    hosted2.put(TEST_PATH, ContentType.TEXT_PLAIN, resolveTestFile(TEST_CONTENT2));

    assertThat(string(groupClient.get(TEST_PATH)),  is(new String(Files.readAllBytes(testFile.toPath()))));
  }

  /**
   * Members that return failure responses are ignored in favor of successful ones.
   */
  @Test
  public void earlyFailuresAreBypassed() throws Exception {
    File testFile = resolveTestFile(TEST_CONTENT);

    // Only the second repository has any content
    hosted2.put(TEST_PATH, ContentType.TEXT_PLAIN, resolveTestFile(TEST_CONTENT));

    assertThat(string(groupClient.get(TEST_PATH)), is(new String(Files.readAllBytes(testFile.toPath()))));
  }

  private static String string(final CloseableHttpResponse content) {
    assertThat(content, hasStatus(200));

    return Optional.ofNullable(content)
        .map(FormatClientSupport::bytes)
        .map(String::new)
        .orElse(null);
  }

  public static Matcher<HttpResponse> hasStatus(final int statusCode) {
    return new TypeSafeDiagnosingMatcher<HttpResponse>()
    {
      @Override
      public void describeTo(final Description description) {
        description
            .appendText("Status code: " + statusCode + " " + Status.fromStatusCode(statusCode).getReasonPhrase());
      }

      @Override
      protected boolean matchesSafely(final HttpResponse response, final Description mismatchDescription) {
        int actualStatus = response.getStatusLine().getStatusCode();
        if (actualStatus != statusCode) {
          mismatchDescription
              .appendText("Status code: " + actualStatus + " " + Status.fromStatusCode(actualStatus).getReasonPhrase());
          return false;
        }
        return true;
      }
    };
  }
}
