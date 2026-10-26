# NEXORA — VERCEL PRODUCTION DEPLOYMENT GUIDE

This guide provides the exact configuration required in the Vercel Dashboard to deploy the NEXORA Quick Commerce Platform with zero 404 errors.

---

## 1. Why Did Vercel Show 404 Before?

1. **Incorrect Output Directory in `vercel.json`:**  
   The previous `vercel.json` in the repository specified `"outputDirectory": "frontend/.next"`. Vercel interprets this as a static site export and expects an `index.html` at that location. Because Next.js 14 App Router does not output static HTML files directly into `.next`, Vercel served `404 NOT_FOUND`.
2. **Missing Monorepo Root Directory:**  
   Nexora is a monorepo (`frontend/` + `backend/`). Vercel must be instructed to run inside the `frontend` subdirectory.

---

## 2. Step-by-Step Vercel Dashboard Configuration

Follow these steps in your Vercel Dashboard:

### Step 1: Open Your Existing Project
1. Go to [https://vercel.com/dashboard](https://vercel.com/dashboard).
2. Click on your `nexora` project (or import `https://github.com/jeetjajal/nexora` if creating fresh).

### Step 2: Configure Root Directory
1. In the project dashboard, click **Settings** tab.
2. Under the **General** section, locate **Root Directory**.
3. Click **Edit**.
4. Type `frontend` (or select the `frontend` folder).
5. Ensure **"Include source files outside of the Root Directory in the Build Step"** remains checked.
6. Click **Save**.

### Step 3: Verify Framework & Build Settings
Under **Settings** → **Build and Deployment**:
- **Framework Preset:** `Next.js` (Vercel will auto-detect this once Root Directory is `frontend`).
- **Build Command:** Leave default (`next build`) — toggle "OVERRIDE" to **OFF**.
- **Output Directory:** Leave default (empty) — toggle "OVERRIDE" to **OFF** (DO NOT type `.next` or `out`).
- **Install Command:** Leave default (`npm install`) — toggle "OVERRIDE" to **OFF**.

### Step 4: Verify Git Connection & Production Branch
Under **Settings** → **Git**:
- **Connected Repository:** `jeetjajal/nexora`
- **Production Branch:** `main`
- Ensure **"Automatically deploy when you push to this branch"** is enabled.

### Step 5: (Optional) Set Environment Variables
Under **Settings** → **Environment Variables**:
- `NEXT_PUBLIC_RAZORPAY_KEY_ID`: `rzp_test_nexora_sandbox` (or your real test key from Razorpay Dashboard)
- `BACKEND_URL`: Leave empty for server-side resilient mock, or provide your hosted Spring Boot URL when deployed.

### Step 6: Trigger a New Production Deployment
1. Go to the **Deployments** tab in Vercel.
2. Click on the three dots `...` next to the latest commit or click **Deploy** / **Redeploy**.
3. Alternatively, simply pushing a commit to `main` on GitHub triggers a fresh build automatically!

---

## 3. Verifying the Deployment
Once the deployment finishes with status **Ready**:
1. Open your production domain: `https://nexora-mu-eosin.vercel.app`
2. You will see the **NEXORA Quick Commerce Platform**:
   - Leaf Green (`#176B45`) and Zesty Lime (`#D8F36A`) branding.
   - 10-Minute Rapid Delivery header.
   - 31 Blinkit-style grocery categories.
   - 365+ catalog products with live search and category filtering.
   - Add/Remove quantity counters with real-time cart drawer.
   - Checkout Modal with coupon `NEXORA50` (50% off).
   - Live Order Tracking Modal with 6-digit delivery OTP and dynamic dispatch status.
   - AI Shopping Assistant popup.
