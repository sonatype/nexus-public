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
import {shallow} from 'enzyme';
import React from 'react';

import Button from './Button';

describe('Button', () => {
  const getButton = (extraProps) => {
    let props = {
      name: 'b1',
      onChange: () => {
      },
      ...extraProps
    };
    return shallow(<Button {...props} />);
  };

  it('renders correctly', () => {
    expect(getButton()).toMatchSnapshot();
  });

  it('renders correctly as a primary button', () => {
    expect(getButton({isPrimary: true})).toMatchSnapshot();
  });

  it('renders correctly when disabled', () => {
    expect(getButton({disabled: true})).toMatchSnapshot();
  });

  it('allows for its style to be overriden', () => {
    expect(getButton({
      style: {
        border: 'none'
      }
    })).toMatchSnapshot();
  });
});
