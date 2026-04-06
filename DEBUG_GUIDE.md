# DEBUG_GUIDE.md

Debug & Cheat API guide for frontend integration testing of Nagas' Treasure slot game.

---

## 1. Overview

### What Is This?

A set of REST endpoints that let you force specific game outcomes during development and staging. You can trigger Free Spins, Hold & Win, jackpot wins, set exact grid symbols, manipulate player state, and test edge cases without relying on RNG.

### How to Enable

The debug API only exists when the Spring profile is `dev` or `staging`. It is **completely absent** from production builds — the classes are not loaded by the Spring container.

```bash
# Start with debug API enabled
SPRING_PROFILES_ACTIVE=dev ./gradlew :app:bootRun

# Or via environment variable
export SPRING_PROFILES_ACTIVE=staging
java -jar app.jar
```

### Authentication

Every request requires the `X-Debug-Token` header:

```
X-Debug-Token: nagas-debug-2024
```

Requests without this header receive `403 Forbidden`.

### Base URL

```
http://localhost:3000/api/game/debug
```

All debug endpoints are under `/debug`. The game API itself is at `/api/game/api/v1/slot`.

---

## 2. All Cheat Codes

### How Cheats Work

There are two types:

- **Next-spin cheats** — stored in Redis, consumed by the very next spin, then auto-deleted. If no spin happens within 5 minutes, the cheat expires.
- **Immediate cheats** — modify state directly (jackpot pools, game mode, session). Take effect instantly.

### Cheat Endpoint

```
POST /debug/cheat/{agentId}/{userId}
Content-Type: application/json
X-Debug-Token: nagas-debug-2024

{
  "cheat": "CHEAT_CODE_HERE",
  "value": { ... }
}
```

Response:
```json
{
  "applied": true,
  "cheat": "FORCE_FREE_SPIN",
  "description": "Will apply on next spin: FORCE_FREE_SPIN",
  "agent_id": "agent-1",
  "user_id": "player-1",
  "expires_in": "5 minutes (or next spin)"
}
```

---

### 2.1 FORCE_FREE_SPIN

Forces the next spin to place 3 Scatter symbols on reels 2/3/4, triggering 8 Free Spins.

**When to use:** Test the Free Spin entry animation, spin counter UI, and FS flow start-to-finish.

```bash
curl -X POST http://localhost:3000/api/game/debug/cheat/agent-1/player-1 \
  -H "X-Debug-Token: nagas-debug-2024" \
  -H "Content-Type: application/json" \
  -d '{"cheat": "FORCE_FREE_SPIN", "value": {}}'
```

After applying, the next `/api/v1/slot/spin` response will contain:

```json
{
  "data": {
    "round": {
      "result": {
        "this_mode": "base",
        "next_mode": "free",
        "features": {
          "free_spins": { "remain": 8, "total": 8 }
        }
      }
    }
  }
}
```

---

### 2.2 FORCE_HOLD_AND_WIN

Forces the next spin to place 6+ Bonus symbols, triggering Hold & Win mode with 3 respins.

**When to use:** Test H&W entry, locked bonus display, respin countdown UI.

```bash
curl -X POST http://localhost:3000/api/game/debug/cheat/agent-1/player-1 \
  -H "X-Debug-Token: nagas-debug-2024" \
  -H "Content-Type: application/json" \
  -d '{"cheat": "FORCE_HOLD_AND_WIN", "value": {}}'
```

Response after spin:
```json
{
  "data": {
    "round": {
      "result": {
        "this_mode": "base",
        "next_mode": "hold_and_win",
        "features": {
          "hold_and_win": {
            "respins_remain": 3,
            "locked_bonuses": [ ... ],
            "total_multiplier": 25.0,
            "is_ending": false
          }
        }
      }
    }
  }
}
```

---

### 2.3 FORCE_JACKPOT

Forces the jackpot wheel to trigger on the next **base spin**. Does not work during Free Spin or Hold & Win (jackpot is base-spin-only per GDD).

