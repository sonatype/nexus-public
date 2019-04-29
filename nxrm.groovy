#!/usr/bin/env groovy

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
 * Requires:
 * - git
 * - unbuffer (apt install expect-dev, brew install expect --with-brewed-tk)
 * - Takari (optional but recommended. Much quicker builds.) - see http://takari.io/book/30-team-maven.html#takari-smart-builder
 *      To enable: Add 'takari=true' to .nxrm/nxrmrc.groovy
 */
@Grab(group = 'ch.qos.logback', module = 'logback-classic', version = '1.2.3')
@Grab(group = 'com.aestasit.infrastructure.sshoogr', module = 'sshoogr', version = '0.9.26')
@Grab(group = 'com.caseyscarborough.colorizer', module = 'groovy-colorizer', version = '1.0.0')
@Grab(group = 'jline', module = 'jline', version = '2.14.2')
@Grab(group = 'org.ajoberstar', module = 'grgit', version = '2.2.1')
@Grab(group = 'org.apache.commons', module = 'commons-compress', version = '1.15')
@Grab(group = 'commons-io', module = 'commons-io', version = '2.6')
@Grab(group = 'org.apache.maven', module = 'maven-model', version = '3.5.0')
@Grab(group = 'org.rauschig', module = 'jarchivelib', version = '0.7.1')

import com.caseyscarborough.colorizer.Colorizer
import org.ajoberstar.grgit.*
import org.ajoberstar.grgit.operation.*
import org.ajoberstar.grgit.service.*
import org.apache.commons.io.FileUtils
import org.apache.maven.model.Model
import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import org.rauschig.jarchivelib.ArchiveFormat
import org.rauschig.jarchivelib.ArchiverFactory
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.Logger
import java.nio.charset.StandardCharsets

import static ch.qos.logback.classic.Level.*
import static com.aestasit.infrastructure.ssh.DefaultSsh.*
import static org.slf4j.Logger.ROOT_LOGGER_NAME as ROOT
import static org.slf4j.LoggerFactory.getLogger

// set default log level (jgit seems to have something on DEBUG by default)
((Logger) getLogger(ROOT)).setLevel(INFO)

ant = new AntBuilder()
ant.project.buildListeners[0].messageOutputLevel = 0

HR = "".padRight(jline.TerminalFactory.get().getWidth() - 7, '-') // terminal line width
LOCK_FILE = "target/sonatype-work/nexus3/lock"
SONATYPE_WORK = "target/sonatype-work"
SONATYPE_WORK_BACKUP = System.getProperty("java.io.tmpdir") + "/nxrm-sonatype-work"
TAKARI_SMART_BUILD_VERSION = "0.5.0"
TAKARI_LOCAL_REPO_VERSION = "0.11.2"
TAKARI_FILE_MANAGER_VERSION = "0.8.3"

env = System.getenv()
changedProjects = [] as Set
ignoredProjects = [':nexus-buildsupport-scripts'] // always ignore these projects
cliOptions = null
positionalOptions = null
buildLog = new File("build.log")

// test projects - generated with command: for i in `find -name pom.xml`; do cd `dirname $i`; xmllint --xpath "//*[local-name()='project']/*[local-name()='artifactId']/text()" pom.xml; cd -; done 
testProjects = [':nexus-insight-testsupport', ':nexus-docker-testsupport', ':nexuspro-migration-testsuite', ':nexus-testlm-edition', ':nexus-stress-master-instance', ':nexuspro-testsuite', ':nexus-testsuite-data', ':testplugin', ':simple-it', ':nexuspro-fabric-testsuite', ':nexus-fabric-testsupport', ':functional-testsuite', ':pax-exam-spock', ':nexus-migration-testsupport', ':nexus-upgrade-testsupport', ':nexuspro-modern-testsuite', ':nexus-stress-testsuite', ':nexus-repository-testsupport', ':nexus-contributedhandler-testsupport', ':nexuspro-performance-testsuite' ]

/**
 Customize these by creating a .nxrm/nxrmrc.groovy. Sample contents:
   javaMaxMem="4g"
   directMaxMem="4g"
   vmOptions="-XX:-MaxFDLimit"
   port=8082
   sslPort=8444
   karafSshPort=8023
   javaDebugPort=5006
   ssl=true
   orient=true
   elastic=false
   takari=false
   deploy=true
   //backup=false
   //restore=false
   //tests="custom Maven test arguments here"
   //assemblies="custom Maven assembly arguments here"
   //sources="custom Maven sources arguments here"
 */
configDefaults = [
    javaMaxMem   : '2g',
    directMaxMem : '2g',
    vmOptions    : '',
    port         : 8081,
    sslPort      : 8443,
    karafSshPort : 8022,
    javaDebugPort: 5005,
    ssl          : true,   // SSL is enabled by default
    orient       : true,   // Orient access (binary/Studio) is enabled by default
    elastic      : false,  // Elastic is disabled by default
    takari       : false,  // Takari is disabled by default
    deploy       : true,   // Deployment is performed by default
    backup       : false,   // Backup sonatype-work disabled by default
    restore      : false,   // Restore backup of sonatype-work disabled by default
    builder      : '-T 1C', // default one thread per core
    randomPassword: false
]

buildOptions = [
    buildMode              : "",
    buildModeDesc          : "",
    mavenGoalsAndPhases    : "",
    mavenGoalsAndPhasesDesc: "",
    tests                  : "",
    testsDesc              : "",
    assemblies             : "",
    assemblyDesc           : "",
    sources                : "",
    sourcesDesc            : "",
]

