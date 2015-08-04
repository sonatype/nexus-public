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
import org.sonatype.nexus.ruby.client.RubyGroupRepository
import org.sonatype.nexus.ruby.client.RubyHostedRepository
import org.sonatype.nexus.ruby.client.RubyProxyRepository

import static com.google.common.base.Preconditions.checkNotNull

/**
 * Support for Rubygems scenarios.
 */
abstract class RubyScenarioSupport
    extends ScenarioSupport
{
  private final File gemConfigFile

  private final File nexusGemConfigFile

  // ==

  protected RubyHostedRepository hosted

  protected RubyProxyRepository proxy

  protected RubyGroupRepository group

  protected RubyScenarioSupport(final String id, final File workdir, final NexusClient nexusClient) {
    super(id, workdir, nexusClient)
    this.gemConfigFile = new File(workdir, 'gem.cfg')
    this.nexusGemConfigFile = new File(workdir, 'nexus-gem.cfg')
  }


  /**
   * Invokes "bundler".
   */
  void bundle(List<String> args) {
    exec(['bundle'] + args)
  }

  /**
   * Invokes "gem" with config of this env. Passed in args should not contain "gem" command nor "--config-file", only
   * the arguments for invocation.
   */
  void gem(List<String> args) {
    exec(['gem', '--config-file', gemConfigFile.absolutePath] + args)
  }

  /**
   * Invokes "gem nexus" with config of this env. It must be invoked from within the directory (see #cwd) where the
   * Gem file to be installed resides and the parameter should be the filename of the Gem to be deployed.
   */
  void nexus(String gem) {
    exec(['gem', '--config-file', gemConfigFile.absolutePath, 'nexus', '--nexus-config',
          nexusGemConfigFile.absolutePath, file(gem).absolutePath])
  }

  @Override
  void configure() {
    super.configure()
    hosted = createRubyHostedRepository('rubygems-hosted')
    proxy = createRubyProxyRepository('rubygems-proxy', 'https://rubygems.org/')
    group = createRubyGroupRepository('rubygems-group', hosted.id(), proxy.id())

    // this command install, upgrades or is no-op if jruby is up to date
    exec(['rvm', 'install', 'jruby'])
    exec(['rvm', '--default', 'use', 'jruby'])

    gem(['sources', '-c'])
    gem(['sources', '-r', 'https://rubygems.org/'])
    gem(['sources', '-a', group.contentUri() + '/']) // TODO: gem source MUST end with slash!

    exec(['rvm', 'gemset', 'create', "trial-${id}"])
    exec(['rvm', "jruby@trial-${id}"])

    gem(['install', 'nexus'])
    nexusGemConfigFile.text = "---\n:url: ${hosted.contentUri()}\n:authorization: Basic ZGVwbG95bWVudDpkZXBsb3ltZW50MTIz"

    gem(['install', 'bundler'])
    // TODO: figue out bundler config, I see no way to make it use "private" (non-shared) conf!!!
    // TODO: hola works, as it does not need bundler, but other scenario needs it
    bundle(['config',
            'mirror.http://rubygems.org', group.contentUri()])
    bundle(['config',
            'mirror.https://rubygems.org', group.contentUri()])
  }

  // ==

  RubyHostedRepository createRubyHostedRepository(final String id) {
    checkNotNull(id)
    return repositories().create(RubyHostedRepository.class, id).withName(id).save()
  }

  RubyProxyRepository createRubyProxyRepository(final String id, final String url) {
    checkNotNull(id)
    checkNotNull(url)
    return repositories().create(RubyProxyRepository.class, id).withName(id).asProxyOf(url).save()
  }

  RubyGroupRepository createRubyGroupRepository(final String id, final String... members) {
    checkNotNull(id)
    return repositories().create(RubyGroupRepository.class, id).withName(id).addMember(members).save()
  }
}
