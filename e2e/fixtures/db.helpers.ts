import { execFileSync } from "node:child_process";
import {
  SEEDED_PASSWORD_BCRYPT_HASH,
  SEEDED_VINS,
  UNVERIFIED_REFRESH_TOKEN,
} from "./test-data";

const PSQL_ENV = { ...process.env, PGPASSWORD: "honestcar" };

function runSql(sql: string): void {
  execFileSync(
    "psql",
    [
      "-h",
      "localhost",
      "-U",
      "honestcar",
      "-d",
      "honestcar",
      "-v",
      "ON_ERROR_STOP=1",
      "-c",
      sql,
    ],
    { env: PSQL_ENV, stdio: "pipe" },
  );
}

export function linkSeededCamryToEmptyGarageUser(): void {
  runSql(
    `INSERT INTO user_vin (user_id, vin_num, current_mileage, available_image_urls, selected_image_url) SELECT 2, '${SEEDED_VINS.camry}', 46000, available_image_urls, selected_image_url FROM user_vin WHERE user_id = 1 AND vin_num = '${SEEDED_VINS.camry}' ON CONFLICT DO NOTHING;`,
  );
}

export function unlinkCamryFromEmptyGarageUser(): void {
  runSql(
    `DELETE FROM user_vin WHERE user_id = 2 AND vin_num = '${SEEDED_VINS.camry}';`,
  );
}

export function deleteUserByEmail(email: string): void {
  const escaped = email.replace(/'/g, "''");
  runSql(
    `DELETE FROM email_verification_code WHERE user_id IN (SELECT user_id FROM user_detail WHERE user_email = '${escaped}');
DELETE FROM account_change_request WHERE user_id IN (SELECT user_id FROM user_detail WHERE user_email = '${escaped}');
DELETE FROM refresh_token WHERE user_id IN (SELECT user_id FROM user_detail WHERE user_email = '${escaped}');
DELETE FROM user_vin WHERE user_id IN (SELECT user_id FROM user_detail WHERE user_email = '${escaped}');
DELETE FROM completed_maintenance WHERE user_id IN (SELECT user_id FROM user_detail WHERE user_email = '${escaped}');
DELETE FROM completed_recall WHERE user_id IN (SELECT user_id FROM user_detail WHERE user_email = '${escaped}');
DELETE FROM user_detail WHERE user_email = '${escaped}';`,
  );
}

export function resetPasswordChangeUser(): void {
  runSql(
    `UPDATE user_detail SET user_pw = '${SEEDED_PASSWORD_BCRYPT_HASH}', user_email = 'password.change@example.com', user_sms = '+15559876543' WHERE user_email IN ('password.change@example.com') OR user_id = 4;
DELETE FROM account_change_request WHERE user_id = 4;
DELETE FROM refresh_token WHERE user_id = 4;
INSERT INTO refresh_token (token, expiry_date, user_id) VALUES ('e2e-password-change-refresh-token', CURRENT_TIMESTAMP + INTERVAL '30 days', 4) ON CONFLICT (token) DO UPDATE SET expiry_date = EXCLUDED.expiry_date;`,
  );
}

/** Bcrypt hash of `123456` — matches E2E fixed verification code. */
const UNVERIFIED_EMAIL_CODE_HASH =
  "$2a$10$.9ABP0s9xuhY8m31.jNs6eagUOnLtizizZmnd6gdnCjVFq5cUvCC2";

export function resetUnverifiedUser(): void {
  runSql(
    `UPDATE user_detail SET email_verified = FALSE, email_verified_at = NULL, failed_login_attempts = 0, lockout_end = NULL WHERE user_id = 3;
DELETE FROM email_verification_code WHERE user_id = 3;
DELETE FROM refresh_token WHERE user_id = 3;
INSERT INTO refresh_token (token, expiry_date, user_id) VALUES ('${UNVERIFIED_REFRESH_TOKEN}', CURRENT_TIMESTAMP + INTERVAL '30 days', 3);
INSERT INTO email_verification_code (user_id, code_hash, expires_at, sign_in_challenge_hash, created_at, failed_attempts)
VALUES (3, '${UNVERIFIED_EMAIL_CODE_HASH}', CURRENT_TIMESTAMP + INTERVAL '30 minutes', NULL, CURRENT_TIMESTAMP, 0);`,
  );
}