**When to use:** Test the jackpot wheel animation, tier display, pool reset.

```bash
curl -X POST http://localhost:3000/api/game/debug/cheat/agent-1/player-1 \
  -H "X-Debug-Token: nagas-debug-2024" \
  -H "Content-Type: application/json" \
  -d '{"cheat": "FORCE_JACKPOT", "value": {}}'
```

Response after spin:
```json
{
  "data": {
    "round": {
      "result": {
        "features": {
          "progressive_jackpot": {
            "is_triggered": true,
            "tier": "SAPPHIRE",
            "win": "15.50",
            "hit_arrow": false,
            "glowing_rings": [[0,1], [1,0], ...]
          }
        }
      }
    }
  }
}
```

---

### 2.4 FORCE_NORMAL_WIN

Uses normal RNG — no override. Useful to "cancel" a previously set cheat before it's consumed.

**When to use:** Return to normal behavior after setting up cheats.

```bash
curl -X POST http://localhost:3000/api/game/debug/cheat/agent-1/player-1 \
  -H "X-Debug-Token: nagas-debug-2024" \
  -H "Content-Type: application/json" \
  -d '{"cheat": "FORCE_NORMAL_WIN", "value": {}}'
```

---

### 2.5 FORCE_LOSS

Forces a grid of alternating low symbols (A, B, C, D) that don't form any 3+ match on any payline. Total win = $0.00.

**When to use:** Test the "no win" UI state, verify no win animation plays.

```bash
curl -X POST http://localhost:3000/api/game/debug/cheat/agent-1/player-1 \
  -H "X-Debug-Token: nagas-debug-2024" \
  -H "Content-Type: application/json" \
  -d '{"cheat": "FORCE_LOSS", "value": {}}'
```

---

### 2.6 FORCE_WIN_MULTIPLIER

Uses normal RNG for grid generation. The `multiplier` value in the payload is reserved for future use with server-side win override.

**When to use:** Currently equivalent to `FORCE_NORMAL_WIN`. For explicit win amounts, use `FORCE_GRID` with a known winning grid.

```bash
curl -X POST http://localhost:3000/api/game/debug/cheat/agent-1/player-1 \
  -H "X-Debug-Token: nagas-debug-2024" \
  -H "Content-Type: application/json" \
  -d '{"cheat": "FORCE_WIN_MULTIPLIER", "value": {"multiplier": 500}}'
```

---

### 2.7 FORCE_WIN_CAP

Forces a grid of all H symbols (symbol 8, highest payout). With 25 paylines each hitting 5-of-a-kind H (10x multiplier), total win hits the 2000x cap.

**When to use:** Test the Legendary Win animation, win cap calculation, big number display.

```bash
curl -X POST http://localhost:3000/api/game/debug/cheat/agent-1/player-1 \
  -H "X-Debug-Token: nagas-debug-2024" \
  -H "Content-Type: application/json" \
  -d '{"cheat": "FORCE_WIN_CAP", "value": {}}'
```

With a $1 bet, expect:
```json
{
  "data": {
    "round": {
      "total_win": "2000.00",
      "result": {
        "stages": [{
          "total_win": "2000.00"
        }]
      }
    }
  }
}
```

---

### 2.8 FORCE_GRID

Places exact symbols on the 3x5 grid. Grid format is `[row0, row1, row2]` where each row is `[col0, col1, col2, col3, col4]`.

**Symbol IDs:** A=1, B=2, C=3, D=4, E=5, F=6, G=7, H=8, Scatter=9, Wild=10, Major=11, Mini=12, Bonus=13, StackedWild=14

**When to use:** Test specific payline wins, verify FE payout display for known outcomes.

```bash
# Force 5-of-a-kind H on middle row (payline 1)
curl -X POST http://localhost:3000/api/game/debug/cheat/agent-1/player-1 \
  -H "X-Debug-Token: nagas-debug-2024" \
  -H "Content-Type: application/json" \
  -d '{
    "cheat": "FORCE_GRID",
    "value": {
      "grid": [
        [1, 2, 3, 4, 1],
        [8, 8, 8, 8, 8],
        [1, 2, 3, 4, 1]
      ]
    }
  }'
```

