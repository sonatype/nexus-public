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
package org.sonatype.nexus.content.testsuite;

import java.io.File;

import org.sonatype.nexus.content.testsuite.groups.SQLTestGroup;
import org.sonatype.nexus.content.testsupport.raw.RawClient;
import org.sonatype.nexus.content.testsupport.raw.RawITSupport;

import com.google.common.io.Files;
import org.apache.http.HttpResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.apache.http.entity.ContentType.TEXT_PLAIN;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.sonatype.nexus.content.testsupport.FormatClientSupport.bytes;
import static org.sonatype.nexus.content.testsupport.FormatClientSupport.status;
import static org.sonatype.nexus.repository.http.HttpStatus.NOT_FOUND;

@Category(SQLTestGroup.class)
public class RawGroupIT
    extends RawITSupport
{
  private static final String HOSTED_REPO_1 = "raw-test-hosted-repo1";

  private static final String HOSTED_REPO_2 = "raw-test-hosted-repo2";

  private static final String GROUP_REPO = "raw-test-group-repo";

  private static final String TEST_CONTENT_1 = "alphabet.txt";

  private static final String TEST_CONTENT_2 = "alphabet2.txt";

  private static final String TEST_PATH_1 = TEST_CONTENT_1;

  private RawClient rawHostedClient1;

  private RawClient rawHostedClient2;

  private RawClient groupClient;

  @Before
  public void setup() throws Exception {
    rawHostedClient1 = rawClient(repos.createRawHosted(HOSTED_REPO_1));
    rawHostedClient2 = rawClient(repos.createRawHosted(HOSTED_REPO_2));
    groupClient = rawClient(repos.createRawGroup(GROUP_REPO, HOSTED_REPO_1, HOSTED_REPO_2));
  }

  @Test
  public void emptyMembersReturn404() throws Exception {
    HttpResponse httpResponse = groupClient.get(TEST_CONTENT_1);
    assertThat(status(httpResponse), is(NOT_FOUND));
  }

  /**
   * The group consults members in order, returning the first success.
   */
  @Test
  public void firstSuccessfulResponseWins() throws Exception {
    File testFile = resolveTestFile(TEST_CONTENT_1);
    rawHostedClient1.put(TEST_PATH_1, TEXT_PLAIN, testFile);
    rawHostedClient2.put(TEST_PATH_1, TEXT_PLAIN, resolveTestFile(TEST_CONTENT_2));

    assertThat(bytes(groupClient.get(TEST_PATH_1)), is(Files.toByteArray(testFile)));
    rawHostedClient1.delete(TEST_PATH_1);
    rawHostedClient2.delete(TEST_PATH_1);
  }

  /**
   * Members that return failure responses are ignored in favor of successful ones.
   */
  @Test
  public void earlyFailuresAreBypassed() throws Exception {
    File testFile = resolveTestFile(TEST_CONTENT_1);

    // Only the second repository has any content
    rawHostedClient2.put(TEST_PATH_1, TEXT_PLAIN, resolveTestFile(TEST_CONTENT_1));

    assertThat(bytes(groupClient.get(TEST_PATH_1)), is(Files.toByteArray(testFile)));
  }

  @Test
  public void shouldFindAssetsInGroupRepository() throws Exception {
    File testFile1 = resolveTestFile(TEST_CONTENT_1);
    File testFile2 = resolveTestFile(TEST_CONTENT_2);

    rawHostedClient1.put(TEST_CONTENT_1, TEXT_PLAIN, testFile1);
    rawHostedClient2.put(TEST_CONTENT_2, TEXT_PLAIN, testFile2);

    assertThat(bytes(groupClient.get(TEST_CONTENT_1)), is(Files.toByteArray(testFile1)));
    assertThat(bytes(groupClient.get(TEST_CONTENT_2)), is(Files.toByteArray(testFile2)));

    rawHostedClient1.delete(TEST_CONTENT_1);
    rawHostedClient2.delete(TEST_CONTENT_2);
  }
}
