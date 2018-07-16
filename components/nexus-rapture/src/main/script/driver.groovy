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

/**
 * gmavenplus-plugin doesn't set System properties on project.properties, but they are available to the AntBuilder
 */
String systemProperty(String key) {
  ant.properties.project.properties[key]
}

// Sanity check sencha cmd
def senchaExe = 'sencha'
try {
  ant.exec(executable: senchaExe, failonerror: true) {
    arg(line: 'which')
  }
} catch (e) {
  log.error 'Sencha CMD not found.'
  log.error 'Please install the Sencha CMD to rebuild application styles.'
  log.error 'https://www.sencha.com/products/extjs/cmd-download/'
  throw e
}

String mode = systemProperty('mode')
assert mode: 'Missing property: mode'
log.info "Mode: $mode"

def baseappDir = new File(project.basedir, 'src/main/baseapp')
def outputDir = new File(project.basedir, 'src/main/resources/static/rapture')

/**
 * Convert flavor to environment name.
 */
def flavorToEnv = {flavor ->
  switch (flavor) {
    case 'debug':
      return 'testing'
    case 'prod':
      return 'production'
    default:
      throw Exception("Unknown flavor: $flavor")
  }
}

//
// mode=ext
//

def do_ext = {
  def extDir = new File(project.basedir, 'target/ext')
  if (extDir.exists()) {
    return
  }

  def extZip = project.artifactMap['com.sencha:ext'].file
  ant.mkdir(dir: extDir)
  ant.unzip(src: extZip, dest: extDir) {
    cutdirsmapper(dirs: 1)
    patternset {
      exclude(name: 'ext-*/docs/**')
      exclude(name: 'ext-*/welcome/**')
    }
  }
}

//
// mode=clobber
//

def do_clobber = {
  ant.delete {
    fileset(dir: outputDir) {
      include(name: 'baseapp-*.js')
      include(name: 'ignore.html')
      include(name: 'resources/baseapp-*.css')
      include(name: 'resources/Readme.md')
      include(name: 'resources/font-awesome/**')
    }

    fileset(dir: "$outputDir/resources/images") {
      exclude(name: '*')
      exclude(name: 'form/fa-*')
    }
  }
}

//
// mode=build
//

def do_build = {
  def flavors =  systemProperty('flavors')
  if (flavors) {
    flavors = flavors.split(',')
  }
  else {
    flavors = ['debug', 'prod']
  }
  log.info "Flavors: $flavors"

  do_ext()

  flavors.each { flavor ->
    def env = flavorToEnv flavor
    ant.exec(executable: senchaExe, dir: baseappDir, failonerror: true) {
      arg(line: "app build $env")
    }

    // Strip out any multiline comments from the generated css
    ant.replaceregexp(match: '/\\*.*?\\*/', replace: '', flags: 'g', byline: true) {
      fileset(dir: outputDir) {
        include(name: 'resources/baseapp-debug*.css')
      }
    }

    // We don't need app.json, so remove it
    ant.delete() {
      fileset(dir: outputDir) {
        include(name: 'app.json')
      }
    }
  }
}

//
// mode=watch
//

def do_watch = {
  do_ext()

  ant.exec(executable: senchaExe, dir: baseappDir, failonerror: true) {
    arg(line: "app watch testing")
  }
}

// switch mode
switch (mode) {
  case 'ext':
    do_ext()
    break

  case 'clobber':
    do_clobber()
    break

  case 'build':
    do_build()
    break

  case 'watch':
    do_watch()
    break

  default:
    throw new Exception("Unknown mode: $mode")
}


