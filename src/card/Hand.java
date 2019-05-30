package card;

import java.util.ArrayList;
import java.util.List;

public class Hand {
  private List<Card> hand;

  public Hand(List<Card> initialHand) {
    this.hand = new ArrayList<>(initialHand);
  }

  public Card playCard() {
    if (hand.size() == 0) { return null; }
    int index = (int)(Math.random() * hand.size());
    Card toPlay = hand.get( index );
    hand.remove(index);
    return toPlay;
  }

  public void acceptCards(List<Card> cards) {
    hand.addAll(cards);
  }

  public int getHandSize() {
    return hand.size();
  }

  public List<Card> getHand() {
    return hand;
  }

  private Card getNextCard() {
    if (hand.size() == 0) {
      return null;
    } else {
      Card toReturn = hand.get(0);
      hand.remove(0);
      return toReturn;
    }
  }

  public List<Card> getCardAmount(int amount) {
    List<Card> toReturn = new ArrayList<>();
    for (int i = 0; i < amount; ++i) {
      toReturn.add(getNextCard());
    }
    return toReturn;
  }
}
