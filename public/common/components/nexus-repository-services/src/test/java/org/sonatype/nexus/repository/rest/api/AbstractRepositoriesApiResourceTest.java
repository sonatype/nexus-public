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
package org.sonatype.nexus.repository.rest.api;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.rest.api.cdn.CdnDefaultsService;
import org.sonatype.nexus.repository.rest.api.model.AbstractRepositoryApiRequest;
import org.sonatype.nexus.repository.rest.api.model.StorageAttributes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AbstractRepositoriesApiResourceTest
{
  @Mock
  private CdnDefaultsService cdnDefaultsService;

  private TestRepositoriesApiResource underTest;

  @BeforeEach
  void setUp() {
    underTest = new TestRepositoriesApiResource();
  }

  @Test
  void applyGlobalDistributionDefault_delegatesToServiceWhenPresent() {
    underTest.setCdnDefaultsService(cdnDefaultsService);
    Configuration config = mock(Configuration.class);

    underTest.applyGlobalDistributionDefault(config);

    verify(cdnDefaultsService).applyDefaultIfAbsent(config);
  }

  @Test
  void applyGlobalDistributionDefault_isNoOpWhenServiceAbsent() {
    underTest.setCdnDefaultsService(null);
    Configuration config = mock(Configuration.class);

    underTest.applyGlobalDistributionDefault(config);

    verifyNoInteractions(cdnDefaultsService, config);
  }

  @Test
  void updateRepositorySourceDoesNotInvokeCdnDefaultsApplier() throws Exception {
    // Regression guard: applyGlobalDistributionDefault is create-only. Updates must preserve
    // whatever value the caller already stored — overwriting storage.globalDistribution with the
    // capability default would silently mutate existing repositories. A runtime negative
    // assertion here would drag in the full Shiro + Validate aspect fixture; scanning the source
    // of updateRepository catches the copy-paste mistake without any of that machinery.
    Path source = Paths.get(
        "src/main/java/org/sonatype/nexus/repository/rest/api/AbstractRepositoriesApiResource.java");
    assertThat(Files.exists(source))
        .as("expected to find AbstractRepositoriesApiResource.java relative to the module root")
        .isTrue();
    String content = Files.readString(source);

    int signature = content.indexOf("public Response updateRepository(");
    assertThat(signature).as("updateRepository signature").isNotNegative();
    int bodyStart = content.indexOf('{', signature);
    assertThat(bodyStart).as("updateRepository opening brace").isNotNegative();

    int depth = 0;
    int end = bodyStart;
    for (int i = bodyStart; i < content.length(); i++) {
      char ch = content.charAt(i);
      if (ch == '{') {
        depth++;
      }
      else if (ch == '}') {
        depth--;
        if (depth == 0) {
          end = i;
          break;
        }
      }
    }
    String methodBody = content.substring(bodyStart, end + 1);

    assertThat(methodBody).doesNotContain("applyGlobalDistributionDefault");
  }

  private static class TestRequest
      extends AbstractRepositoryApiRequest<StorageAttributes>
  {
    TestRequest(final StorageAttributes storage) {
      super("test-repo", "maven2", "hosted", true, storage);
    }
  }

  private static class TestRepositoriesApiResource
      extends AbstractRepositoriesApiResource<TestRequest>
  {
    @Override
    public Configuration adapt(final TestRequest request) {
      return mock(Configuration.class);
    }
  }
}
