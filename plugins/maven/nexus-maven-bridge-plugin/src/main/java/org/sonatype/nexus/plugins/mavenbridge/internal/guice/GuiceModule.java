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
package org.sonatype.nexus.plugins.mavenbridge.internal.guice;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.aether.spi.locator.ServiceLocator;
import org.sonatype.sisu.maven.bridge.MavenArtifactResolver;
import org.sonatype.sisu.maven.bridge.MavenDependencyTreeResolver;
import org.sonatype.sisu.maven.bridge.MavenModelResolver;
import org.sonatype.sisu.maven.bridge.support.artifact.RemoteMavenArtifactResolver;
import org.sonatype.sisu.maven.bridge.support.dependency.RemoteMavenDependencyTreeResolver;
import org.sonatype.sisu.maven.bridge.support.model.RemoteMavenModelResolver;

import com.google.inject.AbstractModule;
import org.apache.maven.repository.internal.DefaultServiceLocator;

@Named
@Singleton
public class GuiceModule
    extends AbstractModule
{

  @Override
  protected void configure() {
    bind(ServiceLocator.class).to(DefaultServiceLocator.class);
    bind(MavenArtifactResolver.class).to(RemoteMavenArtifactResolver.class);
    bind(MavenModelResolver.class).to(RemoteMavenModelResolver.class);
    bind(MavenDependencyTreeResolver.class).to(RemoteMavenDependencyTreeResolver.class);
  }

}
