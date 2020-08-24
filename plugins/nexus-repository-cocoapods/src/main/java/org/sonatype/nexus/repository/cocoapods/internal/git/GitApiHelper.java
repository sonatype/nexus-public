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
package org.sonatype.nexus.repository.cocoapods.internal.git;

import java.net.URI;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @since 3.19
 */
@Named
public class GitApiHelper
{
  private static final String GITHUB_API_GET_REPO_SNAPSHOT_URL_TEMPLATE = "%1$s/repos/%2$s/%3$s/tarball/%4$s";

  private static final String BITBUCKET_API_GET_REPO_SNAPSHOT_URL_TEMPLATE = "%1$s/%2$s/%3$s/get/%4$s.tar.gz";

  private static final String GITLAB_API_GET_REPO_SNAPSHOT_URL_TEMPLATE = "%1$s/%2$s/%3$s/-/archive/%4$s/%4$s.tar.gz";

  private static final String DEFAULT_BRANCH = "master";

  private String githubApiUri;

  private String bitbucketApiUri;

  private String gitlabApiUri;

  @Inject
  public GitApiHelper(
      @Named("${nexus.cocoapods.gitHubApiUri:-https://api.github.com}") final String githubApiUri,
      @Named("${nexus.cocoapods.bitbucketApiUri:-https://bitbucket.org}") final String bitbucketApiUri,
      @Named("${nexus.cocoapods.gitlabApiUri:-https://gitlab.com}") final String gitlabApiUri) {

    this.githubApiUri = checkNotNull(githubApiUri);
    this.bitbucketApiUri = checkNotNull(bitbucketApiUri);
    this.gitlabApiUri = checkNotNull(gitlabApiUri);
  }

  public URI buildDownloadURI(final URI gitRepositoryUri, @Nullable final String ref)
  {
    GitArtifactInfo info = GitRepoUriParser.parseGitRepoUri(gitRepositoryUri, ref);

    GitProviderConfig config = prepareConfig(info.getHost());

    String downloadLink = String.format(
        config.template,
        config.apiUri,
        info.getVendor(),
        info.getRepository(),
        info.getRef() != null ? info.getRef() : config.defaultBranch);

    return URI.create(downloadLink);
  }

  private GitProviderConfig prepareConfig(final String host) {
    switch (host) {
      case GitConstants.GITHUB_HOST:
        return new GitProviderConfig(
            GITHUB_API_GET_REPO_SNAPSHOT_URL_TEMPLATE,
            githubApiUri,
            "");
      case GitConstants.BITBUCKET_HOST:
        return new GitProviderConfig(
            BITBUCKET_API_GET_REPO_SNAPSHOT_URL_TEMPLATE,
            bitbucketApiUri,
            DEFAULT_BRANCH);
      case GitConstants.GITLAB_HOST:
        return new GitProviderConfig(
            GITLAB_API_GET_REPO_SNAPSHOT_URL_TEMPLATE,
            gitlabApiUri,
            DEFAULT_BRANCH);
      default:
        throw new IllegalArgumentException("invalid host: " + host);
    }
  }

  private static class GitProviderConfig
  {
    String template;

    String apiUri;

    String defaultBranch;

    GitProviderConfig(final String template, final String apiUri, final String defaultBranch) {
      this.template = template;
      this.apiUri = apiUri;
      this.defaultBranch = defaultBranch;
    }
  }
}
