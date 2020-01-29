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
package org.sonatype.nexus.datastore.mybatis;

import java.io.IOException;
import java.util.function.Predicate;

import org.sonatype.nexus.security.PasswordHelper;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.util.JsonParserDelegate;

import static com.fasterxml.jackson.core.JsonToken.VALUE_STRING;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link JsonParser} wrapper that decrypts the values of sensitive attributes.
 *
 * @since 3.21
 */
final class SensitiveJsonParser
    extends JsonParserDelegate
{
  private final PasswordHelper passwordHelper;

  private final Predicate<String> attributeFilter;

  SensitiveJsonParser(final JsonParser delegate,
                      final PasswordHelper passwordHelper,
                      final Predicate<String> atributeFilter)
  {
    super(delegate);
    this.passwordHelper = checkNotNull(passwordHelper);
    this.attributeFilter = checkNotNull(atributeFilter);
  }

  @Override
  public String getText() throws IOException {
    String text = super.getText();
    if (text != null && currentToken() == VALUE_STRING && isSensitiveAttribute()) {
      text = passwordHelper.decrypt(text);
    }
    return text;
  }

  private boolean isSensitiveAttribute() {
    String attributName = getParsingContext().getCurrentName();
    return attributName != null && attributeFilter.test(attributName);
  }
}