def hr = {
  info("$HR")
}

def info(String message) {
  println Colorizer.colorize("|[lightBlue]INFO[default]| ${message}")
  buildLog << "|INFO| ${message}\n"
}

def warn(String message) {
  println Colorizer.colorize("|[lightYellow]WARNING[default]| ${message}")
  buildLog << "|WARNING| ${message}\n"
}

def error(String message) {
  println Colorizer.colorize("|[lightRed]ERROR[default]| ${message}")
  buildLog << "|ERROR| ${message}\n"
}

def debug(String message) {
  if (cliOptions.verbose) {
    println Colorizer.colorize("|[lightYellow]DEBUG[default]| ${message}")
    buildLog << "|DEBUG| ${message}\n"
  }
}

File searchUp(File directory) {
  if (!directory) {
    return null
  }

  File pom = new File(directory, "pom.xml")
  if (pom.exists()) {
    return pom
  }
  else {
    return searchUp(directory.getParentFile())
  }
}

def getChangedProjects() {
  def grgit = Grgit.open()
  def changes = grgit.status().staged.getAllChanges() + grgit.status().unstaged.getAllChanges()

  // for all changes, search up for a pom.xml, and get artifactId out
  MavenXpp3Reader reader = new MavenXpp3Reader()
  def projects = [] as Set
  changes.each {
    File pom = searchUp(new File(it).getParentFile())
    // note: this ignores files in the project root
    if (pom) {
      Model model = reader.read(new FileReader(pom))
      projects.add(":" + model.getArtifactId())
    }
  }
  return projects
}

ConfigObject processRcConfigFile() {
  // start with defaults
  ConfigObject config = new ConfigObject()
  config.putAll(configDefaults)

  // combine with overrides
  def rcFile = new File('.nxrm/nxrmrc.groovy')
  if(rcFile.exists()) {
    def overrides = new ConfigSlurper().parse(rcFile.toURI().toURL())
    config = config.merge(overrides)
  }
  else {
    // create .nxrm folder if it doesn't exist
    File nxrmFolder = new File('.nxrm/')
    if(!nxrmFolder.exists()) {
      nxrmFolder.mkdir()
      info("First run. Creating .nxrm folder")
    }
  }

  // assign any CLI options
  config.port = cliOptions.'port' ?: config.port
  config.sslPort = cliOptions.'ssl-port' ?: config.sslPort
  config.sslIp = cliOptions.'ssl-ip' ?: null
  config.karafSshPort = cliOptions.'karaf-ssh-port' ?: config.karafSshPort
  config.javaDebugPort = cliOptions.'java-debug-port' ?: config.javaDebugPort

  config.ssl = assign('ssl', 'no-ssl', config.ssl)
  config.orient = assign('orient', 'no-orient', config.orient)
  config.elastic = assign('elastic', 'no-elastic', config.elastic)
  config.takari = assign('takari', 'no-takari', config.takari)
  config.deploy = assign('deploy', 'no-deploy', config.deploy)
  config.backup = assign('backup', 'no-backup', config.backup)
  config.restore = assign('restore', 'no-restore', config.restore)
  config.randomPassword = assign('random-password', 'no-random-password', config.randomPassword)

  debug("config read from RC and merged with defaults: ${config}")

  return config
}

def assign(def trueOption, def falseOption, def defaultValue){
  debug("assign(${trueOption}, ${falseOption}, ${defaultValue})")
  if(cliOptions[trueOption])
    return true
  else if(cliOptions[falseOption])
    return false
  else
    return defaultValue
}

static ConfigObject processLastBuild() {
  def lastBuildFile = new File('.nxrm/last_build')

  if (!lastBuildFile.exists()) {
    return new ConfigObject()
  }

  return new ConfigSlurper().parse(lastBuildFile.toURI().toURL())
}

def saveLastBuild() {
  def grgit = Grgit.open()
  def currentBranch = grgit.branch.getCurrent().name
  def currentHead = grgit.head().abbreviatedId

  def lastProjects = buildOptions.buildMode == "full" ? [] : changedProjects

  ConfigObject configObject = new ConfigObject()
  configObject['BRANCH'] = currentBranch
  configObject['HEAD'] = currentHead
  configObject['LAST_PROJECTS'] = lastProjects

  File lastBuild = new File(".nxrm/last_build")
  lastBuild.delete()
  lastBuild.text = configObject.prettyPrint()
}

def processBuildMode() {
  if (cliOptions.full) {
    info("Full build selected")
    buildOptions.buildMode = "full"
    return
  }

  if (!new File("target").exists()) {
    warn("No root target folder present. Full build required.")
    buildOptions.buildMode = "full"
    return
  }

  if (!new File(".nxrm/last_build").exists()) {
    warn("No previous build detected. Full build required.")
    buildOptions.buildMode = "full"
    return
  }

  ConfigObject lastBuild = processLastBuild()

  debug("Last build info: ${lastBuild}")

  def grgit = Grgit.open()
  def currentBranch = grgit.branch.getCurrent().name
  def currentHead = grgit.head().abbreviatedId

  debug("Git current branch: ${currentBranch}")
  debug("Git current HEAD: ${currentHead}")

  if (lastBuild.BRANCH == null) {
    warn("No previous branch detected. Full build required.")
    buildOptions.buildMode = "full"
    return
  }
  else if (lastBuild.BRANCH != currentBranch) {
    warn("Detected new branch. Full build required.")
    buildOptions.buildMode = "full"
    return
  }

  if (lastBuild.HEAD == null) {
    warn("No previous HEAD detected. Full build required.")
    buildOptions.buildMode = "full"
    return
  }
  else if (lastBuild.HEAD != currentHead) {
    warn("Detected new HEAD. Full build required.")
    buildOptions.buildMode = "full"
    return
  }

  // default is an incremental build
  buildOptions.buildMode = "incremental"
}

