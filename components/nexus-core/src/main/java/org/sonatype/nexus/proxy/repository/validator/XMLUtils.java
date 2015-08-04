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
package org.sonatype.nexus.proxy.repository.validator;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Scanner;

import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.repository.validator.FileTypeValidator.FileTypeValidity;

/**
 * Static helper methods to make "XML like" files probation against some patterns or expected content easier and
 * reusable.
 *
 * @author cstamas
 * @since 2.0
 */
public class XMLUtils
{
  /**
   * Validate an "XML like file" using at most 200 lines from it's beginning. See
   * {@link #validateXmlLikeFile(StorageFileItem, String, int)} for details.
   *
   * @param file            file who's content needs to be checked for.
   * @param expectedPattern the expected String pattern to search for.
   * @return {@link FileTypeValidity.VALID} if pattern found, {@link FileTypeValidity.INVALID} otherwise.
   * @throws IOException in case of IO problem while reading file content.
   */
  public static FileTypeValidity validateXmlLikeFile(final StorageFileItem file, final String expectedPattern)
      throws IOException
  {
    return validateXmlLikeFile(file, expectedPattern, 200);
  }

  /**
   * Validate an "XML like file" by searching for passed in patterns (using plain string matching), consuming at most
   * lines as passed in as parameter.
   *
   * @param file            file who's content needs to be checked for.
   * @param expectedPattern the expected String pattern to search for.
   * @param linesToCheck    amount of lines (as detected by Scanner) to consume during check.
   * @return {@link FileTypeValidity.VALID} if pattern found, {@link FileTypeValidity.INVALID} otherwise.
   * @throws IOException in case of IO problem while reading file content.
   */
  public static FileTypeValidity validateXmlLikeFile(final StorageFileItem file, final String expectedPattern,
                                                     final int linesToCheck)
      throws IOException
  {
    int lineCount = 0;
    try (BufferedInputStream bis = new BufferedInputStream(file.getInputStream())) {
      Scanner scanner = new Scanner(bis);
      while (scanner.hasNextLine() && lineCount < linesToCheck) {
        lineCount++;
        String line = scanner.nextLine();
        if (line.contains(expectedPattern)) {
          return FileTypeValidity.VALID;
        }
      }
    }

    return FileTypeValidity.INVALID;
  }

}
