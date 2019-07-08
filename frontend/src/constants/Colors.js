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

const BASE = {
  ALMOST_BLACK: 'rgba(51, 51, 51, 1)',
  ALMOST_WHITE: 'rgba(243, 243, 243, 1)',
  BLUE: 'rgba(10, 108, 184, 1)',
  GRAY: 'rgba(194, 194, 194, 1)',
  LIGHT_GRAY: 'rgba(210, 210, 210, 1)',
  RED: 'rgba(207, 76, 53, 1)',
  TRANSPARENT: 'rgba(0, 0, 0, 0.5)',
  WHITE: 'white'
};

export default {
  BUTTON: {
    PRIMARY: {
      BORDER: BASE.ALMOST_BLACK,
      BACKGROUND: BASE.BLUE,
      FONT: BASE.WHITE
    },
    SECONDARY: {
      BORDER: BASE.ALMOST_BLACK,
      BACKGROUND: BASE.ALMOST_WHITE,
      FONT: BASE.ALMOST_BLACK
    }
  },
  LOADING_MASK: {
    BACKGROUND: BASE.TRANSPARENT,
    FONT: BASE.WHITE
  },
  SELECT: {
    BORDER: BASE.LIGHT_GRAY,
    BORDER_TOP: BASE.GRAY,
    BACKGROUND: BASE.WHITE
  },
  SETTINGS_SECTION: {
    BACKGROUND: BASE.WHITE,
    BORDER: BASE.LIGHT_GRAY
  },
  TEXTFIELD: {
    BORDER: BASE.LIGHT_GRAY,
    BORDER_TOP: BASE.GRAY,
    ERROR: {
      BORDER: BASE.RED,
      BORDER_TOP: BASE.RED,
      FONT: BASE.RED
    }
  }
}
