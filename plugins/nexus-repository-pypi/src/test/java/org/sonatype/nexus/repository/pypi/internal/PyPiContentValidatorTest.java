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
package org.sonatype.nexus.repository.pypi.internal;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.mime.DefaultContentValidator;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.sonatype.nexus.repository.pypi.internal.PyPiContentValidator.TEXT_FILE_EXTENSION;

public class PyPiContentValidatorTest
    extends TestSupport
{
  @Mock
  private DefaultContentValidator defaultContentValidator;

  @InjectMocks
  private PyPiContentValidator pyPiContentValidator;

  @Test
  public void should_Append_Txt_Extension_When_Name_Ends_With_Asc_Extension() throws Exception {
    String contentName = "sample/sample-file-1.23.whl.asc";

    pyPiContentValidator.determineContentType(true, null, null,
        contentName, null);

    verify(defaultContentValidator)
        .determineContentType(eq(true), any(), any(), eq(contentName + TEXT_FILE_EXTENSION), any());
  }

  @Test
  public void should_Do_Nothing_When_Name_Is_Null() throws Exception {
    pyPiContentValidator.determineContentType(true, null, null,
        null, null);

    verify(defaultContentValidator)
        .determineContentType(eq(true), any(), any(), eq(null), any());
  }

  @Test
  public void should_Not_Alter_Name() throws Exception {
    String contentName = "sample/sample-file-1.23.whl";

    pyPiContentValidator.determineContentType(true, null, null,
        contentName, null);

    verify(defaultContentValidator)
        .determineContentType(eq(true), any(), any(), eq(contentName), any());
  }
}
