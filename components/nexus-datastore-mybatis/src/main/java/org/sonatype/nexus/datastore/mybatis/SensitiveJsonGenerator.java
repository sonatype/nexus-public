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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.util.JsonGeneratorDelegate;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link JsonGenerator} wrapper that encrypts the values of sensitive attributes.
 *
 * @since 3.21
 */
final class SensitiveJsonGenerator
    extends JsonGeneratorDelegate
{
  private final PasswordHelper passwordHelper;

  private final Predicate<String> attributeFilter;

  SensitiveJsonGenerator(final JsonGenerator delegate,
                         final PasswordHelper passwordHelper,
                         final Predicate<String> attributeFilter)
  {
    super(delegate, false);
    this.passwordHelper = checkNotNull(passwordHelper);
    this.attributeFilter = checkNotNull(attributeFilter);
  }

  @Override
  public void writeString(final String text) throws IOException {
    if (text != null && isSensitiveAttribute()) {
      super.writeString(passwordHelper.encrypt(text));
    }
    else {
      super.writeString(text);
    }
  }

  @Override
  public void writeString(final SerializableString text) throws IOException {
    if (text != null && isSensitiveAttribute()) {
      super.writeString(passwordHelper.encrypt(text.getValue()));
    }
    else {
      super.writeString(text);
    }
  }

  @Override
  public void writeString(final char[] text, final int offset, final int length) throws IOException {
    if (text != null && isSensitiveAttribute()) {
      super.writeString(passwordHelper.encryptChars(text, offset, length));
    }
    else {
      super.writeString(text, offset, length);
    }
  }

  private boolean isSensitiveAttribute() {
    String attributeName = getOutputContext().getCurrentName();
    return attributeName != null && attributeFilter.test(attributeName);
  }
}