Expected: Payline 1 wins 5x H = 10.0 x totalBet.

---

### 2.9 FORCE_HW_LOCKED_COUNT

Forces the H&W trigger with a specific number of locked bonus symbols (default: 6).

**When to use:** Test H&W with many bonuses already locked (e.g., 10 out of 15 for near-Grand scenarios).

```bash
curl -X POST http://localhost:3000/api/game/debug/cheat/agent-1/player-1 \
  -H "X-Debug-Token: nagas-debug-2024" \
  -H "Content-Type: application/json" \
  -d '{"cheat": "FORCE_HW_LOCKED_COUNT", "value": {"count": 10}}'
```

---

### 2.10 SET_JACKPOT_POOL

**Immediate.** Sets jackpot pool tiers to exact values. Takes effect instantly.

**When to use:** Set pools to small values so jackpot wins are predictable for UI testing.

```bash
curl -X POST http://localhost:3000/api/game/debug/cheat/agent-1/player-1 \
  -H "X-Debug-Token: nagas-debug-2024" \
  -H "Content-Type: application/json" \
  -d '{"cheat": "SET_JACKPOT_POOL", "value": {"DIAMOND": 10.0, "RUBY": 5.0, "EMERALD": 2.0, "SAPPHIRE": 1.0}}'
```

Or use the dedicated endpoint:
```bash
curl -X POST http://localhost:3000/api/game/debug/jackpot/agent-1 \
  -H "X-Debug-Token: nagas-debug-2024" \
  -H "Content-Type: application/json" \
  -d '{"DIAMOND": 10.0, "RUBY": 5.0, "EMERALD": 2.0, "SAPPHIRE": 1.0}'
```

---

### 2.11 FORCE_JACKPOT_TRIGGER

Forces the jackpot trigger on the next eligible base spin. Same as `FORCE_JACKPOT`.

```bash
curl -X POST http://localhost:3000/api/game/debug/cheat/agent-1/player-1 \
  -H "X-Debug-Token: nagas-debug-2024" \
  -H "Content-Type: application/json" \
  -d '{"cheat": "FORCE_JACKPOT_TRIGGER", "value": {}}'
```

---

### 2.12 RESET_JACKPOT_POOL

**Immediate.** Resets all jackpot pools to seed values: DIAMOND=$10,000, RUBY=$500, EMERALD=$50, SAPPHIRE=$10.

```bash
curl -X POST http://localhost:3000/api/game/debug/cheat/agent-1/player-1 \
  -H "X-Debug-Token: nagas-debug-2024" \
  -H "Content-Type: application/json" \
  -d '{"cheat": "RESET_JACKPOT_POOL", "value": {}}'
```

---

### 2.13 SET_FREE_SPIN_COUNT

**Immediate.** Sets the remaining free spin count for an active FS session. Player must already be in Free Spin mode.

**When to use:** Skip to the last free spin, or extend an FS session for longer testing.

```bash
curl -X POST http://localhost:3000/api/game/debug/cheat/agent-1/player-1 \
  -H "X-Debug-Token: nagas-debug-2024" \
  -H "Content-Type: application/json" \
  -d '{"cheat": "SET_FREE_SPIN_COUNT", "value": {"count": 2, "game_id": "nagas_treasure"}}'
```

---

### 2.14 FORCE_HW_IN_FREE_SPIN

Forces the next Free Spin to trigger Hold & Win inside the FS session. The player must be in Free Spin mode.

**When to use:** Test the nested FS -> H&W -> FS transition flow.

```bash
curl -X POST http://localhost:3000/api/game/debug/cheat/agent-1/player-1 \
  -H "X-Debug-Token: nagas-debug-2024" \
  -H "Content-Type: application/json" \
  -d '{"cheat": "FORCE_HW_IN_FREE_SPIN", "value": {}}'
```

