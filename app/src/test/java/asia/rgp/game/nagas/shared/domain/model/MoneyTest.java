package asia.rgp.game.nagas.shared.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import asia.rgp.game.nagas.shared.domain.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MoneyTest {

  @Nested
  @DisplayName("Construction & Validation")
  class ConstructionTests {

    @Test
    @DisplayName("Creates Money with positive amount")
    void positiveAmount() {
      Money money = Money.of(10.50);
      assertEquals(10.50, money.getAmount(), 0.001);
    }

    @Test
    @DisplayName("Creates Money.zero()")
    void zeroAmount() {
      Money money = Money.zero();
      assertEquals(0.0, money.getAmount(), 0.001);
      assertFalse(money.isGreaterThanZero());
    }

    @Test
    @DisplayName("Rounds to 2 decimal places")
    void roundsToTwoDecimals() {
      Money money = Money.of(10.555);
      assertEquals(10.56, money.getAmount(), 0.001);
    }

    @Test
    @DisplayName("Negative amount throws DomainException")
    void negativeAmountThrows() {
      assertThrows(DomainException.class, () -> Money.of(-1.0));
    }
  }

  @Nested
  @DisplayName("Arithmetic Operations")
  class ArithmeticTests {

    @Test
    @DisplayName("plus() adds two Money values")
    void plusOperation() {
      Money a = Money.of(5.50);
      Money b = Money.of(3.25);
      Money result = a.plus(b);
      assertEquals(8.75, result.getAmount(), 0.001);
    }

    @Test
    @DisplayName("times() multiplies by scalar (GDD payout formula)")
    void timesOperation() {
      Money bet = Money.of(1.0);
      Money payout = bet.times(2.0); // Scatter 3-match multiplier
      assertEquals(2.0, payout.getAmount(), 0.001);
    }

    @Test
    @DisplayName("times() with zero returns zero")
    void timesZero() {
      Money bet = Money.of(100.0);
      Money result = bet.times(0.0);
      assertEquals(0.0, result.getAmount(), 0.001);
    }

    @Test
    @DisplayName("times() with negative throws DomainException")
    void timesNegativeThrows() {
      Money bet = Money.of(1.0);
      assertThrows(DomainException.class, () -> bet.times(-1.0));
    }

    @Test
    @DisplayName("divide() splits evenly")
    void divideOperation() {
      Money total = Money.of(25.0);
      Money perLine = total.divide(25);
      assertEquals(1.0, perLine.getAmount(), 0.001);
    }

    @Test
    @DisplayName("divide() by zero throws DomainException")
    void divideByZeroThrows() {
      Money money = Money.of(10.0);
      assertThrows(DomainException.class, () -> money.divide(0));
    }
  }

  @Nested
  @DisplayName("Comparisons")
  class ComparisonTests {

    @Test
    @DisplayName("isGreaterThanZero() for positive amount")
    void greaterThanZero() {
      assertTrue(Money.of(0.01).isGreaterThanZero());
      assertFalse(Money.of(0.0).isGreaterThanZero());
    }

    @Test
    @DisplayName("isGreaterThan() compares two Money values")
    void greaterThan() {
      assertTrue(Money.of(10.0).isGreaterThan(Money.of(5.0)));
      assertFalse(Money.of(5.0).isGreaterThan(Money.of(10.0)));
      assertFalse(Money.of(5.0).isGreaterThan(Money.of(5.0)));
    }

    @Test
    @DisplayName("equals() and hashCode() work correctly")
    void equalsAndHashCode() {
      Money a = Money.of(5.0);
      Money b = Money.of(5.0);
      assertEquals(a, b);
      assertEquals(a.hashCode(), b.hashCode());
      assertNotEquals(a, Money.of(5.01));
    }
  }

  @Nested
  @DisplayName("GDD Payout Formulas")
  class GddFormulaTests {

    @Test
    @DisplayName("Line payout: 5-match H x $1 totalBet = $10 (GDD 2.3)")
    void linePayoutFormula() {
      Money totalBet = Money.of(1.0);
      double multiplier = 10.0; // H 5-match
      Money payout = totalBet.times(multiplier);
      assertEquals(10.0, payout.getAmount(), 0.001);
    }

    @Test
    @DisplayName("Scatter payout: 2.0 x $1 totalBet = $2 (GDD 2.3)")
    void scatterPayoutFormula() {
      Money totalBet = Money.of(1.0);
      double scatterMultiplier = 2.0;
      Money scatterPayout = totalBet.times(scatterMultiplier);
      assertEquals(2.0, scatterPayout.getAmount(), 0.001);
    }

    @Test
    @DisplayName("Win cap: min(payout, 2000 x bet) (GDD 2.3)")
    void winCapFormula() {
      Money totalBet = Money.of(2.0);
      Money winCap = totalBet.times(2000.0); // 2000 x $2 = $4000
      assertEquals(4000.0, winCap.getAmount(), 0.001);

      Money payout = Money.of(4504.0);
      Money finalPayout = payout.isGreaterThan(winCap) ? winCap : payout;
      assertEquals(4000.0, finalPayout.getAmount(), 0.001);
    }

    @Test
    @DisplayName("Buy feature cost: 70 x $1 bet = $70 (GDD 6.3/7.3)")
    void buyFeatureCost() {
      Money bet = Money.of(1.0);
      Money cost = bet.times(70.0);
      assertEquals(70.0, cost.getAmount(), 0.001);
    }

    @Test
    @DisplayName("Bonus value: totalBet x multiplier (GDD 3.2)")
    void bonusSymbolValue() {
      Money totalBet = Money.of(2.0);
      double bonusMultiplier = 15.0;
      Money bonusValue = totalBet.times(bonusMultiplier);
      assertEquals(30.0, bonusValue.getAmount(), 0.001);
    }
  }
}
