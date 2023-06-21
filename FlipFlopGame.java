import java.awt.*;
// import java.awt.event.*;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
// import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FlipFlopGame extends JFrame {
    private final int GRID_ROWS = 4;
    private final int GRID_COLS = 5;
    private final int NUM_TILES = GRID_ROWS * GRID_COLS;
    private final int NUM_PAIRS = NUM_TILES / 2;

    private JButton[] buttons;
    private String[] alphabet;
    private int[] tileIndices;
    private int firstTileIndex;
    private int numTilesFlipped;
    private int numTurns;
    private boolean isClickable;
    private Timer gameTimer;
    private int seconds;
    private int minutes;
    private JLabel timerLabel;
    private JLabel turnsLabel;
    private JTextField nameField;
    private JButton startButton;

    public FlipFlopGame() {
        super("Flip Flop Memory Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel namePanel = new JPanel(new FlowLayout());
        JLabel nameLabel = new JLabel("Name:");
        nameField = new JTextField(10);
        startButton = new JButton("Start");
        startButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String playerName = nameField.getText();
                if (!playerName.isEmpty()) {
                    nameLabel.setVisible(false);
                    startGame(playerName);
                } else {
                    JOptionPane.showMessageDialog(FlipFlopGame.this, "Please enter your name.");
                }
            }
        });

        namePanel.add(nameLabel);
        namePanel.add(nameField);
        namePanel.add(startButton);

        add(namePanel, BorderLayout.SOUTH);

        setSize(500, 500);
        setLocationRelativeTo(null);
        setResizable(false);
        setVisible(true);
    }

    private void startGame(String playerName) {
        nameField.setVisible(false);
        startButton.setVisible(false);

        alphabet = loadImagesFromDirectory("images");

        tileIndices = new int[NUM_TILES];
        buttons = new JButton[NUM_TILES];
        numTilesFlipped = 0;
        numTurns = 0;
        isClickable = true;
        seconds = 0;
        minutes = 0;

        initializeTiles();
        initializeTimer();

        JLabel nameLabel = new JLabel("Player: " + playerName, SwingConstants.CENTER);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 16));
        JPanel topPanel = new JPanel(new GridLayout(2, 2));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        topPanel.add(timerLabel);
        topPanel.add(turnsLabel);
        topPanel.add(nameLabel);
        add(topPanel, BorderLayout.NORTH);

        JPanel gridPanel = new JPanel(new GridLayout(GRID_ROWS, GRID_COLS));
        for (int i = 0; i < NUM_TILES; i++) {
            gridPanel.add(buttons[i]);
        }
        add(gridPanel, BorderLayout.CENTER);

        revalidate();
    }

    private String[] loadImagesFromDirectory(String directoryPath) {
        File directory = new File(directoryPath);
        File[] imageFiles = directory.listFiles((dir, name) -> name.endsWith(".png"));
        List<String> imagePaths = new ArrayList<>();
        for (File file : imageFiles) {
            imagePaths.add(file.getPath());
        }
        Collections.shuffle(imagePaths);
        return imagePaths.toArray(new String[0]);
    }

    private void initializeTiles() {
        for (int i = 0; i < NUM_PAIRS; i++) {
            tileIndices[i] = i;
            tileIndices[i + NUM_PAIRS] = i;
        }

        // Shuffle the tile indices
        for (int i = NUM_TILES - 1; i > 0; i--) {
            int j = (int) (Math.random() * (i + 1));
            int temp = tileIndices[i];
            tileIndices[i] = tileIndices[j];
            tileIndices[j] = temp;
        }

        for (int i = 0; i < NUM_TILES; i++) {
            buttons[i] = new JButton();
            buttons[i].setBackground(Color.BLUE);
            buttons[i].addActionListener(new TileActionListener(i));
        }
    }

    private void initializeTimer() {
        timerLabel = new JLabel("Time: 00:00", SwingConstants.CENTER);
        timerLabel.setFont(new Font("Arial", Font.BOLD, 16));

        turnsLabel = new JLabel("Turns: 0", SwingConstants.CENTER);
        turnsLabel.setFont(new Font("Arial", Font.BOLD, 16));

        gameTimer = new Timer(1000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                seconds++;
                if (seconds == 60) {
                    minutes++;
                    seconds = 0;
                }
                updateTimerLabel();
            }
        });
        gameTimer.start();
    }

    private void updateTimerLabel() {
        String timeString = String.format("%02d:%02d", minutes, seconds);
        timerLabel.setText("Time: " + timeString);
    }

    private void updateTurnsLabel() {
        turnsLabel.setText("Turns: " + numTurns);
    }

    private void flipTile(int tileIndex) {
        String imagePath = alphabet[tileIndices[tileIndex]];
        ImageIcon icon = new ImageIcon(imagePath);
        Image image = icon.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
        buttons[tileIndex].setIcon(new ImageIcon(image));
        buttons[tileIndex].setBackground(Color.YELLOW);
    }

    private void hideTile(int tileIndex) {
        buttons[tileIndex].setIcon(null);
        buttons[tileIndex].setBackground(Color.BLUE);
    }

    private void disableTile(int tileIndex) {
        buttons[tileIndex].setEnabled(false);
    }

    private void checkTiles(int tileIndex) {
        if (numTilesFlipped == 0) {
            firstTileIndex = tileIndex;
            numTilesFlipped++;
        } else if (numTilesFlipped == 1 && firstTileIndex != tileIndex) {
            isClickable = false;
            flipTile(tileIndex);
            numTilesFlipped++;
            numTurns++;
            updateTurnsLabel();

            String imagePath1 = alphabet[tileIndices[firstTileIndex]];
            String imagePath2 = alphabet[tileIndices[tileIndex]];

            if (imagePath1.equals(imagePath2)) {
                disableTile(firstTileIndex);
                disableTile(tileIndex);
                numTilesFlipped = 0;
                isClickable = true;
                checkWin();
            } else {
                Thread timerThread = new Thread(() -> {
                    try {
                        Thread.sleep(1000); // Introduce a 1-second delay
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }

                    SwingUtilities.invokeLater(() -> {
                        hideTile(firstTileIndex);
                        hideTile(tileIndex);
                        numTilesFlipped = 0;
                        isClickable = true;
                    });
                });
                timerThread.start();
            }
        }
    }

    private void checkWin() {
        for (int i = 0; i < NUM_TILES; i++) {
            if (buttons[i].isEnabled()) {
                return;
            }
        }
        gameTimer.stop();
        String playerName = nameField.getText();
        String timeTaken = timerLabel.getText();
        String numTurnsString = Integer.toString(numTurns);
        JOptionPane.showMessageDialog(this,
                "Congratulations, " + playerName + "! You won!\nTime taken: " + timeTaken + "\nNumber of Turns: "
                        + numTurnsString);
        saveToDatabase(playerName, timeTaken, numTurnsString);
        System.exit(0);
    }

    private void saveToDatabase(String playerName, String timeTaken, String numTurns) {
        try {
            String url = "jdbc:ucanaccess://FF_db.accdb";
            Connection conn = DriverManager.getConnection(url);

            String query = "INSERT INTO FlipFlop (Name, Top_Record, Turns) VALUES (?, ?, ?)";
            PreparedStatement statement = conn.prepareStatement(query);
            statement.setString(1, playerName);
            statement.setString(2, timeTaken);
            statement.setString(3, numTurns);
            statement.executeUpdate();

            statement.close();
            conn.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private class TileActionListener implements ActionListener {
        private int index;

        public TileActionListener(int index) {
            this.index = index;
        }

        public void actionPerformed(ActionEvent e) {
            if (isClickable && buttons[index].isEnabled()) {
                flipTile(index);
                checkTiles(index);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new FlipFlopGame();
            }
        });
    }
}
