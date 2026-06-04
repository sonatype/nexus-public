# tree-sitter-stub

This is an empty stub package used to exclude native tree-sitter dependencies from the build.

## Why this exists

`swagger-ui-react` pulls in the following native Node.js packages as transitive dependencies
(via `@swagger-api/apidom-parser-adapter-json` and `@swagger-api/apidom-parser-adapter-yaml-1-2`):

- `tree-sitter` — native Node.js bindings (requires node-gyp)
- `tree-sitter-json` — native JSON grammar (requires node-gyp)
- `web-tree-sitter` — WebAssembly implementation (not native, but unused)
- `@tree-sitter-grammars/tree-sitter-yaml` — native YAML grammar (requires node-gyp)

These packages require `node-gyp` compilation and are **not needed** for browser-based UI
rendering. They add unnecessary download time and build complexity.

## How it works

The root `package.json` uses Yarn `resolutions` with the `portal:` protocol to redirect all
four tree-sitter packages to this stub directory. Any `require('tree-sitter')` (or the others)
resolves to this directory's `index.js`, which exports an empty object.

## Packages replaced

| Package | Resolution in root `package.json` |
|---------|-----------------------------------|
| `tree-sitter` | `portal:./public/common/components/nexus-ui-plugin/stubs/tree-sitter-stub` |
| `tree-sitter-json` | `portal:./public/common/components/nexus-ui-plugin/stubs/tree-sitter-stub` |
| `web-tree-sitter` | `portal:./public/common/components/nexus-ui-plugin/stubs/tree-sitter-stub` |
| `@tree-sitter-grammars/tree-sitter-yaml` | `portal:./public/common/components/nexus-ui-plugin/stubs/tree-sitter-stub` |

## Note on `name` field

The `package.json` `name` field is set to `tree-sitter` (matching the primary package being
stubbed). Since all four packages resolve to this same directory via `portal:`, tooling that
introspects the `name` field (e.g., license scanners) will see `tree-sitter` for all four.
This is intentional — the stub is not a real package and the name is only for identification.
