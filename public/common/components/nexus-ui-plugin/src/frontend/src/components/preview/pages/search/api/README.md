# API Connection - Agent 3 Instructions

**Agent:** 3  
**Task:** Connect npm/NuGet/Docker search hooks to real API  
**Priority:** 🔴 HIGH

---

## 📍 Location

**Worktree:**
```
/Users/mitchellsjohnson/.cursor/worktrees/nexus-internal__Workspace_/sgi
```

**Files to Modify:**
```
/search/npm/useNpmSearch.ts
/search/nuget/useNuGetSearch.ts
/search/docker/useDockerSearch.ts
```

---

## 🎯 Your Mission

Replace mock API calls with real API calls using the existing `/service/rest/v1/search` endpoint.

**Pattern to Follow:** `/search/core/searchApi.ts` and `/search/results/useGASearch.ts`

---

## 📖 Reference: How Maven Does It

### searchApi.ts Pattern

```typescript
import Axios from 'axios';

async function fetchSearchResults(params) {
  const queryParams = new URLSearchParams();
  
  if (params.format) queryParams.set('format', params.format);
  if (params.query) queryParams.set('q', params.query);
  if (params.repository) queryParams.set('repository', params.repository);
  if (params.continuationToken) queryParams.set('continuationToken', params.continuationToken);
  
  const url = `/service/rest/v1/search?${queryParams.toString()}`;
  const response = await Axios.get(url);
  return response.data;
}
```

### useGASearch.ts Pattern

```typescript
const USE_REAL_API = true;

const search = async (params) => {
  const response = USE_REAL_API
    ? await searchMavenGA(params)    // Real API
    : await mockSearchApi(params);    // Mock
  // ...
};
```

---

## 📋 Tasks

### 1. npm Search (`useNpmSearch.ts`)

**Current:** Uses `mockNpmSearchApi`  
**Change to:** Real API call

**API Endpoint:**
```
GET /service/rest/v1/search?format=npm&q={query}&npm.scope={scope}
```

**npm-specific params:**
- `format=npm`
- `q` - Free text query
- `npm.scope` - @scope (e.g., @angular)
- `repository` - Repository name
- `continuationToken` - Pagination

### 2. NuGet Search (`useNuGetSearch.ts`)

**Current:** Uses `mockNuGetSearchApi`  
**Change to:** Real API call

**API Endpoint:**
```
GET /service/rest/v1/search?format=nuget&q={query}&nuget.id={packageId}
```

**NuGet-specific params:**
- `format=nuget`
- `q` - Free text query
- `nuget.id` - Package ID
- `version` - Specific version
- `repository` - Repository name
- `continuationToken` - Pagination

### 3. Docker Search (`useDockerSearch.ts`)

**Current:** Uses `mockSearchApi`  
**Change to:** Real API call

**API Endpoint:**
```
GET /service/rest/v1/search?format=docker&q={query}&docker.imageName={name}
```

**Docker-specific params:**
- `format=docker`
- `q` - Free text query
- `docker.imageName` - Image name (e.g., nginx)
- `docker.imageTag` - Image tag (e.g., latest)
- `repository` - Repository name
- `continuationToken` - Pagination

---

## 🔧 Implementation Steps

For each hook:

1. **Import Axios:**
```typescript
import Axios from 'axios';
```

2. **Create search function:**
```typescript
async function searchNpm(params: NpmSearchFilters): Promise<NpmSearchResponse> {
  const queryParams = new URLSearchParams();
  queryParams.set('format', 'npm');
  
  if (params.query) queryParams.set('q', params.query);
  if (params.scope) queryParams.set('npm.scope', params.scope);
  if (params.repository) queryParams.set('repository', params.repository);
  if (params.continuationToken) queryParams.set('continuationToken', params.continuationToken);
  
  const url = `/service/rest/v1/search?${queryParams.toString()}`;
  const response = await Axios.get(url);
  
  // Transform raw response to typed response
  return {
    items: response.data.items.map(transformItem),
    totalCount: response.data.items.length,
    continuationToken: response.data.continuationToken,
  };
}
```

3. **Add feature flag:**
```typescript
const USE_REAL_API = true;
```

4. **Update search callback:**
```typescript
const response = USE_REAL_API
  ? await searchNpm(filters)
  : await mockNpmSearchApi(filters);
```

---

## 📊 API Response Format

The `/service/rest/v1/search` API returns:

```json
{
  "items": [
    {
      "id": "unique-id",
      "repository": "npm-proxy-v1",
      "format": "npm",
      "group": "@scope",       // null if no scope
      "name": "package-name",
      "version": "1.0.0",
      "assets": [
        {
          "id": "asset-id",
          "path": "path/to/file",
          "downloadUrl": "http://..."
        }
      ]
    }
  ],
  "continuationToken": "token-for-next-page"
}
```

---

## ⚠️ Rules

1. **ONLY modify the three useSearch hooks**
2. **Keep mock data imports** (use feature flag to switch)
3. **Import Axios** for HTTP requests
4. **Transform API response** to match existing type contracts
5. **Update TASK_PLAN.md** when complete

---

## ✅ Done Criteria

- [ ] `useNpmSearch.ts` calls real API
- [ ] `useNuGetSearch.ts` calls real API
- [ ] `useDockerSearch.ts` calls real API
- [ ] Feature flag allows switching to mock
- [ ] Search results display correctly
- [ ] TASK_PLAN.md updated

---

## 📝 When Complete

Update `/search/TASK_PLAN.md`:
```markdown
#### Agent 3 (API Connection)
- [x] T-P3-3.1: Connect `useNpmSearch.ts` to real API
- [x] T-P3-3.2: Connect `useNuGetSearch.ts` to real API
- [x] T-P3-3.3: Connect `useDockerSearch.ts` to real API
- [x] T-P3-3.4: Test all three with real data
```

Then tell Agent 0 to run integration tests.


