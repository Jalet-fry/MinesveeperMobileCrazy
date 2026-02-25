package code;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Minesweeper extends JFrame {
    private Main.Settings settings;
    private JButton[][] buttons;
    private int[][] mineValues;
    private int[][] adjacentSums;
    private boolean[][] revealed;
    private int[][] flagged;
    private boolean firstClick = true;
    private boolean isGameOver = false;

    // UI элементы
    private JLabel[] countLabels = new JLabel[4]; // 1, 2, 3, -1
    private int[] totalMinesCount = new int[4];
    private List<Integer> activeFlagTypes = new ArrayList<>();

    public Minesweeper(Main.Settings settings) {
        this.settings = settings;
        // Определяем, какие флаги разрешены
        if (settings.p1 > 0) activeFlagTypes.add(1);
        if (settings.p2 > 0) activeFlagTypes.add(2);
        if (settings.p3 > 0) activeFlagTypes.add(3);
        if (settings.pAnti > 0) activeFlagTypes.add(-1);

        initUI();
    }

    private void initUI() {
        setTitle("Advanced Minesweeper");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Верхняя панель со счетчиками
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        infoPanel.setBackground(Color.LIGHT_GRAY);
        String[] icons = {"1х:", "2x:", "3x:", "-1х:"};
        for (int i = 0; i < 4; i++) {
            countLabels[i] = new JLabel(icons[i] + " 0");
            countLabels[i].setFont(new Font("Arial", Font.BOLD, 14));
            infoPanel.add(countLabels[i]);
        }
        add(infoPanel, BorderLayout.NORTH);

        setupBoard();

        setSize(Math.min(1200, settings.cols * 35 + 100), Math.min(900, settings.rows * 35 + 150));
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void setupBoard() {
        JPanel boardPanel = new JPanel(new GridLayout(settings.rows, settings.cols));
        buttons = new JButton[settings.rows][settings.cols];
        mineValues = new int[settings.rows][settings.cols];
        adjacentSums = new int[settings.rows][settings.cols];
        revealed = new boolean[settings.rows][settings.cols];
        flagged = new int[settings.rows][settings.cols];
        isGameOver = false;

        for (int r = 0; r < settings.rows; r++) {
            for (int c = 0; c < settings.cols; c++) {
                buttons[r][c] = createButton(r, c);
                boardPanel.add(buttons[r][c]);
            }
        }
        add(new JScrollPane(boardPanel), BorderLayout.CENTER);
    }

    private JButton createButton(int r, int c) {
        JButton btn = new JButton();
        btn.setPreferredSize(new Dimension(30, 30));
        btn.setMargin(new Insets(0,0,0,0));
        btn.setFocusPainted(false);
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (isGameOver) return;
                if (SwingUtilities.isRightMouseButton(e)) toggleFlag(r, c);
                else if (SwingUtilities.isLeftMouseButton(e)) onLeftClick(r, c);
            }
        });
        return btn;
    }

    private void onLeftClick(int r, int c) {
        if (flagged[r][c] != 0) return;
        if (firstClick) {
            generateLevel(r, c);
            firstClick = false;
            updateCounters();
        }
        handleReveal(r, c);
    }

    private void generateLevel(int startR, int startC) {
        Random rnd = new Random();
        int total = settings.rows * settings.cols;

        totalMinesCount[0] = (int)(total * (settings.p1/100.0));
        totalMinesCount[1] = (int)(total * (settings.p2/100.0));
        totalMinesCount[2] = (int)(total * (settings.p3/100.0));
        totalMinesCount[3] = (int)(total * (settings.pAnti/100.0));

        int[] types = {1, 2, 3, -1};
        for (int i = 0; i < 4; i++) {
            int placed = 0;
            while (placed < totalMinesCount[i]) {
                int r = rnd.nextInt(settings.rows), c = rnd.nextInt(settings.cols);
                if (mineValues[r][c] == 0 && (Math.abs(r-startR) > 1 || Math.abs(c-startC) > 1)) {
                    mineValues[r][c] = types[i];
                    placed++;
                }
            }
        }

        for (int r = 0; r < settings.rows; r++) {
            for (int c = 0; c < settings.cols; c++) {
                if (mineValues[r][c] == 0) adjacentSums[r][c] = calculateSum(r, c);
            }
        }
    }

    private int calculateSum(int r, int c) {
        int sum = 0;
        for (int i = -1; i <= 1; i++)
            for (int j = -1; j <= 1; j++)
                if (isValid(r+i, c+j)) sum += mineValues[r+i][c+j];
        return sum;
    }

    private void handleReveal(int r, int c) {
        if (!isValid(r, c) || revealed[r][c] || flagged[r][c] != 0 || isGameOver) return;

        // 1. Если наступили на мину (любую) — проигрыш
        if (mineValues[r][c] != 0) {
            triggerGameOver(false);
            return;
        }

        revealed[r][c] = true;
        buttons[r][c].setEnabled(false);
        buttons[r][c].setBackground(Color.LIGHT_GRAY);

        int sum = adjacentSums[r][c];

        // 2. Отображаем сумму, если она не равна 0
        if (sum != 0) {
            buttons[r][c].setText(String.valueOf(sum));
            setNumberColor(buttons[r][c], sum);
        }

        // 3. РЕКУРСИЯ: Только если в радиусе 1 клетки НЕТ ВООБЩЕ НИКАКИХ МИН
        // Даже если сумма 0 (например 1 + -1), мы НЕ открываем соседей автоматически.
        if (isAreaAbsolutelySafe(r, c)) {
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    handleReveal(r + i, c + j);
                }
            }
        }

        checkWin();
    }

    // Новый вспомогательный метод для жесткой проверки безопасности
    private boolean isAreaAbsolutelySafe(int r, int c) {
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                int nr = r + i, nc = c + j;
                if (isValid(nr, nc)) {
                    // Если хоть в одной соседней клетке есть мина (любого типа),
                    // эта зона не считается "абсолютно безопасной" для авто-раскрытия.
                    if (mineValues[nr][nc] != 0) return false;
                }
            }
        }
        return true;
    }

    private void toggleFlag(int r, int c) {
        if (revealed[r][c]) return;

        int current = flagged[r][c];
        int nextIndex = -1;

        // Поиск следующего активного типа флага
        if (current == 0) {
            nextIndex = 0;
        } else {
            for (int i = 0; i < activeFlagTypes.size(); i++) {
                if (activeFlagTypes.get(i) == current) {
                    nextIndex = i + 1;
                    break;
                }
            }
        }

        if (nextIndex == -1 || nextIndex >= activeFlagTypes.size()) flagged[r][c] = 0;
        else flagged[r][c] = activeFlagTypes.get(nextIndex);

        updateFlagVisual(r, c);
        updateCounters();
    }

    private void updateFlagVisual(int r, int c) {
        String[] icons = {"", "🚩", "🚩²", "🚩³", "-🚩"};
        int val = flagged[r][c];
        int idx = (val == -1) ? 4 : val;
        buttons[r][c].setText(icons[idx]);
        buttons[r][c].setForeground(val == -1 ? Color.BLUE : Color.RED);
    }

    private void updateCounters() {
        int[] currentFlags = new int[4]; // 1, 2, 3, -1
        for (int r = 0; r < settings.rows; r++) {
            for (int c = 0; c < settings.cols; c++) {
                int f = flagged[r][c];
                if (f == 1) currentFlags[0]++;
                else if (f == 2) currentFlags[1]++;
                else if (f == 3) currentFlags[2]++;
                else if (f == -1) currentFlags[3]++;
            }
        }
        countLabels[0].setText("1х: " + (totalMinesCount[0] - currentFlags[0]));
        countLabels[1].setText("2x: " + (totalMinesCount[1] - currentFlags[1]));
        countLabels[2].setText("3x: " + (totalMinesCount[2] - currentFlags[2]));
        countLabels[3].setText("-1х: " + (totalMinesCount[3] - currentFlags[3]));
    }

    private void triggerGameOver(boolean won) {
        if (isGameOver) return; // Исправленный Баг №2
        isGameOver = true;

        revealAll();
        JOptionPane.showMessageDialog(this, won ? "Победа!" : "Вы подорвались!");
        dispose();
        Main.startNewGame();
    }

    private void revealAll() {
        for (int r = 0; r < settings.rows; r++) {
            for (int c = 0; c < settings.cols; c++) {
                if (mineValues[r][c] != 0) {
                    buttons[r][c].setText(getMineIcon(mineValues[r][c]));
                    buttons[r][c].setBackground(Color.RED);
                }
            }
        }
    }

    private String getMineIcon(int val) {
        return switch(val) { case 1 -> "1х"; case 2 -> "2x"; case 3 -> "3x"; case -1 -> "-1х"; default -> ""; };
    }

    private void checkWin() {
        int count = 0;
        for (int r = 0; r < settings.rows; r++)
            for (int c = 0; c < settings.cols; c++)
                if (revealed[r][c]) count++;

        int totalMines = 0;
        for(int n : totalMinesCount) totalMines += n;

        if (count == (settings.rows * settings.cols) - totalMines) triggerGameOver(true);
    }

    private void setNumberColor(JButton btn, int n) {
        if (n == 0) return;
        Color[] colors = {Color.BLUE, new Color(0, 128, 0), Color.RED, new Color(0, 0, 128), Color.MAGENTA};
        int idx = Math.abs(n) - 1;
        btn.setForeground(idx < colors.length ? colors[idx] : Color.BLACK);
    }

    private boolean isValid(int r, int c) {
        return r >= 0 && r < settings.rows && c >= 0 && c < settings.cols;
    }
}