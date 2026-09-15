import { setFailed } from './actionsEnv.js';
import { main } from './main.js';

main().catch((error: unknown) => {
  setFailed(error instanceof Error ? error.message : String(error));
});
