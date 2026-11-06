# Implementation Plan - Strict Uniform Website Redesign & Codebase Refactoring

This plan provides comprehensive, step-by-step instructions for Gemma to completely unify the layout, design, borders, padding, margins, and card styling across every PHP page on the website, moving hardcoded inline styles into `mr-speed-theme.css`, while **strictly preserving** the header, menus, live chat, footer, and custom cursors.

## User Review Required

> [!IMPORTANT]
> **Preserved Components**: Header (`header_nav.php`), navigation menus, live chat widget (`livechat.js` / `livechat.css`), footer (`footer.php`), and custom cursors MUST remain 100% untouched and functional.

> [!CAUTION]
> **Backup Reminder**: The user has confirmed they have a full backup of the project in case any structural changes need reversion.

## Proposed Changes

### Global Styling & CSS Centralization
- [MODIFY] [mr-speed-theme.css](file:///E:/My-Speed-Test-App/web/css/mr-speed-theme.css)
  - Enforce global `.uniform-main` and container rules (`max-width: 1150px`, `margin: 0 auto`, `border-radius: 20px`, uniform padding and box-sizing).
  - Enforce strict light theme variables, table styles (`MY INVOICES & RECEIPTS`), toast styles, and amber button text color (`color: #000 !important`).

### Page Template & PHP Refactoring
- [MODIFY] Core PHP Pages (`index.php`, `pricing.php`, `download.php`, `account.php`, `admin.php`, `cart.php`, `login.php`, `register.php`, `tools.php`, `status.php`, `documentation.php`, etc.)
  - Remove hardcoded inline styles and wrapper divs that conflict with the uniform container layout.
  - Wrap main content in standardized semantic containers using `.uniform-main` / `.glass`.

## Verification Plan

### Automated & Manual Checks
- Verify build and syntax across all modified PHP files.
- Inspect rendered pages in both dark and light themes for uniform margins, card borders, and amber button text color.
- Confirm header, menus, live chat, footer, and custom cursors remain fully operational.
