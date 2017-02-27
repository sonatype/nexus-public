/*
 * Copyright (c) 2007-2014 Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.bolyuba.nexus.plugin.npm.service.internal;

import java.io.IOException;

import javax.annotation.Nullable;

import org.sonatype.nexus.proxy.RequestContext;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import com.bolyuba.nexus.plugin.npm.NpmRepository;
import com.bolyuba.nexus.plugin.npm.service.PackageRequest;
import com.bolyuba.nexus.plugin.npm.service.PackageRoot;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

public class GeneratorSupportTest
    extends TestSupport
{
  GeneratorSupport underTest;

  @Mock
  NpmRepository npmRepository;

  @Mock
  MetadataParser metadataParser;

  @Mock
  ResourceStoreRequest resourceStoreRequest;

  @Mock
  RequestContext requestContext;

  @Before
  public void setUp() throws Exception {
    when(resourceStoreRequest.getRequestContext()).thenReturn(requestContext);
    underTest = new GeneratorSupport(npmRepository, metadataParser)
    {
      @Override
      protected PackageRootIterator doGenerateRegistryRoot(final PackageRequest request) throws IOException {
        return null;
      }

      @Nullable
      @Override
      protected PackageRoot doGeneratePackageRoot(final PackageRequest request) throws IOException {
        return null;
      }
    };
  }

  @Test
  public void testScopeOnlyNotNpmMetadataServiced() {
    when(resourceStoreRequest.getRequestPath()).thenReturn("/@sonatype/");
    assertThat(underTest.isNpmMetadataServiced(resourceStoreRequest), is(false));
  }

  @Test
  public void testScopeOnlyNotNpmMetadataServiced_noTrailingSlash() {
    when(resourceStoreRequest.getRequestPath()).thenReturn("/@sonatype");
    assertThat(underTest.isNpmMetadataServiced(resourceStoreRequest), is(false));
  }

  @Test
  public void testScopedPackageIsServiced() {
    when(resourceStoreRequest.getRequestPath()).thenReturn("/@sonatype/custom-project");
    assertThat(underTest.isNpmMetadataServiced(resourceStoreRequest), is(true));
  }

  @Test
  public void testPackageIsServiced() {
    when(resourceStoreRequest.getRequestPath()).thenReturn("/custom-project");
    assertThat(underTest.isNpmMetadataServiced(resourceStoreRequest), is(true));
  }
}
