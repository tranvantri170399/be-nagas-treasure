package asia.rgp.game.nagas.modules.slot.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SlotResultResponse {
  private String type;
  private DataContent data;

  @Getter
  @Builder
  public static class DataContent {
    private RoundContent round;
    private ControlContent control;
  }

  @Getter
  @Builder
  public static class RoundContent {
    private Map<String, Object> transactionId;
    private Map<String, Object> parentId;
    private String currency;
    private String totalBet;
    private String totalWin;
    private boolean endsSuperround;
    private String type;
    private String createdAt;
    private String roundId;
    private String parentRoundId;
    private String thisMode;
    private String nextMode;
    private String bonusSpinCampaignId;
    private List<SubGame> subgames;
    private Map<String, Object> promoInfo;
    private ResultDetails result;
  }

  @Getter
  @Builder
  public static class ResultDetails {
    private Map<String, Object> features;
    private String totalWin;
    private Map<String, Object> events;

    private String id;
    private int lines;
    private String nextMode;
    private int round;
    private String currency;
    private String totalBet;
    private SuperRound superRound;
    private String sessionId;
    private List<StageContent> stages;
    private String thisMode;
    private boolean displayCoinValues;
  }

  @Getter
  @Builder
  public static class StageContent {
    private List<WinDetail> wins;
    private String totalWin;
    private int[][] screen;
    private int stage;
    private Map<String, Object> events;
  }

  @Getter
  @Builder
  public static class WinDetail {
    private int symbol;
    private int occurs;
    private List<Integer> matching;
    private String win;
    private String mode;
    private List<int[]> positions;
    private String payline;
    private String type;
  }

  @Getter
  @Builder
  public static class SuperRound {
    private boolean buyFeature;
    private String totalWin;
    private int roundOffset;
    private boolean ends;
    private String betSize;
    private String totalBet;
    private String totalGambleBet;
    private String totalGambleWin;
    private Map<String, Object> parentId;
  }

  @Getter
  @Builder
  public static class ControlContent {
    private String balance;
  }

  @Getter
  @Builder
  public static class SubGame {
    private String win;
    private String bet;
    private int id;
    private int weight;
    private String type;
  }
}
