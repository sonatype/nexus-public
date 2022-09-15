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
import {render, screen} from '@testing-library/react';

import DynamicFormField from './DynamicFormField';

describe('DynamicFormField', () => {
  describe('renders the string type', () => {
    const stringFieldProps = {
      id: 'test',
      current: {
        context: {
          data: {
            test: ''
          },
          pristineData: {
            test: ''
          }
        }
      }
    };

    it('renders', () => {
      const {container} = render(<DynamicFormField {...stringFieldProps} dynamicProps={{
        type: 'string',
        attributes: {}
      }}/>);

      expect(container.querySelector('input[type=text]')).toBeInTheDocument();
    });

    it('renders a long field', () => {
      const {container} = render(<DynamicFormField {...stringFieldProps} dynamicProps={{
        type: 'string',
        attributes: {long: true}
      }}/>);

      expect(container.querySelector('.nx-text-input--long')).toBeInTheDocument();
    });

    it('renders a disabled field', () => {
      const {getByRole} = render(<DynamicFormField {...stringFieldProps} dynamicProps={{
        type: 'string',
        attributes: {},
        disabled: true
      }}/>);

      expect(getByRole('textbox')).toBeDisabled();
    });

    it('renders a readonly field', () => {
      const {getByRole} = render(<DynamicFormField {...stringFieldProps} dynamicProps={{
        type: 'string',
        attributes: {},
        readOnly: true
      }}/>);

      expect(getByRole('textbox')).toHaveAttribute('readonly');
    });

    it('renders a MultiSelect field', () => {
      const options = ['transfer-list-option-1', 'transfer-list-option-2'];
      const dynamicProps = {
        type: 'itemselect',
        attributes: {
          options
        }
      }
      const {container} = render(<DynamicFormField
         {...makeContext('test', [])} dynamicProps={dynamicProps}
        />);

      expect(screen.getByText('Available Items')).toBeInTheDocument();  
      expect(screen.getByText('Transferred Items')).toBeInTheDocument(); 
      expect(screen.getByLabelText(options[0])).toBeInTheDocument();
      expect(screen.getByLabelText(options[1])).toBeInTheDocument();
    });

    it('renders a Select field', () => {
      const {container} = render(<DynamicFormField {...makeContext('test', 'a')} dynamicProps={{
        type: 'combobox',
        attributes: {
          options: {'a': 'a', 'b':'b'}
        }
      }}/>);

      expect(container.querySelector('.nx-form-select')).toBeInTheDocument();
      expect(container.querySelector('option[value=a]')).toBeInTheDocument();
      expect(container.querySelector('option[value=b]')).toBeInTheDocument();
    });

    function makeContext(id, value) {
      return {
        id,
        current: {
          context: {
            data: {
              [id]: value
            },
            pristineData: {
              [id]: value
            }
          }
        }
      }
    }
  });
});
