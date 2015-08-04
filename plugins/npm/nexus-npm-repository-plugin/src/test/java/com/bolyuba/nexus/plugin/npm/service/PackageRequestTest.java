/*
 * Copyright (c) 2007-2014 Sonatype, Inc. and Georgy Bolyuba. All rights reserved.
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
package com.bolyuba.nexus.plugin.npm.service;

import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

/**
 * @author Georgy Bolyuba (georgy@bolyuba.com)
 */
public class PackageRequestTest
    extends TestSupport
{

  @Mock
  ResourceStoreRequest mockRequest;

  @Test
  public void sameRequestReturned() throws IllegalArgumentException {
    Mockito.when(mockRequest.getRequestPath()).thenReturn("/");
    PackageRequest packageRequest = new PackageRequest(mockRequest);

    assertThat("Expected to get same store request beginObj back", packageRequest.getStoreRequest(),
        equalTo(mockRequest));
  }

  @Test
  public void correctPathReturned() throws IllegalArgumentException {
    Mockito.when(mockRequest.getRequestPath()).thenReturn("/-/all");
    PackageRequest packageRequest = new PackageRequest(mockRequest);

    assertThat(packageRequest.getPath(), equalTo("/-/all"));
  }

  @Test
  public void registryRoot() throws IllegalArgumentException {
    Mockito.when(mockRequest.getRequestPath()).thenReturn("/");
    PackageRequest packageRequest = new PackageRequest(mockRequest);

    assertThat(packageRequest.isPackage(), is(false));
    assertThat(packageRequest.isPackageRoot(), is(false));
    assertThat(packageRequest.isPackageVersion(), is(false));
    assertThat(packageRequest.isRegistrySpecial(), is(false));
    assertThat(packageRequest.isRegistryRoot(), is(true));
  }

  @Test
  public void packageRoot() throws IllegalArgumentException {
    Mockito.when(mockRequest.getRequestPath()).thenReturn("/golem");
    PackageRequest packageRequest = new PackageRequest(mockRequest);

    assertThat(packageRequest.isPackage(), is(true));
    assertThat(packageRequest.isPackageRoot(), is(true));
    assertThat(packageRequest.isPackageVersion(), is(false));
    assertThat(packageRequest.isRegistrySpecial(), is(false));
    assertThat(packageRequest.isRegistryRoot(), is(false));
  }

  @Test
  public void packageVersion() throws IllegalArgumentException {
    Mockito.when(mockRequest.getRequestPath()).thenReturn("/golem/139.16");
    PackageRequest packageRequest = new PackageRequest(mockRequest);

    assertThat(packageRequest.isPackage(), is(true));
    assertThat(packageRequest.isPackageRoot(), is(false));
    assertThat(packageRequest.isPackageVersion(), is(true));
    assertThat(packageRequest.isRegistrySpecial(), is(false));
  }

  @Test
  public void registrySpecial() throws IllegalArgumentException {
    Mockito.when(mockRequest.getRequestPath()).thenReturn("/-/all");
    PackageRequest packageRequest = new PackageRequest(mockRequest);

    assertThat(packageRequest.isPackage(), is(false));
    assertThat(packageRequest.isPackageRoot(), is(false));
    assertThat(packageRequest.isPackageVersion(), is(false));
    assertThat(packageRequest.isRegistrySpecial(), is(true));
  }

  @Test
  public void scopedPackage() throws IllegalArgumentException {
    Mockito.when(mockRequest.getRequestPath()).thenReturn("/@sonatype/package");
    PackageRequest packageRequest = new PackageRequest(mockRequest);

    assertThat(packageRequest.isPackage(), is(true));
    assertThat(packageRequest.isScoped(), is(true));
    assertThat(packageRequest.isPackageRoot(), is(true));
    assertThat(packageRequest.isPackageVersion(), is(false));
    assertThat(packageRequest.isRegistrySpecial(), is(false));

    assertThat(packageRequest.getName(), equalTo("@sonatype/package"));
    assertThat(packageRequest.getScope(), equalTo("sonatype"));
  }


  // Check that content cache escapes registry namespace
  @Test(expected = IllegalArgumentException.class)
  public void packageRootContentCache_InvalidPackageRequest() throws IllegalArgumentException {
    Mockito.when(mockRequest.getRequestPath()).thenReturn("/golem/-content.json");
    new PackageRequest(mockRequest);
    fail("Expected InvalidPackageRequestException");
  }

  @Test(expected = IllegalArgumentException.class)
  public void packageVersionContentCache_InvalidPackageRequest() throws IllegalArgumentException {
    Mockito.when(mockRequest.getRequestPath()).thenReturn("/golem/1.42.0/-content.json");
    new PackageRequest(mockRequest);
    fail("Expected InvalidPackageRequestException");
  }
}
