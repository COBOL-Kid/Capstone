#!/usr/bin/env bash
set -euo pipefail

export NVM_DIR="${NVM_DIR:-$HOME/.nvm}"
if [ ! -s "$NVM_DIR/nvm.sh" ]; then
  echo "nvm not found at $NVM_DIR" >&2
  exit 1
fi

# shellcheck source=/dev/null
. "$NVM_DIR/nvm.sh"

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$repo_root"

nvm install
nvm alias default "$(tr -d '[:space:]' < .nvmrc)"
nvm use

# Prefer the nvm-managed Node over the exec-daemon binary on Cloud Agent VMs.
export PATH="$NVM_BIN:$PATH"

corepack enable
corepack prepare pnpm@11.3.0 --activate

marker="# honest-car node setup"
if ! grep -qF "$marker" "$HOME/.bashrc"; then
  cat >>"$HOME/.bashrc" <<'EOF'
# honest-car node setup
if [ -s "$HOME/.nvm/nvm.sh" ]; then
  export NVM_DIR="$HOME/.nvm"
  # shellcheck source=/dev/null
  . "$NVM_DIR/nvm.sh"
  if [ -f /workspace/.nvmrc ]; then
    nvm use >/dev/null
  fi
  export PATH="$NVM_BIN:$PATH"
fi
EOF
fi

node --version
pnpm --version