def processCliOptions(args) {
  CliBuilder cli = new CliBuilder(usage: './nxrm.groovy [options] [-x options to be passed through]',
      header: 'Nexus Repository Manager control script')
  cli.with {
    h longOpt: 'help', 'Show usage information'
    v longOpt: 'verbose', 'show verbose debug information'
    // modes (exclusive from each other)
    b longOpt: 'build', 'Build mode [default]. Intelligently does a full or incremental build.'
    r longOpt: 'run', args: 1, 'Run mode. Starts Nexus with the specified assembly. Add \'debug\' for Nexus debug mode (i.e. remote debugging).'
    _ longOpt: 'geb', '''Process dependencies for Geb test execution in your IDE.'''
    _ longOpt: 'sass', '''Compile Sass files to CSS.'''
    e longOpt: 'extract', 'Re-run the assembly extraction'
    // build mode options
    f longOpt: 'full', 'Force full build'
    t longOpt: 'tests', args: 1, '''Control test execution. Options:
                             skipAll: Skip all building & execution [default]
                             skip: Build unit & integration but skip execution
                             unit: Build and execute unit tests only
                             skipITs: Build unit & integration, only execute unit
                             all: Build and execute all tests'''
    a longOpt: 'assemblies', args: 1, '''Control assembly execution. Options:
                             skip: Skip all assembly building [default - ignored if full build is triggered]
                             all: Build all assemblies'''
    s longOpt: 'sources', args: 1, '''Control building of sources. Options:
                             skip: Skip all source creation [default]
                             all: Build all sources'''
    n longOpt: 'deploy', 'Enable automatic deployment for incremental builds (if disabled by config). Note this will enable SSH on Karaf.'
    n longOpt: 'no-deploy', 'Disable automatic deployment for incremental builds (enabled by default)'
    _ longOpt: 'backup', "Enable backup of any existing target/sonatype-work folder to ${SONATYPE_WORK_BACKUP} (if disabled by config)"
    _ longOpt: 'no-backup', "Disable backup (if enabled by config)"
    // run mode options
    p longOpt: 'port', args: 1, 'Set NXRM port. Defaults to 8081'
    _ longOpt: 'ssl-port', args: 1, 'Set NXRM SSL port. Defaults to 8443'
    _ longOpt: 'karaf-ssh-port', args: 1, 'Set Karaf SSH port. Defaults to 8022'
    _ longOpt: 'java-debug-port', args: 1, 'Set JDWP debug port. Defaults to 5005'
    _ longOpt: 'ssl', 'Enable SSL (if disabled by config)'
    _ longOpt: 'no-ssl', 'Disable SSL (enabled by default)'
    _ longOpt: 'ssl-ip', args: 1, 'Provide the ip address of this machine to generate an SSL certificate for use with Docker'
    _ longOpt: 'orient', 'Enable Orient (if disabled by config)'
    _ longOpt: 'no-orient', 'Disable Orient (enabled by default)'
    _ longOpt: 'elastic', 'Enable Elastic plugins (disabled by default)'
    _ longOpt: 'no-elastic', 'Disable Elastic plugins (if enabled by config)'
    _ longOpt: 'takari', 'Enable Takari (disabled by default)'
    _ longOpt: 'no-takari', 'Disable Takari (if enabled by config)'
    _ longOpt: 'restore', "Enable restore of backup from ${SONATYPE_WORK_BACKUP} to target/sonatype-work (disabled by default)"
    _ longOpt: 'no-restore', "Disable restore of backup (if enabled by config)"
    _ longOpt: 'random-password', "Enable generation of random password for admin user on initial start"
    _ longOpt: 'no-random-password', "Disable generation of random password (default)"

    // general options
    d longOpt: 'dry-run', 'Dry run, don\'t actually execute anything'
    _ longOpt: 'no-docker', 'Disable the docker build'
    _ longOpt: 'single-threaded', "Don't build in parallel"

  }

  cliOptions = cli.parse(args)
  if (!cliOptions) {
    return false
  }
  if (cliOptions.h) {
    cli.usage()
    // CliBuilder automatically wraps ALL text at ~80 chars which doesn't make for good help. Manually print it out
    println '''
Examples:
  ./nxrm.groovy                     Normal 'should just work' use case. Will perform a full build or incremental build as necessary.
                                    Same as ./nxrm.groovy --build
  ./nxrm.groovy -f                  Force a full build.
                                    Same as ./nxrm.groovy --build --full
  ./nxrm.groovy -r pro debug        Run Nexus. Assembly is required. 'debug' is optional and starts Nexus in debug mode.
                                    Same as ./nxrm.groovy --run
                                    If NXRM is running when you do a build, it will automatically be re-deployed and re-started.
  ./nxrm.groovy -t skip             Compile tests but skip execution. Note that the default is to skip all tests, even compilation, to save time.
  ./nxrm.groovy -e                  Re-run the assembly extraction. This is for if you nuke your 'target/' folder and want to start your NXRM clean.
                                    Same as ./nxrm.groovy --extract
  ./nxrm.groovy clean package       Maven goals and phases can be passed in directly. Otherwise will default to 'clean install' for full build and 'install' for incremental.
  ./nxrm.groovy -x -rf :nexus-main  Due to a limitation in the Groovy CliBuilder, any positional parameters you wish to pass into the maven build or run commands need to be after the '-x' parameter, and be last.
  ./nxrm.groovy -x -U               Example usage to force Maven snapshot updates.
  ./nxrm.groovy --geb               Enables Geb in your IDE. See https://docs.sonatype.com/display/Nexus/Nexus+Repository+Manager+Developer+Onboarding#NexusRepositoryManagerDeveloperOnboarding-TestingWithGeb
'''
    return false
  }

  // get the positional arguments (i.e. the arguments to pass through to NXRM)
  // Have to do this through special unspecified '-x' option which indicates what follows are arguments to be passed to build or run command
  // don't need the '-x' itself though, so remove it

  positionalOptions = cliOptions.arguments()
  positionalOptions.removeAll { it == '-x' }
  if(positionalOptions.size > 0) {
    debug("Positional options: " + positionalOptions.toString())
  }
  return true
}

