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

@Grab(group = 'ch.qos.logback', module = 'logback-classic', version = '1.2.3')
@Grab(group = 'org.apache.maven', module = 'maven-model', version = '3.5.0')
@Grab(group = 'org.ajoberstar', module = 'grgit', version = '2.2.1')

import ch.qos.logback.classic.Logger

import org.ajoberstar.grgit.*
import org.ajoberstar.grgit.operation.*
import org.ajoberstar.grgit.service.*
import org.apache.maven.model.Model
import org.apache.maven.model.io.xpp3.MavenXpp3Reader

import static ch.qos.logback.classic.Level.*

import static org.slf4j.Logger.ROOT_LOGGER_NAME
import static org.slf4j.LoggerFactory.getLogger

// set default log level (jgit seems to have something on DEBUG by default)
((Logger) getLogger(ROOT_LOGGER_NAME)).setLevel(OFF)

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

def projects = getChangedProjects()
println projects ? '-pl ' + projects.join(',') : ''
