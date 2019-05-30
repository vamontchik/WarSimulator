package card;

public class Card implements Comparable<Card> {
  public enum Value {
    SIX(6), SEVEN(7), EIGHT(8), NINE(9), TEN(10), JACK(11), QUEEN(12), KING(13), ACE(14);

    private int value;

    Value(int value) {
      this.value = value;
    }

    public int getValue() {
      return value;
    }
  }

  public enum Suit {
    HEARTS(1), DIAMONDS(2), SPADES(3), CLUBS(4);

    private int value;

    Suit(int value) {
      this.value = value;
    }

    public int getValue() {
      return value;
    }
  }

  private Value value;
  private Suit suit;
  private int roundWins;
  private int roundLoss;

  public Card(Value value, Suit suit) {
    this.value = value;
    this.suit = suit;
    this.roundWins = 0;
    this.roundLoss = 0;
  }

  public int getRoundWins() {
    return roundWins;
  }

  public int getRoundLoss() {
    return roundLoss;
  }

  public void incrementRoundWins() {
    ++roundWins;
  }

  public void incrementRoundLoss() {
    ++roundLoss;
  }

  public Value getValue() {
    return value;
  }

  public Suit getSuit() {
    return suit;
  }

  // first compare by value, then if equal, by suit!
  @Override
  public int compareTo(Card o) {
    int diffValue = value.getValue() - o.value.getValue();
    if (diffValue != 0) { return diffValue; }
    return suit.getValue() - o.suit.getValue();
  }

  @Override
  public int hashCode() {
    int result = 17;
    result = 31 * result + value.hashCode();
    result = 31 * result + suit.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) { return true; }
    if (!(o instanceof Card)) { return false; }

    Card otherCard = (Card) o;
    return value.equals(otherCard.value) && suit.equals(otherCard.suit);
  }

  @Override
  public String toString() {
    return value.toString() + " of " + suit.toString();
  }
}
