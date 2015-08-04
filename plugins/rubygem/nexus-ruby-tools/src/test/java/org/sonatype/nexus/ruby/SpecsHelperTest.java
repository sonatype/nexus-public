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
package org.sonatype.nexus.ruby;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.sonatype.sisu.litmus.testsupport.TestSupport;

import org.apache.commons.io.IOUtils;
import org.jruby.embed.PathType;
import org.jruby.runtime.builtin.IRubyObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

public class SpecsHelperTest
    extends TestSupport
{
  @Rule
  public TestJRubyContainerRule scriptingContainerRule = new TestJRubyContainerRule();

  private SpecsHelper specsHelper;

  private IRubyObject check;

  @Before
  public void setUp() throws Exception {
    check = scriptingContainerRule.getScriptingContainer().parse(PathType.CLASSPATH, "nexus/check.rb").run();
    specsHelper = scriptingContainerRule.getRubygemsGateway().newSpecsHelper();
  }

  @Test
  public void testEmptySpecs() throws Exception {
    File empty = new File("target/empty");

    dumpStream(specsHelper.createEmptySpecs(), empty);

    int size = scriptingContainerRule.getScriptingContainer().callMethod(check, "specs_size",
        empty.getAbsolutePath(), Integer.class);
    assertThat("specsfile size", size, equalTo(0));
  }

  @Test
  public void testListAllVersions() throws Exception {
    File some = new File("src/test/resources/some_specs");

    List<String> versions = specsHelper.listAllVersions("bla_does_not_exist",
        new FileInputStream(some));
    assertThat("versions size", versions, hasSize(0));

    versions = specsHelper.listAllVersions("activerecord", new FileInputStream(some));
    assertThat("versions size", versions, hasSize(1));
    assertThat("version", versions.get(0), equalTo("3.2.11-ruby"));
  }

  @Test
  public void testAddLatestGemToSpecs() throws Exception {
    File empty = new File("src/test/resources/empty_specs");
    File target = new File("target/test_specs");
    File gem = new File("src/test/resources/gems/n/nexus-0.1.0.gem");

    IRubyObject spec1 = scriptingContainerRule.getRubygemsGateway().newGemspecHelperFromGem(new FileInputStream(gem)).gemspec();

    // add gem
    InputStream is = specsHelper.addSpec(spec1, new FileInputStream(empty),
        SpecsIndexType.LATEST);

    // add another gem with different platform
    gem = new File("src/test/resources/gems/n/nexus-0.1.0-java.gem");
    IRubyObject specJ = scriptingContainerRule.getRubygemsGateway().newGemspecHelperFromGem(new FileInputStream(gem)).gemspec();
    is = specsHelper.addSpec(specJ, is, SpecsIndexType.LATEST);

    dumpStream(is, target);

    int size = scriptingContainerRule.getScriptingContainer().callMethod(check, "specs_size",
        target.getAbsolutePath(), Integer.class);
    assertThat("specsfile size", size, equalTo(2));

    // add a gem with newer version
    gem = new File("src/test/resources/gems/n/nexus-0.2.0.gem");
    IRubyObject spec = scriptingContainerRule.getRubygemsGateway().newGemspecHelperFromGem(new FileInputStream(gem)).gemspec();
    is = specsHelper.addSpec(spec, new FileInputStream(target), SpecsIndexType.LATEST);

    dumpStream(is, target);

    size = scriptingContainerRule.getScriptingContainer().callMethod(check, "specs_size",
        target.getAbsolutePath(), Integer.class);
    assertThat("specsfile size", size, equalTo(2));

    // add both the gems with older version
    is = specsHelper.addSpec(spec1, new FileInputStream(target), SpecsIndexType.LATEST);
    assertThat("no change", is, nullValue());
    is = specsHelper.addSpec(specJ, new FileInputStream(target), SpecsIndexType.LATEST);
    assertThat("no change", is, nullValue());
  }

  @Test
  public void testDeleteLatestGemToSpecs() throws Exception {
    File empty = new File("src/test/resources/empty_specs");
    File latestSpecsFile = new File("target/test_latest_specs");
    File releaseSpecsFile = new File("target/test_release_specs");

    File gem = new File("src/test/resources/gems/n/nexus-0.1.0.gem");
    IRubyObject spec = scriptingContainerRule.getRubygemsGateway().newGemspecHelperFromGem(new FileInputStream(gem)).gemspec();

    // add gem
    InputStream releaseStream = specsHelper.addSpec(spec, new FileInputStream(empty), SpecsIndexType.RELEASE);

    // add another gem with different platform
    gem = new File("src/test/resources/gems/n/nexus-0.1.0-java.gem");
    spec = scriptingContainerRule.getRubygemsGateway().newGemspecHelperFromGem(new FileInputStream(gem)).gemspec();
    releaseStream = specsHelper.addSpec(spec, releaseStream, SpecsIndexType.RELEASE);
    dumpStream(releaseStream, releaseSpecsFile);

    // add a gem with newer version to release and latest index
    gem = new File("src/test/resources/gems/n/nexus-0.2.0.gem");
    IRubyObject s = scriptingContainerRule.getRubygemsGateway().newGemspecHelperFromGem(new FileInputStream(gem)).gemspec();

    releaseStream = specsHelper.addSpec(s, new FileInputStream(releaseSpecsFile), SpecsIndexType.RELEASE);
    dumpStream(releaseStream, releaseSpecsFile);

    InputStream is = specsHelper.addSpec(s, new FileInputStream(empty), SpecsIndexType.LATEST);
    dumpStream(is, latestSpecsFile);

    // check the latest index
    int size = scriptingContainerRule.getScriptingContainer().callMethod(check, "specs_size",
        latestSpecsFile.getAbsolutePath(), Integer.class);
    assertThat("specsfile size", size, equalTo(1));

    // check the release index
    size = scriptingContainerRule.getScriptingContainer().callMethod(check, "specs_size",
        releaseSpecsFile.getAbsolutePath(), Integer.class);
    assertThat("specsfile size", size, equalTo(3));

    // first delete it from the release specs index
    is = specsHelper.deleteSpec(s, new FileInputStream(releaseSpecsFile), SpecsIndexType.RELEASE);
    dumpStream(is, releaseSpecsFile);

    size = scriptingContainerRule.getScriptingContainer().callMethod(check, "specs_size",
        releaseSpecsFile.getAbsolutePath(), Integer.class);
    assertThat("specsfile size", size, equalTo(2));

    // then delete it from the latest specs index
    is = specsHelper.deleteSpec(s, new FileInputStream(latestSpecsFile), SpecsIndexType.LATEST);
    dumpStream(is, latestSpecsFile);

    size = scriptingContainerRule.getScriptingContainer().callMethod(check, "specs_size",
        latestSpecsFile.getAbsolutePath(), Integer.class);
    assertThat("specsfile size", size, equalTo(2));

    // now delete one more spec
    specsHelper = scriptingContainerRule.getRubygemsGateway().newSpecsHelper();
    is = specsHelper.deleteSpec(spec, new FileInputStream(releaseSpecsFile), SpecsIndexType.RELEASE);
    dumpStream(is, releaseSpecsFile);

    size = scriptingContainerRule.getScriptingContainer().callMethod(check, "specs_size",
        releaseSpecsFile.getAbsolutePath(), Integer.class);
    assertThat("specsfile size", size, equalTo(1));

    is = specsHelper.deleteSpec(spec, new FileInputStream(latestSpecsFile), SpecsIndexType.LATEST);
    dumpStream(is, latestSpecsFile);

    size = scriptingContainerRule.getScriptingContainer().callMethod(check, "specs_size",
        latestSpecsFile.getAbsolutePath(), Integer.class);
    assertThat("specsfile size", size, equalTo(1));
  }

  @Test
  public void testAddDeleteReleasedGemToSpecs() throws Exception {
    File empty = new File("src/test/resources/empty_specs");
    File target = new File("target/test_specs");
    File gem = new File("src/test/resources/gems/n/nexus-0.1.0.gem");

    IRubyObject spec = scriptingContainerRule.getRubygemsGateway().newGemspecHelperFromGem(new FileInputStream(gem)).gemspec();

    // add released gem
    InputStream is = specsHelper.addSpec(spec, new FileInputStream(empty), SpecsIndexType.RELEASE);
    dumpStream(is, target);

    int size = scriptingContainerRule.getScriptingContainer().callMethod(check, "specs_size",
        target.getAbsolutePath(), Integer.class);
    assertThat("specsfile size", size, equalTo(1));

    // delete gem
    is = specsHelper.deleteSpec(spec, new FileInputStream(target), SpecsIndexType.RELEASE);
    dumpStream(is, target);

    size = scriptingContainerRule.getScriptingContainer().callMethod(check, "specs_size",
        target.getAbsolutePath(), Integer.class);
    assertThat("specsfile size", size, equalTo(0));

    // try adding released gem as prereleased
    is = specsHelper.addSpec(spec, new FileInputStream(empty), SpecsIndexType.PRERELEASE);
    assertThat("no change", is, nullValue());

    // adding to latest
    is = specsHelper.addSpec(spec, new FileInputStream(empty), SpecsIndexType.LATEST);
    dumpStream(is, target);

    size = scriptingContainerRule.getScriptingContainer().callMethod(check, "specs_size",
        target.getAbsolutePath(), Integer.class);
    assertThat("specsfile size", size, equalTo(1));
  }

  @Test
  public void testAddDeletePrereleasedGemToSpecs() throws Exception {
    File empty = new File("src/test/resources/empty_specs");
    File target = new File("target/test_specs");
    File gem = new File("src/test/resources/gems/n/nexus-0.1.0.pre.gem");

    IRubyObject spec = scriptingContainerRule.getRubygemsGateway().newGemspecHelperFromGem(new FileInputStream(gem)).gemspec();

    // add prereleased gem
    InputStream is = specsHelper.addSpec(spec, new FileInputStream(empty), SpecsIndexType.PRERELEASE);
    dumpStream(is, target);

    int size = scriptingContainerRule.getScriptingContainer().callMethod(check, "specs_size",
        target.getAbsolutePath(), Integer.class);
    assertThat("specsfile size", size, equalTo(1));

    // delete gem
    is = specsHelper.deleteSpec(spec, new FileInputStream(target), SpecsIndexType.PRERELEASE);
    dumpStream(is, target);

    size = scriptingContainerRule.getScriptingContainer().callMethod(check, "specs_size",
        target.getAbsolutePath(), Integer.class);
    assertThat("specsfile size", size, equalTo(0));

    // try adding prereleased gem as released
    is = specsHelper.addSpec(spec, new FileInputStream(empty), SpecsIndexType.RELEASE);
    assertThat("no change", is, nullValue());

    // adding to latest
    is = specsHelper.addSpec(spec, new FileInputStream(empty), SpecsIndexType.LATEST);
    assertThat("no change", is, nullValue());

    size = scriptingContainerRule.getScriptingContainer().callMethod(check, "specs_size",
        target.getAbsolutePath(), Integer.class);
    assertThat("specsfile size", size, equalTo(0));
  }

  private void dumpStream(final InputStream is, File target)
      throws IOException
  {
    try {
      FileOutputStream output = new FileOutputStream(target);
      try {
        IOUtils.copy(is, output);
      }
      finally {
        IOUtils.closeQuietly(output);
      }
    }
    finally {
      IOUtils.closeQuietly(is);
    }
  }
}
