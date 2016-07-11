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
package org.sonatype.nexus.selector.internal;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.selector.JexlSelector;
import org.sonatype.nexus.selector.Selector;
import org.sonatype.nexus.selector.SelectorConfiguration;
import org.sonatype.nexus.selector.SelectorEvaluationException;
import org.sonatype.nexus.selector.SelectorEvaluator;
import org.sonatype.nexus.selector.VariableSource;

/**
 * Evaluate jexl selectors using a provided VariableSource
 *
 * @since 3.1
 */
@Named("jexl")
@Singleton
public class JexlSelectorEvaluator
    implements SelectorEvaluator
{
  @Override
  public boolean evaluate(SelectorConfiguration selectorConfiguration, VariableSource variableSource)
      throws SelectorEvaluationException
  {
    Selector selector = createSelector(selectorConfiguration);

    try {
      return selector.evaluate(variableSource);
    }
    catch (Exception e) {
      throw new SelectorEvaluationException("Selector '" + selectorConfiguration.getName() + "' evaluation in error",
          e);
    }
  }

  private Selector createSelector(SelectorConfiguration config) throws SelectorEvaluationException {
    if ("jexl".equals(config.getType())) {
      return new JexlSelector((String) config.getAttributes().get("expression"));
    }

    throw new SelectorEvaluationException("Invalid selector type encountered: " + config.getType());
  }
}