def processMavenGoalsAndPhases() {
  if (buildOptions.mavenGoalsAndPhases == "") {
    if (buildOptions.buildMode == "full") {
      buildOptions.mavenGoalsAndPhases = "clean install"
      buildOptions.mavenGoalsAndPhasesDesc = "${buildOptions.mavenGoalsAndPhases} [default full build]"
    }
    else {
      buildOptions.mavenGoalsAndPhases = "install"
      buildOptions.mavenGoalsAndPhasesDesc = "${buildOptions.mavenGoalsAndPhases} [default]"

      // see if positional options contains any maven phases/goals (anything at the beginning of the list that isn't prefixed with a '-')
      if (positionalOptions.size) {
        def items = []
        positionalOptions.removeAll {
          if(!it.startsWith('-')) {
            items.add(it)
          }
          !it.startsWith('-')
        }
        debug("items: ${items}")
        if (items) {
          buildOptions.mavenGoalsAndPhases = items.join(" ")
          buildOptions.mavenGoalsAndPhasesDesc = "${buildOptions.mavenGoalsAndPhases} [custom]"
        }
      }
    }
  }
  else {
    buildOptions.mavenGoalsAndPhasesDesc = "${buildOptions.mavenGoalsAndPhases}"
  }
}

def processTestArgs() {
  // Test options
  // Precendence: environment variable, parameter, rc
  debug("Initial value for 'tests' environment variable: $env.tests")
  debug("Initial value for '-tests' CLI: $cliOptions.tests")
  debug("Initial value for 'tests' RC config: $rcConfig.tests")

  def testsArg
  if (env.tests) {
    testsArg = env.tests
    debug("Using test arguments from environment '$testsArg'")
  }
  else if (cliOptions.tests) {
    testsArg = cliOptions.tests
    debug("Using test arguments from CLI '$testsArg'")
  }
  else if (rcConfig.tests) {
    testsArg = rcConfig.tests
    debug("Using test arguments from RC '$testsArg'")
  }
  else {
    testsArg = "skipAll"
    if (buildOptions.buildMode == "full") {
      debug("Detected full build mode. Defaulting to '-t skipAll' to skip all test compilation and execution")
    }
    else {
      debug("No test params specified. Defaulting to '-t skipAll'")
    }
  }

  if (testsArg == "skipAll") {
    buildOptions.testsDesc = "Skipping all compiling & execution"
    buildOptions.tests = "-Dmaven.test.skip=true"
  }
  else if (testsArg == "skip") {
    buildOptions.testsDesc = "Compiling all (unit & integration) but skipping execution"
    buildOptions.tests = "-DskipTests -Dit"
  }
  else if (testsArg == "unit") {
    buildOptions.testsDesc = "Building and executing unit tests only"
    buildOptions.tests = ""
  }
  else if (testsArg == "skipITs") {
    buildOptions.testsDesc = "Compiling all (unit & integration), executing unit tests only"
    buildOptions.tests = "-DskipITs -Dit"
  }
  else if (testsArg == "all") {
    buildOptions.testsDesc = "Compiling and executing all tests (unit & integration)"
    buildOptions.tests = "-Dit"
  }
  else {
    // custom option
    buildOptions.testsDesc = "Using custom arguments"
    buildOptions.tests = testsArg
  }

  debug("Final test arguments: $buildOptions.tests")
}

def processAssemblyArgs() {
  // Assembly options
  // Precendence: environment variable, parameter, rc
  debug("Initial value for 'assemblies' environment variable: $env.assemblies")
  debug("Initial value for '-assemblies' CLI: $cliOptions.assemblies")
  debug("Initial value for 'assemblies' RC config: $rcConfig.assemblies")

  if (buildOptions.buildMode == "full") {
    debug("Detected full build mode. Assemblies required")
    buildOptions.assemblies = ""
    buildOptions.assembliesDesc = "Yes (Build mode = full, assemblies required)"
  }
  else if (buildOptions.buildMode == "incremental" && !isNxrmRunning()) {
    debug("Incremental build mode, but Nexus not running or no lock file exists so assemblies required")
    buildOptions.assemblies = ""
    buildOptions.assembliesDesc = "Yes (Incremental mode, but Nexus not running or no lock file exists so assemblies " +
        "required)"
  }
  else {
    def assembliesArg
    if (env.assemblies) {
      assembliesArg = env.assemblies
      debug("Using assembly arguments from environment '$assembliesArg'")
    }
    else if (cliOptions.assemblies) {
      assembliesArg = cliOptions.assemblies
      debug("Using assembly arguments from CLI '$assembliesArg'")
    }
    else if (rcConfig.assemblies) {
      assembliesArg = rcConfig.assemblies
      debug("Using assembly arguments from RC '$assembliesArg'")
    }
    else {
      assembliesArg = "skip"
    }

    if (assembliesArg == "skip") {
      buildOptions.assembliesDesc = "Skipping all assembly"
      buildOptions.assemblies = "-Dassembly.skipAssembly=true"
    }
    else if (assembliesArg == "all") {
      buildOptions.assembliesDesc = "Building all assemblies"
      buildOptions.assemblies = ""
    }
    else {
      buildOptions.assembliesDesc = "Using custom assembly arguments"
      buildOptions.assemblies = assembliesArg
    }
  }

  debug("Final assembly arguments: $buildOptions.assemblies")
}

