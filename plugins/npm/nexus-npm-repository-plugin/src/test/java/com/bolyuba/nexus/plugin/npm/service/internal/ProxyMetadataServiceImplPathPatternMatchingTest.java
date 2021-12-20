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

import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Matcher;

import org.sonatype.sisu.goodies.testsupport.TestSupport;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(Parameterized.class)
public class ProxyMetadataServiceImplPathPatternMatchingTest
    extends TestSupport
{

  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        {"/@angular/forms/-/forms-2.4.8.tgz", true, "@angular/forms", "forms-2.4.8.tgz"},
        {"/@angular/forms/-/forms-beta-2.4.8.tgz", true, "@angular/forms", "forms-beta-2.4.8.tgz"},
        {"/angular-forms/-/forms-0.1.1.tgz", true, "angular-forms", "forms-0.1.1.tgz"},
        {"/angular-forms/-/forms-alpha-0.1.1.tgz", true, "angular-forms", "forms-alpha-0.1.1.tgz"},
        {"/cdb/-/cdb-0.3.4.tgz", true, "cdb", "cdb-0.3.4.tgz"},
        {"/left-pad/-/left-pad-1.1.3.tgz", true, "left-pad", "left-pad-1.1.3.tgz"},
        {"/angular/forms/-/forms-beta-2.4.8.tgz", false, null, null},
        {"/cdb/-/cdb-0.3.4.zip", false, null, null}
    });
  }

  private final String path;

  private final boolean matches;

  private final String expectedPackageName;

  private final String expectedFilename;

  public ProxyMetadataServiceImplPathPatternMatchingTest(final String path,
                                                         boolean matches,
                                                         final String expectedPackageName,
                                                         final String expectedFilename)
  {
    this.path = path;
    this.matches = matches;
    this.expectedPackageName = expectedPackageName;
    this.expectedFilename = expectedFilename;
  }

  @Test
  public void test() {
    Matcher matcher = ProxyMetadataServiceImpl.TARBALL_PATH_PATTERN.matcher(path);
    assertThat(matcher.matches(), is(matches));

    if (matches) {
      assertThat(matcher.group(1), is(expectedPackageName));
      assertThat(matcher.group(2), is(expectedFilename));
    }
  }
}
