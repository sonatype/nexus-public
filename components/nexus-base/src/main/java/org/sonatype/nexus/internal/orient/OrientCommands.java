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
package org.sonatype.nexus.internal.orient;

import java.lang.reflect.Method;
import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.commands.CommandSupport;
import org.sonatype.nexus.common.text.Strings2;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.orientechnologies.common.console.annotation.ConsoleCommand;
import com.orientechnologies.orient.console.OConsoleDatabaseApp;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Function;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.SessionFactory;
import org.eclipse.sisu.EagerSingleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.LOWER_UNDERSCORE;

/**
 * Registers OrientDB commands with Karaf's shell; handles execution requests and displays results.
 * 
 * @since 3.0
 */
@Named
@EagerSingleton
public class OrientCommands
    extends OConsoleDatabaseApp
{
  private static final Logger log = LoggerFactory.getLogger(OrientCommands.class);

  private static final String SCOPE = "orient";

  private final SessionFactory sessionFactory;

  @Inject
  public OrientCommands(@Nullable final SessionFactory sessionFactory) {
    super(new String[0]);

    this.sessionFactory = sessionFactory; // might be null during tests

    for (Method method : getConsoleMethods().keySet()) {
      if (method.isAnnotationPresent(ConsoleCommand.class)) {
        register(createOrientCommand(method));
      }
    }

    onBefore(); // set OrientDB command defaults
  }

  private void register(final Function command) {
    log.debug("Registering command: {}", command);
    if (sessionFactory != null) {
      sessionFactory.getRegistry().register(command);
    }
    else {
      log.warn("Unable to register command, sessionFactory is null: {}", command);
    }
  }

  @Override
  protected void printApplicationInfo() {
    // hide banner as it doesn't apply here
  }

  private Function createOrientCommand(final Method method) {
    return new CommandSupport(Action.class)
    {
      // OrientDB expects the method name to be transformed from camel-case into lower-case with spaces
      private final String command = LOWER_CAMEL.to(LOWER_UNDERSCORE, method.getName()).replace('_', ' ');

      @Override
      public Object execute(final Session session, final List<Object> arguments) throws Exception {
        return OrientCommands.this.execute(method, command, arguments);
      }

      @Override
      public String getScope() {
        return SCOPE;
      }

      @Override
      public String getName() {
        return method.getName();
      }

      @Override
      public String getDescription() {
        return method.getAnnotation(ConsoleCommand.class).description();
      }

      @Override
      public Completer getCompleter(final boolean scoped) {
        return null; // no special completion
      }

      @Override
      public Action createNewAction(final Session session) {
        return null; // not used
      }

      @Override
      protected void releaseAction(final Action action) {
        // no-op
      }

      @Override
      public String toString() {
        return method.toString();
      }
    };
  }

  public Object execute(final Method method, final String command, final List<Object> params) {
    if (params.isEmpty() || !"--help".equals(params.get(0))) {

      // rebuild expression so OrientDB can re-parse it to handle optional params, etc.
      List<?> expression = ImmutableList.builder().add(command).addAll(params).build();
      execute(Joiner.on(' ').useForNull("null").join(expression));

      return Strings2.NL;
    }

    syntaxError(command, method);
    return ""; // avoid spurious newline
  }

  @Override
  public void error(final String message, final Object... args) {
    // clean up error messages to remove redundant/irrelevant content
    if (!message.contains("Unrecognized command")) {
      super.error(message.replaceFirst("(?s)!Wrong syntax.*Expected", "Syntax"), args);
    }
  }
}