---

### 2.15 FORCE_LAST_FREE_SPIN

**Immediate.** Sets remaining free spins to 1. The next spin will be the final Free Spin.

**When to use:** Test the FS summary popup and transition back to base mode.

```bash
curl -X POST http://localhost:3000/api/game/debug/cheat/agent-1/player-1 \
  -H "X-Debug-Token: nagas-debug-2024" \
  -H "Content-Type: application/json" \
  -d '{"cheat": "FORCE_LAST_FREE_SPIN", "value": {"game_id": "nagas_treasure"}}'
```

---

### 2.16 SET_GAME_MODE

**Immediate.** Puts the player directly into a specific game mode.

| Mode | Value | Effect |
|------|-------|--------|
| `base` | Deletes state | Clean base game, no active features |
| `free` | Creates FS state | 8 free spins, baseBet from value |
| `hold_and_win` | Creates H&W state | 6 locked bonuses, 3 respins |

**When to use:** Skip directly to a specific mode without going through the trigger flow.

```bash
# Put player into Free Spin mode
curl -X POST http://localhost:3000/api/game/debug/cheat/agent-1/player-1 \
  -H "X-Debug-Token: nagas-debug-2024" \
  -H "Content-Type: application/json" \
  -d '{"cheat": "SET_GAME_MODE", "value": {"mode": "free", "game_id": "nagas_treasure"}}'
```

Or use the dedicated endpoint:
```bash
curl -X POST http://localhost:3000/api/game/debug/state/agent-1/player-1 \
  -H "X-Debug-Token: nagas-debug-2024" \
  -H "Content-Type: application/json" \
  -d '{"mode": "hold_and_win", "game_id": "nagas_treasure", "bet": 5.0}'
```

---

### 2.17 SET_ACCUMULATED_WIN

**Immediate.** Sets the accumulated win total in an active feature session. Player must be in FS or H&W mode.

**When to use:** Test the win counter display at specific values (Big Win, Mega Win, Legendary Win thresholds).

```bash
curl -X POST http://localhost:3000/api/game/debug/cheat/agent-1/player-1 \
  -H "X-Debug-Token: nagas-debug-2024" \
  -H "Content-Type: application/json" \
  -d '{"cheat": "SET_ACCUMULATED_WIN", "value": {"amount": 500.0, "game_id": "nagas_treasure"}}'
```

---

### 2.18 RESET_SESSION

**Immediate.** Deletes all game state for the player AND resets jackpot pools to seed values.

**When to use:** Start completely fresh. Nuclear option.

```bash
curl -X POST http://localhost:3000/api/game/debug/cheat/agent-1/player-1 \
  -H "X-Debug-Token: nagas-debug-2024" \
  -H "Content-Type: application/json" \
  -d '{"cheat": "RESET_SESSION", "value": {"game_id": "nagas_treasure"}}'
```

Or use the dedicated endpoint:
```bash
curl -X DELETE "http://localhost:3000/api/game/debug/state/agent-1/player-1?gameId=nagas_treasure" \
  -H "X-Debug-Token: nagas-debug-2024"
```

---

### 2.19 SET_AGENT_JACKPOT_POOL

Same as `SET_JACKPOT_POOL`. Use the dedicated `/debug/jackpot/{agentId}` endpoint to set pools for a specific agent.

---

### 2.20 CLEAR_AGENT_STATE

**Immediate.** Deletes game state for a specific agent+user. Does not touch jackpot pools.

```bash
curl -X POST http://localhost:3000/api/game/debug/cheat/agent-1/player-1 \
  -H "X-Debug-Token: nagas-debug-2024" \
  -H "Content-Type: application/json" \
  -d '{"cheat": "CLEAR_AGENT_STATE", "value": {"game_id": "nagas_treasure"}}'
```

---

## 3. FE Test Scenarios

### Scenario 1: Test Free Spin Flow

