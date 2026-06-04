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

import { useState, useEffect, useCallback, useMemo } from 'react';
import { restClient, ENDPOINTS, parseApiError, isNotFoundError } from '../../../../../interface/api';

import type {
  UploadDefinitionExtended,
  RepositorySettings,
  UploadComponentField,
  UploadFieldDefinition,
  UseUploadDefinitionResult,
} from '../upload.types';

// REST API endpoints
const UPLOAD_SPECS_ENDPOINT = '/service/rest/v1/formats/upload-specs';

/**
 * Groups component fields by their group property.
 */
function groupFieldsByGroup(
  fields: UploadComponentField[]
): Record<string, UploadComponentField[]> {
  return fields.reduce((acc, field) => {
    const group = field.group || 'Other';
    if (!acc[group]) {
      acc[group] = [];
    }
    acc[group].push(field);
    return acc;
  }, {} as Record<string, UploadComponentField[]>);
}

/**
 * Hook to fetch upload definition for a specific repository.
 *
 * Fetches both the repository settings and the upload definition for
 * the repository's format, providing all the field metadata needed
 * to render the upload form.
 *
 * @param repositoryName - The name of the repository to upload to
 * @returns Upload definition state and actions
 */
export function useUploadDefinition(repositoryName: string): UseUploadDefinitionResult {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [uploadDefinition, setUploadDefinition] = useState<UploadDefinitionExtended | null>(null);
  const [repositorySettings, setRepositorySettings] = useState<RepositorySettings | null>(null);

  /**
   * Fetch repository settings and upload definition.
   * Uses REST endpoints instead of ExtDirect.
   */
  const fetchData = useCallback(async () => {
    if (!repositoryName) {
      setError('Repository name is required');
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const fetchUploadDefinitions = async (): Promise<{
        definitions: UploadDefinitionExtended[];
        is404: boolean;
      }> => {
        try {
          const definitions = await restClient.get<UploadDefinitionExtended[]>(
            UPLOAD_SPECS_ENDPOINT
          );
          return { definitions: definitions || [], is404: false };
        } catch (err) {
          const apiError = parseApiError(err);
          if (isNotFoundError(apiError)) {
            return { definitions: [], is404: true };
          }
          throw err;
        }
      };

      const fetchRepositories = async (): Promise<RepositorySettings[]> => {
        try {
          return await restClient.get<RepositorySettings[]>(ENDPOINTS.REPOSITORIES_DETAILS);
        } catch (err) {
          const apiError = parseApiError(err);
          if (isNotFoundError(apiError)) {
            return await restClient.get<RepositorySettings[]>(ENDPOINTS.REPOSITORIES);
          }
          throw err;
        }
      };

      const [repositories, { definitions: uploadDefinitions, is404: uploadSpecsNotFound }] =
        await Promise.all([
          fetchRepositories(),
          fetchUploadDefinitions(),
        ]);

      // Find the repository by name
      const repoSettings = (repositories || []).find(
        (repo: RepositorySettings) => repo.name === repositoryName
      );

      if (!repoSettings) {
        throw new Error(`Repository "${repositoryName}" not found`);
      }

      // Find the upload definition for this repository's format
      const definition = (uploadDefinitions || []).find(
        (def: UploadDefinitionExtended) => def.format === repoSettings.format
      );

      if (!definition) {
        if (uploadSpecsNotFound) {
          // Endpoint does not exist (e.g. cloud deployment) - fail silently
          setRepositorySettings(repoSettings);
          setUploadDefinition(null);
          setError(null);
        } else {
          throw new Error(
            `No upload definition found for format "${repoSettings.format}"`
          );
        }
        setLoading(false);
        return;
      }

      // Verify the repository supports UI upload
      // Note: REST API /v1/formats/upload-specs does not return uiUpload field,
      // so we assume formats in upload-specs support UI upload unless explicitly false
      if (definition.uiUpload === false) {
        throw new Error(`Repository "${repositoryName}" does not support upload through the web UI`);
      }

      // Verify it's a hosted repository
      if (repoSettings.type !== 'hosted') {
        throw new Error(`Repository "${repositoryName}" is not a hosted repository`);
      }

      setRepositorySettings(repoSettings);
      setUploadDefinition(definition);
    } catch (err) {
      const apiError = parseApiError(err);
      setError(apiError.message);
      setUploadDefinition(null);
      setRepositorySettings(null);
    } finally {
      setLoading(false);
    }
  }, [repositoryName]);

  // Fetch data on mount or when repository name changes
  useEffect(() => {
    fetchData();
  }, [fetchData]);

  /**
   * Computed component fields with group support.
   */
  const componentFields = useMemo((): UploadComponentField[] => {
    return (uploadDefinition?.componentFields || []) as UploadComponentField[];
  }, [uploadDefinition]);

  /**
   * Component fields grouped by their group property.
   */
  const componentFieldsByGroup = useMemo(() => {
    return groupFieldsByGroup(componentFields);
  }, [componentFields]);

  /**
   * Asset fields for file uploads.
   */
  const assetFields = useMemo((): UploadFieldDefinition[] => {
    return uploadDefinition?.assetFields || [];
  }, [uploadDefinition]);

  /**
   * Whether multiple file uploads are allowed.
   */
  const multipleUpload = useMemo(() => {
    return uploadDefinition?.multipleUpload ?? false;
  }, [uploadDefinition]);

  /**
   * Regex map for automatic field extraction from filename.
   */
  const regexMap = useMemo(() => {
    return uploadDefinition?.regexMap || null;
  }, [uploadDefinition]);

  return {
    loading,
    error,
    uploadDefinition,
    repositorySettings,
    componentFields,
    componentFieldsByGroup,
    assetFields,
    multipleUpload,
    regexMap,
    refetch: fetchData,
  };
}

export default useUploadDefinition;

