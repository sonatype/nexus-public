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
  NxButton,
  NxButtonBar,
  NxCheckbox,
  NxFileUpload,
  NxFontAwesomeIcon,
  NxForm,
  NxFormGroup,
  NxFormRow,
  NxH1,
  NxH2,
  NxInfoAlert,
  NxPageMain,
  NxPageTitle,
  NxStatefulForm,
  NxTextInput,
  NxTile
} from '@sonatype/react-shared-components';
import { faPlus, faUpload, faTrashAlt } from '@fortawesome/free-solid-svg-icons';
import { filter, isEmpty, keys, map, match, mapObjIndexed, path, split, test, values } from 'ramda';

import { FormUtils } from '@sonatype/nexus-ui-plugin';

import UploadStrings from '../../../../constants/pages/browse/upload/UploadStrings';

import machine from './UploadDetailsMachine.js';
import { COMPOUND_FIELD_PARENT_NAME, ASSET_NUM_MATCHER, MAVEN_FORMAT, MAVEN_COMPONENT_COORDS_GROUP }
  from './UploadDetailsUtils.js';

import './UploadDetails.scss';

/**
 * React component that renders a form group from a componentField/assetField structure and the
 * machine state
 */
function Field({ displayName, helpText, name, type, optional, machineState, send, validatable }) {
  const disabled = !!path(split('.', name), machineState.context.disabledFields);

  return type === 'BOOLEAN' ?
      <NxCheckbox { ...FormUtils.checkboxProps(name, machineState) }
                  disabled={disabled}
                  onChange={FormUtils.handleUpdate(name, send)}>
        {displayName}
      </NxCheckbox> :
      <NxFormGroup label={displayName} sublabel={helpText} isRequired={!optional}>
        <NxTextInput { ...FormUtils.fieldProps(name, machineState) }
                      disabled={disabled}
                     onChange={FormUtils.handleUpdate(name, send)}
                     validatable={!optional || validatable} />
      </NxFormGroup>;
}

export default function UploadDetails({ itemId }) {
  const [state, send] = useMachine(machine, {
        context: {
          repoId: decodeURIComponent(itemId)
        },
        devTools: true
      }),
      { multipleUpload, repoSettings, componentFieldsByGroup, assetFields, data, hasPomExtension } = state.context,
      assetStateKeys = filter(test(ASSET_NUM_MATCHER), keys(data));

  const mkField = field => <Field key={field.name} { ...field } machineState={state} send={send} />,
      mkAssetField = prefix => field => mkField({
        ...field,
        name: `${prefix}.${field.name}`,
        validatable: true
      });

  function deleteAssetGroup(assetKey) {
    send({ type: 'DELETE_ASSET', assetKey });
  }

  return (
    <NxPageMain id="nxrm-upload-details">
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
                        onCancel={() => send({type: 'CANCEL'})}
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
                { map(assetKey => {
                    const fileUploadName = `${assetKey}.${COMPOUND_FIELD_PARENT_NAME}`,
                        assetNum = parseInt(match(ASSET_NUM_MATCHER, assetKey)[1]),
                        groupName = UploadStrings.UPLOAD.DETAILS.ASSET_GROUP_NAME(assetNum + 1);

                    return (
                      <fieldset aria-label={groupName} className="nxrm-upload-details__asset-group" key={assetKey}>
                        <NxFormGroup label={UploadStrings.UPLOAD.DETAILS.FILE_UPLOAD_LABEL} isRequired>
                          <NxFileUpload isRequired
                                        { ...FormUtils.fileUploadProps(fileUploadName, state) }
                                        onChange={FormUtils.handleUpdate(fileUploadName, send)} />
                        </NxFormGroup>
                        { !(!assetFields || isEmpty(assetFields)) &&
                          <NxFormRow>
                            { map(mkAssetField(assetKey), assetFields) }
                            { assetStateKeys.length > 1 &&
                              <NxButtonBar>
                                <NxButton type="button" variant="tertiary" onClick={() => deleteAssetGroup(assetKey)}>
                                  <NxFontAwesomeIcon icon={faTrashAlt} />
                                  <span>Delete</span>
                                </NxButton>
                              </NxButtonBar>
                            }
                          </NxFormRow>
                        }
                      </fieldset>
                    );
                  }, assetStateKeys)
                }
                { multipleUpload &&
                  <NxFormRow className="nxrm-upload-details__add-asset-row">
                    <NxButtonBar>
                      <NxButton type="button" variant="tertiary" onClick={() => send({type: 'ADD_ASSET'})}>
                        <NxFontAwesomeIcon icon={faPlus} />
                        <span>{UploadStrings.UPLOAD.DETAILS.ADD_ANOTHER_ASSET_BTN_LABEL}</span>
                      </NxButton>
                    </NxButtonBar>
                  </NxFormRow>
                }
              </NxTile.Subsection>
              { values(mapObjIndexed((fields, group) => {
                  const sectionId = `upload-details-group-${group.toLowerCase().replace(' ', '-')}`,
                      isMavenComponentCoords =
                          repoSettings.format === MAVEN_FORMAT && group === MAVEN_COMPONENT_COORDS_GROUP,
                      disabled = isMavenComponentCoords && hasPomExtension;

                  return (
                    <NxTile.Subsection aria-labelledby={sectionId} key={group}>
                      <NxTile.SubsectionHeader>
                        <NxH2 id={sectionId}>{group}</NxH2>
                      </NxTile.SubsectionHeader>
                      { disabled &&
                        <NxInfoAlert>{UploadStrings.UPLOAD.DETAILS.COORDINATES_EXTRACTED_FROM_POM_MESSAGE}</NxInfoAlert>
                      }
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
