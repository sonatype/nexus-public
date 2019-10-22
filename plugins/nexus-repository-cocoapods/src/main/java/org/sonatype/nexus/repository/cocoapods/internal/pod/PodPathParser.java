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
import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Named;

import static org.sonatype.nexus.repository.cocoapods.internal.pod.PodsUtils.unescapePathToUri;
import static org.sonatype.nexus.repository.cocoapods.internal.pod.git.GitConstants.GITHUB_POD_EXTENSION;

/**
 * @since 3.19
 */
@Named
public class PodPathParser
{
  private static final int NAME_POSITION = 1;

  private static final int VERSION_POSITION = 2;

  private static final int URL_START_POSITION = 3;

  private static final int URL_HOST_POSITION = 4;

  private static final String URL_START_SEGMENT_HTTP = "http";

  private static final String URL_START_SEGMENT_HTTPS = "https";

  private static final int MINIMUM_SEGMENT_LENGTH = 5;

  private String githubApiHost;

  @Inject
  public PodPathParser(@Named("${nexus.cocoapods.gitHubApiHost:-api.github.com}") final String githubApiHost) {
    this.githubApiHost = githubApiHost;
  }

  /**
   * Returns {@link PodInfo} from podPath
   * <p>
   * PodPath Example: pods/NameOfPod/VersionOfPod/https/api.github.com/repos/VendorOfPod/NameOfPod/tarball/1.0.0.tar.gz
   *
   * @param podPath path where cocoapods pod file is or will be stored.
   * @return {@link PodInfo}
   */
  public PodInfo parse(final String podPath) {
    String[] segments = podPath.split("/");
    validatePodPath(segments, podPath);

    final String name = segments[NAME_POSITION];
    final String version = segments[VERSION_POSITION];
    final String uriPath = getUriPath(segments);

    return new PodInfo(name, version, formatPathToUri(uriPath, segments[URL_HOST_POSITION]));
  }

  private void validatePodPath(final String[] segments, final String podPath) {
    if (segments.length < MINIMUM_SEGMENT_LENGTH) {
      throw new IllegalArgumentException("Invalid segments count in filename: " + podPath);
    }
    if (!URL_START_SEGMENT_HTTP.equals(segments[URL_START_POSITION])
        && !URL_START_SEGMENT_HTTPS.equals(segments[URL_START_POSITION])) {
      throw new IllegalArgumentException("Invalid URI in filename: " + podPath);
    }
  }

  private String getUriPath(String[] segments) {
    return String.join("/", Arrays.copyOfRange(segments, URL_START_POSITION, segments.length));
  }

  private URI formatPathToUri(final String path, final String host) {
    String uri = unescapePathToUri(path);
    if (githubApiHost.equals(host)) {
      uri = uri.replace(GITHUB_POD_EXTENSION, "");
    }
    return URI.create(uri);
  }
}
