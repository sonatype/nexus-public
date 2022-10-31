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

import React, {useState} from 'react';

import {FormUtils, ValidationUtils} from '@sonatype/nexus-ui-plugin';
import {isEmpty} from 'ramda';

import {faTrashAlt, faPlusCircle} from '@fortawesome/free-solid-svg-icons';

import {
  NxCheckbox,
  NxFieldset,
  NxFormGroup,
  NxTextInput,
  NxList,
  NxFontAwesomeIcon,
  NxButton,
  NxFormRow,
} from '@sonatype/react-shared-components';

import UIStrings from '../../../../../constants/UIStrings';

const {EDITOR} = UIStrings.REPOSITORIES;

export default function DockerForeignLayerConfiguration({parentMachine}) {
  const [currentParent, sendParent] = parentMachine;

  const {dockerProxy} = currentParent.context.data;

  const cacheForeignLayers = dockerProxy?.cacheForeignLayers || false;
  const foreignLayerUrlWhitelist = dockerProxy?.foreignLayerUrlWhitelist || [];

  const [prevWhitelist, setWhitelist] = useState(
    isEmpty(foreignLayerUrlWhitelist) ? ['.*'] : foreignLayerUrlWhitelist
  );

  const updateForeignLayerUrlWhiteList = (value) => {
    sendParent({
      type: 'UPDATE',
      name: 'dockerProxy.foreignLayerUrlWhitelist',
      value,
    });
  };

  const toggleForeignLayer = () => {
    let whiteList = foreignLayerUrlWhitelist;
    const cacheForeignLayers = !dockerProxy.cacheForeignLayers;

    whiteList = cacheForeignLayers ? prevWhitelist : [];

    sendParent({
      type: 'UPDATE',
      name: 'dockerProxy.cacheForeignLayers',
      value: cacheForeignLayers,
    });

    updateForeignLayerUrlWhiteList(whiteList);
  };

  const addUrlPattern = () => {
    const current = dockerProxy.foreignLayerUrlWhitelist || [];
    const foreignLayer = dockerProxy?.foreignLayer || '';
    const existLayer = current.includes(foreignLayer);
    const isValidLayer = !existLayer && !ValidationUtils.isBlank(foreignLayer);
    const whiteList = isValidLayer ? [...current, foreignLayer] : current;

    sendParent({
      type: 'UPDATE',
      name: 'dockerProxy.foreignLayer',
      value: '',
    });

    if (isValidLayer) {
      updateForeignLayerUrlWhiteList(whiteList);
      setWhitelist(whiteList);
    }
  };

  const removeUrlPattern = (index) => {
    if (foreignLayerUrlWhitelist.length > 1) {
      const whiteList = dockerProxy.foreignLayerUrlWhitelist.filter(
          (_, i) => i !== index
      );

      updateForeignLayerUrlWhiteList(whiteList);
      setWhitelist(whiteList);
    }
  };

  const handleEnter = (event) => {
    if (event.key === 'Enter') {
      event.preventDefault();
      addUrlPattern();
    }
  };

  return (
    <>
      <NxFieldset label={EDITOR.FOREIGN_LAYER.CACHING} isRequired>
        <NxCheckbox
          {...FormUtils.checkboxProps(
            'dockerProxy.cacheForeignLayers',
            currentParent
          )}
          onChange={toggleForeignLayer}
        >
          {EDITOR.FOREIGN_LAYER.CHECKBOX}
        </NxCheckbox>
      </NxFieldset>
      {cacheForeignLayers && (
        <NxFormRow className="nx-foreign-layer">
          <>
            <NxFormGroup
              label={EDITOR.FOREIGN_LAYER.URL}
              sublabel={EDITOR.FOREIGN_LAYER.URL_SUBLABEL}
              isRequired
            >
              <NxTextInput
                className="nx-text-input--long"
                {...FormUtils.fieldProps(
                  'dockerProxy.foreignLayer',
                  currentParent
                )}
                onChange={FormUtils.handleUpdate(
                  'dockerProxy.foreignLayer',
                  sendParent
                )}
                onKeyDown={handleEnter}
              />
            </NxFormGroup>
            <NxButton
              variant="icon-only"
              title={EDITOR.FOREIGN_LAYER.ADD}
              onClick={addUrlPattern}
              type="button"
            >
              <NxFontAwesomeIcon icon={faPlusCircle} />
            </NxButton>
          </>
        </NxFormRow>
      )}
      {cacheForeignLayers && foreignLayerUrlWhitelist.length > 0 && (
        <NxList className="nx-foreign-layer">
          {foreignLayerUrlWhitelist.map((layer, index) => (
            <NxList.Item key={layer}>
              <NxList.Text>{layer}</NxList.Text>
              <NxList.Actions>
                <NxButton
                  title={EDITOR.FOREIGN_LAYER.REMOVE}
                  variant="icon-only"
                  onClick={() => removeUrlPattern(index)}
                  className={foreignLayerUrlWhitelist.length <= 1 ? 'disabled' : null}
                  type="button"
                >
                  <NxFontAwesomeIcon icon={faTrashAlt} />
                </NxButton>
              </NxList.Actions>
            </NxList.Item>
          ))}
        </NxList>
      )}
    </>
  );
}
