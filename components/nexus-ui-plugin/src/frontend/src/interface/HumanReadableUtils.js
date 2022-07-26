
/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
import UIStrings from '../constants/UIStrings';
import fileSize from 'file-size';

/**
 * @since 3.41
 *
 * Utility methods for creating human readable strings from data.
 */
export default class HumanReadableUtils {
  /**
   * Convert a size in bytes to a human readable string
   * @param bytes
   * @param unitOfMeasurement - si, iec, or jedec, more information available at https://www.npmjs.com/package/file-size
   * @return {*}
   */
  static bytesToString(bytes, unitNotation = 'jedec') {
    if (bytes < 0) {
      return UIStrings.UNAVAILABLE;
    }
    return fileSize(bytes).human(unitNotation);
  }
}
