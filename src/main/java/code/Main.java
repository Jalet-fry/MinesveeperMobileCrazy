package code;

import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> startNewGame());
    }

    public static void startNewGame() {
        Settings settings = showSettingsDialog();
        if (settings != null) new Minesweeper(settings);
    }

    public static Settings showSettingsDialog() {
        JPanel panel = new JPanel(new GridLayout(0, 2));
        JTextField[] fields = {
                new JTextField("10"), new JTextField("10"), // rows, cols
                new JTextField("10"), new JTextField("5"),  // p1, p2
                new JTextField("2"), new JTextField("3")    // p3, pAnti
        };
        String[] labels = {"Строк:", "Столбцов:", "Обычные (1) %:", "Двойные (2) %:", "Тройные (3) %:", "Анти (-1) %:"};

        for (int i = 0; i < labels.length; i++) {
            panel.add(new JLabel(labels[i]));
            panel.add(fields[i]);
        }

        if (JOptionPane.showConfirmDialog(null, panel, "Настройки", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                return new Settings(
                        Integer.parseInt(fields[0].getText()), Integer.parseInt(fields[1].getText()),
                        Double.parseDouble(fields[2].getText()), Double.parseDouble(fields[3].getText()),
                        Double.parseDouble(fields[4].getText()), Double.parseDouble(fields[5].getText())
                );
            } catch (Exception e) { return showSettingsDialog(); }
        }
        return null;
    }

    public static class Settings {
        int rows, cols;
        double p1, p2, p3, pAnti;
        Settings(int r, int c, double p1, double p2, double p3, double pa) {
            this.rows = r; this.cols = c;
            this.p1 = p1; this.p2 = p2; this.p3 = p3; this.pAnti = pa;
        }
    }
}