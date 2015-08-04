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
package com.bolyuba.nexus.plugin.npm.internal;

import org.sonatype.sisu.litmus.testsupport.TestSupport;

import com.bolyuba.nexus.plugin.npm.NpmRepository;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Georgy Bolyuba (georgy@bolyuba.com)
 */
public class NpmMimeRulesSourceTest
    extends TestSupport
{
  private NpmMimeRulesSource sut = new NpmMimeRulesSource();

  @Test
  public void nullPath_noSuggestion() {
    String ruleForPath = sut.getRuleForPath(null);

    assertThat(ruleForPath, nullValue());
  }

  @Test
  public void randomPath_noSuggestion() {
    String ruleForPath = sut.getRuleForPath("/random/path");

    assertThat(ruleForPath, notNullValue());
    assertThat(ruleForPath, equalTo(NpmRepository.JSON_MIME_TYPE));
  }

  @Test
  public void packageRootContent_JsonMimeType() {
    String ruleForPath = sut.getRuleForPath("/package/-content.json");

    assertThat(ruleForPath, nullValue());
  }

  @Test
  public void packageVersionContent_JsonMimeType() {
    String ruleForPath = sut.getRuleForPath("/package/42.42.42/-content.json");

    assertThat(ruleForPath, nullValue());
  }

  @Test
  public void tarball_TarballMimeType() {
    String ruleForPath = sut.getRuleForPath("/package/-/package-1.0.0.tgz");

    assertThat(ruleForPath, notNullValue());
    assertThat(ruleForPath, equalTo(NpmRepository.TARBALL_MIME_TYPE));
  }
}
