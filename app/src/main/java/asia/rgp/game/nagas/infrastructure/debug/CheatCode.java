package asia.rgp.game.nagas.infrastructure.debug;

public enum CheatCode {
  // 1. Force Game Mode (next spin only)
  FORCE_FREE_SPIN,
  FORCE_HOLD_AND_WIN,
  FORCE_JACKPOT,
  FORCE_NORMAL_WIN,
  FORCE_LOSS,

  // 2. Force Specific Outcomes
  FORCE_WIN_MULTIPLIER,
  FORCE_WIN_CAP,
  FORCE_GRID,
  FORCE_HW_LOCKED_COUNT,

  // 3. Force Jackpot Scenarios
  SET_JACKPOT_POOL,
  FORCE_JACKPOT_TRIGGER,
  RESET_JACKPOT_POOL,

  // 4. Force Free Spin Scenarios
  SET_FREE_SPIN_COUNT,
  FORCE_HW_IN_FREE_SPIN,
  FORCE_LAST_FREE_SPIN,

  // 5. Force Session State
  SET_GAME_MODE,
  SET_ACCUMULATED_WIN,
  RESET_SESSION,

  // 6. Multi Agent
  SET_AGENT_JACKPOT_POOL,
  CLEAR_AGENT_STATE
}