def processSourceArgs() {
  // Source options
  // Precendence: environment variable, parameter, rc
  debug("Initial value for 'sources' environment variable: $env.sources")
  debug("Initial value for '-sources' CLI: $cliOptions.sources")
  debug("Initial value for 'sources' RC config: $rcConfig.sources")

  def sourcesArg
  if (env.sources) {
    sourcesArg = env.sources
    debug("Using source arguments from environment '$sourcesArg'")
  }
  else if (cliOptions.sources) {
    sourcesArg = cliOptions.sources
    debug("Using source arguments from CLI '$sourcesArg'")
  }
  else if (rcConfig.sources) {
    sourcesArg = rcConfig.sources
    debug("Using source arguments from RC '$sourcesArg'")
  }
  else {
    sourcesArg = "skip"
  }

  if (sourcesArg == "skip") {
    buildOptions.sourcesDesc = "Skipping all source building"
    buildOptions.sources = "-Dmaven.source.skip=true"
  }
  else if (sourcesArg == "all") {
    buildOptions.sourcesDesc = "Building all sources"
    buildOptions.sources = ""
  }
  else {
    buildOptions.sourcesDesc = "Using custom source arguments"
    buildOptions.sources = sourcesArg
  }

  debug("Final source arguments: $buildOptions.sources")
}

def processBuilder() {
  if (rcConfig.takari) {
    rcConfig.builder = "--builder smart -T 1C"

    File file = new File('.mvn/extensions.xml')
    if (file.exists() && file.text.contains("takari-smart-builder")) {
      debug("Takari enabled and detected in .mvn/extensions.xml")
    }
    else {
      error("Takari enabled but not detected in .mvn/extensions.xml")
      warn("Installing Takari now")
      if (file.exists()) {
        error(".mvn/extensions.xml already exists. Unable to install Takari")
        rcConfig.builder = ""
      }
      else {
        file.write """<extensions xmlns="http://maven.apache.org/EXTENSIONS/1.0.0" xmlns:xsi="http://www.w3
.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/EXTENSIONS/1.0.0 http://maven.apache.org/xsd/core-extensions-1.0.0
	.xsd">
	<extension>
		<groupId>io.takari.maven</groupId>
		<artifactId>takari-smart-builder</artifactId>
		<version>${TAKARI_SMART_BUILD_VERSION}</version>
	</extension>
	<extension>
		<groupId>io.takari.aether</groupId>
		<artifactId>takari-local-repository</artifactId>
		<version>${TAKARI_LOCAL_REPO_VERSION}</version>
	</extension>
	<extension>
		<groupId>io.takari</groupId>
		<artifactId>takari-filemanager</artifactId>
		<version>${TAKARI_FILE_MANAGER_VERSION}</version>
	</extension>
</extensions>"""
        info("Takari enabled!")
      }
      sleep(3000)
    }
  }
  else if (cliOptions['single-threaded']) {
    rcConfig.builder = ''
  }
}

def processMavenCommand() {
  changedProjects = getChangedProjects()
  def projects = changedProjects

  // include the last projects built as well to avoid issues with git reset/checkout
  projects += lastBuild.LAST_PROJECTS

  // remove ignored entries
  projects -= ignoredProjects

  // if tests are not built, don't care about changes on test projects
  if(buildOptions.tests == "-Dmaven.test.skip=true") {
	  projects -= testProjects 
  }

  // if there are no projects, then a full build is needed
  if (!projects) {
    buildOptions.buildMode = "full"
    warn("No projects to build, nothing to do but perform full build")
  }

  if (buildOptions.buildMode == "full") {
    buildOptions.buildModeDesc = "Full"
    // Note: Assembly arguments not included here so assemblies will be generated.
    buildOptions.mavenCommand = "${buildOptions.mavenGoalsAndPhases} ${rcConfig.builder} ${buildOptions.tests} " +
        "${buildOptions.sources}"
  }
  else if (projects) {
    buildOptions.buildModeDesc = "Incremental on ${projects}"
    buildOptions.projects = projects.join(',')
    buildOptions.mavenCommand = "${buildOptions.mavenGoalsAndPhases} ${rcConfig.builder} ${buildOptions.tests} " +
        "${buildOptions.sources} ${buildOptions.assemblies} -pl ${projects.join(',')} -amd"
  }
  else {
    buildOptions.buildModeDesc = "Full (Incremental mode, but no code changes detected)"
    buildOptions.mavenCommand = "${buildOptions.mavenGoalsAndPhases} ${rcConfig.builder} ${buildOptions.tests} " +
        "${buildOptions.sources} ${buildOptions.assemblies}"
  }

  if (cliOptions['no-docker']) {
    buildOptions.mavenCommand += ' -Dno-docker'
  }

  buildOptions.mavenCommand += ' ' + positionalOptions.join(' ')
}

