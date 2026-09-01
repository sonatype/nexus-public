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
package org.sonatype.nexus.cleanup.config;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;

/**
 * Tests for {@link CleanupPolicyConstants}.
 */
public class CleanupPolicyConstantsTest
{
  @Test
  public void repositoriesFieldSupportedFormatsContainsExactlyTheExpectedFormats() {
    assertThat(CleanupPolicyConstants.REPOSITORIES_FIELD_SUPPORTED_FORMATS, hasSize(11));
    assertThat(CleanupPolicyConstants.REPOSITORIES_FIELD_SUPPORTED_FORMATS, containsInAnyOrder(
        "npm", "pypi", "go", "helm", "nuget",
        "yum", "rubygems", "terraform", "swift", "apt", "pub"));
  }

  @Test
  public void repositoriesFieldSupportedFormatsDoesNotContainMaven2() {
    assertThat(CleanupPolicyConstants.REPOSITORIES_FIELD_SUPPORTED_FORMATS, not(hasItem("maven2")));
  }

  @Test
  public void stringConstantsEqualTheirLiteralValues() {
    assertThat(CleanupPolicyConstants.CLEANUP_ATTRIBUTES_KEY, is("cleanup"));
    assertThat(CleanupPolicyConstants.CLEANUP_NAME_KEY, is("policyName"));
    assertThat(CleanupPolicyConstants.IS_PRERELEASE_KEY, is("isPrerelease"));
    assertThat(CleanupPolicyConstants.LAST_BLOB_UPDATED_KEY, is("lastBlobUpdated"));
    assertThat(CleanupPolicyConstants.LAST_DOWNLOADED_KEY, is("lastDownloaded"));
    assertThat(CleanupPolicyConstants.RETAIN_KEY, is("retain"));
    assertThat(CleanupPolicyConstants.RETAIN_SORT_BY_KEY, is("sortBy"));
    assertThat(CleanupPolicyConstants.REGEX_KEY, is("regex"));
    assertThat(CleanupPolicyConstants.MAVEN2_FORMAT, is("maven2"));
    assertThat(CleanupPolicyConstants.DOCKER_FORMAT, is("docker"));
  }
}
