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
import {render, screen} from '@testing-library/react';
import {when} from 'jest-when';
import userEvent from '@testing-library/user-event';
import FormUtils from '../../../interface/FormUtils';
import UseNexusTruststore from './UseNexusTruststore';

import UIStrings from '../../../constants/UIStrings';

const {
  USE_TRUST_STORE: LABELS,
  PERMISSION_ERROR,
  SSL_CERTIFICATE_DETAILS,
} = UIStrings;

describe('UseNexusTruststore', () => {
  const selectors = {
    label: () => screen.getByText(LABELS.LABEL),
    description: () => screen.getByText(LABELS.DESCRIPTION),
    checkbox: () => screen.getByLabelText(LABELS.DESCRIPTION),
    viewCertificate: () =>
      screen.getByText(LABELS.VIEW_CERTIFICATE).closest('button'),
    tooltipMessage: (selector) => selector().closest('span'),
  };

  const permissions = {
    read: 'nexus:ssl-truststore:read',
    update: 'nexus:ssl-truststore:update',
    create: 'nexus:ssl-truststore:create',
  };

  const mock = jest.fn();

  const mockPermission = (permission, value = true) => {
    when(mock).calledWith(permission).mockReturnValueOnce(value);
  };

  const name = 'truststore';
  const host = 'smtp.gmail.com';
  const port = '465';
  const remoteUrl = `https://${host}:${port}`;
  const send = jest.fn();
  const state = {
    context: {
      data: {},
    },
  };
  const notSecureUrl = `http://${host}:${port}`;

  const renderView = (newUrl) => {
    const url = newUrl || remoteUrl;

    return render(
      <UseNexusTruststore
        remoteUrl={url}
        onChange={FormUtils.handleUpdate(name, send)}
        {...FormUtils.checkboxProps(name, state)}
      />
    );
  };

  const rerenderView = (render) => {
    return render(
      <UseNexusTruststore
        remoteUrl={remoteUrl}
        onChange={FormUtils.handleUpdate(name, send)}
        {...FormUtils.checkboxProps(name, state)}
      />
    );
  };

  beforeAll(() => {
    global.NX = {
      Permissions: {check: mock},
    };
  });

  it('renders correctly', () => {
    mockPermission(permissions.read);

    renderView();

    expect(selectors.label()).toBeInTheDocument();
    expect(selectors.description()).toBeInTheDocument();
    expect(selectors.viewCertificate()).toBeInTheDocument();
  });

  it('users can not mark checkbox if user does not have enough permissions', () => {
    const {tooltipMessage, checkbox} = selectors;

    mockPermission(permissions.create, false);

    const {rerender} = renderView();

    expect(checkbox()).toBeDisabled();
    expect(tooltipMessage(checkbox)).toHaveAttribute('title', PERMISSION_ERROR);

    mockPermission(permissions.create);
    mockPermission(permissions.update, false);

    rerenderView(rerender);

    expect(checkbox()).toBeDisabled();
    expect(tooltipMessage(checkbox)).toHaveAttribute('title', PERMISSION_ERROR);

    mockPermission(permissions.update);
    mockPermission(permissions.create);

    rerenderView(rerender);

    expect(selectors.checkbox()).not.toBeDisabled();
  });

  it('users can not view certificate if user does not have enough permissions', () => {
    const {viewCertificate} = selectors;

    mockPermission(permissions.read, false);

    const {rerender} = renderView();

    expect(viewCertificate()).toHaveClass('disabled');
    expect(viewCertificate()).toHaveAttribute('aria-disabled', 'true');
    expect(viewCertificate()).toHaveAttribute('title', PERMISSION_ERROR);

    mockPermission(permissions.read);

    rerenderView(rerender);

    expect(selectors.viewCertificate()).not.toBeDisabled();
  });

  it('users can not add certificate if the url is not secure', () => {
    const {tooltipMessage, viewCertificate, checkbox} = selectors;

    mockPermission(permissions.read);
    mockPermission(permissions.create);
    mockPermission(permissions.update);

    renderView(notSecureUrl);

    expect(checkbox()).toBeDisabled();
    expect(tooltipMessage(checkbox)).toHaveAttribute(
      'title',
      LABELS.NOT_SECURE_URL
    );
    console.log(viewCertificate());
    expect(viewCertificate()).toHaveClass('disabled');
    expect(viewCertificate()).toHaveAttribute('aria-disabled', 'true');
    expect(viewCertificate()).toHaveAttribute('title', PERMISSION_ERROR);
  });

  it('users can view certificate', () => {
    const {viewCertificate} = selectors;

    mockPermission(permissions.read);
    mockPermission(permissions.create);
    mockPermission(permissions.update);

    renderView();

    expect(viewCertificate()).not.toBeDisabled();

    userEvent.click(viewCertificate());

    expect(screen.getByText(SSL_CERTIFICATE_DETAILS.TITLE)).toBeInTheDocument();
  });

  it('users can mark as checked the truststore checkbox', () => {
    const {checkbox} = selectors;

    mockPermission(permissions.read);
    mockPermission(permissions.create);
    mockPermission(permissions.update);

    renderView();

    expect(checkbox()).not.toBeDisabled();

    userEvent.click(checkbox());

    expect(send).toHaveBeenCalled();
  });

  it('shows the corresponding tooltip error message', () => {
    const {tooltipMessage, checkbox} = selectors;

    mockPermission(permissions.create);
    mockPermission(permissions.update);

    const {rerender} = renderView(notSecureUrl);

    expect(tooltipMessage(checkbox)).toHaveAttribute(
      'title',
      LABELS.NOT_SECURE_URL
    );

    rerenderView(rerender);

    expect(tooltipMessage(checkbox)).toHaveAttribute('title', PERMISSION_ERROR);
  });
});
