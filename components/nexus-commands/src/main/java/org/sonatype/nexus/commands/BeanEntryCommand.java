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
package org.sonatype.nexus.commands;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import com.google.inject.Key;
import com.google.inject.name.Names;
import org.apache.karaf.shell.commands.Action;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.CommandWithAction;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.AggregateCompleter;
import org.apache.karaf.shell.console.completer.NullCompleter;
import org.eclipse.sisu.BeanEntry;
import org.eclipse.sisu.inject.BeanLocator;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Adapts Sisu {@link BeanEntry} (carrying {@link Action}) to a Karaf {@link CommandWithAction}.
 *
 * @since 3.0
 */
public class BeanEntryCommand
    extends CommandSupport
{
  private final BeanLocator beanLocator;

  private final BeanEntry<?, Action> beanEntry;

  public BeanEntryCommand(final BeanLocator beanLocator,
                          final BeanEntry<?, Action> beanEntry)
  {
    this.beanLocator = checkNotNull(beanLocator);
    this.beanEntry = checkNotNull(beanEntry);

    discoverCompleters(beanEntry.getImplementationClass());
  }

  private void discoverCompleters(final Class<Action> type) {
    for (Field field : type.getDeclaredFields()) {
      Completer completer = discoverCompleter(field);
      if (completer == null) {
        continue;
      }

      Argument argument = field.getAnnotation(Argument.class);
      Option option = field.getAnnotation(Option.class);
      if (argument == null && option == null) {
        log.warn("Missing @Argument or @Option on field: {}", field);
        continue;
      }

      if (argument != null) {
        addArgumentCompleter(argument.index(), completer);
      }
      else { // option
        addOptionCompleter(option.name(), completer);
      }
    }
  }

  @Nullable
  private Completer discoverCompleter(final Field field) {
    Complete complete = field.getAnnotation(Complete.class);

    // skip if no completion configuration is present
    if (complete == null) {
      return null;
    }

    List<Completer> completers = Lists.newArrayList();
    for (String name : complete.value()) {
      if ("null".equals(name)) {
        completers.add(NullCompleter.INSTANCE);
      }
      else {
        Completer completer = lookupCompleter(name);
        if (completer != null) {
          if (completer instanceof CompleterTargetAware) {
            ((CompleterTargetAware) completer).setCompleterTarget(field);
          }
          completers.add(completer);
        }
        else {
          log.warn("Missing completer with name: {}", name);
        }
      }
    }

    log.trace("Discovered completers: {}", completers);

    // wrap with aggregate if > 1 completer
    if (completers.size() == 1) {
      return completers.get(0);
    }
    else {
      return new AggregateCompleter(completers);
    }
  }

  @Nullable
  private Completer lookupCompleter(final String name) {
    Iterator<? extends BeanEntry<Annotation, Completer>> iter =
        beanLocator.locate(Key.get(Completer.class, Names.named(name))).iterator();
    if (iter.hasNext()) {
      return iter.next().getValue();
    }
    else {
      return null;
    }
  }

  @Override
  public Class<? extends org.apache.felix.gogo.commands.Action> getActionClass() {
    return beanEntry.getImplementationClass();
  }

  @Override
  public Action createNewAction() {
    return beanEntry.getProvider().get();
  }

  @Override
  public String toString() {
    return beanEntry.toString();
  }
}
