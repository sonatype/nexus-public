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
import java.util.Dictionary;
import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.commands.CommandHelper;
import org.sonatype.nexus.commands.CommandSupport;
import org.sonatype.nexus.common.text.Strings2;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.orientechnologies.common.console.annotation.ConsoleCommand;
import com.orientechnologies.orient.console.OConsoleDatabaseApp;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Function;
import org.apache.karaf.shell.commands.Action;
import org.apache.karaf.shell.commands.basic.AbstractCommand;
import org.apache.karaf.shell.console.SubShellAction;
import org.eclipse.sisu.EagerSingleton;
import org.osgi.framework.BundleContext;
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

  private final BundleContext bundleContext;

  // TODO: Consider a mass-command register/unregister component with mediator to avoid needing eager-singleton
  // TODO: and injected bundle-context

  @Inject
  public OrientCommands(final @Nullable BundleContext bundleContext) {
    super(new String[0]);

    // HACK: should be non-null, but in test env is null
    this.bundleContext = bundleContext;

    // define our own Karaf sub-shell: type 'orient' to enter, and 'exit' to leave
    register(createShellCommand(), CommandHelper.config("*", SCOPE));

    for (Method method : getConsoleMethods().keySet()) {
      if (method.isAnnotationPresent(ConsoleCommand.class)) {
        register(createOrientCommand(method), CommandHelper.config(SCOPE, method.getName()));
      }
    }

    onBefore(); // set OrientDB command defaults
  }

  private void register(final Function function, final Dictionary<String,?> config) {
    // HACK: should be non-null, but in test env is null
    if (bundleContext == null) {
      log.warn("Unable to register function, bundle-context is null: {}, config: {}", function, config);
      return;
    }

    log.debug("Registering function: {}, config: {}", function, config);
    bundleContext.registerService(Function.class, function, config);
  }

  @Override
  protected void printApplicationInfo() {
    // hide banner as it doesn't apply here
  }

  private AbstractCommand createOrientCommand(final Method method) {
    return new CommandSupport()
    {
      // OrientDB expects the method name to be transformed from camel-case into lower-case with spaces
      private final String command = LOWER_CAMEL.to(LOWER_UNDERSCORE, method.getName()).replace('_', ' ');

      @Override
      public Object execute(final CommandSession session, final List<Object> params) throws Exception {
        return OrientCommands.this.execute(method, command, params);
      }

      @Override
      public Class<? extends Action> getActionClass() {
        return Action.class;
      }

      @Override
      public Action createNewAction() {
        return null; // not used
      }

      @Override
      public String toString() {
        return method.toString();
      }
    };
  }

  private static AbstractCommand createShellCommand() {
    return new AbstractCommand()
    {
      @Override
      public Action createNewAction() {
        SubShellAction subShell = new SubShellAction();
        subShell.setSubShell(SCOPE);
        return subShell;
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
