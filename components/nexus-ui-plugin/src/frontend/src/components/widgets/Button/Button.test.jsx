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
import '@testing-library/jest-dom/extend-expect';

import Button from './Button';

describe('Button', () => {
  const renderButton = (extraProps) => {
    let props = {
      name: 'b1',
      onChange: () => {
      },
      ...extraProps
    };
    let {container} = render(<Button {...props} />);
    return container;
  };

  it('renders correctly', () => {
    expect(renderButton()).toMatchSnapshot();
  });

  it('renders correctly as a primary button', () => {
    expect(renderButton({variant: 'primary'})).toMatchSnapshot();
  });

  it('renders correctly when disabled', () => {
    expect(renderButton({disabled: true})).toMatchSnapshot();
  });

  it('allows for its style to be overriden', () => {
    expect(renderButton({
      style: {
        border: 'none'
      }
    })).toMatchSnapshot();
  });
});
