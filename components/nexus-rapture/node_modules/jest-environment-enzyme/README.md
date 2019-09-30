# jest-environment-enzyme

[![npm version](https://img.shields.io/npm/v/jest-environment-enzyme.svg)](https://www.npmjs.com/package/jest-environment-enzyme)
![License](https://img.shields.io/npm/l/chai-enzyme.svg)

### Overview

Setting up Enzyme with Jest and React is a somewhat complicated process. There are a lot of dependencies, configuring adapters, etc. This module will take lessen that setup and make it more declarative.
This package will also simplify your test files by declaring React, and enzyme wrappers in the global scope. This means that all of your test files don't need to include imports for React or enzyme.

The setup can be as simple as this:

```
yarn add jest-environment-enzyme jest-enzyme enzyme-adapter-react-* --dev
```

> (Where * is your app's adapter that matches your React version)

For example, if you are using react-16, it should look like:

```
yarn add jest-environment-enzyme jest-enzyme enzyme-adapter-react-16 --dev
```

```js
// package.json
"jest": {
  "setupTestFrameworkScriptFile": "jest-enzyme",
  "testEnvironment": "enzyme"
}
```

Additionally, you can specify which enzyme adapter you want to use through the `testEnvironmentOptions.enzymeAdapter`.

Valid options are:

* `react13`
* `react14`
* `react15`
* `react15.4`
* `react16` (default)

```js
// package.json
"jest": {
  "setupTestFrameworkScriptFile": "jest-enzyme",
  "testEnvironment": "enzyme",
  "testEnvironmentOptions": {
    "enzymeAdapter": "react16"
  }
}
```

*Lastly, and _most importantly_*, this library has a hard requirement on `jest-enzyme`.
