# Security Policy

## 🔒 Security & Privacy Guarantees

"**I Manage**" is developed with a strict offline-first, zero-telemetry philosophy.

1. **No Internet Permission:**
   - The app's `AndroidManifest.xml` does not contain `android.permission.INTERNET`.
   - The OS kernel physically restricts any network calls or socket connections.

2. **Safe Vault Cryptography:**
   - Files and directories inside the Safe Vault are encrypted with **AES-256 (CBC with PKCS7 padding)**.
   - Master keys are hardware-backed and stored in the **Android KeyStore (TEE / StrongBox)**.
   - Initialized using Android KeyStore random IV generation.

3. **Secure File Shredder:**
   - Uses multi-pass byte overwriting (`0x00`, `0xFF`, and `SecureRandom` cryptographic bytes) before unlinking file inodes.

4. **Anti-Snooping (`FLAG_SECURE`):**
   - Window manager flags prevent system screenshots and app switcher recording.

## 🛡️ Reporting a Vulnerability

If you discover any security vulnerability in this project, please open a Private Security Advisory on GitHub or submit a report to the repository maintainer.
