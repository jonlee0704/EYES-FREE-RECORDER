# Multi-Agent Git Workflow (mandatory)

JY works on this repo with several agents at once (Antigravity, Claude Code local, Claude Code web). GitHub is the only shared state. Full details are in AGENTS.md section 1.

1. Never commit directly to `master`. Make one branch per task, prefixed by who made it:
   - `ag/<topic>` for Antigravity
   - `claude/<topic>` for Claude
   - `jy/<topic>` for JY
2. Before starting, run `git fetch origin && git switch master && git pull --ff-only`, then create your branch.
3. Push your branch at the end of every session, even unfinished work.
4. Merge into `master` only through a pull request. JY reviews and merges.
5. Never push to another agent's branch.
6. After `master` moves, rebase your open branch with `git pull --rebase origin master`.
7. Never commit secrets: google-services.json, keystores, signing passwords, local.properties.
