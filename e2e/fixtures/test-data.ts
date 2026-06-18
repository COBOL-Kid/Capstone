/** Bcrypt hash for `Password123!` — matches seeded E2E users in seed-e2e-data.sql. */
export const SEEDED_PASSWORD_BCRYPT_HASH =
  "$2y$10$Zy4xFp/QwJaDF5kkE5ob1uzhr8YD3VsqTuV8bFLr.jSeyfEgBUyjq";

export const TEST_USER = {
  email: "test.user@example.com",
  password: "Password123!",
  firstName: "Test",
  lastName: "User",
};

export const EMPTY_GARAGE_USER = {
  email: "empty.garage@example.com",
  password: "Password123!",
  firstName: "Empty",
  lastName: "Garage",
};

export const UNVERIFIED_USER = {
  email: "unverified.user@example.com",
  password: "Password123!",
  firstName: "Unverified",
  lastName: "User",
};

export const PASSWORD_CHANGE_USER = {
  email: "password.change@example.com",
  password: "Password123!",
  newPassword: "NewPassword123!",
  firstName: "Password",
  lastName: "Change",
};

export const PASSWORD_CHANGE_REFRESH_TOKEN =
  "e2e-password-change-refresh-token";

export const SEEDED_VINS = {
  camry: "4T1C11AK5LU123456",
  civic: "2HGFC2F59JH543210",
};

export const VEHICLE_LABELS = {
  camry: "2020 Toyota Camry",
  civic: "2018 Honda Civic",
};

/** Bcrypt hash of `123456` — matches seeded email/account verification codes. */
export const VERIFICATION_CODE = "123456";

export const UNVERIFIED_REFRESH_TOKEN = "e2e-unverified-refresh-token";

export const AUTH_FILES = {
  verified: ".auth/user.json",
  emptyGarage: ".auth/empty-garage.json",
  unverified: ".auth/unverified.json",
} as const;
