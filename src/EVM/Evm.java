package EVM;

import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.io.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

class EVM {
    private static String currentDB = "evm_db";
    private static final String DB_URL_PREFIX = "jdbc:mysql://localhost:3306/";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "NITIN_00";
    private static Map<String, String> pastElections = new HashMap<String, String>();
    private static final String LAST_ELECTION_FILE = "last_election.txt";

    private static JFrame frame;
    private static JTextArea outputArea;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                loadLastElection();
                createAndShowGUI();
                initializeDatabase();
                if (currentDB.equals("evm_db")) {
                    JOptionPane.showMessageDialog(frame, "No election found. Please set up a new election to start.", "Welcome", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception e) {
                System.err.println("Unexpected error in main: " + e.getMessage());
                JOptionPane.showMessageDialog(null, "Application error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        });
    }

    private static void loadLastElection() {
        try {
            File file = new File(LAST_ELECTION_FILE);
            if (file.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String lastDB = reader.readLine();
                if (lastDB != null && !lastDB.trim().isEmpty()) {
                    currentDB = lastDB.trim();
                }
                reader.close();
                Connection conn = DriverManager.getConnection(DB_URL_PREFIX + "mysql", DB_USER, DB_PASSWORD);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SHOW DATABASES LIKE 'evm_db_%'");
                while (rs.next()) {
                    String dbName = rs.getString(1);
                    if (!dbName.equals(currentDB)) {
                        pastElections.put("Election " + pastElections.size(), dbName);
                    }
                }
                conn.close();
            }
        } catch (Exception e) {
            System.err.println("Error loading last election: " + e.getMessage());
            JOptionPane.showMessageDialog(null, "Failed to load last election data: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void saveLastElection() {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(LAST_ELECTION_FILE));
            writer.write(currentDB);
            writer.close();
        } catch (IOException e) {
            System.err.println("Error saving last election: " + e.getMessage());
        }
    }

    private static String getDBUrl() {
        return DB_URL_PREFIX + currentDB;
    }

    private static void createAndShowGUI() {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
        }

        frame = new JFrame("Electronic Voting Machine");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setSize(500, 450);
        frame.setMinimumSize(new Dimension(500, 450));
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout(10, 10));
        frame.getContentPane().setBackground(new Color(240, 248, 255));

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                confirmExit();
            }
        });

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(240, 248, 255));
        headerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel header = new JLabel("Electronic Voting Machine", SwingConstants.CENTER);
        header.setFont(new Font("Arial", Font.BOLD, 24));
        header.setForeground(new Color(25, 25, 112));
        headerPanel.add(header, BorderLayout.CENTER);
        frame.add(headerPanel, BorderLayout.NORTH);

        outputArea = new JTextArea(10, 40);
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        outputArea.setBackground(new Color(245, 245, 220));
        outputArea.setForeground(new Color(47, 79, 79));
        outputArea.setBorder(BorderFactory.createLineBorder(new Color(176, 224, 230), 2));
        frame.add(new JScrollPane(outputArea), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(new Color(240, 248, 255));
        buttonPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        buttonPanel.setLayout(new GridLayout(1, 4, 10, 0));

        JButton voteButton = createStyledButton("Cast Vote", new Color(60, 179, 113));
        JButton resultsButton = createStyledButton("View Results", new Color(70, 130, 180));
        JButton newElectionButton = createStyledButton("New Election", new Color(255, 165, 0));
        JButton exitButton = createStyledButton("Exit", new Color(220, 20, 60));

        buttonPanel.add(voteButton);
        buttonPanel.add(resultsButton);
        buttonPanel.add(newElectionButton);
        buttonPanel.add(exitButton);
        frame.add(buttonPanel, BorderLayout.SOUTH);

        voteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                castVoteGUI();
            }
        });
        resultsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                displayResultsGUI();
            }
        });
        newElectionButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setupNewElection();
            }
        });
        exitButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                confirmExit();
            }
        });

        outputArea.append("=== Welcome to EVM ===\n");
        frame.setVisible(true);
    }

    private static void confirmExit() {
        int result = JOptionPane.showConfirmDialog(frame, "Do you really want to exit?", "Confirm Exit", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (result == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }

    private static JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 16));
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createRaisedBevelBorder());
        button.setPreferredSize(new Dimension(120, 40));
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(bgColor.brighter());
            }

            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
            }
        });
        return button;
    }

    private static void initializeDatabase() {
        try {
            Connection conn = DriverManager.getConnection(getDBUrl(), DB_USER, DB_PASSWORD);
            Statement stmt = conn.createStatement();

            stmt.execute("CREATE TABLE IF NOT EXISTS voters (voter_id VARCHAR(50) PRIMARY KEY, has_voted BOOLEAN NOT NULL DEFAULT FALSE)");
            stmt.execute("CREATE TABLE IF NOT EXISTS candidates (name VARCHAR(50) PRIMARY KEY, votes INTEGER NOT NULL DEFAULT 0)");

            stmt.close();
            conn.close();
        } catch (SQLException e) {
            outputArea.append("Database initialization error: " + e.getMessage() + "\n");
            JOptionPane.showMessageDialog(frame, "Failed to initialize database: " + e.getMessage() + "\nPlease check MySQL server and credentials.", "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void castVoteGUI() {
        if (currentDB.equals("evm_db")) {
            JOptionPane.showMessageDialog(frame, "No election set up yet. Please create a new election first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JTextField voterIdField = new JTextField(15);
        JPanel panel = new JPanel(new GridLayout(2, 1, 5, 5));
        panel.add(new JLabel("Enter your unique voter ID:"));
        panel.add(voterIdField);

        int result = JOptionPane.showConfirmDialog(frame, panel, "Cast Vote", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        String voterId = voterIdField.getText().trim();
        if (voterId.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Voter ID cannot be empty!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            Connection conn = DriverManager.getConnection(getDBUrl(), DB_USER, DB_PASSWORD);

            outputArea.append("Casting vote using database: " + currentDB + "\n");

            PreparedStatement checkStmt = conn.prepareStatement("SELECT has_voted FROM voters WHERE voter_id = ?");
            checkStmt.setString(1, voterId);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next() && rs.getBoolean("has_voted")) {
                JOptionPane.showMessageDialog(frame, "You have already voted!", "Error", JOptionPane.ERROR_MESSAGE);
                conn.close();
                return;
            }

            Statement candidateStmt = conn.createStatement();
            ResultSet candidateRs = candidateStmt.executeQuery("SELECT name FROM candidates");
            ArrayList<String> candidateList = new ArrayList<String>();
            while (candidateRs.next()) {
                String candidateName = candidateRs.getString("name");
                candidateList.add(candidateName);
            }
            String[] candidateArray = candidateList.toArray(new String[0]);

            outputArea.append("Candidates in voting list: ");
            for (String candidate : candidateArray) {
                outputArea.append(candidate + ", ");
            }
            outputArea.append("\n");

            if (candidateArray.length == 0) {
                JOptionPane.showMessageDialog(frame, "No candidates available in the current election!", "Error", JOptionPane.ERROR_MESSAGE);
                conn.close();
                return;
            }

            JComboBox<String> candidateCombo = new JComboBox<String>(candidateArray);
            candidateCombo.setFont(new Font("Arial", Font.PLAIN, 14));
            JPanel votePanel = new JPanel(new GridLayout(2, 1, 5, 5));
            votePanel.add(new JLabel("Select a candidate:"));
            votePanel.add(candidateCombo);

            int voteResult = JOptionPane.showConfirmDialog(frame, votePanel, "Choose Candidate", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (voteResult != JOptionPane.OK_OPTION) {
                conn.close();
                return;
            }

            String selectedCandidate = (String) candidateCombo.getSelectedItem();

            conn.setAutoCommit(false);
            try {
                PreparedStatement voterStmt = conn.prepareStatement(
                        "INSERT INTO voters (voter_id, has_voted) VALUES (?, ?) ON DUPLICATE KEY UPDATE has_voted = ?");
                voterStmt.setString(1, voterId);
                voterStmt.setBoolean(2, true);
                voterStmt.setBoolean(3, true);
                voterStmt.executeUpdate();

                PreparedStatement voteStmt = conn.prepareStatement("UPDATE candidates SET votes = votes + 1 WHERE name = ?");
                voteStmt.setString(1, selectedCandidate);
                voteStmt.executeUpdate();

                conn.commit();
                outputArea.append("Vote casted successfully for " + selectedCandidate + "!\n");
            } catch (SQLException e) {
                conn.rollback();
                JOptionPane.showMessageDialog(frame, "Voting error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } finally {
                conn.setAutoCommit(true);
            }

            conn.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void displayResultsGUI() {
        JPasswordField passwordField = new JPasswordField(15);
        JPanel panel = new JPanel(new GridLayout(2, 1, 5, 5));
        panel.add(new JLabel("Enter admin password:"));
        panel.add(passwordField);

        int result = JOptionPane.showConfirmDialog(frame, panel, "Admin Access", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        String password = new String(passwordField.getPassword());
        if (!password.equals("admin123")) {
            JOptionPane.showMessageDialog(frame, "Unauthorized access!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String[] options = new String[pastElections.size() + 1];
        options[0] = "Current Election (" + currentDB + ")";
        int i = 1;
        for (String election : pastElections.keySet()) {
            options[i] = election + " (" + pastElections.get(election) + ")";
            i++;
        }

        String selectedElection = (String) JOptionPane.showInputDialog(frame, "Select an election to view results:",
                "View Results", JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (selectedElection == null) return;

        String dbToQuery;
        if (selectedElection.startsWith("Current")) {
            dbToQuery = currentDB;
        } else {
            String electionName = selectedElection.split(" \\(")[0];
            dbToQuery = pastElections.get(electionName);
        }

        try {
            Connection conn = DriverManager.getConnection(DB_URL_PREFIX + dbToQuery, DB_USER, DB_PASSWORD);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT name, votes FROM candidates");

            outputArea.append("\n=== Voting Results for " + selectedElection + " ===\n");
            int totalVotes = 0;
            String winner = "";
            int maxVotes = -1;

            while (rs.next()) {
                String name = rs.getString("name");
                int votes = rs.getInt("votes");
                outputArea.append(name + ": " + votes + " votes\n");
                totalVotes += votes;
                if (votes > maxVotes) {
                    maxVotes = votes;
                    winner = name;
                }
            }

            outputArea.append("Total votes casted: " + totalVotes + "\n");
            if (totalVotes == 0) {
                outputArea.append("No votes casted yet!\n");
            } else {
                outputArea.append("Winner: " + winner + " with " + maxVotes + " votes\n");
            }

            conn.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Results error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void setupNewElection() {
        JPasswordField passwordField = new JPasswordField(15);
        JPanel panel = new JPanel(new GridLayout(2, 1, 5, 5));
        panel.add(new JLabel("Enter admin password to start new election:"));
        panel.add(passwordField);

        int result = JOptionPane.showConfirmDialog(frame, panel, "New Election", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        String password = new String(passwordField.getPassword());
        if (!password.equals("admin123")) {
            JOptionPane.showMessageDialog(frame, "Unauthorized access!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JTextField candidateField = new JTextField(20);
        JPanel candidatePanel = new JPanel(new GridLayout(2, 1, 5, 5));
        candidatePanel.add(new JLabel("Enter candidate names (comma-separated):"));
        candidatePanel.add(candidateField);

        int candidateResult = JOptionPane.showConfirmDialog(frame, candidatePanel, "Add Candidates", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (candidateResult != JOptionPane.OK_OPTION) return;

        String candidatesInput = candidateField.getText().trim();
        if (candidatesInput.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Candidate list cannot be empty!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String[] candidates = candidatesInput.split(",");
        for (int i = 0; i < candidates.length; i++) {
            candidates[i] = candidates[i].trim();
            if (candidates[i].isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Invalid candidate name!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        String newDBName = "evm_db_" + System.currentTimeMillis();
        try {
            Connection conn = DriverManager.getConnection(DB_URL_PREFIX + "mysql", DB_USER, DB_PASSWORD);
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE DATABASE " + newDBName);

            if (!currentDB.equals("evm_db")) {
                pastElections.put("Election " + pastElections.size(), currentDB);
            }
            currentDB = newDBName;
            saveLastElection();
            conn.close();

            initializeDatabase();

            conn = DriverManager.getConnection(getDBUrl(), DB_USER, DB_PASSWORD);
            PreparedStatement candidateStmt = conn.prepareStatement("INSERT INTO candidates (name, votes) VALUES (?, 0)");
            for (String candidate : candidates) {
                candidateStmt.setString(1, candidate);
                candidateStmt.executeUpdate();
            }
            conn.close();

            outputArea.append("New election setup completed in " + newDBName + "!\n");
            outputArea.append("Candidates added: " + String.join(", ", candidates) + "\n");
        } catch (SQLException e) {
            outputArea.append("New election setup error: " + e.getMessage() + "\n");
            JOptionPane.showMessageDialog(frame, "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}