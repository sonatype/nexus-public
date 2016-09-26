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

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.goodies.common.ComponentSupport;

import com.google.inject.Key;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.console.SessionFactory;
import org.eclipse.sisu.BeanEntry;
import org.eclipse.sisu.EagerSingleton;
import org.eclipse.sisu.Mediator;
import org.eclipse.sisu.inject.BeanLocator;
import org.osgi.framework.BundleContext;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Manages registration of Karaf {@link Action} instances.
 *
 * @since 3.0
 */
@Named
@EagerSingleton
public class ActionRegistrar
    extends ComponentSupport
{
  private final BeanLocator beanLocator;

  private final SessionFactory sessionFactory;

  @Inject
  public ActionRegistrar(final BeanLocator beanLocator,
                         @Nullable final SessionFactory sessionFactory,
                         @Nullable final BundleContext bundleContext)
  {
    this.beanLocator = checkNotNull(beanLocator);
    this.sessionFactory = sessionFactory; // might be null during tests

    // HACK: BundleContext may be null in present injected-UT environment
    if (bundleContext != null) {
      beanLocator.watch(Key.get(Action.class, Named.class), new ActionMediator(), bundleContext);
    }
    else {
      log.warn("BundleContext is not available, unable to watch action components for registration");
    }
  }

  private class ActionMediator
      implements Mediator<Named, Action, BundleContext>
  {
    @Override
    public void add(final BeanEntry<Named, Action> beanEntry, final BundleContext bundleContext) throws Exception {
      Command command = beanEntry.getImplementationClass().getAnnotation(Command.class);
      if (command != null) {
        log.debug("Registering command: {}", beanEntry);
        sessionFactory.getRegistry().register(new BeanEntryCommand(beanLocator, beanEntry));
      }
      else {
        log.warn("Missing @Command annotation on action: {}", beanEntry);
      }
    }

    @Override
    public void remove(final BeanEntry<Named, Action> beanEntry, final BundleContext bundleContext) throws Exception {
      Command command = beanEntry.getImplementationClass().getAnnotation(Command.class);
      if (command != null) {
        log.debug("Unregistering command: {}", beanEntry);
        sessionFactory.getRegistry().unregister(new BeanEntryCommand(beanLocator, beanEntry));
      }
    }
  }
}
