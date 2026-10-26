# NEXORA — MASTER DEPLOYMENT RECOVERY & ROOT CAUSE REPORT

**Date:** October 26, 2026  
**Repository:** `https://github.com/jeetjajal/nexora.git`  
**Production Branch:** `main`  
**Production Domain:** `https://nexora-mu-eosin.vercel.app`  
**Verified HTTP Status:** `HTTP/1.1 200 OK` (Live and verified!)  

---

## 1. Root Cause Analysis

### A. Why did Vercel show 404 NOT_FOUND?
1. **The Erroneous `vercel.json` (`outputDirectory: "frontend/.next"`):**
   - In commit `8e10c2e`, a root-level `vercel.json` specified `outputDirectory: "frontend/.next"`.
   - Next.js 14 App Router does NOT output static HTML at the root of `.next`. When `outputDirectory` is overridden in Vercel, Vercel disables its native Next.js Serverless Function / Edge builder and switches to a **Static Deployment mode**, looking for static `index.html` at the root of `frontend/.next`. Not finding it, Vercel served **`404 NOT_FOUND`**.
2. **Old Stale Prebuilt Deployment:**
   - The deployment that had `Ready` status was an old 3-second deployment with `Framework: Other` and `Using prebuilt build output from .vercel/output` containing legacy static HTML files.
   - Because subsequent commits (`4b9e3e8` and `44ee107`) failed or were not deployed under the new settings, Vercel never replaced the old production deployment, leaving the domain on the stale 404 state.

### B. What was the Red X / Check Failure on GitHub Commit `44ee107`?
- **Verified via GitHub Checks API (`gh api repos/jeetjajal/nexora/commits/44ee107/status`):**
  - Context: `Vercel`
  - State: `failure`
  - Target URL: `https://vercel.com/jajaljeet2005-9477/nexora/JCSkvabyx4cjpemgCS4khUkaKxMG`
  - Description: `Deployment has failed — run this Vercel CLI command: npx vercel inspect dpl_JCSkvabyx4cjpemgCS4khUkaKxMG --logs`
- **Why did it fail?**
  - When `44ee107` was pushed, `vercel.json` was deleted. At that moment (11:55:02 UTC), Vercel was still attempting to build from repository root without a build configuration for root.
  - Vercel's automated deployment failed at that moment, producing the Red X.
  - **Crucial Rule:** Changing Vercel settings in the dashboard (Root Directory: `frontend`, Framework Preset: `Next.js`) **does not retroactively re-trigger a deployment** for past commits. A fresh push to `main` was required.

### C. `.vercel/output` Origin
- **Audit Findings:**
  - Neither `.vercel` nor `.vercel/output` is tracked in Git (verified via `git log --all --full-history -- "**.vercel*"`).
  - Both root `.gitignore` and `frontend/.gitignore` ignore `.vercel`.
  - The message `Using prebuilt build output from .vercel/output` was from an earlier CLI deployment where prebuilt static assets were deployed directly.

---

## 2. Changes Applied

1. **Root Directory Optimization:**
   - Added npm workspace definition in root `package.json` (`"workspaces": ["frontend"]`).
   - Removed conflicting root `vercel.json`.
2. **Frontend Protection:**
   - Created `frontend/.gitignore` preventing any `.vercel` or `.next` artifacts from being tracked.
   - Verified 100% case-sensitivity of all TypeScript/React imports across `frontend/src`.
3. **CI Automation:**
   - Added `.github/workflows/ci.yml` for automated GitHub Actions verification on push to `main` and PRs (Next.js production build + Maven test compilation).
4. **Environment Templates:**
   - Created `frontend/.env.example` and `.env.example`.

---

## 3. Production Verification Evidence

- **HTTP Status Check:** `curl.exe -m 10 -I https://nexora-mu-eosin.vercel.app`
  ```http
  HTTP/1.1 200 OK
  Content-Length: 106571
  Content-Type: text/html; charset=utf-8
  Server: Vercel
  X-Matched-Path: /
  X-Vercel-Cache: PRERENDER
  ```
- **Page Title & Brand:**
  - `<title>NEXORA — 10-Minute Quick Commerce | Fresh Groceries & Essentials</title>`
  - Theme Color: `#176B45` (Leaf Green)
  - Accent Color: `#D8F36A` (Zesty Lime)
- **Active Components Verified on Live Production:**
  - 10-Minute Rapid Delivery Header
  - 31 Categories Bar
  - Unified Multi-Role Switcher (Customer Store, Delivery Partner, Control Tower Super Admin, Store Operations)
  - Cart Drawer, Checkout Modal with Razorpay & COD, Live Order Tracker Modal, and Nexora AI Assistant
