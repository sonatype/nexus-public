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
package org.sonatype.nexus.ruby;

import org.jruby.embed.ScriptingContainer;

public abstract class ScriptWrapper
{
  protected final ScriptingContainer scriptingContainer;

  private final Object object;

  public ScriptWrapper(ScriptingContainer scriptingContainer, Object object) {
    this.scriptingContainer = scriptingContainer;
    this.object = object;
  }

  protected void callMethod(String methodName, Object singleArg) {
    scriptingContainer.callMethod(object, methodName, singleArg);
  }

  protected <T> T callMethod(String methodName, Object singleArg, Class<T> returnType) {
    return scriptingContainer.callMethod(object, methodName, singleArg, returnType);
  }

  protected <T> T callMethod(String methodName, Object[] args, Class<T> returnType) {
    return scriptingContainer.callMethod(object, methodName, args, returnType);
  }

  protected <T> T callMethod(String methodName, Class<T> returnType) {
    return scriptingContainer.callMethod(object, methodName, returnType);
  }

  protected void callMethod(String methodName) {
    scriptingContainer.callMethod(object, methodName);
  }
}