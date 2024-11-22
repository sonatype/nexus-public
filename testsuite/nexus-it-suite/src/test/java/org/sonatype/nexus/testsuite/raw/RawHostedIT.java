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
import java.time.Duration;
import java.util.function.BiConsumer;

import javax.inject.Inject;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.capability.GlobalRepositorySettings;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.raw.ContentDisposition;
import org.sonatype.nexus.repository.raw.ContentDispositionHandler;
import org.sonatype.nexus.testsuite.testsupport.fixtures.LastDownloadedIntervalRule;
import org.sonatype.nexus.testsuite.testsupport.raw.RawClient;
import org.sonatype.nexus.testsuite.testsupport.raw.RawITSupport;

import org.apache.http.HttpResponse;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static java.lang.Thread.sleep;
import static org.apache.http.entity.ContentType.TEXT_PLAIN;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.sonatype.nexus.repository.http.HttpStatus.BAD_REQUEST;
import static org.sonatype.nexus.repository.http.HttpStatus.CREATED;
import static org.sonatype.nexus.repository.http.HttpStatus.OK;
import static org.sonatype.nexus.testsuite.testsupport.FormatClientSupport.bytes;
import static org.sonatype.nexus.testsuite.testsupport.FormatClientSupport.status;

/**
 * IT for hosted raw repositories
 */
public class RawHostedIT
    extends RawITSupport
{
  public static final String HOSTED_REPO = "raw-test-hosted";

  public static final String TEST_CONTENT = "alphabet.txt";

  @Inject
  private GlobalRepositorySettings repositorySettings;

  @Rule
  public LastDownloadedIntervalRule lastDownloadedRule = new LastDownloadedIntervalRule(() -> repositorySettings);

  private RawClient rawClient;

  @Before
  public void createHostedRepository() throws Exception {
    rawClient = rawClient(repos.createRawHosted(HOSTED_REPO));
  }

  @SuppressWarnings("java:S2699") // sonar isn't detecting nested assertions
  @Test
  public void uploadAndDownload() throws Exception {
    uploadAndDownload(rawClient, TEST_CONTENT);
  }

  @SuppressWarnings("java:S2699") // sonar isn't detecting nested assertions
  @Test
  public void redeploy() throws Exception {
    uploadAndDownload(rawClient, TEST_CONTENT);
    uploadAndDownload(rawClient, TEST_CONTENT);
  }

  @Test
  public void failWhenRedeployNotAllowed() throws Exception {
    rawClient = rawClient(repos.createRawHosted(testName.getMethodName(), "ALLOW_ONCE"));

    File testFile = resolveTestFile(TEST_CONTENT);

    assertThat(rawClient.put(TEST_CONTENT, TEXT_PLAIN, testFile), is(CREATED));

    assertThat(rawClient.put(TEST_CONTENT, TEXT_PLAIN, testFile), is(BAD_REQUEST));
  }

  @Test
  public void setLastDownloadOnGetNotPut() throws Exception {
    Repository repository = repos.createRawHosted(testName.getMethodName(), "ALLOW_ONCE");

    rawClient = rawClient(repository);

    File testFile = resolveTestFile(TEST_CONTENT);

    assertThat(rawClient.put(TEST_CONTENT, TEXT_PLAIN, testFile), is(CREATED));
    assertThat(getLastDownloadedTime(repository, testFile.getName()), is(equalTo(null)));

    HttpResponse response = rawClient.get(TEST_CONTENT);

    assertThat(status(response), is(OK));
    assertThat(bytes(response), is(Files.readAllBytes(testFile.toPath())));
    assertThat(getLastDownloadedTime(repository, testFile.getName()).isBeforeNow(), is(equalTo(true)));
  }

  @Test
  public void lastDownloadedIsUpdatedWhenFrequencyConfigured() throws Exception {
    lastDownloadedRule.setLastDownloadedInterval(Duration.ofSeconds(1));

    verifyLastDownloadedTime((newDate, initialDate) -> assertThat(newDate, is(greaterThan(initialDate))));
  }

  @Test
  public void lastDownloadedIsNotUpdatedWhenFrequencyNotExceeded() throws Exception {
    lastDownloadedRule.setLastDownloadedInterval(Duration.ofSeconds(10));

    verifyLastDownloadedTime((newDate, initialDate) -> assertThat(newDate, is(equalTo(initialDate))));
  }

  @Test
  public void inlineContentDispositionSetsHeader() throws Exception {
    Configuration configuration = repos.createHosted(testName.getMethodName(), "raw-hosted", "ALLOW_ONCE", true);
    configuration.attributes("raw")
        .set(ContentDispositionHandler.CONTENT_DISPOSITION_CONFIG_KEY, ContentDisposition.INLINE.name());

    Repository repository = repos.createRepository(configuration);

    rawClient = rawClient(repository);

    File testFile = resolveTestFile(TEST_CONTENT);

    assertThat(rawClient.put(TEST_CONTENT, TEXT_PLAIN, testFile), is(CREATED));
    assertThat(getLastDownloadedTime(repository, testFile.getName()), is(equalTo(null)));

    HttpResponse response = rawClient.get(TEST_CONTENT);
    assertThat(response.getFirstHeader("Content-Disposition").getValue(), is("inline"));
  }

  @Test
  public void attachmentContentDispositionSetsHeader() throws Exception {
    Configuration configuration = repos.createHosted(testName.getMethodName(), "raw-hosted", "ALLOW_ONCE", true);
    configuration.attributes("raw")
        .set(ContentDispositionHandler.CONTENT_DISPOSITION_CONFIG_KEY, ContentDisposition.ATTACHMENT.name());

    Repository repository = repos.createRepository(configuration);

    rawClient = rawClient(repository);

    File testFile = resolveTestFile(TEST_CONTENT);

    assertThat(rawClient.put(TEST_CONTENT, TEXT_PLAIN, testFile), is(CREATED));
    assertThat(getLastDownloadedTime(repository, testFile.getName()), is(equalTo(null)));

    HttpResponse response = rawClient.get(TEST_CONTENT);
    assertThat(response.getFirstHeader("Content-Disposition").getValue(), is("attachment"));
  }

  private void verifyLastDownloadedTime(final BiConsumer<DateTime, DateTime> matcher) throws Exception {
    Repository repository = repos.createRawHosted(testName.getMethodName(), "ALLOW_ONCE");

    RawClient rawClient = rawClient(repository);

    File testFile = resolveTestFile(TEST_CONTENT);
    assertThat(rawClient.put(TEST_CONTENT, TEXT_PLAIN, testFile), is(CREATED));

    rawClient.get(TEST_CONTENT);
    DateTime firstLastDownloadedTime = getLastDownloadedTime(repository, testFile.getName());

    sleep(2000);

    rawClient.get(TEST_CONTENT);
    DateTime newLastDownloadedTime = getLastDownloadedTime(repository, testFile.getName());

    matcher.accept(newLastDownloadedTime, firstLastDownloadedTime);
  }
}
