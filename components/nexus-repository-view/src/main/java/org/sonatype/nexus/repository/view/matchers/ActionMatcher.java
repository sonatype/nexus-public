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
package org.sonatype.nexus.repository.view.matchers;

import java.util.List;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Matcher;
import org.sonatype.nexus.repository.view.Request;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.asList;

/**
 * Matches if the {@link Request#getAction() request action} is in the allowed list.
 *
 * @since 3.0
 */
public class ActionMatcher
    extends ComponentSupport
    implements Matcher
{
  private final List<String> allowedActions;

  public ActionMatcher(final String... allowedActions) {
    checkNotNull(allowedActions);
    checkArgument(allowedActions.length > 0, "at least one allowed action must be specified");
    this.allowedActions = asList(allowedActions);
  }

  @Override
  public boolean matches(final Context context) {
    final String action = context.getRequest().getAction();
    return allowedActions.contains(action);
  }

  @Override
  public String toString() {
    return "ActionMatcher [allowedActions=" + allowedActions + "]";
  }
}
