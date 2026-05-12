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

import React, { useCallback } from 'react';
import { useCurrentStateAndParams, useRouter } from '@uirouter/react';
import { Button, Callout, Spinner, Text } from '@radix-ui/themes';
import { AlertCircle, ArrowLeft } from 'lucide-react';

import { useUploadDefinition } from './hooks/useUploadDefinition';
import { useUploadableRepositories } from './hooks/useUploadableRepositories';
import { UploadForm } from './UploadForm';

import './UploadFormPage.scss';

/**
 * Page shell for the Upload Form.
 *
 * Reads route params, fetches the upload definition, and delegates
 * all form rendering to UploadForm once data is ready.
 */
export function UploadFormContainer(): JSX.Element {
  const { params } = useCurrentStateAndParams();
  const router = useRouter();
  const repositoryName = params?.repoName
    ? decodeURIComponent(params.repoName)
    : '';

  const {
    loading,
    error,
    repositorySettings,
    componentFields,
    componentFieldsByGroup,
    assetFields,
    multipleUpload,
    regexMap,
  } = useUploadDefinition(repositoryName);

  const { repositories: availableRepositories } = useUploadableRepositories();

  const handleBack = useCallback(() => {
    router.stateService.go('preview.browse.upload.list');
  }, [router]);

  const handleRepositoryChange = useCallback(
    (newRepoName: string) => {
      if (newRepoName && newRepoName !== repositoryName) {
        router.stateService.go('preview.browse.upload.form', { repoName: newRepoName });
      }
    },
    [router, repositoryName],
  );

  if (loading) {
    return (
      <div className="upload-form-page">
        <div className="upload-form-page__loading">
          <div className="upload-form-page__loading-spinner">
            <Spinner size="3" />
          </div>
          <Text size="3" weight="medium">Loading upload configuration...</Text>
          <Text size="2" color="gray">Preparing form for {repositoryName}</Text>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="upload-form-page">
        <div className="upload-form-page__error">
          <Button variant="ghost" onClick={handleBack} className="upload-form-page__back">
            <ArrowLeft size={16} />
            Back to Upload
          </Button>
          <Callout.Root color="red" size="2">
            <Callout.Icon>
              <AlertCircle size={16} />
            </Callout.Icon>
            <Callout.Text>{error}</Callout.Text>
          </Callout.Root>
        </div>
      </div>
    );
  }

  return (
    <div className="upload-form-page">
      <UploadForm
        repositoryName={repositoryName}
        repositorySettings={repositorySettings}
        componentFields={componentFields}
        componentFieldsByGroup={componentFieldsByGroup}
        assetFields={assetFields}
        multipleUpload={multipleUpload}
        regexMap={regexMap}
        availableRepositories={availableRepositories}
        onRepositoryChange={handleRepositoryChange}
        onBack={handleBack}
      />
    </div>
  );
}

export default UploadFormContainer;