def removeBuildLog() {
  if (buildLog.exists()) {
    buildLog.delete()
  }
}

def isNxrmRunning() {
  File lockFile = new File("$LOCK_FILE")
  if (!lockFile.exists()) {
    debug("No lock file, NXRM is not running")
    return false
  }
  else {
    def pid = lockFile.text - ~/@.*/
    if ("kill -0 ${pid}".execute().waitFor() == 0) {
      debug("NXRM running at pid $pid")
      return true
    }
    else {
      debug("Lock file exists, but NXRM not running")
      return false
    }
  }
}

def isNxrmFullyUp() {
  try {
    def url = "http://localhost:${rcConfig.port}/service/metrics/ping"
    String encoded = Base64.getEncoder().encodeToString("admin:admin123".getBytes(StandardCharsets.UTF_8))
    HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection()
    connection.setConnectTimeout(30000)
    connection.setReadTimeout(30000)
    connection.setRequestMethod("GET")
    connection.setRequestProperty("Authorization", "Basic ${encoded}")
    int responseCode = connection.getResponseCode()
    return (200 <= responseCode && responseCode <= 399)
  }
  catch (IOException exception) {
    debug("Unable to connect to NXRM. Message was ${exception.getMessage()}")
    return false
  }
}

String getSshHost() {
  return "admin:admin123@localhost:${rcConfig.karafSshPort}"
}

def runBuild() {
  if (buildOptions.buildMode == "full" && isNxrmRunning()) {
    // Full mode means Maven clean which nukes target folder which kills Nexus
    warn("Shutting down NXRM due to full build")
    try {
      remoteSession(getSshHost(), trustUnknownHosts = true) {
        exec 'system:shutdown -f'
      }
    } catch (com.jcraft.jsch.JSchException e) {
      e.printStackTrace()
      error("Unable to connect to NXRM to shut it down. Attempting to continue.")
    }
  }

  if (buildOptions.buildMode == "full" && rcConfig.backup) {
    // perform backup (if enabled) on full build only due to 'clean'
    File sonatypeWork = new File(SONATYPE_WORK)
    if ((sonatypeWork).exists()) {
      def backupFolder = new File(SONATYPE_WORK_BACKUP)
      info("Backing up ${sonatypeWork.getCanonicalPath()} to ${backupFolder.getCanonicalPath()}")

      if (backupFolder.exists()) {
        def newName = new File(SONATYPE_WORK_BACKUP + new Date().format('-yyyyMMddHHmm'))
        info("Moving existing backup to ${newName.getCanonicalPath()}")
        FileUtils.moveDirectory(backupFolder, newName)
      }

      FileUtils.moveDirectory(sonatypeWork, backupFolder)
    }
    else {
      info("No target/sonatype-work to backup!")
    }
  }

  // execute command
  def exitValue = mvnw(buildOptions.mavenCommand).exitValue()

  debug("Build process exit value: $exitValue")

  // stop if mvn execution failed
  if (exitValue == 0) {
    info "Build succeeded"

    // TODO - should this be run on build failure still?
    saveLastBuild()
    return true
  }
  else {
    error "Build failed"
    return false
  }
}

def runDeploy() {
  if (buildOptions.buildMode == "full") {
    // only extract the assemblies on full builds
    deploy()
  }
  else if (!rcConfig.deploy) {
    info("Skipping deployment (no-deploy=true)")
  }
  else if (!((new File(LOCK_FILE)).exists())) {
    info("No lock file detected at $LOCK_FILE. Performing regular deployment.")
    deploy()
  }
  else if (isNxrmRunning()) {
    info("Nexus found running. Restarting...")

	try {
		remoteSession(getSshHost(), trustUnknownHosts = true) {
		  exec 'system:shutdown -f -r'
		}
	} catch (com.jcraft.jsch.JSchException e) {
		e.printStackTrace()
		error("Unable to connect to NXRM to restart it down. Attempting to continue.")
	}

    // give it a few to shut down
    sleep(3000)

    // loop until we can ping it again
    def counter = 0
    while (!isNxrmFullyUp()) {
      counter++
      print "."
      if (counter >= 60) {
        info("Failed to detect Nexus start within 60 seconds. Exiting")
        return
      }
      sleep 1000
    }
    println "done"
    info("Nexus Ready")
  }
  else {
    deploy()
    info("Nexus is not running. You may start it now with './nxrm.groovy -r <assembly> [debug]'.")
  }

}

def deploy() {
  extract("./assemblies/nexus-base-template/target/", "nexus-base-template-*.zip")
  extract("./private/assemblies/nexus-oss/target/", "nexus-*-bundle.zip")
  extract("./private/assemblies/nexus-pro/target/", "nexus-professional-*-bundle.zip")

  // Tell Karaf to load bundles from local .m2 folder
  def files = new FileNameFinder().getFileNames("target", "nexus*/**/org.ops4j.pax.url.mvn.cfg")
  files.each {
    // comment out localRepository
    ant.replace(file: it, token: "org.ops4j.pax.url.mvn.localRepository",
        value: "#org.ops4j.pax.url.mvn.localRepository")
    // point defaultRepositories to .m2
    ant.replace(file: it, token: 'file:${karaf.base}/${karaf.default.repository}@id=system.repository@snapshots',
        value: 'file:\${user.home}/.m2/repository@id=system.repository@snapshots')
  }
}

