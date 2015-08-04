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
package org.sonatype.nexus.plugins.p2bridge.internal.guice;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.plugins.p2bridge.internal.ArtifactRepositoryProvider;
import org.sonatype.nexus.plugins.p2bridge.internal.CompositeRepositoryProvider;
import org.sonatype.nexus.plugins.p2bridge.internal.HttpProxyProvider;
import org.sonatype.nexus.plugins.p2bridge.internal.MetadataRepositoryProvider;
import org.sonatype.nexus.plugins.p2bridge.internal.PublisherProvider;
import org.sonatype.p2.bridge.ArtifactRepository;
import org.sonatype.p2.bridge.CompositeRepository;
import org.sonatype.p2.bridge.HttpProxy;
import org.sonatype.p2.bridge.MetadataRepository;
import org.sonatype.p2.bridge.Publisher;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

@Named
@Singleton
public class GuiceModule
    extends AbstractModule
{

  @Override
  protected void configure() {
    bind(ArtifactRepository.class).toProvider(ArtifactRepositoryProvider.class).in(Scopes.SINGLETON);
    bind(MetadataRepository.class).toProvider(MetadataRepositoryProvider.class).in(Scopes.SINGLETON);
    bind(CompositeRepository.class).toProvider(CompositeRepositoryProvider.class).in(Scopes.SINGLETON);
    bind(Publisher.class).toProvider(PublisherProvider.class).in(Scopes.SINGLETON);
    bind(HttpProxy.class).toProvider(HttpProxyProvider.class).in(Scopes.SINGLETON);
  }

}