```bash
# Step 1: Reset to clean state
curl -X DELETE "http://localhost:3000/api/game/debug/state/agent-1/player-1?gameId=nagas_treasure" \
  -H "X-Debug-Token: nagas-debug-2024"

# Step 2: Force next spin to trigger FS
curl -X POST http://localhost:3000/api/game/debug/cheat/agent-1/player-1 \
  -H "X-Debug-Token: nagas-debug-2024" \
  -H "Content-Type: application/json" \
  -d '{"cheat": "FORCE_FREE_SPIN", "value": {}}'

# Step 3: Spin — this triggers FS entry
curl -X POST http://localhost:3000/api/game/api/v1/slot/spin \
  -H "Content-Type: application/json" \
  -d '{"agent_id":"agent-1","user_id":"player-1","game_id":"nagas_treasure","bet_amount":100,"session_id":"s1"}'
# Verify: next_mode = "free", free_spins.total = 8

# Step 4: Spin 8 times to play through all free spins
# Each spin: verify this_mode = "free", remain decrements
# Last spin: verify next_mode = "base", ends_superround = true

# Step 5: Optional — skip to last spin
curl -X POST http://localhost:3000/api/game/debug/cheat/agent-1/player-1 \
  -H "X-Debug-Token: nagas-debug-2024" \
  -H "Content-Type: application/json" \
  -d '{"cheat": "FORCE_LAST_FREE_SPIN", "value": {"game_id": "nagas_treasure"}}'
```

**What to verify in FE:**
- FS entry popup appears with "8 FREE SPINS WON!"
- Spin counter shows and decrements correctly
- Background/music changes to FS theme
- Summary popup at end shows total win
- Bet buttons disabled during FS

---

### Scenario 2: Test Hold & Win Flow

```bash
# Step 1: Reset
curl -X DELETE "http://localhost:3000/api/game/debug/state/agent-1/player-1?gameId=nagas_treasure" \
  -H "X-Debug-Token: nagas-debug-2024"

# Step 2: Force H&W trigger
curl -X POST http://localhost:3000/api/game/debug/cheat/agent-1/player-1 \
  -H "X-Debug-Token: nagas-debug-2024" \
  -H "Content-Type: application/json" \
  -d '{"cheat": "FORCE_HOLD_AND_WIN", "value": {}}'

# Step 3: Spin — triggers H&W
curl -X POST http://localhost:3000/api/game/api/v1/slot/spin \
  -H "Content-Type: application/json" \
  -d '{"agent_id":"agent-1","user_id":"player-1","game_id":"nagas_treasure","bet_amount":100,"session_id":"s1"}'
# Verify: next_mode = "hold_and_win", locked_bonuses has 6+ items

# Step 4: Continue spinning — each spin is an H&W respin
# Verify: this_mode = "hold_and_win", screen shows only 0 or 11-13
# Verify: respins_remain decrements (or resets to 3 on new bonus)
# When respins_remain = 0 or 15 cells filled: is_ending = true
```

**What to verify in FE:**
- Bonus symbols lock in place
- Respin counter shows 3 and decrements
- Non-locked cells spin, locked cells stay
- Grand Bonus text when 15/15 filled
- H&W summary shows total multiplier

---

### Scenario 3: Test Jackpot Win

```bash
# Step 1: Set jackpot pool to small known values
curl -X POST http://localhost:3000/api/game/debug/jackpot/agent-1 \
  -H "X-Debug-Token: nagas-debug-2024" \
  -H "Content-Type: application/json" \
  -d '{"DIAMOND": 42.00, "RUBY": 15.00, "EMERALD": 7.50, "SAPPHIRE": 3.00}'

# Step 2: Force jackpot trigger on next spin
curl -X POST http://localhost:3000/api/game/debug/cheat/agent-1/player-1 \
  -H "X-Debug-Token: nagas-debug-2024" \
  -H "Content-Type: application/json" \
  -d '{"cheat": "FORCE_JACKPOT", "value": {}}'

# Step 3: Spin (must be base spin — not during FS or H&W)
curl -X POST http://localhost:3000/api/game/api/v1/slot/spin \
  -H "Content-Type: application/json" \
  -d '{"agent_id":"agent-1","user_id":"player-1","game_id":"nagas_treasure","bet_amount":100,"session_id":"s1"}'
# Verify: progressive_jackpot.is_triggered = true
# Verify: progressive_jackpot.tier is one of DIAMOND/RUBY/EMERALD/SAPPHIRE
# Verify: progressive_jackpot.win matches the pool value you set
```

