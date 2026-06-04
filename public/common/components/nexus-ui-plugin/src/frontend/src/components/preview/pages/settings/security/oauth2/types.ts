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

/**
 * OAuth2 Configuration data
 */
export interface OAuth2Config {
  clientId: string;
  clientSecret: string;
  idpAuthorizationUrl: string;
  idpLogoutUrl: string;
  idpTokenUrl: string;
  idpJwksUrl: string;
  idpJwsAlgorithm: string;
  idpJwks?: string;
  usernameClaim: string;
  firstNameClaim: string;
  lastNameClaim: string;
  emailClaim: string;
  groupsClaim: string;
  exactMatchClaims?: string;
  authorizationCustomParams?: string;
  tokenRequestCustomParams?: string;
}

/**
 * Default OAuth2 configuration values
 */
export const DEFAULT_OAUTH2_CONFIG: OAuth2Config = {
  clientId: '',
  clientSecret: '',
  idpAuthorizationUrl: '',
  idpLogoutUrl: '',
  idpTokenUrl: '',
  idpJwksUrl: '',
  idpJwsAlgorithm: '',
  idpJwks: '',
  usernameClaim: 'sub',
  firstNameClaim: '',
  lastNameClaim: '',
  emailClaim: '',
  groupsClaim: '',
  exactMatchClaims: '',
  authorizationCustomParams: '',
  tokenRequestCustomParams: '',
};

/**
 * Page props
 */
export interface OAuth2PageProps {
  className?: string;
}


