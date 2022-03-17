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

import org.apache.commons.codec.digest.DigestUtils
import org.sonatype.nexus.client.core.NexusClient
import org.sonatype.sisu.goodies.testsupport.hamcrest.FileMatchers

import static org.hamcrest.MatcherAssert.assertThat

/**
 * The "Hola roundtrip" scenario for Rubygems.
 *
 * <ul>
 * <li>have Git on the path</li>
 * </ul>
 */
class HolaScenario
    extends RubyScenarioSupport
{
  HolaScenario(String id, File workdir, NexusClient nexusClient) {
    super(id, workdir, nexusClient)
  }

  @Override
  void perform() {
    // clone hola
    exec(['git', 'clone', 'git@github.com:qrush/hola.git'])
    // cd into it
    cd 'hola'
    // edit gemspec file, set version to "1.0.XXX" to make it really latest but also unique
    final String suffix = DigestUtils.sha1Hex(UUID.randomUUID().toString())
    // to make GEM unique
    final String version = "1.0.0-${suffix}"
    // this goes into gemspec
    final File gemspecFile = file 'hola.gemspec'
    gemspecFile.text = gemspecFile.text.replace('= \"0.0.1\"', "= \"${version}\"")
    // build it
    gem(['build', 'hola.gemspec'])
    // publish it
    final String gemFileVersion = "1.0.0.pre.${suffix}"
    // this is how gem file will be named
    nexus("hola-${gemFileVersion}.gem")
    // install it
    gem(['install', 'hola', '--pre'])

    assertThat(lastOutFile, FileMatchers.contains("Successfully installed hola-${gemFileVersion}"))
  }
}
