/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
import {ValidationUtils} from '@sonatype/nexus-ui-plugin';
import UIStrings from '../../../../constants/UIStrings';

export const genericValidators = {
  proxy: (data) => ({
    proxy: {
      remoteUrl:
        ValidationUtils.validateNotBlank(data.proxy.remoteUrl) ||
        ValidationUtils.validateIsUrl(data.proxy.remoteUrl),
      contentMaxAge: validateTimeToLive(data.proxy.contentMaxAge),
      metadataMaxAge: validateTimeToLive(data.proxy.metadataMaxAge)
    },
    negativeCache: {
      timeToLive: validateTimeToLive(data.negativeCache.timeToLive)
    },
    httpClient: {
      authentication: {
        username: validateHttpAuthCreds(data, 'username'),
        password: validateHttpAuthCreds(data, 'password'),
        ntlmHost: validateHttpAuthNtlm(data, 'ntlmHost'),
        ntlmDomain: validateHttpAuthNtlm(data, 'ntlmDomain')
      },
      connection: {
        retries: validateHttpConnectionRetries(data),
        timeout: validateHttpConnectionTimeout(data)
      }
    }
  }),
  group: (data) => ({
    group: {
      memberNames: !data.group.memberNames.length ? UIStrings.ERROR.FIELD_REQUIRED : null
    }
  }),
  hosted: () => ({})
};

const validateTimeToLive = (value) =>
  ValidationUtils.isInRange({value, min: -1, allowDecimals: false}) ||
  ValidationUtils.validateNotBlank(value);

const validateHttpAuthCreds = (data, attrName) => {
  const type = data.httpClient?.authentication?.type;
  if (type) {
    const attrValue = data.httpClient.authentication[attrName];
    return ValidationUtils.validateNotBlank(attrValue);
  }
};

const validateHttpAuthNtlm = (data, attrName) => {
  const type = data.httpClient?.authentication?.type;
  if (type && type === 'ntlm') {
    const attrValue = data.httpClient.authentication[attrName];
    return ValidationUtils.validateNotBlank(attrValue);
  }
};

const validateHttpConnectionRetries = (data) =>
  data.httpClient.connection &&
  ValidationUtils.isInRange({
    value: data.httpClient.connection.retries,
    min: 0,
    max: 10,
    allowDecimals: false
  });

const validateHttpConnectionTimeout = (data) =>
  data.httpClient.connection &&
  ValidationUtils.isInRange({
    value: data.httpClient.connection.timeout,
    min: 0,
    max: 3600,
    allowDecimals: false
  });

export const validateDockerConnectorPort = (data, attrName) => {
  const {httpPort, httpsPort} = data.docker;

  if (httpPort && httpsPort && parseInt(httpPort) === parseInt(httpsPort)) {
    return UIStrings.REPOSITORIES.EDITOR.DOCKER.CONNECTORS.SAME_PORTS_ERROR;
  }

  const port = data.docker[attrName];
  return port === null
    ? null
    : ValidationUtils.validateNotBlank(port) ||
        ValidationUtils.isInRange({
          value: port,
          min: 1,
          max: 65536,
          allowDecimals: false
        });
};

export const validateDockerIndexUrl = (data) => {
  const {indexUrl} = data.dockerProxy;
  return indexUrl === null
    ? null
    : ValidationUtils.validateNotBlank(indexUrl) || ValidationUtils.validateIsUrl(indexUrl);
};

export const validateDockerSubdomain = (data) => {
  const {subdomain} = data.docker;

  if (subdomain === null) {
    return null;
  }

  const notBlank = ValidationUtils.validateNotBlank(subdomain);

  if (notBlank) {
    return notBlank;
  }

  const subdomainRegex = /^[A-Za-z][A-Za-z0-9\-]{0,61}[A-Za-z0-9]$/;
  const isSubdomain = subdomainRegex.test(subdomain);

  if (!isSubdomain) {
    return UIStrings.REPOSITORIES.EDITOR.DOCKER.CONNECTORS.SUBDOMAIN.VALIDATION_ERROR;
  }
};

export const validateNugetQueryCacheItemMaxAge = (data) => {
  const {queryCacheItemMaxAge} = data.nugetProxy;
  return (
    ValidationUtils.isInRange({value: queryCacheItemMaxAge, min: 0, allowDecimals: false}) ||
    ValidationUtils.validateNotBlank(queryCacheItemMaxAge)
  );
};

export const validateWritableMember = (data) => {
  const {memberNames, writableMember} = data.group;
  if (writableMember && !memberNames.includes(writableMember)) {
    const message = UIStrings.REPOSITORIES.EDITOR.WRITABLE.VALIDATION_ERROR(writableMember);
    console.error(message);
    return message;
  }
};
