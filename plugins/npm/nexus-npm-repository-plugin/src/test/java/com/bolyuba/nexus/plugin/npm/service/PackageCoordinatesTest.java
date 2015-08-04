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

import org.sonatype.sisu.litmus.testsupport.TestSupport;

import com.bolyuba.nexus.plugin.npm.service.PackageRequest.PackageCoordinates;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;


/**
 * @author <a href="mailto:georgy@bolyuba.com">Georgy Bolyuba</a>
 */
public class PackageCoordinatesTest
    extends TestSupport
{

  @Test
  public void coordinatesFromUrl_RegistryRoot() throws IllegalArgumentException {
    PackageCoordinates coordinates = PackageCoordinates.coordinatesFromUrl("/");

    assertThat(coordinates.getType(), equalTo(PackageCoordinates.Type.REGISTRY_ROOT));
    assertThat(coordinates.getPackageName(), nullValue());
    assertThat(coordinates.getPackageVersion(), nullValue());
  }

  @Test
  public void coordinatesFromUrl_PackageRoot() throws IllegalArgumentException {
    PackageCoordinates coordinates = PackageCoordinates.coordinatesFromUrl("/gonogo");

    assertThat(coordinates.getType(), equalTo(PackageCoordinates.Type.PACKAGE_ROOT));
    assertThat(coordinates.getPackageName(), equalTo("gonogo"));
    assertThat(coordinates.getPackageVersion(), nullValue());
  }

  @Test
  public void coordinatesFromUrl_PackageVersion() throws IllegalArgumentException {
    PackageCoordinates coordinates = PackageCoordinates.coordinatesFromUrl("/gonogo/1.42.0");

    assertThat(coordinates.getType(), equalTo(PackageCoordinates.Type.PACKAGE_VERSION));
    assertThat(coordinates.getPackageName(), equalTo("gonogo"));
    assertThat(coordinates.getPackageVersion(), equalTo("1.42.0"));

  }

  @Test(expected = IllegalArgumentException.class)
  public void coordinatesFromUrl_UnrecognizedCrapAtTheEnd_ShouldThrow() throws IllegalArgumentException {
    PackageCoordinates.coordinatesFromUrl("/gonogo/1.42.0/gimmefive");

    fail("Expected coordinatesFromUrl to throw IllegalArgumentException");
  }

  @Test
  public void coordinatesFromUrl_PackageNameDash_ShouldThrow() throws IllegalArgumentException {
    PackageCoordinates coordinates = PackageCoordinates.coordinatesFromUrl("/-/all/");

    assertThat(coordinates.getType(), equalTo(PackageCoordinates.Type.REGISTRY_SPECIAL));
    assertThat(coordinates.getPackageVersion(), nullValue());
    assertThat(coordinates.getPackageName(), nullValue());
  }

  @Test(expected = IllegalArgumentException.class)
  public void coordinatesFromUrl_PackageNameStartWithDash_ShouldThrow() throws IllegalArgumentException {
    PackageCoordinates.coordinatesFromUrl("/-gonogo/1.42.0/");
    fail("Expected coordinatesFromUrl to throw IllegalArgumentException");
  }

  @Test(expected = IllegalArgumentException.class)
  public void coordinatesFromUrl_PackageNameDot_ShouldThrow() throws IllegalArgumentException {
    PackageCoordinates.coordinatesFromUrl("/./1.42.0/");
    fail("Expected coordinatesFromUrl to throw IllegalArgumentException");
  }

  @Test(expected = IllegalArgumentException.class)
  public void coordinatesFromUrl_PackageNameTwoDots_ShouldThrow() throws IllegalArgumentException {
    PackageCoordinates.coordinatesFromUrl("/../1.42.0/");
    fail("Expected coordinatesFromUrl to throw IllegalArgumentException");
  }

  @Test(expected = IllegalArgumentException.class)
  public void coordinatesFromUrl_PackageVersionStartWithDash_ShouldThrow() throws IllegalArgumentException {
    PackageCoordinates.coordinatesFromUrl("/gonogo/-1.42.0/");
    fail("Expected coordinatesFromUrl to throw IllegalArgumentException");
  }

  @Test(expected = IllegalArgumentException.class)
  public void coordinatesFromUrl_PackageVersionDot_ShouldThrow() throws IllegalArgumentException {
    PackageCoordinates.coordinatesFromUrl("/gonogo/./");
    fail("Expected coordinatesFromUrl to throw IllegalArgumentException");
  }

  @Test(expected = IllegalArgumentException.class)
  public void coordinatesFromUrl_PackageVersionTwoDots_ShouldThrow() throws IllegalArgumentException {
    PackageCoordinates.coordinatesFromUrl("/gonogo/../");
    fail("Expected coordinatesFromUrl to throw IllegalArgumentException");
  }


  @Test
  public void coordinatesFromUrl_CaseDoesNotMatter() throws IllegalArgumentException {
    PackageCoordinates coordinates = PackageCoordinates.coordinatesFromUrl("/GoNoGO/1.42.Rc1");

    assertThat(coordinates.getType(), equalTo(PackageCoordinates.Type.PACKAGE_VERSION));
    assertThat(coordinates.getPackageName(), equalTo("GoNoGO"));
    assertThat(coordinates.getPackageVersion(), equalTo("1.42.Rc1"));
    assertThat(coordinates.getPath(), equalTo("/GoNoGO/1.42.Rc1"));
  }
}
