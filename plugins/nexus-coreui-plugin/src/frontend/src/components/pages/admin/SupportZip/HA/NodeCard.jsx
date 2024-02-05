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
import {useActor} from '@xstate/react';
import classNames from 'classnames';
import {
  NxCard,
  NxFontAwesomeIcon,
  NxH3,
  NxP,
  NxTextLink,
  NxTooltip,
  NxLoadingSpinner,
  NxButton,
} from '@sonatype/react-shared-components';

import {ExtJS, DateUtils} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../../constants/UIStrings';
import {
  faSync,
  faCheckCircle,
  faTimesCircle,
} from '@fortawesome/free-solid-svg-icons';

import './SupportZipHa.scss';

const {SUPPORT_ZIP: LABELS} = UIStrings;

export default function NodeCard({actor, createZip, isBlobStoreConfigured}) {
  const [state] = useActor(actor);

  const {data} = state.context;
  const isNodeActive = data.status !== 'NODE_UNAVAILABLE';
  const zipNotCreated = data.status === 'NOT_CREATED';
  const zipCreating = data.status === 'CREATING';
  const nodeError = data.status === 'FAILED';

  const zipLastUpdatedHtml = () => {
    if (!isNodeActive) {
      return (
        <NxP className="nxrm-p-zip-updated">
          {LABELS.NODE_UNAVAILABLE_CANNOT_CREATE}
        </NxP>
      );
    }

    if (!isBlobStoreConfigured) {
      return (
        <NxP className="nxrm-p-zip-updated">
          {LABELS.NO_BLOB_STORE_CONFIGURED}
        </NxP>
      );
    }

    if (zipCreating) {
      return <NxLoadingSpinner>{LABELS.CREATING_ZIP}</NxLoadingSpinner>;
    }

    if (zipNotCreated) {
      return <NxP className="nxrm-p-zip-updated">{LABELS.NO_ZIP_CREATED}</NxP>;
    }

    return (
      <NxP className="nxrm-p-zip-updated">
        <NxTextLink href={ExtJS.urlOf(`service/rest/wonderland/download/${data.blobRef}`)} download>
          {LABELS.DOWNLOAD_ZIP}
          <br />
          {DateUtils.prettyDateTime(new Date(data.lastUpdated))}
        </NxTextLink>
      </NxP>
    );
  };

  const generateButtonDisabled =
    !isNodeActive || !isBlobStoreConfigured || zipCreating;

  const handleGenerate = () => {
    if (!generateButtonDisabled) {
      createZip();
    }
  };

  return (
    <NxCard>
      <NxTooltip
        title={isNodeActive ? LABELS.NODE_IS_ACTIVE : LABELS.NODE_IS_INACTIVE}
        placement="top-middle"
      >
        <NxCard.Header>
          <NxH3>
            {isNodeActive ? (
              <NxFontAwesomeIcon
                icon={faCheckCircle}
                className="nxrm-node-green-checkmark"
              />
            ) : (
              <NxFontAwesomeIcon icon={faTimesCircle} />
            )}{' '}
            {data.hostname}
          </NxH3>
        </NxCard.Header>
      </NxTooltip>
      <NxCard.Content>
        <NxCard.Text>{zipLastUpdatedHtml()}</NxCard.Text>
      </NxCard.Content>
      <NxCard.Footer>
        {nodeError && (
          <NxP className="error-message">{LABELS.GENERATE_ERROR}</NxP>
        )}
        <NxButton
          variant={nodeError ? 'error' : 'secondary'}
          onClick={handleGenerate}
          className={classNames({
            disabled: generateButtonDisabled,
          })}
        >
          {nodeError ? (
            <>
              <NxFontAwesomeIcon icon={faSync} />
              <span>{LABELS.RETRY}</span>
            </>
          ) : (
            LABELS.GENERATE_NEW_ZIP_FILE
          )}
        </NxButton>
      </NxCard.Footer>
    </NxCard>
  );
}
