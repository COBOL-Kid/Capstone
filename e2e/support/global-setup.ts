const BACKEND_URL = "http://localhost:8080/actuator/health";
const FRONTEND_URL = "http://localhost:4200/";

export default async function globalSetup(): Promise<void> {
  const failures: string[] = [];

  try {
    const backend = await fetch(BACKEND_URL);
    if (!backend.ok) {
      failures.push(
        `Backend health check failed (${backend.status}) at ${BACKEND_URL}`,
      );
    }
  } catch {
    failures.push(`Backend is not reachable at ${BACKEND_URL}`);
  }

  try {
    const frontend = await fetch(FRONTEND_URL);
    if (!frontend.ok) {
      failures.push(
        `Frontend check failed (${frontend.status}) at ${FRONTEND_URL}`,
      );
    }
  } catch {
    failures.push(`Frontend is not reachable at ${FRONTEND_URL}`);
  }

  if (failures.length > 0) {
    throw new Error(
      [
        "E2E prerequisites are not running:",
        ...failures.map((message) => `- ${message}`),
        "",
        "Start the local stack in separate terminals:",
        "  Terminal A: cd Server && SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun",
        "  Terminal B: cd Client && pnpm start",
        "",
        "Seed the database before running garage tests:",
        "  psql -U honestcar -d honestcar -f e2e/scripts/seed-e2e-data.sql",
      ].join("\n"),
    );
  }
}
