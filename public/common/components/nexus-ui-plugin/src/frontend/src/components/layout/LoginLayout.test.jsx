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

import React from "react";
import { render, screen } from "@testing-library/react";
import { ExtJS } from "@sonatype/nexus-ui-plugin";
import LoginLayout from "./LoginLayout";

jest.mock("@sonatype/nexus-ui-plugin", () => ({
  ExtJS: {
    useState: jest.fn(),
    state: jest.fn(),
  },
}));

const mockState = {
  getEdition: jest.fn(),
  getValue: jest.fn(),
};

const defaultLogoConfig = {
  proLight: "/logos/pro-light.svg",
  ceLight: "/logos/ce-light.svg",
  coreLight: "/logos/core-light.svg",
};

describe("LoginLayout", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    ExtJS.state.mockReturnValue(mockState);
    mockState.getEdition.mockReturnValue("PRO");
    // By default, no context path
    mockState.getValue.mockReturnValue("");
    ExtJS.useState.mockImplementation((fn) => fn());
  });

  it("render component for Professional", () => {
    const { container } = render(
        <LoginLayout logoConfig={defaultLogoConfig}>
          <div>Test Content</div>
        </LoginLayout>
    );

    // verify structure rendered (light-only theme)
    expect(container.querySelector(".nxrm-login-header")).toBeInTheDocument();
    expect(container.querySelector(".nxrm-login-main")).toBeInTheDocument();
    expect(screen.getByText("Test Content")).toBeInTheDocument();

    // verify home link
    expect(container.querySelector(".nxrm-login-header a[href]")).toHaveAttribute("href", "/#browse/welcome");

    // verify logo image is rendered (light-only, single image)
    const image = container.querySelector('.nxrm-login-header img');
    expect(image).toBeInTheDocument();
    expect(image).toHaveAttribute("src", "/logos/pro-light.svg");
    expect(image).toHaveAttribute("alt", "Sonatype Nexus Repository Professional");
    expect(mockState.getEdition).toHaveBeenCalled();
  });

  describe("logo selection", () => {
    it("handles missing logo gracefully when PRO style is not available", () => {
      const { container } = render(
        <LoginLayout logoConfig={{}}>
          <div>Test Content</div>
        </LoginLayout>
      );

      expect(screen.getByText("Test Content")).toBeInTheDocument();
      // No logo shown when proLight is not available
      const image = container.querySelector('.nxrm-login-header__logo');
      expect(image).toBeInTheDocument();
      expect(mockState.getEdition).toHaveBeenCalled();
    });

    describe("logo selection - Community edition", () => {
      it("uses ceLight logo when available", () => {
        mockState.getEdition.mockReturnValue("COMMUNITY");

        const { container } = render(
            <LoginLayout logoConfig={defaultLogoConfig}>
              <div>Test Content</div>
            </LoginLayout>
        );

        expect(screen.getByText("Test Content")).toBeInTheDocument();
        const image = container.querySelector('.nxrm-login-header img');
        expect(image).toHaveAttribute("src", "/logos/ce-light.svg");
        expect(image).toHaveAttribute("alt", "Sonatype Nexus Repository Community");
        expect(mockState.getEdition).toHaveBeenCalled();
      });

      it("falls back to proLight when Community style is not available", () => {
        mockState.getEdition.mockReturnValue("COMMUNITY");

        const logoConfigWithoutCE = {
          proLight: "/logos/pro-light.svg",
        };

        const { container } = render(
            <LoginLayout logoConfig={logoConfigWithoutCE}>
              <div>Test Content</div>
            </LoginLayout>
        );

        expect(screen.getByText("Test Content")).toBeInTheDocument();
        const image = container.querySelector('.nxrm-login-header img');
        expect(image).toHaveAttribute("src", "/logos/pro-light.svg");
        expect(mockState.getEdition).toHaveBeenCalled();
      });
    });

    describe("logo selection - Core edition", () => {
      beforeEach(() => {
        mockState.getEdition.mockReturnValue("distinctToCommunityAndPro");
      });

      it("uses coreLight logo when available", () => {
        const { container } = render(
            <LoginLayout logoConfig={defaultLogoConfig}>
              <div>Test Content</div>
            </LoginLayout>
        );

        expect(screen.getByText("Test Content")).toBeInTheDocument();
        const image = container.querySelector('.nxrm-login-header img');
        expect(image).toHaveAttribute("src", "/logos/core-light.svg");
        expect(image).toHaveAttribute("alt", "Sonatype Nexus Repository Core");
        expect(mockState.getEdition).toHaveBeenCalled();
      });

      it("falls back to proLight when Core style is not available", () => {
        const logoConfigWithoutCore = {
          proLight: "/logos/pro-light.svg",
        };

        const { container } = render(
            <LoginLayout logoConfig={logoConfigWithoutCore}>
              <div>Test Content</div>
            </LoginLayout>
        );

        expect(screen.getByText("Test Content")).toBeInTheDocument();
        const image = container.querySelector('.nxrm-login-header img');
        expect(image).toHaveAttribute("src", "/logos/pro-light.svg");
        expect(mockState.getEdition).toHaveBeenCalled();
      });
    });
  });

  it("uses context path from ExtJS state when provided", () => {
    mockState.getValue.mockReturnValue("/nexus-context");

    const { container } = render(
        <LoginLayout logoConfig={defaultLogoConfig}>
          <div>Test Content</div>
        </LoginLayout>
    );

    expect(screen.getByText("Test Content")).toBeInTheDocument();
    const homeLink = container.querySelector(".nxrm-login-header a[href]");
    expect(homeLink).toHaveAttribute("href", "/nexus-context/#browse/welcome");
  });

  describe("edge cases", () => {
    it("handles missing logoConfig gracefully", () => {
      const { container } = render(
        <LoginLayout logoConfig={undefined}>
          <div>Test Content</div>
        </LoginLayout>
      );

      expect(screen.getByText("Test Content")).toBeInTheDocument();
      // Image element exists but has no src
      const image = container.querySelector('.nxrm-login-header__logo');
      expect(image).toBeInTheDocument();
      expect(mockState.getEdition).toHaveBeenCalled();
    });

    it("handles empty logoConfig gracefully", () => {
      const { container } = render(
        <LoginLayout logoConfig={{}}>
          <div>Test Content</div>
        </LoginLayout>
      );

      expect(screen.getByText("Test Content")).toBeInTheDocument();
      const image = container.querySelector('.nxrm-login-header__logo');
      expect(image).toBeInTheDocument();
      expect(mockState.getEdition).toHaveBeenCalled();
    });

    it("handles null logoConfig gracefully", () => {
      const { container } = render(
        <LoginLayout logoConfig={null}>
          <div>Test Content</div>
        </LoginLayout>
      );

      expect(screen.getByText("Test Content")).toBeInTheDocument();
      const image = container.querySelector('.nxrm-login-header__logo');
      expect(image).toBeInTheDocument();
      expect(mockState.getEdition).toHaveBeenCalled();
    });
  });
});
