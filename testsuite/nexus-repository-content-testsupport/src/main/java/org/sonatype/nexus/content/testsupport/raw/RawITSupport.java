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
package org.sonatype.nexus.content.testsupport.raw;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.sonatype.nexus.content.testsupport.FormatClientSupport;
import org.sonatype.nexus.content.testsupport.NexusITSupport;
import org.sonatype.nexus.content.testsupport.fixtures.RepositoryRule;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.http.HttpStatus;
import org.sonatype.nexus.repository.manager.RepositoryManager;

import org.apache.http.entity.ContentType;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Rule;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class RawITSupport
    extends NexusITSupport
{
  protected static final String SLASH_REPO_SLASH = "/repository/";

  @Inject
  protected RepositoryManager repositoryManager;

  @Rule
  public RepositoryRule repos = createRepositoryRule();

  public RawITSupport() {
    testData.addDirectory(resolveBaseFile("target/it-resources/raw"));
  }

  protected RepositoryRule createRepositoryRule() {
    return new RepositoryRule(() -> repositoryManager);
  }

  @Nonnull
  protected URL repositoryBaseUrl(final Repository repository) {
    return resolveUrl(nexusUrl, SLASH_REPO_SLASH + repository.getName() + "/");
  }

  @Nonnull
  protected RawClient rawClient(final Repository repository) throws Exception {
    checkNotNull(repository);
    return rawClient(repositoryBaseUrl(repository));
  }

  protected RawClient rawClient(final URL repositoryUrl) throws Exception {
    return new RawClient(
        clientBuilder(repositoryUrl).build(),
        clientContext(),
        repositoryUrl.toURI());
  }

  protected void uploadAndDownload(final RawClient rawClient, final String file) throws Exception {
    final File testFile = resolveTestFile(file);
    final int response = rawClient.put(file, ContentType.TEXT_PLAIN, testFile);
    MatcherAssert.assertThat(response, Matchers.is(HttpStatus.CREATED));

    MatcherAssert.assertThat(FormatClientSupport.bytes(rawClient.get(file)), is(Files.readAllBytes(testFile.toPath())));

    MatcherAssert.assertThat(FormatClientSupport.status(rawClient.delete(file)), Matchers.is(HttpStatus.NO_CONTENT));

    assertThat("content should be deleted", FormatClientSupport.status(rawClient.get(file)),
        Matchers.is(HttpStatus.NOT_FOUND));
  }
}
