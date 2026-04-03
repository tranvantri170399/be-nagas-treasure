package asia.rgp.game.nagas.shared.domain.model;

import asia.rgp.game.nagas.shared.domain.exception.DomainException;
import asia.rgp.game.nagas.shared.error.ErrorCode;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Objects;

public final class Money implements Serializable {

  private final double amount;

  @JsonCreator
  public Money(@JsonProperty("amount") double amount) {
    if (amount < 0) {
      throw new DomainException("Money amount cannot be negative", ErrorCode.INVALID_ARGUMENT);
    }
    this.amount = Math.round(amount * 100.0) / 100.0;
  }

  public static Money of(double amount) {
    return new Money(amount);
  }

  public static Money zero() {
    return new Money(0.0);
  }

  public double getAmount() {
    return amount;
  }

  public Money plus(Money other) {
    Objects.requireNonNull(other);
    return new Money(this.amount + other.amount);
  }

  public Money times(double multiplier) {
    if (multiplier < 0) {
      throw new DomainException("Multiplier cannot be negative", ErrorCode.INVALID_ARGUMENT);
    }
    return new Money(this.amount * multiplier);
  }

  public Money divide(int divisor) {
    if (divisor <= 0) {
      throw new DomainException("Divisor must be greater than zero", ErrorCode.INVALID_ARGUMENT);
    }
    return new Money(this.amount / (double) divisor);
  }

  @JsonIgnore
  public boolean isGreaterThanZero() {
    return this.amount > 0;
  }

  @JsonIgnore
  public boolean isGreaterThan(Money other) {
    Objects.requireNonNull(other);
    return this.amount > other.amount;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Money money = (Money) o;
    return Double.compare(money.amount, amount) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(amount);
  }

  @Override
  public String toString() {
    return String.format("%.2f", amount);
  }
}