// helper to unzip assemblies
def extract(path, zipRegex) {
  List<String> files = new FileNameFinder().getFileNames(path, zipRegex)
  if (!files) {
    error("MISSING: ${path}${zipRegex}")
  }
  else {
    File file = new File(files.get(0))
    info("Extracting: ${file.toString()}")
    ArchiverFactory.createArchiver(ArchiveFormat.ZIP).extract(file, new File("target"))
  }
}

def checkSSL() {
  // Assumes if keystore.jks is already copied that it is already enabled
  if (rcConfig.ssl) {
    debug("Enabling SSL")

    // see if keystore.jks is already created
    def keystore = new File(".nxrm/keystore.jks")

    if (rcConfig.sslIp != null && keystore.exists()) {
      // If we have an ssl ip address set, then we need to generate a new keystore
      keystore.delete()
    }

    if (!keystore.exists()) {
      info "Generating .nxrm/keystore.jks"
      def keytool = ["keytool", "-genkeypair", "-keystore", ".nxrm/keystore.jks", "-storepass",
          "password", "-keypass", "password", "-alias", "self-signed-example", "-keyalg", "RSA", "-keysize", "2048",
          "-validity", "5000", "-dname", "CN=localhost, OU=Example, O=Example, L=Unspecified, ST=Unspecified, C=US",
          "-ext", "BC=ca:true"]
      if (rcConfig.sslIp) {
        keytool << "-ext"
        keytool << "SAN=IP:${rcConfig.sslIp},DNS:localhost"
      }

      def process = new ProcessBuilder(keytool as String[]).redirectErrorStream(true).start()
      process.inputStream.eachLine {
        println it
      }
      process.waitFor()
      def exitValue = process.exitValue()

      debug("keytool exit value: $exitValue")
    }

    // Copy to etc/ssl folder
    List<String> files = new FileNameFinder().getFileNames("target", "nexus*/NOTICE.txt")
    files.each {
      File dest = new File(new File(it).getParent(), "etc/ssl/")
      debug("SSL: Copying keystore.jks to $dest")
      ant.copy(file: keystore, todir: dest)
    }

    // Update nexus.properties (and default files)
    files = new FileNameFinder().getFileNames("target", "nexus*/**/nexus*.properties")
    files.each {
      ant.replace(file: it, token: '${jetty.etc}/jetty-http.xml,${jetty.etc}/jetty-requestlog.xml',
          value: '${jetty.etc}/jetty-http.xml,${jetty.etc}/jetty-https.xml,${jetty.etc}/jetty-requestlog.xml')
    }
  }
  else {
    debug("Skipping SSL")
  }
}

def ensurePresentInFile(File file, String line) {
  debug("Ensuring '$line' present in $file")
  ant.touch(file: file)
  if (!file.getText().contains(line)) {
    file << "$line\n"
  }
}

def checkSSH() {
  if (!rcConfig.'deploy') {
    debug("TODO no deploy")
    return
  }

  debug("Enabling SSH")

  // Add ssh option to all cfg files in target
  List<String> files = new FileNameFinder().getFileNames("target", "nexus*/**/org.apache.karaf.features.cfg")
  files.each {
    debug("Updating $it to enable SSH")
    ant.replaceregexp(file: it, match: "\\(wrap\\), ", replace: "\\(wrap\\),ssh,")
  }

  // Karaf ssh port
  files = new FileNameFinder().getFileNames("target", "nexus*/**/org.apache.karaf.shell.cfg")
  files.each {
    debug("Updating $it to set Karaf SSH port")
    ant.replaceregexp(file: it, match: "sshPort = 8022", replace: "sshPort = ${rcConfig.karafSshPort}")
  }

}

def checkOrient() {
  if (rcConfig.orient) {
    debug("Enabling Orient")

    // Update nexus.properties (and default files)
    List<String> files = new FileNameFinder().getFileNames("target", "nexus*/**/nexus*.properties")
    files.each {
      ensurePresentInFile(new File(it), "nexus.orient.binaryListenerEnabled=true")
      ensurePresentInFile(new File(it), "nexus.orient.httpListenerEnabled=true")
      ensurePresentInFile(new File(it), "nexus.orient.dynamicPlugins=true")
    }
  }
  else {
    debug('Skipping Orient config')
  }
}

def checkElastic() {
  if (rcConfig.elastic) {
    debug("Enabling Elastic")

    // Update elasticsearch.yml (and default files)
    List<String> files = new FileNameFinder().getFileNames("target", "nexus*/**/elasticsearch.yml")
    files.each {
      debug("Updating $it to set enable Elastic HTTP")
      ant.replace(file: it, token: "http.enabled: false", value: "http.enabled: true")
    }

    // Update nexus.properties (and default files)
    files = new FileNameFinder().getFileNames("target", "nexus*/**/nexus*.properties")
    files.each {
      ensurePresentInFile(new File(it), 'nexus.elasticsearch.plugins=license,marvel-agent,mobz/elasticsearch-head,lmenezes/elasticsearch-kopf,xyu/elasticsearch-whatson')
    }
  }
  else {
    debug('Skipping Elastic config')
  }
}

def checkPorts() {
  debug("Checking application ports")

  // Update nexus.properties (and default files)
  List<String> files = new FileNameFinder().getFileNames("target", "nexus*/etc/nexus*.properties sonatype-work/nexus3/etc/nexus*properties")
  files.each {
    ensurePresentInFile(new File(it), "application-port=${rcConfig.port}")
    ensurePresentInFile(new File(it), "application-port-ssl=${rcConfig.sslPort}")
  }
}

