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
import React from 'react';
import {render} from '@testing-library/react';

import Textfield from './Textfield';
import UIStrings from '../../../constants/UIStrings';

describe('Textfield', () => {
  it('renders correctly without an error message', () => {
    const {queryByText} = render(<Textfield value="" />);

    expect(queryByText(UIStrings.ERROR.FIELD_REQUIRED)).not.toBeInTheDocument();
  });

  it('renders with a single error message', () => {
    const {queryByText} = render(<Textfield value="" validationErrors={UIStrings.ERROR.FIELD_REQUIRED} />);

    expect(queryByText(UIStrings.ERROR.FIELD_REQUIRED)).toBeInTheDocument();
  });

  it('shows the first error', () => {
    const {queryByText} = render(<Textfield value="" validationErrors={[UIStrings.ERROR.FIELD_REQUIRED, "ERROR_MESSAGE"]}/>);

    expect(queryByText(UIStrings.ERROR.FIELD_REQUIRED)).toBeInTheDocument();
    expect(queryByText("ERROR_MESSAGE")).not.toBeInTheDocument();
  });
});
