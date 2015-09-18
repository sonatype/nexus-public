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
package org.sonatype.nexus.internal.commands

import javax.inject.Inject
import javax.inject.Named

import org.sonatype.nexus.commands.Complete
import org.sonatype.nexus.common.app.ApplicationDirectories
import org.sonatype.nexus.common.app.GlobalComponentLookupHelper

import org.apache.karaf.shell.commands.Argument
import org.apache.karaf.shell.commands.Command
import org.apache.karaf.shell.console.AbstractAction
import org.codehaus.groovy.control.CompilerConfiguration
import org.eclipse.sisu.inject.BeanLocator
import org.slf4j.LoggerFactory

/**
 * Action to execute a Groovy script.
 *
 * @since 3.0
 */
@Named
@Command(name='groovy', scope = 'nexus', description = 'Execute a Groovy script')
class GroovyAction
  extends AbstractAction
{
  @Inject
  @Named('nexus-uber')
  ClassLoader classLoader

  @Inject
  BeanLocator beanLocator

  @Inject
  GlobalComponentLookupHelper lookupHelper

  @Inject
  ApplicationDirectories applicationDirectories

  @Argument(name = 'file', required = true, index = 0, description = 'Groovy script file to execute')
  @Complete('auto')
  File file

  @Argument(name = 'args', index = 1, multiValued = true, description = 'Optional script arguments')
  List<String> args

  @Override
  protected def doExecute() {
    assert file.exists() : "Missing file: $file"

    Binding binding = new Binding()
    binding.setVariable('beanLocator', beanLocator)
    binding.setVariable('container', lookupHelper)
    binding.setVariable('session', session)
    binding.setVariable('args', args)

    CompilerConfiguration cc = new CompilerConfiguration()
    cc.output = new PrintWriter(session.console)
    cc.targetDirectory = new File(applicationDirectories.temporaryDirectory, 'groovy-classes')

    GroovyShell shell = new GroovyShell(classLoader, binding, cc)

    log.debug "Parsing script from file: $file"
    Script script = shell.parse(file)
    binding.setVariable('log', LoggerFactory.getLogger(script.getClass()))

    log.debug 'Running'
    def result = script.run()
    log.debug "Result: $result"

    return result
  }
}
