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
import {fireEvent, render, wait} from '@testing-library/react';
import UIStrings from '../constants/UIStrings';

/**
 * @since 3.24
 */
export default class TestUtils {
  static render(view, extraSelectors) {
    const selectors = render(view);
    const {queryByText} = selectors;
    return {
      ...selectors,
      loadingMask: () => queryByText('Loading…'),
      savingMask: () => queryByText(UIStrings.SAVING),
      ...extraSelectors(selectors)};
  }

  static async changeField(fieldSelector, value) {
    fireEvent.change(fieldSelector(), {
      currentTarget: {
        name: fieldSelector().name,
        value
      },
      target: {
        name: fieldSelector().name,
        value
      }
    });
    try {
      await wait(() => expect(fieldSelector()).toHaveValue(value));
    } catch(error) {
      throw new Error(`${fieldSelector().name} with value ${fieldSelector().value} did not match the expected value \n ${error.message}`);
    }
  }
}
