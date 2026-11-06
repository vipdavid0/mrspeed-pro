# WhatsApp 2FA & Facebook Login Testing & Approval Checklist

> [!IMPORTANT]
> This document serves as the persistent engineering record for Gemini / AI assistants regarding the current deployment status, pending tests, and Meta Developer Dashboard requirements for WhatsApp 2FA and Facebook Login.

## 📌 Current Technical Status

### 1. WhatsApp Business API
- **App ID**: `1096787226162983`
- **Business Account ID**: `104806072536924`
- **Phone Number ID**: `110484235294911`
- **Registered Business Number**: `+1 (226) 268-7645`
- **Webhook Endpoint**: `https://www.mrspeed.ca/backend/api/webhook.php` (Verify token: `mrspeed_whatsapp_verify_token_2026`)
- **Authentication Template**: `mrspeed_login_2fa` (**Approved & Active**)
- **Backend Senders**: [whatsapp_api.php](file:///E:/My-Speed-Test-App/web/backend/api/whatsapp_api.php) & [auth.php](file:///E:/My-Speed-Test-App/web/backend/api/auth.php) (`send_whatsapp_2fa`)
- **Frontend UI**: [login.php](file:///E:/My-Speed-Test-App/web/login.php) (Green WhatsApp 2FA button added)

### 2. Facebook Login (OAuth)
- **OAuth Endpoint**: [login_facebook.php](file:///E:/My-Speed-Test-App/web/login_facebook.php)
- **Redirect URI**: `https://www.mrspeed.ca/login_facebook.php`
- **Frontend UI**: [login.php](file:///E:/My-Speed-Test-App/web/login.php) & [register.php](file:///E:/My-Speed-Test-App/web/register.php) ("Continue with Facebook" buttons)
- **Security Intercept**: Automatically enforces 2FA if enabled on the user account.

---

## 🚧 Why Publishing / Go-Live is Currently Blocked

1. **Meta Development Mode Restrictions**:
   - Meta requires all applications to complete end-to-end testing of live API requests while in Development Mode before allowing public "Live Mode" publishing.
2. **Self-Messaging Limitation**:
   - The registered business sender number (`+1 226-268-7645`) cannot send WhatsApp messages to itself due to Meta Cloud API restrictions.
3. **App Review Submissions**:
   - Permissions (`whatsapp_business_messaging`, `whatsapp_business_management`) must be tested and verified with at least one successful external API response before submitting for final Meta App Review.

---

## 🧪 Detailed Step-by-Step Testing Guide (To Be Executed with a Friend)

### Phase 1: Meta Developer Dashboard Setup
- [ ] **Step 1**: Log in to the [Meta Developer Dashboard](https://developers.facebook.com/).
- [ ] **Step 2**: Navigate to **WhatsApp > Getting Started**.
- [ ] **Step 3**: Under **Step 2: Send and receive messages**, add a friend's mobile phone number to the **Test Recipients** list.
- [ ] **Step 4**: Have your friend accept/verify the test recipient prompt on their WhatsApp.

### Phase 2: Live WhatsApp 2FA Testing
- [ ] **Step 1**: Update your friend's phone number in the website database (`users` table -> `phone_number` for their test account) or register a test account with their phone number.
- [ ] **Step 2**: On your website (`https://www.mrspeed.ca/login.php`), log in with that test account (with 2FA enabled).
- [ ] **Step 3**: In the 2FA authorization modal, click the green **"SEND WHATSAPP 2FA VERIFICATION CODE"** button.
- [ ] **Step 4**: Confirm that your friend receives the `mrspeed_login_2fa` template message containing the 6-digit verification code on their WhatsApp.
- [ ] **Step 5**: Enter the 6-digit code on the website to verify successful login completion.
- [ ] **Check Logs**: Verify successful API response logging in `web/backend/api/debug_ai.log`.

### Phase 3: Facebook Login Testing
- [ ] **Step 1**: Go to `https://www.mrspeed.ca/login.php` or `register.php`.
- [ ] **Step 2**: Click **"CONTINUE WITH FACEBOOK"**.
- [ ] **Step 3**: Authorize the Meta OAuth permission dialog.
- [ ] **Step 4**: Verify that the account is successfully created/logged in and redirected to `account.php` (or intercepted by the 2FA modal if 2FA is enabled).

### Phase 4: Final Meta Publishing & App Review
- [ ] **Step 1**: Return to the Meta Developer Dashboard.
- [ ] **Step 2**: Navigate to **App Review > Permissions and Features**.
- [ ] **Step 3**: Click **"Submit for Review"** on `whatsapp_business_messaging` and `whatsapp_business_management`.
- [ ] **Step 4**: Switch your Meta App from **Development Mode** to **Live Mode**.
