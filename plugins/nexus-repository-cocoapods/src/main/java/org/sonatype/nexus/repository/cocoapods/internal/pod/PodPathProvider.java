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
package org.sonatype.nexus.repository.cocoapods.internal.pod;

import java.net.URI;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.repository.cocoapods.internal.CocoapodsConfig;
import org.sonatype.nexus.repository.cocoapods.internal.pod.git.GitApiHelper;
import org.sonatype.nexus.repository.cocoapods.internal.pod.git.GitArtifactInfo;
import org.sonatype.nexus.repository.cocoapods.internal.pod.git.GitRepoUriParser;

import static org.sonatype.nexus.repository.cocoapods.internal.pod.PodsUtils.escapeUriToPath;
import static org.sonatype.nexus.repository.cocoapods.internal.pod.git.GitConstants.GITHUB_HOST;

/**
 * @since 3.19
 */
@Named
public class PodPathProvider
{
  private static final String POD_PATH_TEMPLATE = "pods/%s/%s/%s";

  private static final String GITHUB_POD_PATH_TEMPLATE = "pods/%s/%s/%s.tar.gz";

  private CocoapodsConfig config;

  @Inject
  public PodPathProvider(
      @Named("${nexus.cocoapods.gitHubApiUri:-https://api.github.com}") final String githubApiUri,
      @Named("${nexus.cocoapods.bitbucketApiUri:-https://bitbucket.org}") final String bitbucketApiUri,
      @Named("${nexus.cocoapods.gitlabApiUri:-https://gitlab.com}") final String gitlabApiUri)
  {
    config = new CocoapodsConfig(githubApiUri, bitbucketApiUri, gitlabApiUri);
  }

  public String buildNxrmPath(
      final String name,
      final String version,
      final URI gitRepositoryUri,
      @Nullable final String ref)
  {
    GitArtifactInfo info = GitRepoUriParser.parseGitRepoUri(gitRepositoryUri, ref);
    final String gitApiUri = GitApiHelper.buildApiUri(info, config).toString();
    return String.format(
        info.getHost().equals(GITHUB_HOST) ? GITHUB_POD_PATH_TEMPLATE : POD_PATH_TEMPLATE,
        name,
        version,
        escapeUriToPath(gitApiUri));
  }

  public String buildNxrmPath(
      final String name, final String version, final String httpDownloadUri)
  {
    return String.format(
        POD_PATH_TEMPLATE,
        name,
        version,
        escapeUriToPath(httpDownloadUri));
  }
}
