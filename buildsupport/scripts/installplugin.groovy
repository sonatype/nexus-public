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
//
// Helper to [re]install a plugin bundle into a nexus installation.

CliBuilder cli = new CliBuilder(
    usage: 'groovy installplugin.groovy -i <nexus-install-directory> -p <nexus-plugin-bundle-zip>',
    header: 'A utility to install a plugin for development purposes'
)
cli.with {
  p longOpt: 'plugin', args: 1, 'Path to <nexus-plugin-bundle-zip>', required: true
  i longOpt: 'install', args: 1, 'Path to <nexus-install-directory>', required: true
}
def options = cli.parse(args)
if(!options) {
  return
}

def pluginFile = options.p as File
assert pluginFile && pluginFile.exists()

def installDir = options.i as File
assert installDir && installDir.exists()

def repoDir = new File(installDir, 'nexus/WEB-INF/plugin-repository')

def ant = new AntBuilder()

// strip off -bundle.zip
assert pluginFile.name.endsWith('-bundle.zip')
def pluginPrefix = pluginFile.name[0..-12]

// delete the plugin dir, if it exists
def pluginDir = new File(repoDir, pluginPrefix)
if (pluginDir.exists()) {
  ant.delete(dir: pluginDir)
}

// unzip the new plugin
ant.unzip(src: pluginFile, dest: repoDir)
