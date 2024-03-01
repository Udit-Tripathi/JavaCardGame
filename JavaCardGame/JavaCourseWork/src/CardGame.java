import java.io.*;
import java.util.*;
import java.util.concurrent.locks.*;
class CardGame {
    // Used to 'pause' the game whilst waiting for other players to finish their turn
    public static final Object gameLock = new Object();

    // Change this to the desired number of players
    public static int NUM_PLAYERS;
    // List of players
    public static List<Player> players;
    // Bool to check whether a player has won
    public static boolean playerHasWon = false;
    // ID of the winning player
    public static int winningPlayer;

    // CardGame constructor
    public CardGame(int numPlayers) {
        this.NUM_PLAYERS = numPlayers;
        this.players = new ArrayList<>();
    }


    public void initializeGame(String packFileName) {
        // Initialize players
        for (int i = 1; i <= NUM_PLAYERS; i++) {
            players.add(new Player(i, new ArrayList<>(), i));
        }

        // Read input from file
        List<Card> cards = readPackFromFile(packFileName);

        // Distribute cards to players in a round-robin fashion
        distributeCards(cards);

        // Distribute remaining cards to decks in a round-robin fashion
        distributeRemainingCards(cards);

        // Start player threads
        for (Player player : players) {
            new Thread(player).start();
        }

    }

    private List<Card> readPackFromFile(String packFileName) {
        List<Card> cards = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(packFileName))) {
            List<String> lines = new ArrayList<>();
            String line;

            // Read all lines from the file
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }

            // Check if the number of rows is valid
            int expectedRowCount = 8 * NUM_PLAYERS;
            if (lines.size() != expectedRowCount) {
                System.out.println("Invalid pack file. The file should contain exactly " + expectedRowCount + " rows.");
                System.exit(1);
            }

            // Parse lines and add cards to the list
            for (String row : lines) {
                cards.add(new Card(Integer.parseInt(row.trim())));
            }
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
            System.out.println("Invalid pack file. Please provide a valid pack file.");
            System.exit(1);
        }
        return cards;
    }
    private void distributeCards(List<Card> cards) {
        int playerIndex = 0;

        // Loops through each player in a round-robin fashion to distribute cards
        for (int i = 0; i < NUM_PLAYERS * 4; i++) {
            // Remove the top card from the list
            Card card = cards.remove(0);
            players.get(playerIndex).receiveCard(card);
            playerIndex = (playerIndex + 1) % NUM_PLAYERS;
        }
    }

    private void distributeRemainingCards(List<Card> cards) {
        int playerIndex = 0;

        // Distributing remaining cards to each deck in a round-robin fashion
        for (Card card : cards) {
            players.get(playerIndex).addToDeck(card);
            playerIndex = (playerIndex + 1) % NUM_PLAYERS;
        }
    }

    public void startGame() {
        // Notify the first player to start the game
        synchronized (gameLock) {
            gameLock.notify();
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Asking user for the number of players to play with
        System.out.print("Enter the number of players (n): ");
        int numPlayers = scanner.nextInt();

        if (numPlayers <= 0) {
            System.out.println("Number of players should be a positive integer.");
            System.exit(1);
        }

        // Asking user for the location of the pack to read from
        System.out.print("Enter the file location for the deck: ");
        String packFileName = scanner.next();

        // Starting game
        CardGame cardGame = new CardGame(numPlayers);
        cardGame.initializeGame(packFileName);
        cardGame.startGame();

        scanner.close();
    }
}