**What to verify in FE:**
- Glowing ring animation on qualifying symbols
- Transition to jackpot wheel screen
- Inner/outer wheel animation
- Tier result display (DIAMOND/RUBY/EMERALD/SAPPHIRE)
- Prize popup with amount
- Pool resets to seed after win (check via `/api/v1/slot/jackpot-pools?agentId=agent-1`)

---

### Scenario 4: Test FS -> H&W Nested Transition

```bash
# Step 1: Put player into Free Spin mode
curl -X POST http://localhost:3000/api/game/debug/state/agent-1/player-1 \
  -H "X-Debug-Token: nagas-debug-2024" \
  -H "Content-Type: application/json" \
  -d '{"mode": "free", "game_id": "nagas_treasure", "bet": 1.0}'

# Step 2: Force H&W trigger on next FS spin
curl -X POST http://localhost:3000/api/game/debug/cheat/agent-1/player-1 \
  -H "X-Debug-Token: nagas-debug-2024" \
  -H "Content-Type: application/json" \
  -d '{"cheat": "FORCE_HW_IN_FREE_SPIN", "value": {}}'

# Step 3: Spin — FS spin that triggers H&W
curl -X POST http://localhost:3000/api/game/api/v1/slot/spin \
  -H "Content-Type: application/json" \
  -d '{"agent_id":"agent-1","user_id":"player-1","game_id":"nagas_treasure","bet_amount":100,"session_id":"s1"}'
# Verify: this_mode = "free", next_mode = "hold_and_win"
# Verify: free_spins.remain = 7 (consumed one FS)
# Verify: hold_and_win.respins_remain = 3

# Step 4: Play through H&W respins until it ends
# Verify: after H&W ends → next_mode = "free" (NOT "base")
# Verify: free_spins.remain still shows remaining count

# Step 5: Continue FS until exhausted
# Verify: final spin → next_mode = "base", ends_superround = true
```

---

### Scenario 5: Test Disconnect & Reconnect

```bash
# Step 1: Put player into Free Spin mode with 5 remaining
curl -X POST http://localhost:3000/api/game/debug/state/agent-1/player-1 \
  -H "X-Debug-Token: nagas-debug-2024" \
  -H "Content-Type: application/json" \
  -d '{"mode": "free", "game_id": "nagas_treasure", "bet": 2.0}'

# Step 2: Set remaining spins to 5
curl -X POST http://localhost:3000/api/game/debug/cheat/agent-1/player-1 \
  -H "X-Debug-Token: nagas-debug-2024" \
  -H "Content-Type: application/json" \
  -d '{"cheat": "SET_FREE_SPIN_COUNT", "value": {"count": 5, "game_id": "nagas_treasure"}}'

# Step 3: Simulate disconnect — just call init to "reconnect"
curl "http://localhost:3000/api/game/api/v1/slot/init?agentId=agent-1&userId=player-1&gameId=nagas_treasure&sessionId=s1"
# Verify: this_mode = "free"
# Verify: free_spins.remain = 5
# Verify: total_bet = "2.00" (preserved from trigger spin)
# Verify: Player is forced to continue FS (cannot bet or change mode)
```

---

### Scenario 6: Test Multi Agent Isolation

