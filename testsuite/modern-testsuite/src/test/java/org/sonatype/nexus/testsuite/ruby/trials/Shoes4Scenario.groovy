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
package org.sonatype.nexus.testsuite.ruby.trials

import org.sonatype.nexus.client.core.NexusClient
import org.sonatype.sisu.goodies.testsupport.hamcrest.FileMatchers

import static org.hamcrest.MatcherAssert.assertThat

/**
 * The "Shoes4 roundtrip" scenario for Rubygems.
 *
 * <ul>
 * <li>have Git on the path</li>
 * </ul>
 */
class Shoes4Scenario
    extends RubyScenarioSupport
{
  public Shoes4Scenario(String id, File workdir, NexusClient nexusClient) {
    super(id, workdir, nexusClient)
  }

  @Override
  public void perform() {
    // clone shoes4
    exec(['git', 'clone', 'git@github.com:shoes/shoes4.git'])
    // cd into it
    cd 'shoes4'
    // use tagged release
    exec(['git', 'checkout', 'v4.0.0.pre2'])
    // read version (is defined in src)
    String version
    file('lib/shoes/version.rb').text.eachLine { if (it =~ /VERSION =/) version = it }
    assert version != null
    version = (version =~ /"([0-9a-zA-Z\.]+)"/)[0][1]
    assert version != null
    // invoke bundle install
    bundle(['install'])
    // invoke rake build:all to have Gems built
    exec(['rake', 'build:all'])
    // gems are in pkg/
    cd 'pkg'
    // deploy them
    nexus "shoes-dsl-${version}.gem"
    nexus "shoes-swt-${version}.gem"
    nexus "shoes-${version}.gem"
    // install them
    gem(['install', 'shoes', '--version', "${version}"])

    assertThat(lastOutFile, FileMatchers.contains("Successfully installed shoes-${version}"))
  }
}
