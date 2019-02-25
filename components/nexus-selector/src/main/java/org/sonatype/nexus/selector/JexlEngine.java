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
package org.sonatype.nexus.selector;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sonatype.nexus.selector.internal.SandboxJexlUberspect;

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlInfo;
import org.apache.commons.jexl3.internal.Engine;
import org.apache.commons.jexl3.parser.ASTJexlScript;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.regex.Pattern.compile;
import static org.sonatype.nexus.selector.LeadingSlashScriptTransformer.trimLeadingSlashes;

/**
 * JEXL engine that provides access to the underlying syntax tree of expressions.
 *
 * @since 3.next
 */
class JexlEngine
    extends Engine
{
  // this stops JEXL from using expensive new Throwable().getStackTrace() to find caller info
  private static final JexlInfo CALLER_INFO = new JexlInfo("Selector", 0, 0);

  private static final Pattern JEXL_CONDENSED_INFO_HEADER = compile("Selector@\\d+(?::\\d+)?(?:![\\d+,\\d+]:)? *");

  JexlEngine() {
    super(new JexlBuilder().uberspect(new SandboxJexlUberspect()));
  }

  /**
   * Parses the given string as JEXL and returns the parsed expression as a script.
   */
  public ASTJexlScript parseExpression(final String expression) {
    String source = trimSource(checkNotNull(expression));
    return parse(CALLER_INFO, source, null, false, true);
  }

  /**
   * Builds a new {@link JexlExpression} from the given string.
   */
  public JexlExpression buildExpression(final String expression) {
    ASTJexlScript script = trimLeadingSlashes(parseExpression(expression));
    return new JexlExpression(this, expression, script);
  }

  /**
   * Returns detail about the given JEXL exception, expanded to make it more readable.
   */
  public static String expandExceptionDetail(final JexlException e) {
    StringBuilder detailBuilder = new StringBuilder(e.getMessage());

    JexlInfo info = e.getInfo();
    if (info != null) {
      // remove condensed header, replaced below with something more readable
      Matcher matcher = JEXL_CONDENSED_INFO_HEADER.matcher(detailBuilder);
      if (matcher.find()) {
        detailBuilder.delete(matcher.start(), matcher.end());
      }

      // add more detail if we have it and it's not already part of the message
      Optional<String> detail = ofNullable(info.getDetail()).map(Object::toString);
      if (detail.isPresent() && detailBuilder.indexOf(detail.get()) < 0) {
        addContext(detailBuilder, format("in '%s'", detail.get()));
      }

      // finally add the location in a more readable form
      addContext(detailBuilder, format("at line %d column %d", info.getLine(), info.getColumn()));
    }

    return detailBuilder.toString();
  }

  /**
   * Adds more context to the exception detail, separated by a space if necessary.
   */
  private static StringBuilder addContext(final StringBuilder detailBuilder, final String context) {
    if (detailBuilder.length() > 0) {
      detailBuilder.append(' ');
    }
    return detailBuilder.append(context);
  }
}
