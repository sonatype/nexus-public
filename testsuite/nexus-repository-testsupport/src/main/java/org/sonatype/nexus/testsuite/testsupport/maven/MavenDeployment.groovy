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
package org.sonatype.nexus.testsuite.testsupport.maven

import java.nio.charset.StandardCharsets

import javax.annotation.Nullable

import org.sonatype.nexus.common.io.DirectoryHelper

import com.google.common.io.Files
import groovy.transform.ToString
import groovy.transform.builder.Builder
import groovy.util.logging.Slf4j

import static com.google.common.base.Preconditions.checkState

/**
 * Describe and modify a Maven deployment based on some template files and the desired configuration.
 * @since 3.2
 */
@Builder
@Slf4j
@ToString(includeNames = true)
class MavenDeployment
{
  /**
   * Template location of the project to copy from, should contain pom.xml and any other files required for the deployment.
   */
  File projectTemplateDir

  /**
   * Template of the settings.xml to be used.
   */
  File settingsTemplate

  /**
   * The directory to build from, contents of the template project will be copied here.
   */
  File projectDir

  /**
   * URL used for resolving dependencies.
   */
  URL proxyUrl

  /**
   * URL to deploy to.
   */
  URL deployUrl

  /**
   * URL to run maven site to.
   */
  @Nullable
  URL siteUrl

  /**
   * Group id, with default.
   */
  String groupId = 'org.sonatype.nexus.testsuite'

  /**
   * Artifact id, with default.
   */
  String artifactId = 'testproject'

  /**
   * Version to set.
   */
  String version

  /**
   * Enforce that the project directory doesn't exist when we init.
   */
  boolean ensureCleanOnInit = true

  /**
   * Initialize a new maven project folder and configure pom.xml and settings.xml files as specified.
   */
  public MavenDeployment init() {
    checkState(projectTemplateDir.isDirectory(), "Project template dir is missing: $projectTemplateDir")
    checkState(settingsTemplate.exists(), "Settings file is missing: $settingsTemplate")
    checkState(proxyUrl != null, 'Proxy url is not set')
    checkState(version != null, 'Version is not set')
    if (ensureCleanOnInit) {
      checkState(!projectDir.exists(), "Project dir already exists: $projectDir")
      checkState(projectDir.mkdirs(), "Could not create project directory: $projectDir")
    }

    log.info this.toString()
    
    DirectoryHelper.copy(projectTemplateDir.toPath(), projectDir.toPath())

    String settingsContent = Files.toString(settingsTemplate, StandardCharsets.UTF_8)
        .replace('${proxyUrl}', proxyUrl.toString())
    File settings = settingsFile()
    settings.createNewFile() 
    settings.text = settingsContent
    
    File pom = pomFile()
    String pomContent = Files.toString(pom, StandardCharsets.UTF_8)
        .replace('${project.groupId}', groupId)
        .replace('${project.artifactId}', artifactId)
        .replace('${project.version}', version)
    if(siteUrl) {
      pomContent = pomContent.replace('${site.url}', siteUrl.authority + siteUrl.path)
    }
    pom.text = pomContent

    return this
  }

  public File pomFile() {
    new File(projectDir, 'pom.xml')
  }

  public File settingsFile() {
    new File(projectDir, 'settings.xml')
  }

}
