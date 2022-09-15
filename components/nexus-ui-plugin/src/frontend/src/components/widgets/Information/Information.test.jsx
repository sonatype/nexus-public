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

import Information from './Information';

describe('Information', () => {
  it('renders boolean values correctly', () => {
    const info = {
      'boolean': false
    };
    const {getByText} = render(<Information information={info}/>);

    expect(getByText('false')).toBeInTheDocument();
  });

  it('renders numeric values correctly', () => {
    const info = {
      'numeric': 0
    };
    const {getByText} = render(<Information information={info}/>);

    expect(getByText('0')).toBeInTheDocument();
  });

  it('renders text values correctly', () => {
    const info = {
      'text': 'test'
    };
    const {getByText} = render(<Information information={info}/>);

    expect(getByText('test')).toBeInTheDocument();
  });
});
