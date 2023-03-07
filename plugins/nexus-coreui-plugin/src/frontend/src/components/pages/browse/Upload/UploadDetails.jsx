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
import React, { useRef } from 'react';
import { useMachine } from '@xstate/react';
import {
  NxPageMain,
  NxPageTitle,
  NxH1,
  NxFontAwesomeIcon,
  NxTile,
  NxStatefulForm,
  NxH2,
  NxForm,
  NxFormGroup,
  NxTextInput,
  NxFileUpload,
  NxFormRow
} from '@sonatype/react-shared-components';
import { faUpload} from '@fortawesome/free-solid-svg-icons';
import { map, mapObjIndexed, values } from 'ramda';

import { FormUtils } from '@sonatype/nexus-ui-plugin';

import UploadStrings from '../../../../constants/pages/browse/upload/UploadStrings';

import machine from './UploadDetailsMachine.js';

/**
 * React component that renders a form group from a componentField/assetField structure and the
 * machine state
 */
function Field({ displayName, helpText, name, optional, machineState, send }) {
  return (
    <NxFormGroup label={displayName} sublabel={helpText} isRequired={!optional}>
      <NxTextInput { ...FormUtils.fieldProps(name, machineState) }
                   validatable={!optional}
                   onChange={FormUtils.handleUpdate(name, send)} />
    </NxFormGroup>
  );
}

export default function UploadDetails({ itemId }) {
  const [state, send] = useMachine(machine, {
        context: {
          pristineData: {
            id: decodeURIComponent(itemId)
          }
        },
        devTools: true
      }),
      { repoSettings, componentFieldsByGroup, assetFields, data } = state.context;

  const mkField = field => <Field key={field.name} { ...field } machineState={state} send={send} />;

  return (
    <NxPageMain>
      <NxPageTitle>
        <NxH1 id="upload-details-title">
          <NxFontAwesomeIcon icon={faUpload} />
          <span>{UploadStrings.UPLOAD.DETAILS.TITLE}</span>
        </NxH1>
        <NxPageTitle.Description id="upload-details-description">
          {UploadStrings.UPLOAD.DETAILS.DESCRIPTION}
        </NxPageTitle.Description>
      </NxPageTitle>
      {/* Not using NxTile because it renders a <section> which we don't want in this case since this tile
        * contains multiple top-level sections
        */}
      <div className="nx-tile">
        <NxStatefulForm { ...FormUtils.formProps(state, send) }
                        onCancel={() => send('CANCEL')}
                        submitBtnText={UploadStrings.UPLOAD.DETAILS.SUBMIT_BTN_LABEL}
                        aria-labelledby="upload-details-title"
                        aria-describedby="upload-details-description">
          {() =>
            <NxTile.Content>
              <NxForm.RequiredFieldNotice />
              <NxTile.Subsection aria-labelledby="upload-details-tile-title">
                <NxTile.SubsectionHeader>
                  <NxH2 id="upload-details-tile-title">
                    {UploadStrings.UPLOAD.DETAILS.TILE_TITLE(repoSettings?.name)}
                  </NxH2>
                </NxTile.SubsectionHeader>
                <NxFormGroup label={UploadStrings.UPLOAD.DETAILS.FILE_UPLOAD_LABEL} isRequired>
                  <NxFileUpload isRequired
                                { ...FormUtils.fileUploadProps('asset0._', state) }
                                onChange={FormUtils.handleUpdate('asset0._', send)} />
                </NxFormGroup>
                <NxFormRow>{ map(mkField, assetFields) }</NxFormRow>
              </NxTile.Subsection>
              { values(mapObjIndexed((fields, group) => {
                  const sectionId = `upload-details-group-${group.toLowerCase().replace(' ', '-')}`;
                  return (
                    <NxTile.Subsection aria-labelledby={sectionId} key={group}>
                      <NxTile.SubsectionHeader>
                        <NxH2 id={sectionId}>{group}</NxH2>
                      </NxTile.SubsectionHeader>
                      { map(mkField, fields) }
                    </NxTile.Subsection>
                  );
                }, componentFieldsByGroup))
              }
            </NxTile.Content>
          }
        </NxStatefulForm>
      </div>
    </NxPageMain>
  );
}
