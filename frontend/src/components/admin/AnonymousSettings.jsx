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
import React, {useState, useEffect} from 'react';

import Axios from 'axios';
import Button from '../widgets/Button';
import ContentBody from '../ContentBody';
import Checkbox from '../widgets/Checkbox';
import ExtJS from '../../interface/ExtJS';
import SettingsSection from './SettingsSection';
import Select from '../widgets/Select/Select';
import Textfield from '../widgets/Textfield';
import UIStrings from '../../constants/UIStrings';

export default function AnonymousSettings() {
  const [anonymousSettings, setAnonymousSettings] = useState({
    enabled: false,
    realmName: '',
    userId: ''
  });

  const [pristineSettings, setPristineSettings] = useState({});

  const [realmTypes, setRealmTypes] = useState([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    if (!isLoading) {
      return;
    }

    const cancelTokenSource = Axios.CancelToken.source();
    const config = {
      cancelToken: cancelTokenSource.token
    };

    Axios.all([
      Axios.get('/service/rest/internal/ui/realms/types', config),
      Axios.get('/service/rest/internal/ui/anonymous-settings', config)
    ])
        .then(Axios.spread((realmTypes, settings) => {
          setIsLoading(false);
          setRealmTypes(realmTypes.data);
          setAnonymousSettings({...settings.data});
          setPristineSettings({...settings.data});
        }))
        .catch((error) => {
          if (!Axios.isCancel(error)) {
            ExtJS.showErrorMessage(UIStrings.ANONYMOUS_SETTINGS.MESSAGES.LOAD_ERROR);
            console.error(error);
          }
        });

    return function cleanup() {
      cancelTokenSource.cancel();
    }
  });

  const handleInputChange = (event) => {
    const target = event.target;
    const value = target.type === 'checkbox' ? target.checked : target.value;
    const name = target.name;
    setAnonymousSettings({
      ...anonymousSettings,
      [name]: value
    });
  };

  const handleDiscard = () => setAnonymousSettings({...pristineSettings});

  const handleSave = () => {
    Axios.put('/service/rest/internal/ui/anonymous-settings', anonymousSettings)
        .then(() => {
          setPristineSettings({...anonymousSettings});
          ExtJS.showSuccessMessage(UIStrings.ANONYMOUS_SETTINGS.MESSAGES.SAVE_SUCCESS);
        })
        .catch((error) => {
          ExtJS.showErrorMessage(UIStrings.ANONYMOUS_SETTINGS.MESSAGES.SAVE_ERROR);
          console.error(error);
        });
  };

  const isPristine = Object.keys(pristineSettings).every(
      (key) => pristineSettings[key] === anonymousSettings[key]
  );
  const {enabled, userId, realmName} = anonymousSettings;
  const primaryButtonStyle = {marginRight: '10px'};
  const widthOverride = {width: '250px'};

  return <ContentBody>
    <SettingsSection isLoading={isLoading}>
      <SettingsSection.FieldWrapper
          labelText={UIStrings.ANONYMOUS_SETTINGS.ENABLED_CHECKBOX_LABEL}
      >
        <Checkbox
            name='enabled'
            isChecked={enabled}
            onChange={handleInputChange}
            labelText={UIStrings.ANONYMOUS_SETTINGS.ENABLED_CHECKBOX_DESCRIPTION}
        />
      </SettingsSection.FieldWrapper>
      <SettingsSection.FieldWrapper
          labelText={UIStrings.ANONYMOUS_SETTINGS.USERNAME_TEXTFIELD_LABEL}
      >
        <Textfield
            name='userId'
            value={userId}
            onChange={handleInputChange}
            isRequired={!isLoading}
            style={widthOverride}
        />
      </SettingsSection.FieldWrapper>
      <SettingsSection.FieldWrapper
          labelText={UIStrings.ANONYMOUS_SETTINGS.REALM_SELECT_LABEL}
      >
        <Select
            name='realmName'
            value={realmName}
            onChange={handleInputChange}
            style={widthOverride}
        >
          {
            realmTypes.map((realmType) =>
                <option key={realmType.id} value={realmType.id}>{realmType.name}</option>
            )
          }
        </Select>
      </SettingsSection.FieldWrapper>
      <SettingsSection.Footer>
        <Button
            isPrimary={true}
            style={primaryButtonStyle}
            disabled={isPristine}
            onClick={handleSave}
        >
          {UIStrings.SETTINGS.SAVE_BUTTON_LABEL}
        </Button>
        <Button
            disabled={isPristine}
            onClick={handleDiscard}
        >
          {UIStrings.SETTINGS.DISCARD_BUTTON_LABEL}
        </Button>
      </SettingsSection.Footer>
    </SettingsSection>
  </ContentBody>;
}
