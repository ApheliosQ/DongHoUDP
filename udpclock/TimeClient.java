package udpclock;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.*;

public class TimeClient extends JFrame {
    private final DatagramSocket socket;
    private final InetAddress serverAddr;
    private final int serverPort = 12345;

    // UI Components
    private JLabel lblClock;
    private JTextField alarmHourField, alarmMinuteField;
    private JLabel lblTimerStatus;
    private JTextField timerField;
    private JLabel stopwatchLabel;
    private JLabel lblStatus;

    // Alarm Table
    private DefaultTableModel alarmTableModel;
    private JTable alarmTable;

    public TimeClient(String serverHost) throws Exception {
        super("Smart UDP Clock Client");
        serverAddr = InetAddress.getByName(serverHost);
        socket = new DatagramSocket();

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 650);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(245, 245, 250));

        // Header
        JLabel header = new JLabel("Smart Clock Client", SwingConstants.CENTER);
        header.setFont(new Font("Segoe UI", Font.BOLD, 32));
        header.setOpaque(true);
        header.setBackground(new Color(180, 220, 240));
        header.setForeground(new Color(20, 50, 80));
        header.setBorder(new EmptyBorder(20, 10, 20, 10));
        add(header, BorderLayout.NORTH);

        // Tabs
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        tabs.setBackground(new Color(245, 245, 250));
        tabs.setForeground(new Color(30, 30, 60));

        // --- 1. Clock Tab ---
        JPanel clockPanel = new JPanel(new BorderLayout());
        clockPanel.setBackground(new Color(220, 245, 255));
        lblClock = new JLabel("--:--:--", SwingConstants.CENTER);
        lblClock.setFont(new Font("Monospaced", Font.BOLD, 90));
        lblClock.setForeground(new Color(0, 80, 200));
        lblClock.setOpaque(true);
        lblClock.setBackground(new Color(230, 250, 255));
        clockPanel.add(lblClock, BorderLayout.CENTER);

        JPanel clockButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        clockButtons.setBackground(new Color(220, 245, 255));
        JButton btnSync = createIconButton("Đồng bộ & chạy", "icons/sync.png", new Color(0, 120, 220));
        btnSync.addActionListener(e -> sendCommand("TIME_START"));
        JButton btnStop = createIconButton("Dừng", "icons/stop.png", new Color(220, 60, 60));
        btnStop.addActionListener(e -> sendCommand("TIME_STOP"));
        clockButtons.add(btnSync);
        clockButtons.add(btnStop);
        clockPanel.add(clockButtons, BorderLayout.SOUTH);
        tabs.addTab("Đồng hồ", clockPanel);

        // --- 2. Alarm Tab ---
        JPanel alarmPanel = new JPanel(new BorderLayout(10, 10));
        alarmPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        alarmPanel.setBackground(new Color(250, 250, 255));

        String[] columns = {"Giờ", "Phút", "Trạng thái"};
        alarmTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        alarmTable = new JTable(alarmTableModel);
        alarmTable.setFont(new Font("Monospaced", Font.BOLD, 20));
        alarmTable.setRowHeight(28);
        alarmTable.setBackground(new Color(240, 250, 255));
        alarmTable.setForeground(new Color(0, 120, 80));
        alarmPanel.add(new JScrollPane(alarmTable), BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        inputPanel.setOpaque(false);
        inputPanel.add(new JLabel("Giờ:"));
        alarmHourField = new JTextField("07", 3);
        alarmHourField.setPreferredSize(new Dimension(50, 30));
        inputPanel.add(alarmHourField);
        inputPanel.add(new JLabel("Phút:"));
        alarmMinuteField = new JTextField("30", 3);
        alarmMinuteField.setPreferredSize(new Dimension(50, 30));
        inputPanel.add(alarmMinuteField);

        JButton btnAdd = createButton("Thêm báo thức", new Color(0, 160, 120));
        btnAdd.addActionListener(e -> {
            String hour = alarmHourField.getText().trim();
            String minute = alarmMinuteField.getText().trim();
            sendCommand("ALARM_ADD " + hour + ":" + minute);
            alarmTableModel.addRow(new Object[]{hour, minute, "Bật"});
        });

        JButton btnRemove = createButton("Xóa báo thức", new Color(220, 60, 80));
        btnRemove.addActionListener(e -> {
            int idx = alarmTable.getSelectedRow();
            if (idx >= 0) {
                String time = alarmTableModel.getValueAt(idx, 0) + ":" + alarmTableModel.getValueAt(idx, 1);
                sendCommand("ALARM_REMOVE " + time);
                alarmTableModel.removeRow(idx);
            }
        });

        JButton btnToggle = createButton("Bật/Tắt báo thức", new Color(250, 160, 20));
        btnToggle.addActionListener(e -> {
            int idx = alarmTable.getSelectedRow();
            if (idx >= 0) {
                String time = alarmTableModel.getValueAt(idx, 0) + ":" + alarmTableModel.getValueAt(idx, 1);
                sendCommand("ALARM_TOGGLE " + time);
                String currentStatus = (String) alarmTableModel.getValueAt(idx, 2);
                alarmTableModel.setValueAt(currentStatus.equals("Bật") ? "Tắt" : "Bật", idx, 2);
            }
        });

        inputPanel.add(btnAdd);
        inputPanel.add(btnRemove);
        inputPanel.add(btnToggle);
        alarmPanel.add(inputPanel, BorderLayout.NORTH);
        tabs.addTab("Báo thức", alarmPanel);

        // --- 3. Timer Tab ---
        JPanel timerPanel = new JPanel(new BorderLayout());
        timerPanel.setBackground(new Color(235, 250, 255));
        lblTimerStatus = new JLabel("00:00", SwingConstants.CENTER);
        lblTimerStatus.setFont(new Font("Monospaced", Font.BOLD, 70));
        lblTimerStatus.setForeground(new Color(0, 100, 220));
        timerPanel.add(lblTimerStatus, BorderLayout.CENTER);

        JPanel timerCtrl = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        timerCtrl.setOpaque(false);
        timerField = new JTextField("10", 6);
        timerField.setPreferredSize(new Dimension(60, 30));
        JButton btnStartTimer = createIconButton("Bắt đầu", "icons/start.png", new Color(0, 140, 220));
        btnStartTimer.addActionListener(e -> sendCommand("TIMER_START " + timerField.getText().trim()));
        JButton btnCancelTimer = createIconButton("Hủy", "icons/stop.png", new Color(220, 60, 60));
        btnCancelTimer.addActionListener(e -> sendCommand("TIMER_CANCEL"));
        timerCtrl.add(new JLabel("Giây:"));
        timerCtrl.add(timerField);
        timerCtrl.add(btnStartTimer);
        timerCtrl.add(btnCancelTimer);
        timerPanel.add(timerCtrl, BorderLayout.SOUTH);
        tabs.addTab("Hẹn giờ", timerPanel);

        // --- 4. Stopwatch Tab ---
        JPanel swPanel = new JPanel(new BorderLayout());
        swPanel.setBackground(new Color(240, 250, 255));
        stopwatchLabel = new JLabel("00:00.000", SwingConstants.CENTER);
        stopwatchLabel.setFont(new Font("Monospaced", Font.BOLD, 70));
        stopwatchLabel.setForeground(new Color(220, 140, 0));
        swPanel.add(stopwatchLabel, BorderLayout.CENTER);

        JPanel swBtns = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 10));
        swBtns.setOpaque(false);
        JButton sbStart = createIconButton("Start", "icons/start.png", new Color(0, 180, 120));
        sbStart.addActionListener(e -> sendCommand("STOPWATCH START"));
        JButton sbPause = createIconButton("Pause", "icons/pause.png", new Color(250, 180, 20));
        sbPause.addActionListener(e -> sendCommand("STOPWATCH PAUSE"));
        JButton sbResume = createIconButton("Resume", "icons/resume.png", new Color(0, 140, 220));
        sbResume.addActionListener(e -> sendCommand("STOPWATCH RESUME"));
        JButton sbReset = createIconButton("Reset", "icons/reset.png", new Color(220, 60, 80));
        sbReset.addActionListener(e -> sendCommand("STOPWATCH RESET"));
        swBtns.add(sbStart); swBtns.add(sbPause); swBtns.add(sbResume); swBtns.add(sbReset);
        swPanel.add(swBtns, BorderLayout.SOUTH);
        tabs.addTab("Bấm giờ", swPanel);

        add(tabs, BorderLayout.CENTER);

        // Footer
        lblStatus = new JLabel("Server: " + serverHost + ":" + serverPort);
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblStatus.setBorder(new EmptyBorder(5, 5, 5, 5));
        lblStatus.setForeground(new Color(50, 50, 80));
        add(lblStatus, BorderLayout.SOUTH);

        setVisible(true);
        new Thread(this::listenerLoop).start();
    }

    // --- Nút đẹp chuẩn hóa với chiều cao 40px và tự động tính width ---
    private JButton createIconButton(String text, String iconPath, Color bg) {
        JButton btn = new JButton(text);

        // Icon resize vừa phải
        ImageIcon icon = new ImageIcon(iconPath);
        if (icon.getIconWidth() > 20 || icon.getIconHeight() > 20) {
            Image img = icon.getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH);
            icon = new ImageIcon(img);
        }
        btn.setIcon(icon);

        // Cấu hình font và layout
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setHorizontalTextPosition(SwingConstants.RIGHT);
        btn.setVerticalTextPosition(SwingConstants.CENTER);
        btn.setIconTextGap(8); // Khoảng cách giữa icon và text
        
        // Tự động tính toán preferred size
        calculateButtonSize(btn, icon.getIconWidth());
        
        // Styling
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bg.darker(), 1),
                BorderFactory.createEmptyBorder(0, 12, 0, 12)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        // Chuẩn hóa chiều cao
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width, 40));
        btn.setMinimumSize(new Dimension(80, 40)); // Minimum width để tránh quá nhỏ
        
        return btn;
    }

    private JButton createButton(String text, Color bg) {
        JButton btn = new JButton(text);
        
        // Cấu hình font
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        // Tự động tính toán preferred size
        calculateButtonSize(btn, 0);
        
        // Styling
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bg.darker(), 1),
                BorderFactory.createEmptyBorder(0, 12, 0, 12)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        // Chuẩn hóa chiều cao
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width, 40));
        btn.setMinimumSize(new Dimension(100, 40)); // Minimum width cho nút text
        
        return btn;
    }
    
    // Phương thức tính toán kích thước nút dựa trên text và icon
    private void calculateButtonSize(JButton btn, int iconWidth) {
        // Tạo graphics context tạm thời để đo text
        Graphics g = btn.getGraphics();
        if (g == null) {
            // Nếu chưa có graphics, tạo một context tạm
            g = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
        }
        
        FontMetrics fm = g.getFontMetrics(btn.getFont());
        
        // Tính width của text
        int textWidth = fm.stringWidth(btn.getText());
        
        // Tính tổng width: icon + gap + text + padding
        int totalWidth = textWidth + iconWidth + 24; // 24 = 8(gap)*2 + 8(padding)*2
        
        // Đảm bảo minimum width
        totalWidth = Math.max(totalWidth, 100);
        
        // Set preferred size tạm thời để layout manager tính toán
        btn.setPreferredSize(new Dimension(totalWidth, 40));
    }

    private void sendCommand(String cmd) {
        try {
            byte[] data = cmd.getBytes();
            DatagramPacket p = new DatagramPacket(data, data.length, serverAddr, serverPort);
            socket.send(p);
        } catch (Exception e) {
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this, "Lỗi gửi: " + e.getMessage()));
        }
    }

    private void listenerLoop() {
        try {
            byte[] buf = new byte[1024];
            DatagramPacket pkt = new DatagramPacket(buf, buf.length);
            while (true) {
                socket.receive(pkt);
                String msg = new String(pkt.getData(), 0, pkt.getLength()).trim();
                SwingUtilities.invokeLater(() -> handleServerMessage(msg));
            }
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> lblStatus.setText("Ngừng lắng nghe: " + e.getMessage()));
        }
    }

    private void handleServerMessage(String msg) {
        try {
            if (msg.startsWith("TIME ")) {
                lblClock.setText(msg.substring(5));
            } else if (msg.startsWith("ALARM_TRIGGER")) {
                String[] p = msg.split(" ", 2);
                String time = p.length > 1 ? p[1] : "";
                JOptionPane.showMessageDialog(this, "Báo thức reo lúc " + time, "ALARM", JOptionPane.INFORMATION_MESSAGE);
            } else if (msg.startsWith("TIMER_UPDATE")) {
                String[] p = msg.split(" ", 2);
                lblTimerStatus.setText((p.length > 1 ? p[1] : "0") + "s");
            } else if (msg.startsWith("TIMER_DONE")) {
                lblTimerStatus.setText("Hết giờ!");
                JOptionPane.showMessageDialog(this, "Hẹn giờ kết thúc", "TIMER", JOptionPane.INFORMATION_MESSAGE);
            } else if (msg.startsWith("STOPWATCH_UPDATE")) {
                String[] p = msg.split(" ", 2);
                stopwatchLabel.setText(p.length > 1 ? p[1] : "00:00.000");
            } else if (msg.startsWith("ALARMS ")) {
                alarmTableModel.setRowCount(0);
                String payload = msg.substring(7);
                if (!payload.isEmpty()) {
                    String[] items = payload.split(",");
                    for (String it : items) {
                        String[] parts = it.split(":");
                        if (parts.length >= 2) {
                            String status = parts.length > 2 ? parts[2] : "ON";
                            alarmTableModel.addRow(new Object[]{parts[0], parts[1], status.equals("ON") ? "Bật" : "Tắt"});
                        }
                    }
                }
            } else lblStatus.setText(msg);
        } catch (Exception e) {
            lblStatus.setText("Xử lý msg lỗi: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        final String host = args.length > 0 ? args[0] : "localhost";
        SwingUtilities.invokeLater(() -> {
            try {
                new TimeClient(host);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Không thể khởi động client: " + e.getMessage());
            }
        });
    }
}