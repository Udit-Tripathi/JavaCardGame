import java.io.*;
import java.util.*;
import java.util.concurrent.locks.*;
class Player implements Runnable {
    // ID of the player
    private int playerId;
    // Hand of the player containing a list of Card Objects
    private List<Card> hand;
    // Deck next to player containing a list of Card Objects
    private List<Card> deck;
    // A player's preferred card value e.g. player 1 will prefer a card of value 1
    private int preferredDenomination;
    // Used to write text to an output stream allowing the pack to be read from
    private BufferedWriter outputFileWriter;

    // Player constructor
    public Player(int playerId, List<Card> deck, int preferredDenomination) {
        this.playerId = playerId;
        this.hand = Collections.synchronizedList(new ArrayList<>());
        this.deck = deck;
        this.preferredDenomination = preferredDenomination;


        try {
            this.outputFileWriter = new BufferedWriter(new FileWriter("player" + playerId + "_output.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to add a card to a player's hand
    public void receiveCard(Card card) {
        hand.add(card);
    }

    // Method to add a card to a player's deck
    public void addToDeck(Card card){
        deck.add(card);
    }

    // Method to draw a card during a player's turn from their respective deck
    private void drawCard() {
        Card drawnCard = deck.remove(0);
        hand.add(drawnCard);

        // Print action to output file
        writeToFile("draws a " + drawnCard.getValue() + " from deck " + (playerId));
    }

    // Method to remove a card from a player's hand to the next player's deck
    private void discardCard() {
        // Discard a random card from the player's hand
        if (!hand.isEmpty()) {
            Random random = new Random();
            int randomIndex = random.nextInt(hand.size());
            while(hand.get(randomIndex).getValue() == preferredDenomination){
                randomIndex = random.nextInt(hand.size());
            }
            Card cardToDiscard = hand.get(randomIndex);

            if (cardToDiscard != null) {
                hand.remove(cardToDiscard);
                int nextPlayerIndex = (playerId % CardGame.NUM_PLAYERS) + 1;

                // Update the next player's deck
                CardGame.players.get(nextPlayerIndex - 1).addToDeck(cardToDiscard);

                // Print action to output file
                writeToFile("discards a " + cardToDiscard.getValue() + " to player " + nextPlayerIndex);
            }
        }
    }
    // Method to more easily write to file reducing redundancy in code
    private void writeToFile(String action) {
        try {
            outputFileWriter.write("player " + playerId + " " + action);
            outputFileWriter.newLine();
            outputFileWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // Similar to the writeToFile method but only used when informing other player's that a player has won
    private void informWin(String action) {
        try {
            outputFileWriter.write(action);
            outputFileWriter.newLine();
            outputFileWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to print a player's initial hand to the output file
    private void printInitialHand() {
        StringBuilder initialHandStr = new StringBuilder(playerId + " initial hand:");
        for (Card card : hand) {
            initialHandStr.append(" ").append(card.getValue());
        }
        writeToFile(initialHandStr.toString());
    }
    // Method to write the contents of each deck at the end of the game to a separate output file
    private static void writeDeckOutputFile(int deckNumber, List<Card> deck) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("deck" + deckNumber + "_output.txt"))) {
            writer.write("deck" + deckNumber + " contents:");
            for (Card card : deck) {
                writer.write(" " + card.getValue());
            }
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method used to check for a win e.g. when a player has 4 cards of the same face value
    private boolean checkForWin() {
        // Find the maximum card value in the hand
        int maxCardValue = 0;
        for (Card card : hand) {
            maxCardValue = Math.max(maxCardValue, card.getValue());
        }

        // Initialize the count array with size equal to the maximum card value + 1
        int[] count = new int[maxCardValue + 1];

        for (Card card : hand) {
            count[card.getValue()]++;
            if (count[card.getValue()] == 4) {
                return true;
            }
        }

        return false;
    }


    // Main game method that runs the logic of the game, loops for each player turn until a player has won
    @Override
    public void run() {
        // Print initial hand to output file
        printInitialHand();

        // Check for immediate win
        if (checkForWin()) {
            CardGame.playerHasWon = true;
            CardGame.winningPlayer = playerId;
            System.out.println("player " + playerId + " wins");
            writeToFile("wins");
            writeToFile("exits");
            writeToFile("final hand:" + handToString());
            closeFileWriter();
            writeDeckOutputFile(playerId, deck);
            synchronized (CardGame.gameLock) {
                CardGame.gameLock.notifyAll();

            }

            return;

        }
        while (!CardGame.playerHasWon) {
            synchronized (CardGame.gameLock) {
                // Player's turn logic

                drawCard();
                discardCard();

                // Print current hand to output file
                writeToFile("current hand is " + handToString());

                // Check for win condition
                if (checkForWin()) {
                    CardGame.playerHasWon = true;
                    CardGame.winningPlayer = playerId;
                    System.out.println("player " + playerId + " wins");
                    writeToFile("wins");
                    writeToFile("exits");
                    writeToFile("final hand:" + handToString());
                    closeFileWriter();
                    CardGame.gameLock.notifyAll();
                    writeDeckOutputFile(playerId, deck);

                    return;
                }
                // Notify the game lock after the player's turn is complete
                CardGame.gameLock.notifyAll();
                try {
                    // Wait for the next player to finish their turn
                    CardGame.gameLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        // Check if another player has won and informed
        if (CardGame.winningPlayer != playerId) {
            informWin("player " + CardGame.winningPlayer + " has informed player " + playerId + " that player " + CardGame.winningPlayer + " has won");
            writeToFile("exits");
            writeToFile("hand:" + handToString());
            closeFileWriter(); // Close the file before exiting
        }
        writeDeckOutputFile(playerId, deck);
    }

    // Method to return the current hand of the player
    private String handToString() {
        StringBuilder handStr = new StringBuilder();
        for (Card card : hand) {
            handStr.append(card.getValue()).append(" ");
        }
        return handStr.toString().trim();
    }

    // Method to stop writing to the output file
    private void closeFileWriter() {
        try {
            outputFileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

