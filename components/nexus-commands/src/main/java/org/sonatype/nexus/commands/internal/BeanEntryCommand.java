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

import java.util.Iterator;

import org.sonatype.nexus.commands.CommandSupport;
import org.sonatype.nexus.commands.SessionAware;

import com.google.inject.Key;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.console.Command;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.eclipse.sisu.BeanEntry;
import org.eclipse.sisu.inject.BeanLocator;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Adapts Sisu {@link BeanEntry} (carrying {@link Action}) to a Karaf {@link Command}.
 *
 * @since 3.0
 */
public class BeanEntryCommand
    extends CommandSupport
{
  private final BeanLocator beanLocator;

  private final BeanEntry<?, Action> beanEntry;

  public BeanEntryCommand(final BeanLocator beanLocator, final BeanEntry<?, Action> beanEntry) {
    super(beanEntry.getImplementationClass());

    this.beanLocator = checkNotNull(beanLocator);
    this.beanEntry = checkNotNull(beanEntry);
  }

  @Override
  protected Completer getCompleter(final Class<?> clazz) {
    @SuppressWarnings({ "unchecked", "rawtypes" })
    Iterator<? extends BeanEntry<?, Completer>> itr = beanLocator.locate(Key.get((Class) clazz)).iterator();
    if (itr.hasNext()) {
      return itr.next().getValue();
    }
    return super.getCompleter(clazz);
  }

  @Override
  protected Action createNewAction(final Session session) {
    Action action = beanEntry.getProvider().get(); // create new instance each time
    if (action instanceof SessionAware) {
      ((SessionAware) action).setSession(session);
    }
    return action;
  }

  @Override
  protected void releaseAction(final Action action) {
    // no-op
  }

  @Override
  public String toString() {
    return beanEntry.toString();
  }

  @Override
  public int hashCode() {
    return beanEntry.hashCode();
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof BeanEntryCommand) {
      return beanEntry.equals(((BeanEntryCommand) obj).beanEntry);
    }
    return false;
  }
}