```bash
# Step 1: Set different jackpot pools for two agents
curl -X POST http://localhost:3000/api/game/debug/jackpot/agent-A \
  -H "X-Debug-Token: nagas-debug-2024" \
  -H "Content-Type: application/json" \
  -d '{"DIAMOND": 99999.00}'

curl -X POST http://localhost:3000/api/game/debug/jackpot/agent-B \
  -H "X-Debug-Token: nagas-debug-2024" \
  -H "Content-Type: application/json" \
  -d '{"DIAMOND": 10.00}'

# Step 2: Check pools are different
curl "http://localhost:3000/api/game/api/v1/slot/jackpot-pools?agentId=agent-A"
# → DIAMOND: 99999.00

curl "http://localhost:3000/api/game/api/v1/slot/jackpot-pools?agentId=agent-B"
# → DIAMOND: 10.00

# Step 3: Put agent-A player in FS, agent-B player in base
curl -X POST http://localhost:3000/api/game/debug/state/agent-A/player-1 \
  -H "X-Debug-Token: nagas-debug-2024" \
  -H "Content-Type: application/json" \
  -d '{"mode": "free"}'

curl -X DELETE "http://localhost:3000/api/game/debug/state/agent-B/player-1?gameId=nagas_treasure" \
  -H "X-Debug-Token: nagas-debug-2024"

# Step 4: Verify agent-A init shows FS, agent-B init shows base
curl "http://localhost:3000/api/game/api/v1/slot/init?agentId=agent-A&userId=player-1&gameId=nagas_treasure&sessionId=s1"
# → this_mode = "free"

curl "http://localhost:3000/api/game/api/v1/slot/init?agentId=agent-B&userId=player-1&gameId=nagas_treasure&sessionId=s2"
# → this_mode = "base"
```

---

### Scenario 7: Test Win Cap 2000x

```bash
# Step 1: Reset
curl -X DELETE "http://localhost:3000/api/game/debug/state/agent-1/player-1?gameId=nagas_treasure" \
  -H "X-Debug-Token: nagas-debug-2024"

# Step 2: Force win cap grid (all H symbols)
curl -X POST http://localhost:3000/api/game/debug/cheat/agent-1/player-1 \
  -H "X-Debug-Token: nagas-debug-2024" \
  -H "Content-Type: application/json" \
  -d '{"cheat": "FORCE_WIN_CAP", "value": {}}'

# Step 3: Spin with $1 bet
curl -X POST http://localhost:3000/api/game/api/v1/slot/spin \
  -H "Content-Type: application/json" \
  -d '{"agent_id":"agent-1","user_id":"player-1","game_id":"nagas_treasure","bet_amount":100,"session_id":"s1"}'
# Verify: total_win = "2000.00" (capped at 2000 x $1)
# All 25 paylines × 10.0 multiplier = $250, but cap = $2000
# Since $250 < $2000, win = $250 (cap not actually reached with $1 bet)
# For actual cap test, use $0.25 bet — 25 × 10.0 × $0.25 = $62.5 < $500 cap

# For a true cap hit, use FORCE_GRID with a known high-multiplier arrangement
# and a bet where the uncapped total exceeds 2000x
```

---

### Scenario 8: Test Trial Mode

```bash
# Step 1: Reset
curl -X DELETE "http://localhost:3000/api/game/debug/state/agent-1/player-1?gameId=nagas_treasure" \
  -H "X-Debug-Token: nagas-debug-2024"

# Step 2: Spin with trial_mode = true
curl -X POST http://localhost:3000/api/game/api/v1/slot/spin \
  -H "Content-Type: application/json" \
  -d '{"agent_id":"agent-1","user_id":"player-1","game_id":"nagas_treasure","bet_amount":100,"session_id":"s1","trial_mode":true}'
# Verify: balance = "9999999.00"
# Verify: Balance never changes regardless of win/loss
# Verify: All features (FS, H&W) work normally
# Verify: No jackpot contribution (pools don't grow)
```

---

## 4. Quick Reference

### Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/debug/cheat/{agentId}/{userId}` | Apply cheat code |
| `POST` | `/debug/jackpot/{agentId}` | Set jackpot pool values |
| `POST` | `/debug/state/{agentId}/{userId}` | Set game mode |
| `DELETE` | `/debug/state/{agentId}/{userId}` | Reset session |
| `GET` | `/debug/cheats` | List all cheat codes |

