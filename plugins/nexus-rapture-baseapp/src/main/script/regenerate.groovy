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
def flavors = project.properties['flavors']
if (flavors) {
  flavors = flavors.split(',')
}
else {
  flavors = ['debug', 'prod']
}
println "Flavors: $flavors"

/**
 * Convert flavor to environment name.
 */
def flavorToEnv = { flavor ->
  switch (flavor) {
    case 'debug':
      return 'testing'
    case 'prod':
      return 'production'
    default:
      throw Exception("Unknown flavor: $flavor")
  }
}

/**
 * Helper to display a block header.
 */
def header = { text, lineStyle='-' ->
  println ''
  println lineStyle * 80
  println text
  println lineStyle * 80
}

// Sanity check sencha cmd
def senchaExe = 'sencha'
ant.exec(executable: senchaExe, failonerror: true) {
  arg(line: 'which')
}

// Extract extjs distribution (if needed)
def extDir = new File(project.build.directory, 'ext')
if (!extDir.exists()) {
  header 'Extract ExtJS'
  def extZip = project.artifactMap['com.sencha:ext'].file
  ant.mkdir(dir: extDir)
  ant.unzip(src: extZip, dest: extDir) {
    cutdirsmapper(dirs: 1)
    patternset {
      exclude(name: 'ext-*/builds/**')
      exclude(name: 'ext-*/docs/**')
      exclude(name: 'ext-*/welcome/**')
    }
  }
}

// Re-generate sencha cmd app (if needed)
def baseappDir = new File(project.build.directory, 'baseapp')
if (!baseappDir.exists()) {
  header 'Generate application'

  ant.exec(executable: senchaExe, dir: project.build.directory, failonerror: true) {
    arg(value: '-sdk')
    arg(file: extDir)
    arg(line: 'generate app baseapp')
    arg(file: baseappDir)
  }

  // Strip out muck from generated app template
  ant.delete {
    fileset(dir: baseappDir) {
      include(name: 'app/**')
    }
  }
}

// Apply customizations to app, filter all non-resources
header 'Customizing application'
ant.copy(todir: baseappDir, overwrite: true, filtering: true) {
  fileset(dir: "${project.basedir}/src/main/baseapp") {
    include(name: '**')
    exclude(name: '**/resources/**')
  }
  filterset {
    filter(token: 'project.build.directory', value: "${project.build.directory}")
  }
}
ant.copy(todir: baseappDir, overwrite: true) {
  fileset(dir: "${project.basedir}/src/main/baseapp") {
    include(name: '**/resources/**')
  }
}

// Generate all flavors
flavors.each { flavor ->
  def env = flavorToEnv flavor
  header "Generating flavor: $flavor"
  ant.exec(executable: senchaExe, dir: baseappDir, failonerror: true) {
    arg(line: "app build $env")
  }
}

// Reset resources before copying over newly generated versions
header 'Installing resources'
def outputDir = new File(project.basedir, 'src/main/resources/static/rapture')
ant.mkdir(dir: outputDir)
ant.delete(dir: outputDir) {
  include(name: 'baseapp-*.js')
  include(name: 'resources/**')
}

// Install generated baseapp resources
ant.copy(todir: outputDir) {
  flavors.each { flavor ->
    def env = flavorToEnv flavor
    fileset(dir: "${project.build.directory}/$env/baseapp") {
      include(name: 'baseapp-*.js')
      include(name: 'resources/*.css')
      include(name: 'resources/images/**')
    }
  }
}