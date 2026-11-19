# saas-wq-web

Vue 3 + Vite administration console for the saas-wq platform.

## Stack

Vue 3 (Composition API, `<script setup>` and TypeScript), Vite, Element Plus, Pinia,
Vue Router, Axios and ECharts.

## Scripts

```bash
npm install
npm run dev      # http://localhost:5173, proxies /api to http://localhost:8080
npm run build    # type check with vue-tsc then bundle into dist/
npm run preview  # serve the production build locally
```

The backend address used by the dev proxy can be changed with `VITE_PROXY_TARGET`
in `.env.development`.

## Structure

```
src/
  api/         Axios client, shared types and one module per backend resource
  composables/ useEcharts binding with resize and dispose handling
  layout/      application shell: sidebar, breadcrumb, account menu
  router/      route table, role metadata and authentication guard
  stores/      Pinia stores: auth, shell preferences, API traffic counter
  styles/      design tokens and shared page primitives
  utils/       session persistence and formatting helpers
  views/       login, dashboard, tenants, customers, orders, users
```

## Behaviour worth knowing

- The JWT is stored in `localStorage`; the Axios request interceptor attaches it and the
  response interceptor clears the session and redirects to `/login` on `401`. `403`, `500`
  and payment errors are shown as toasts using the backend message.
- Route metadata declares the roles allowed on a page, so the sidebar and the guard share
  the same source of truth.
- The dashboard API call chart counts real requests issued by the browser during the
  session; the revenue card aggregates the latest 500 orders of the current tenant.
- Plan codes are defined in the backend (`wq.stripe.plans`) and mirrored in
  `OrderListView` only to quote prices before the Stripe session is created.
