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
package org.sonatype.nexus.bootstrap.entrypoint.edition.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Covers {@link ClusteringUnsupportedValidator}: Nexus Repository Core can never cluster, so an instance that sets
 * {@code nexus.datastore.clustered.enabled=true} must fail fast at startup rather than booting a silently
 * non-functional "cluster". Unlike the self-hosted distributions' equivalent guard, there is no edition or
 * license to check, and the refusal message must stay free of licensing language.
 */
class ClusteringUnsupportedValidatorTest
{
  // AT-005: flag true -> throws IllegalStateException, message names the property, nexus.properties,
  // and the DATASTORE_CLUSTERED_ENABLED environment variable that can override it (NexusPropertiesVerifier
  // .applyEnvironmentVariables() applies it over nexus.properties, so a file-only remedy is incomplete), and
  // does NOT mention "license" or "Pro".
  @Test
  void clusteredEnabledThrowsWithoutMentioningLicensingOrPro() {
    Throwable thrown = catchThrowable(() -> underTest(true).enforceClusteringIsNotEnabled());

    assertThat(thrown).isInstanceOf(IllegalStateException.class);
    assertThat(thrown.getMessage())
        .contains("nexus.datastore.clustered.enabled")
        .contains("nexus.properties")
        .contains("DATASTORE_CLUSTERED_ENABLED")
        .doesNotContainIgnoringCase("license")
        // Word-boundary rather than plain substring: "nexus.properties" itself contains "pro", so a
        // naive containsIgnoringCase("Pro") would false-positive against the property-file name this
        // very message is required to mention.
        .doesNotContainPattern("(?i)\\bPro\\b");
  }

  @Test
  void clusteredDisabledDoesNotThrow() {
    assertThatCode(() -> underTest(false).enforceClusteringIsNotEnabled()).doesNotThrowAnyException();
  }

  // The property is opt-in only - it has no entry in nexus-default.properties - and @Value's ":false"
  // default resolves an absent value to false, so an absent value is the ordinary case for a Core
  // instance and must not be mistaken for clustering being switched on.
  private ClusteringUnsupportedValidator underTest(final boolean clusteredModeEnabled) {
    return new ClusteringUnsupportedValidator(clusteredModeEnabled);
  }
}
