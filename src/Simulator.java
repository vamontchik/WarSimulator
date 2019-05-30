import card.Card;
import card.Deck;
import card.Hand;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.System.exit;

public class Simulator {
  private Hand handOne;
  private Hand handTwo;
  private boolean handOneRoundWon;
  private int roundsPlayed;
  private final int threadId;

  private static final Properties propertiesFile;
  private static final int TOTAL_GAMES;
  private static final int DECK_SIZE;
  private static final int THREAD_POOL_SIZE;

  private static final DecimalFormat format;
  private static final List<Integer> allGameRounds;
  private static final Map<Card, List<Integer>> cardToRoundWins;
  private static final ReentrantLock lock;
  private static int thread_id;

  static {
    lock = new ReentrantLock();
    cardToRoundWins = new TreeMap<>();
    allGameRounds = new ArrayList<>();
    thread_id = 0;
    format = new DecimalFormat("##.00000");

    InputStream input = null;
    try {
      input = new FileInputStream("src/resources.properties");
    } catch (IOException e) {
      e.printStackTrace();
      exit(-1);
    }

    propertiesFile = new Properties();

    try {
      propertiesFile.load(input);
    } catch (IOException e) {
      e.printStackTrace();
      exit(-1);
    }

    TOTAL_GAMES = Integer.parseInt(propertiesFile.getProperty("total_games"));
    DECK_SIZE = Integer.parseInt(propertiesFile.getProperty("deck_size"));
    THREAD_POOL_SIZE = Integer.parseInt(propertiesFile.getProperty("thread_pool_size"));
  }

  public Simulator() {
    lock.lock();
    threadId = thread_id;
    ++thread_id;
    lock.unlock();
  }

  public void setupGame() {
    Deck deck = new Deck();
    List<Card> firstGive = deck.getCardAmount(DECK_SIZE / 2);
    List<Card> secondGive = deck.getCardAmount(DECK_SIZE / 2);
    handOne = new Hand(firstGive);
    handTwo = new Hand(secondGive);

    handOneRoundWon = false;
    roundsPlayed = 0;
  }

  public void playGame() {
    while (true) {
      handOneRoundWon = false;
      boolean success = playRound(false);
      if (!success) { break; }
    }
  }

  public boolean playRound(boolean isWar) {
    Card handOnePlay = handOne.playCard();
    if (handOnePlay == null) { return false; }

    Card handTwoPlay = handTwo.playCard();
    if (handTwoPlay == null) { return false; }

    List<Card> cardsPlayed = new ArrayList<>();
    cardsPlayed.add(handOnePlay);
    cardsPlayed.add(handTwoPlay);

    int compare = compareTwoCardsByValue(handOnePlay, handTwoPlay);
    if (compare == 0) {
      /* WAR !!! */

      if (handOne.getHandSize() < 2) { return false; }
      if (handTwo.getHandSize() < 2) { return false; }

      cardsPlayed.addAll(handOne.getCardAmount(2));
      cardsPlayed.addAll(handTwo.getCardAmount(2));

      boolean tried = playRound(true);
      if (!tried) { return false; }

      // unstacking: obtain cards played on this level (2 fodder + play, potentially)
      if (handOneRoundWon) {
        handOne.acceptCards(cardsPlayed);
      } else {
        handTwo.acceptCards(cardsPlayed);
      }

    } else if (compare > 0) {
      /* handOne has the higher card! */

      // special cases:
      //  1) two beats ace
      //  2) three beats king
      if (specialCaseHandTwo(handOnePlay, handTwoPlay)) {
        handTwo.acceptCards(cardsPlayed);
        handTwoPlay.incrementRoundWins();
        handOnePlay.incrementRoundLoss();
        handOneRoundWon = false;
      } else {
        handOne.acceptCards(cardsPlayed);
        handOnePlay.incrementRoundWins();
        handTwoPlay.incrementRoundLoss();
        handOneRoundWon = true;
      }

    } else {
      /* handOne has the smaller card! */

      // special cases:
      //  1) two beats ace
      //  2) three beats king
      if (specialCaseHandOne(handOnePlay, handTwoPlay)) {
        handOne.acceptCards(cardsPlayed);
        handOnePlay.incrementRoundWins();
        handTwoPlay.incrementRoundLoss();
        handOneRoundWon = true;
      } else {
        handTwo.acceptCards(cardsPlayed);
        handTwoPlay.incrementRoundWins();
        handOnePlay.incrementRoundLoss();
        handOneRoundWon = false;
      }
    }

    if (!isWar) { ++roundsPlayed; }
    return true;
  }

  private boolean specialCaseHandOne(Card handOnePlay, Card handTwoPlay) {
    return (handOnePlay.getValue() == Card.Value.SIX && handTwoPlay.getValue() == Card.Value.ACE) ||
           (handOnePlay.getValue() == Card.Value.SEVEN && handTwoPlay.getValue() == Card.Value.KING);
  }

