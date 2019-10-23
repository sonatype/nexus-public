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

import SettingsSection from './SettingsSection';

describe('SettingsSection', () => {
  it('renders correctly', () => {
    expect(shallow(
        <SettingsSection>
          <span>test</span>
        </SettingsSection>
    )).toMatchSnapshot();
  });

  it('renders correctly while isLoading', () => {
    expect(shallow(
        <SettingsSection isLoading={true}>
          <span>test</span>
        </SettingsSection>
    )).toMatchSnapshot();
  });

  describe('SettingsSection.FieldWrapper', () => {
    it('renders correctly without label', () => {
      expect(shallow(
          <SettingsSection.FieldWrapper>
            <input type='text'/>
          </SettingsSection.FieldWrapper>
      )).toMatchSnapshot();
    });

    it('renders correctly with label', () => {
      expect(shallow(
          <SettingsSection.FieldWrapper labelText='test label'>
            <input type='text'/>
          </SettingsSection.FieldWrapper>
      )).toMatchSnapshot();
    });
  });

  describe('SettingsSection.Footer', () => {
    it('renders correctly', () => {
      expect(shallow(
          <SettingsSection.Footer>
            <button>test</button>
          </SettingsSection.Footer>
      )).toMatchSnapshot();
    });
  });
});
