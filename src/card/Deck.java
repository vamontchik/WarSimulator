package card;

import card.Card.Suit;
import card.Card.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Deck {
  private List<Card> cards;

  public Deck() {
    newDeck();
  }

  public Card getNextCard() {
    if (cards.size() == 0) {
      return null;
    } else {
      Card toReturn = cards.get(0);
      cards.remove(0);
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

  public void newDeck() {
    if (cards == null) {
      cards = new ArrayList<>();
    } else {
      cards.clear();
    }

    for (Suit suit : Suit.values()) {
      for (Value value : Value.values()) {
        cards.add(new Card(value, suit));
      }
    }

    Collections.shuffle(cards);
  }
}
