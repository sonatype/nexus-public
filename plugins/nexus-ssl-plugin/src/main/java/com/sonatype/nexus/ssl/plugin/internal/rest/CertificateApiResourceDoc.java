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
package com.sonatype.nexus.ssl.plugin.internal.rest;

import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.Response;

import org.sonatype.nexus.ssl.ApiCertificate;
import com.sonatype.nexus.ssl.plugin.validator.HostnameOrIpAddress;
import com.sonatype.nexus.ssl.plugin.validator.PemCertificate;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;

/**
 * @since 3.19
 */
@Api(value = "Security: certificates")
public interface CertificateApiResourceDoc
{
  @ApiOperation("Helper method to retrieve certificate details from a remote system.")
  @ApiResponses(value = {
      @ApiResponse(code = SC_FORBIDDEN, message = "Insufficient permissions to retrieve remote certificate."),
      @ApiResponse(code = SC_BAD_REQUEST,
          message = "A certificate could not be retrieved, see the message for details.")})
  ApiCertificate retrieveCertificate(
      @ApiParam(value = "The remote system's host name") @NotNull @NotEmpty @HostnameOrIpAddress String host,
      @ApiParam(value = "The port on the remote system to connect to") Integer port,
      @ApiParam(value = "An optional hint of the protocol to try for the connection") String protocolHint);

  @ApiOperation("Retrieve a list of certificates added to the trust store.")
  @ApiResponses(value = {
      @ApiResponse(code = SC_FORBIDDEN, message = "Insufficient permissions to list certificates in the trust store.")})
  List<ApiCertificate> getTrustStoreCertificates();

  @ApiOperation("Add a certificate to the trust store.")
  @ApiResponses(value = {
      @ApiResponse(code = SC_CREATED, message = "The certificate was successfully added.",
          response = ApiCertificate.class),
      @ApiResponse(code = SC_CONFLICT,
          message = "The certificate already exists in the system."),
      @ApiResponse(code = SC_FORBIDDEN, message = "Insufficient permissions to add certificate to the trust store.")})
  Response addCertificate(
      @ApiParam("The certificate to add encoded in PEM format") @NotBlank @PemCertificate String pem);

  @ApiOperation("Remove a certificate in the trust store.")
  @ApiResponses(value = {
      @ApiResponse(code = SC_FORBIDDEN,
          message = "Insufficient permissions to remove certificate from the trust store")})
  void removeCertificate(@ApiParam(value = "The id of the certificate that should be removed.") String id);
}
