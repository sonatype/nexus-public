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
package org.sonatype.nexus.testsuite.testsupport;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.http.HttpResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.io.Files.write;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.readAllBytes;
import static org.apache.http.util.EntityUtils.toByteArray;
import static org.junit.Assert.fail;
import static org.sonatype.nexus.common.io.NetworkHelper.findLocalHostAddress;

/**
 * Support class for Format Client ITs tested through the Docker Test Support.
 *
 * @since 3.6.1
 */
public abstract class FormatClientITSupport
    extends RepositoryITSupport
{
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder(resolveTmpDir());

  /**
   * This reflects the host name that should be used to identify the docker host of the docker client
   */
  protected static final String DOCKER_HOST_NAME = "nexus-docker-testsupport";

  protected File rootTemporaryFolder;

  protected File downloadsTemporaryFolder;

  /**
   * We are doing an override of the {@link NexusITSupport#}
   */
  @Configuration
  public static Option[] configureNexus() {
    return options(RepositoryITSupport.configureNexus(),
        nexusFeature("org.sonatype.nexus.testsuite", "nexus-docker-testsupport"),
        withHttps(resolveBaseFile(format("target/it-resources/ssl/%s.jks", DOCKER_HOST_NAME))));
  }

  /**
   * Convenience method that helps setting up. Currently it sets up our {@link #downloadsTemporaryFolder}
   */
  @Before
  public void onInitializeForFormatClientTesting() throws Exception {
    rootTemporaryFolder = temporaryFolder.getRoot();
    downloadsTemporaryFolder = temporaryFolder.newFolder("downloads");
  }

  /**
   * Convenience method that helps doing cleanup. Currently it deletes the {@link #temporaryFolder}
   */
  @After
  public void onTearDownFormatClientTesting() {
    temporaryFolder.delete();
  }

  /**
   * Convenience method that allows a file to be downloaded from an {@link HttpResponse} into sub directory "downloads"
   * of the directory {@link #temporaryFolder}.
   *
   * @param httpResponse {@link HttpResponse}
   * @param name         file name to be given to downloaded file
   */
  protected File downloadFromHttpResponse(final HttpResponse httpResponse, final String name) {
    checkNotNull(httpResponse);
    checkNotNull(name);

    File file = null;

    try {
      file = new File(downloadsTemporaryFolder, name);
      write(toByteArray(httpResponse.getEntity()), file);
    }
    catch (IOException e) { // NOSONAR
      fail("Failed to download file from HttpResponse");
    }

    return file;
  }

  /**
   * Convenience method that allows adding test data paths.
   *
   * @param path location to add, e.g. "target/it-resources/yum"
   */
  protected void addTestDataDirectory(String path) {
    testData.addDirectory(resolveBaseFile(path));
  }

  private static File resolveTmpDir() {
    File tmpDir = new File(System.getProperty("java.io.tmpdir"));
    try {
      return tmpDir.getCanonicalFile();
    }
    catch (IOException e) { // NOSONAR: fall back to 'best-effort' absolute form
      return tmpDir.getAbsoluteFile();
    }
  }

  /**
   * Write the given file to the {@link #temporaryFolder}.
   *
   * @return File the local tmp file.
   */
  protected File writeTmpFile(final String fileName) throws IOException {
    File file = new File(rootTemporaryFolder, fileName);
    write(readTestDataFile(fileName).getBytes(UTF_8), file);
    return file;
  }

  /**
   * Read the given file from {@link #testData} folder
   *
   * @return String of read bytes.
   */
  protected String readTestDataFile(final String fileName) throws IOException {
    return new String(readAllBytes(testData.resolveFile(fileName).toPath()), UTF_8);
  }

  /**
   * Retrieve the Repo URL and used the {@link #nexusUrl} as its root
   *
   * @param repoName repository for which to get repo path for
   * @return String with Repo URL path
   */
  protected String getRepoUrl(final String repoName) {
    return convertUrl(repoName, nexusUrl);
  }

  /**
   * Retrieve the Repo URL and used the {@link #nexusSecureUrl} as its root
   *
   * @param repoName repository for which to get repo path for
   * @return String with Repo URL path
   */
  protected String getSecureRepoUrl(final String repoName) {
    return convertUrl(repoName, nexusSecureUrl);
  }

  private String convertUrl(final String repoName, final URL nexusSecureUrl) {
    String repoUrl = getRepoUrl(nexusSecureUrl, repoName);

    try {
      repoUrl = repoUrl.replaceAll("localhost", findLocalHostAddress());
    }
    catch (Exception e) {
      throw new RuntimeException("Unable to get Repo URL", e);
    }

    return repoUrl;
  }

  /**
   * Retrieve the Repo URL and used given {@link URL} as its root.
   *
   * @param url the base root
   * @param repoName repository for which to get repo path for
   * @param host name or ip to replace instead of localhost
   * @return String with Repo URL path
   */
  protected String getRepoUrl(final URL url, final String repoName, final String host) {
    return getRepoUrl(url, repoName).replaceAll("localhost", host);
  }

  /**
   * Retrieve the Repo URL and used given {@link URL} as its root
   *
   * @param url the base root
   * @param repoName repository for which to get repo path for
   * @return String with Repo URL path
   */
  protected String getRepoUrl(final URL url, final String repoName) {
    return resolveUrl(url, "/repository/" + repoName + "/").toString();
  }

  /**
   * Convenience method to get the absolute path from inside {@link #rootTemporaryFolder}
   *
   * @param fileName name of file expected to be in the {@link #rootTemporaryFolder}
   * @return String containing the absolute path to given file in the {@link #rootTemporaryFolder}
   */
  protected String fromRoot(final String fileName) {
    return rootTemporaryFolder + "/" + fileName;
  }
}
