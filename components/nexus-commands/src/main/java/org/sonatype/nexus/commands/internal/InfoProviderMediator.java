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
package org.sonatype.nexus.commands.internal;

import javax.inject.Named;

import org.sonatype.goodies.common.ComponentSupport;

import org.apache.karaf.shell.commands.info.InfoProvider;
import org.eclipse.sisu.BeanEntry;
import org.eclipse.sisu.Mediator;
import org.osgi.framework.BundleContext;

// FIXME: This still does not seem to do the trick, to get custom InfoProviders available in shell:info

/**
 * Manages registration of Karaf {@link InfoProvider} instances.
 *
 * @since 3.0
 */
@Named
public class InfoProviderMediator
    extends ComponentSupport
    implements Mediator<Named, InfoProvider, BundleContext>
{
  @Override
  public void add(final BeanEntry<Named, InfoProvider> beanEntry, final BundleContext bundleContext) throws Exception {
    log.debug("Adding: {}", beanEntry);
    bundleContext.registerService(InfoProvider.class, beanEntry.getValue(), null);
  }

  @Override
  public void remove(
      final BeanEntry<Named, InfoProvider> beanEntry,
      final BundleContext bundleContext) throws Exception
  {
    // TODO: implement remove
  }
}
