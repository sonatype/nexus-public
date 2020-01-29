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

import FieldErrorMessage from '../FieldErrorMessage/FieldErrorMessage';
import Textfield from './Textfield';

describe('Textfield', () => {
  const getTextField = (extraProps) => {
    let props = {
      name: 'testTextfield',
      value: 'val',
      onChange: () => {
      },
      ...extraProps
    };
    return shallow(<Textfield {...props} />);
  };

  it('renders correctly', () => {
    expect(getTextField()).toMatchSnapshot();
  });

  it('hides the error message by default', () => {
    const wrapper = getTextField();

    expect(wrapper.containsMatchingElement(<FieldErrorMessage/>)).toBe(false);
    expect(wrapper.find('input').hasClass('missing-required-value')).toBe(false);
  });

  it('hides the error message when the value is required and not empty', () => {
    const wrapper = getTextField({
      isRequired: true
    });

    expect(wrapper.containsMatchingElement(<FieldErrorMessage/>)).toBe(false);
    expect(wrapper.find('input').hasClass('missing-required-value')).toBe(false);
  });

  it('shows the error message when the value is required but empty', () => {
    const wrapper = getTextField({
      isRequired: true,
      value: ''
    });

    expect(wrapper.containsMatchingElement(<FieldErrorMessage/>)).toBe(true);
    expect(wrapper.find('input').hasClass('missing-required-value')).toBe(true);
  });
});
