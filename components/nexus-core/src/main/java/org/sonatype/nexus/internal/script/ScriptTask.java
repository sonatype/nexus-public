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
package org.sonatype.nexus.internal.script;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.goodies.i18n.I18N;
import org.sonatype.goodies.i18n.MessageBundle;
import org.sonatype.nexus.common.script.ScriptService;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskSupport;

import com.google.common.collect.ImmutableMap;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Task to execute a script.
 *
 * @since 3.0
 */
@Named
public class ScriptTask
    extends TaskSupport
{
  private interface Messages
      extends MessageBundle
  {
    @DefaultMessage("Execute script")
    String message();
  }

  private static final Messages messages = I18N.create(Messages.class);

  private final ScriptService scripts;

  private String source;

  @Inject
  public ScriptTask(final ScriptService scripts) {
    this.scripts = checkNotNull(scripts);
  }

  @Override
  public void configure(final TaskConfiguration configuration) {
    super.configure(configuration);
    this.source = configuration.getString(ScriptTaskDescriptor.SOURCE);
  }

  @Override
  public String getMessage() {
    return messages.message();
  }

  @Override
  protected Object execute() throws Exception {
    log.debug("Executing script");

    ImmutableMap<String, Object> customBindings = ImmutableMap.<String, Object>builder()
        .put("log", LoggerFactory.getLogger(ScriptTask.class))
        .put("task", this)
        .build();
    
    // execution script
    log.debug("Evaluating source: {}", source);
    Object result = scripts.eval(getConfiguration().getString(ScriptTaskDescriptor.LANGUAGE), source, customBindings);
    log.trace("Result: {}", result);

    return result;
  }
}