def checkRestore() {
  if (rcConfig.restore) {
    def backupFolder = new File(SONATYPE_WORK_BACKUP)
    if (backupFolder.exists()) {
      def sonatypeWork = new File(SONATYPE_WORK)
      info("Restoring from ${backupFolder.getCanonicalPath()} to ${sonatypeWork.getCanonicalPath()}")

      // move any existing sonatype-work out of the way
      if (sonatypeWork.exists()) {
        def newName = new File(SONATYPE_WORK_BACKUP + new Date().format('-yyyyMMddHHmm'))
        info("Moving existing sonatype-work to ${newName.getCanonicalPath()}")
        FileUtils.moveDirectory(sonatypeWork, newName)
      }

      FileUtils.moveDirectory(backupFolder, sonatypeWork)
    }
    else {
      info("No backup to restore in ${SONATYPE_WORK_BACKUP}!")
    }
  }
}

def runNxrm() {
  info("Starting Nexus on ${rcConfig.port}/${rcConfig.sslPort} (JDWP debug port: ${rcConfig.javaDebugPort})")

  // pre-flight checks
  checkRestore()
  checkSSL()
  checkSSH()
  checkOrient()
  checkElastic()
  checkPorts()

  def assembly = cliOptions.run
  debug("Assembly: $assembly")

  def run = { dir ->
    MavenXpp3Reader reader = new MavenXpp3Reader()
    Model model = reader.read(new FileReader(new File('pom.xml')))
    def version = model.getVersion()

    def nxrmCommand = ["target/${dir}${version}/bin/nexus".toString()]
    nxrmCommand = nxrmCommand.plus(positionalOptions.join(' ')) // add remaining arguments (e.g. -rf :project)
    info("Executing NXRM command: ${nxrmCommand.join(' ')}")

    if (cliOptions.'dry-run') {
      info("Dry run mode - skipping...")
      return
    }

    def processBuilder = new ProcessBuilder(nxrmCommand)
        .inheritIO()

    processBuilder.environment().put('NEXUS_SECURITY_RANDOMPASSWORD', Boolean.toString(rcConfig.randomPassword))
    processBuilder.environment().put('JAVA_MAX_MEM', rcConfig.javaMaxMem)
    processBuilder.environment().put('DIRECT_MAX_MEM', rcConfig.directMaxMem)
    processBuilder.environment().put('JAVA_DEBUG_PORT', Integer.toString(rcConfig.javaDebugPort))
    processBuilder.environment().put('EXTRA_JAVA_OPTS', rcConfig.vmOptions)

    def process = processBuilder.start()
    process.inputStream.eachLine {
      // print to console
      println it
    }
    process.waitFor()
    def exitValue = process.exitValue()
    debug("Run process exit value: $exitValue")
  }

  switch (assembly) {
    case "oss":
      run("nexus-")
      break
    case "pro":
      run("nexus-professional-")
      break
    case "base":
      run("nexus-base-template-")
      break
    default:
      error("Usage: ./nxrm.groovy -r { base | oss | pro } [nexus-options]")
  }
}

def geb() {
  info('Enabling Geb tests for your IDE')
  info("Note: The default build options do NOT even compile tests. Before running '--geb' you should run './nxrm.groovy -f -t skip' to compile ALL test code.")

  mvnw('dependency:properties process-test-resources -Dit -pl :functional-testsuite,:nexuspro-modern-testsuite,:nexuspro-fabric-testsuite')
}

def sass() {
  info('Compiling Sass files')

  mvnw('clean install -Pdriver -Dmode=build -pl :nexus-rapture')
}

/**
 * @param cmd a String with the entire command to execute
 * @return
 */
def mvnw(cmd) {
  info("Running command: ./mvnw $cmd")

  def process = new ProcessBuilder("unbuffer", "./mvnw", *cmd.split()).redirectErrorStream(true).start()
  process.inputStream.eachLine {
    // print to console
    println it
    // dump to build.log (strip colour)
    buildLog << it.replaceAll("\u001B\\[[;\\d]*m", "") + "\n"
  }
  process.waitFor()

  info("Done")
  return process
}

// SCRIPT STARTS HERE
removeBuildLog()

if (!processCliOptions(args)) {
  return
}

rcConfig = processRcConfigFile()

lastBuild = processLastBuild()

// stand-alone options
if (cliOptions.run) {
  runNxrm()
  return
}
else if (cliOptions.extract) {
  deploy()
  return
}
else if (cliOptions.geb) {
  geb()
  return
}
else if (cliOptions.sass) {
  sass()
  return
}
else {
  // build mode is the default
}

processBuildMode()
processMavenGoalsAndPhases()
processTestArgs()
processAssemblyArgs()
processSourceArgs()
processBuilder()
processMavenCommand()

hr()
info("-- nxrm.sh build script")
hr()
info("Build mode   | $buildOptions.buildModeDesc")
info("Goals/phases | $buildOptions.mavenGoalsAndPhasesDesc")
info("Tests        | $buildOptions.testsDesc")
info("Assemblies   | $buildOptions.assembliesDesc")
info("Sources      | $buildOptions.sourcesDesc")
info("Command      | ./mvnw ${buildOptions.mavenCommand}")
hr()

if (cliOptions.'dry-run') {
  info("Dry run mode - skipping...")
  return
}
if(runBuild()) {
  runDeploy()
}