  private boolean specialCaseHandTwo(Card handOnePlay, Card handTwoPlay) {
    return (handTwoPlay.getValue() == Card.Value.SIX && handOnePlay.getValue() == Card.Value.ACE) ||
           (handTwoPlay.getValue() == Card.Value.SEVEN && handOnePlay.getValue() == Card.Value.KING);
  }

  private int compareTwoCardsByValue(Card handOnePlay, Card handTwoPlay) {
    return (handOnePlay.getValue().getValue() - handTwoPlay.getValue().getValue());
  }

  public void saveStats() {
    lock.lock();
    allGameRounds.add(roundsPlayed);
    fillMap(handOne);
    fillMap(handTwo);
    lock.unlock();
  }

  private void fillMap(Hand hand) {
    for (Card card : hand.getHand()) {
      if (cardToRoundWins.containsKey(card)) {
        List<Integer> old = cardToRoundWins.get(card);
        old.set(0, old.get(0) + card.getRoundWins());
        old.set(1, old.get(1) + card.getRoundLoss());
        cardToRoundWins.replace(card, old);
      } else {
        int newWins = card.getRoundWins();
        int newLoss = card.getRoundLoss();
        List<Integer> newCount = new ArrayList<>(Arrays.asList(newWins, newLoss));
        cardToRoundWins.put(card, newCount);
      }
    }
  }

  public static void printStats() {
    System.out.println("\nSTATS\n");

    double sum = allGameRounds.stream().reduce(0, Integer::sum);
    System.out.println("Total rounds played: " + sum);

    System.out.println("Total games played: " + TOTAL_GAMES);

    System.out.println("Average rounds per game: " + sum / allGameRounds.size());

    Map<Integer, Double> valuePercentagesMean = new TreeMap<>();
    int count = 0;
    int numberAt = 6;
    double sumPercents = 0.0;
    for (Map.Entry<Card, List<Integer>> entryInMap : cardToRoundWins.entrySet()) {
      Card card = entryInMap.getKey();
      List<Integer> entry = entryInMap.getValue();
      int roundsWon = entry.get(0);
      int roundsLost = entry.get(1);

      String nameAndPrompt = card + " win percentage";
      double percent = (roundsWon * 1.0)/(roundsWon + roundsLost) * 100;
      System.out.println(nameAndPrompt + ":" + spacesFor(nameAndPrompt, percent) + format.format(percent) + "%");

      sumPercents += percent;
      ++count;
      if (count == Card.Suit.values().length) {
        valuePercentagesMean.put(numberAt, sumPercents / Card.Suit.values().length);
        count = 0;
        sumPercents = 0.0;
        ++numberAt;
      }
    }


    // save averages for graph
    File file;
    FileWriter writer = null;
    try {
      file = new File("save.txt");
      writer = new FileWriter(file);
    } catch (IOException e) {
      e.printStackTrace();
    }
    for (Map.Entry<Integer, Double> entry : valuePercentagesMean.entrySet()) {
      System.out.println("For value " + entry.getKey() + ", average percentage: " + entry.getValue() + "%");

      if (writer != null) {
        try {
          writer.write(entry.getKey() + "\n" + entry.getValue() + "\n");
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    if (writer != null) {
      try {
        writer.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private static String spacesFor(String nameAndPrompt, double percent) {
    final int BASE_LENGTH = 35;
    int length = BASE_LENGTH - nameAndPrompt.length();
    final double TOLERANCE = 0.000001;
    if (percent - 10.0 < TOLERANCE) { ++length; }
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < length; ++i) {
      builder.append(' ');
    }
    return builder.toString();
  }

  public int getThreadNumber() {
    return threadId;
  }

  public static void main(String[] args) {
    ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    Runnable toRun = () -> {
      Simulator simulator = new Simulator();
      final int FULL = TOTAL_GAMES / THREAD_POOL_SIZE;
      final int STEP = TOTAL_GAMES / 100;
      for (int i = 0; i < FULL; ++i) {
        if ((i+1) % STEP == 0) { System.out.println("[Thread " + simulator.getThreadNumber() + "] Starting Game: " + (i+1)); }
        simulator.setupGame();
        simulator.playGame();
        simulator.saveStats();
        if ((i+1) % STEP == 0) { System.out.println("[Thread " + simulator.getThreadNumber() + "] Ending Game: " + (i+1)); }
      }
    };

    // execute THREAD_POOL_SIZE amount of the Runnable
    for (int i = 0; i < THREAD_POOL_SIZE; ++i) {
      executor.execute(toRun);
    }

    // prompts so that when the last process is done,
    // then will shut down the executor
    executor.shutdown();

    // "barrier" , with spurious wake-up check with while loop
    while (!executor.isTerminated()) {
      try {
        executor.awaitTermination(-1, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    // print out stats at the end
    Simulator.printStats();
  }
}
