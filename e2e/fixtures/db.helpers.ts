import { execSync } from "node:child_process";
import { SEEDED_VINS } from "./test-data";

const PSQL =
  "PGPASSWORD=honestcar psql -h localhost -U honestcar -d honestcar -v ON_ERROR_STOP=1 -c";

export function linkSeededCamryToEmptyGarageUser(): void {
  execSync(
    `${PSQL} "INSERT INTO user_vin (user_id, vin_num, current_mileage, available_image_urls, selected_image_url) SELECT 2, '${SEEDED_VINS.camry}', 46000, available_image_urls, selected_image_url FROM user_vin WHERE user_id = 1 AND vin_num = '${SEEDED_VINS.camry}' ON CONFLICT DO NOTHING;"`,
    { stdio: "pipe" },
  );
}

export function unlinkCamryFromEmptyGarageUser(): void {
  execSync(
    `${PSQL} "DELETE FROM user_vin WHERE user_id = 2 AND vin_num = '${SEEDED_VINS.camry}';"`,
    { stdio: "pipe" },
  );
}
