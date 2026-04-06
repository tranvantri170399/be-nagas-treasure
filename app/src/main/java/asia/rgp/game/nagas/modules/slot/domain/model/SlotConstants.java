package asia.rgp.game.nagas.modules.slot.domain.model;

public class SlotConstants {
  public static final String MODE_BASE = "base";
  public static final String MODE_FREE = "free";
  public static final String MODE_HOLD_AND_WIN = "holdAndWin";

  public static final String FEATURE_FREE_SPINS = "freeSpins";
  public static final String FEATURE_HOLD_AND_WIN = "holdAndWin";
  public static final String FEATURE_JACKPOT = "progressiveJackpot";

  public static final String TYPE_CASH = "CASH";
  public static final String TYPE_MINI = "MINI";
  public static final String TYPE_MAJOR = "MAJOR";

  public static final double BUY_FREE_SPINS_COST = 70.0;
  public static final double BUY_HOLD_AND_WIN_COST = 70.0;

  public static final double[] ALLOWED_BET_STEPS = {
    0.25, 0.5, 0.75, 1.0, 1.5, 2.0, 2.5, 5.0, 7.5, 10.0, 15.0, 20.0, 25.0, 50.0, 75.0, 100.0, 150.0,
    200.0, 250.0
  };

  public static final double TRIAL_MODE_BALANCE = 9999999.0;

  public static final int SYMBOL_A = 1;
  public static final int SYMBOL_B = 2;
  public static final int SYMBOL_C = 3;
  public static final int SYMBOL_D = 4;

  public static final int DEFAULT_SYMBOL_SCATTER = 9;
  public static final int DEFAULT_SYMBOL_WILD = 10;

  public static final int SYMBOL_MAJOR = 11;
  public static final int SYMBOL_MINI = 12;
  public static final int SYMBOL_CASH = 13;

  public static final int SYMBOL_STACKED_WILD = 14;

  public static final int SYMBOL_BLANK = 0;

  public static final String JACKPOT_DIAMOND = "DIAMOND";
  public static final String JACKPOT_RUBY = "RUBY";
  public static final String JACKPOT_EMERALD = "EMERALD";
  public static final String JACKPOT_SAPPHIRE = "SAPPHIRE";

  public static final String OVERLAY_GLOWING_RING = "GLOWING_RING";
}