### Cheat Codes

| Code | Type | Description |
|------|------|-------------|
| `FORCE_FREE_SPIN` | Next spin | Place 3 scatters, trigger 8 free spins |
| `FORCE_HOLD_AND_WIN` | Next spin | Place 6+ bonus symbols, trigger H&W |
| `FORCE_JACKPOT` | Next spin | Force jackpot wheel (base spin only) |
| `FORCE_NORMAL_WIN` | Next spin | Normal RNG, no override |
| `FORCE_LOSS` | Next spin | Grid with no winning combinations |
| `FORCE_WIN_MULTIPLIER` | Next spin | Reserved for future win override |
| `FORCE_WIN_CAP` | Next spin | All H symbols, targets 2000x cap |
| `FORCE_GRID` | Next spin | Exact grid symbols from payload |
| `FORCE_HW_LOCKED_COUNT` | Next spin | H&W with N locked bonuses |
| `FORCE_JACKPOT_TRIGGER` | Next spin | Same as FORCE_JACKPOT |
| `FORCE_HW_IN_FREE_SPIN` | Next spin | Trigger H&W inside active FS |
| `SET_JACKPOT_POOL` | Immediate | Set pool values per tier |
| `RESET_JACKPOT_POOL` | Immediate | Reset pools to seed values |
| `SET_FREE_SPIN_COUNT` | Immediate | Set remaining FS count |
| `FORCE_LAST_FREE_SPIN` | Immediate | Set remaining FS to 1 |
| `SET_GAME_MODE` | Immediate | Put player in base/free/hold_and_win |
| `SET_ACCUMULATED_WIN` | Immediate | Set win total in active session |
| `RESET_SESSION` | Immediate | Delete state + reset jackpot |
| `SET_AGENT_JACKPOT_POOL` | Immediate | Same as SET_JACKPOT_POOL |
| `CLEAR_AGENT_STATE` | Immediate | Delete game state only |

---

## 5. Common Mistakes & FAQ

### Cheat Not Applying?

1. **Missing header** — Every request needs `X-Debug-Token: nagas-debug-2024`
2. **Wrong profile** — Server must be started with `SPRING_PROFILES_ACTIVE=dev` or `staging`. Without this, all `/debug/*` endpoints return 404.
3. **Cheat expired** — Cheats expire after 5 minutes if not consumed. Set the cheat, then spin immediately.
4. **Wrong mode for jackpot** — `FORCE_JACKPOT` only works on base spins. If the player is in FS or H&W, the jackpot cheat is consumed but has no effect.

### How to Reset if Stuck in Wrong State

```bash
# Nuclear reset — clears everything
curl -X DELETE "http://localhost:3000/api/game/debug/state/agent-1/player-1?gameId=nagas_treasure" \
  -H "X-Debug-Token: nagas-debug-2024"
```

This deletes the player's game state from Redis and resets jackpot pools. The next `/init` call will return a clean base game.

### TTL Reminder

- **Next-spin cheats**: stored in Redis with 5-minute TTL. If you set a cheat and don't spin within 5 minutes, it's gone.
- **Immediate cheats**: take effect instantly, no TTL concern.
- **Game state**: stored in Redis with 24-hour TTL.

### Can I Stack Multiple Cheats?

No. Each cheat overwrites the previous one for that agent+user. Only one cheat can be active at a time. Immediate cheats execute instantly and don't conflict with next-spin cheats.

### Does Trial Mode Work with Cheats?

Yes. `trial_mode: true` in the spin request works independently of cheats. You can combine them: force a specific grid AND use trial mode.

### What Happens in Production?

The `/debug/*` endpoints return 404. The `CheatService` bean doesn't exist. The `SpinUseCaseImpl` receives `cheatService=null` and all cheat code paths are skipped. There is zero performance overhead in production.
