package udpclock;

import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class TimeServer {
    private static final int PORT = 12345;
    private static final SimpleDateFormat TIME_FMT = new SimpleDateFormat("HH:mm:ss");
    private static final SimpleDateFormat HM_FMT = new SimpleDateFormat("HH:mm");

    // clients that asked TIME_START
    private static final Set<ClientInfo> syncClients = ConcurrentHashMap.newKeySet();

    // alarms (shared list, each alarm has owning client)
    private static final List<Alarm> alarms = new CopyOnWriteArrayList<>();

    // timers per clientKey -> endMillis
    private static final ConcurrentMap<String, Long> timers = new ConcurrentHashMap<>();

    // stopwatches per clientKey
    private static final ConcurrentMap<String, StopwatchState> stopwatches = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        DatagramSocket socket = new DatagramSocket(PORT);
        System.out.println("TimeServer listening on UDP port " + PORT);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

        // Broadcast time to all registered clients every 1s
        scheduler.scheduleAtFixedRate(() -> {
            String nowMsg = "TIME " + TIME_FMT.format(new Date());
            for (ClientInfo ci : syncClients) {
                try {
                    send(socket, nowMsg, ci.addr, ci.port);
                } catch (Exception e) {
                    System.err.println("Failed send TIME to " + ci + " : " + e.getMessage());
                }
            }
        }, 0, 1, TimeUnit.SECONDS);

        // Check alarms each second
        scheduler.scheduleAtFixedRate(() -> {
            String nowHm = HM_FMT.format(new Date());
            for (Alarm a : alarms) {
                if (a.enabled && a.time.equals(nowHm)) {
                    String msg = "ALARM_TRIGGER " + a.time;
                    try {
                        send(socket, msg, a.clientAddr, a.clientPort);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    a.enabled = false; // ring once (you can change if want repeated)
                }
            }
        }, 0, 1, TimeUnit.SECONDS);

        // Timers: broadcast remaining seconds each second
        scheduler.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            for (Map.Entry<String, Long> e : new ArrayList<>(timers.entrySet())) {
                String key = e.getKey();
                long end = e.getValue();
                long remainSec = Math.max(0, (end - now + 999) / 1000);
                String[] parts = key.split(":");
                try {
                    InetAddress addr = InetAddress.getByName(parts[0]);
                    int port = Integer.parseInt(parts[1]);
                    if (remainSec > 0) {
                        send(socket, "TIMER_UPDATE " + remainSec, addr, port);
                    } else {
                        send(socket, "TIMER_DONE", addr, port);
                        timers.remove(key);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }, 0, 1, TimeUnit.SECONDS);

        // Stopwatches: send update every 200ms for active stopwatches
        scheduler.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            for (Map.Entry<String, StopwatchState> e : stopwatches.entrySet()) {
                String key = e.getKey();
                StopwatchState st = e.getValue();
                long elapsed = st.accumulated;
                if (st.running) elapsed += (now - st.lastStart);
                String label = formatMillis(elapsed);
                try {
                    String[] parts = key.split(":");
                    InetAddress addr = InetAddress.getByName(parts[0]);
                    int port = Integer.parseInt(parts[1]);
                    send(socket, "STOPWATCH_UPDATE " + label, addr, port);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }, 0, 200, TimeUnit.MILLISECONDS);

        // Main receive loop
        byte[] buf = new byte[1024];
        while (true) {
            try {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                String req = new String(packet.getData(), 0, packet.getLength()).trim();
                InetAddress clientAddr = packet.getAddress();
                int clientPort = packet.getPort();
                String clientKey = clientAddr.getHostAddress() + ":" + clientPort;

                System.out.println("Received from " + clientKey + " -> " + req);

                // parse command and handle
                String[] tokens = req.split(" ", 2);
                String cmd = tokens[0].toUpperCase();
                String arg = tokens.length > 1 ? tokens[1].trim() : "";

                switch (cmd) {
                    case "TIME_START":
                        syncClients.add(new ClientInfo(clientAddr, clientPort));
                        send(socket, "OK TIME_START", clientAddr, clientPort);
                        break;
                    case "TIME_STOP":
                        syncClients.remove(new ClientInfo(clientAddr, clientPort));
                        send(socket, "OK TIME_STOP", clientAddr, clientPort);
                        break;

                    case "ALARM_ADD": // arg = HH:mm
                        if (!isValidHM(arg)) {
                            send(socket, "ERR Invalid time format (HH:mm)", clientAddr, clientPort);
                        } else {
                            alarms.add(new Alarm(arg, true, clientAddr, clientPort));
                            send(socket, "OK ALARM_ADD " + arg, clientAddr, clientPort);
                        }
                        break;
                    case "ALARM_REMOVE": // arg = HH:mm
                        alarms.removeIf(a -> a.clientAddr.equals(clientAddr) && a.clientPort == clientPort && a.time.equals(arg));
                        send(socket, "OK ALARM_REMOVE " + arg, clientAddr, clientPort);
                        break;
                    case "ALARM_TOGGLE": // arg = HH:mm
                        boolean found = false;
                        for (Alarm a : alarms) {
                            if (a.clientAddr.equals(clientAddr) && a.clientPort == clientPort && a.time.equals(arg)) {
                                a.enabled = !a.enabled;
                                send(socket, "OK ALARM_TOGGLE " + arg + " " + (a.enabled ? "ON" : "OFF"), clientAddr, clientPort);
                                found = true;
                                break;
                            }
                        }
                        if (!found) send(socket, "ERR Alarm not found", clientAddr, clientPort);
                        break;
                    case "GET_ALARMS":
                        // send alarms only for this client
                        StringBuilder list = new StringBuilder();
                        boolean first = true;
                        for (Alarm a : alarms) {
                            if (a.clientAddr.equals(clientAddr) && a.clientPort == clientPort) {
                                if (!first) list.append(",");
                                list.append(a.time).append(":").append(a.enabled ? "ON" : "OFF");
                                first = false;
                            }
                        }
                        send(socket, "ALARMS " + list.toString(), clientAddr, clientPort);
                        break;

                    case "TIMER_START": // arg = seconds
                        try {
                            long sec = Long.parseLong(arg);
                            long endMs = System.currentTimeMillis() + sec * 1000;
                            timers.put(clientKey, endMs);
                            send(socket, "OK TIMER_START " + sec, clientAddr, clientPort);
                        } catch (NumberFormatException nfe) {
                            send(socket, "ERR Invalid seconds", clientAddr, clientPort);
                        }
                        break;
                    case "TIMER_CANCEL":
                        timers.remove(clientKey);
                        send(socket, "OK TIMER_CANCEL", clientAddr, clientPort);
                        break;

                    case "STOPWATCH":
                        // arg may be "START"|"PAUSE"|"RESUME"|"RESET"
                        String action = arg.toUpperCase();
                        StopwatchState st = stopwatches.computeIfAbsent(clientKey, k -> new StopwatchState());
                        switch (action) {
                            case "START":
                                st.running = true;
                                st.lastStart = System.currentTimeMillis();
                                st.accumulated = 0;
                                send(socket, "OK STOPWATCH STARTED", clientAddr, clientPort);
                                break;
                            case "PAUSE":
                                if (st.running) {
                                    st.accumulated += System.currentTimeMillis() - st.lastStart;
                                    st.running = false;
                                }
                                send(socket, "OK STOPWATCH PAUSED", clientAddr, clientPort);
                                break;
                            case "RESUME":
                                if (!st.running) {
                                    st.lastStart = System.currentTimeMillis();
                                    st.running = true;
                                }
                                send(socket, "OK STOPWATCH RESUMED", clientAddr, clientPort);
                                break;
                            case "RESET":
                                st.running = false;
                                st.accumulated = 0;
                                st.lastStart = 0;
                                send(socket, "OK STOPWATCH RESET", clientAddr, clientPort);
                                break;
                            default:
                                send(socket, "ERR Unknown stopwatch action", clientAddr, clientPort);
                        }
                        break;

                    default:
                        send(socket, "ERR Unknown command", clientAddr, clientPort);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        // (scheduler shutdown omitted because main loop is infinite)
    }

    private static void send(DatagramSocket socket, String msg, InetAddress addr, int port) throws Exception {
        byte[] b = msg.getBytes();
        DatagramPacket p = new DatagramPacket(b, b.length, addr, port);
        socket.send(p);
    }

    private static boolean isValidHM(String s) {
        if (s == null) return false;
        return s.matches("^\\d{1,2}:\\d{2}$");
    }

    private static String formatMillis(long ms) {
        long totalMs = Math.max(0, ms);
        long minutes = (totalMs / 1000) / 60;
        long seconds = (totalMs / 1000) % 60;
        long millis = totalMs % 1000;
        return String.format("%02d:%02d.%03d", minutes, seconds, millis);
    }

    // helper classes
    static class Alarm {
        String time; // "HH:mm"
        boolean enabled;
        InetAddress clientAddr;
        int clientPort;
        Alarm(String t, boolean e, InetAddress a, int p) { time = t; enabled = e; clientAddr = a; clientPort = p; }
    }

    static class StopwatchState {
        volatile boolean running = false;
        volatile long lastStart = 0;    // when running: last start time in ms
        volatile long accumulated = 0;  // accumulated ms when paused
    }

    static class ClientInfo {
        InetAddress addr;
        int port;
        ClientInfo(InetAddress a, int p) { addr = a; port = p; }
        @Override public int hashCode() { return Objects.hash(addr, port); }
        @Override public boolean equals(Object o) {
            if (!(o instanceof ClientInfo)) return false;
            ClientInfo c = (ClientInfo)o;
            return addr.equals(c.addr) && port == c.port;
        }
        @Override public String toString() { return addr.getHostAddress() + ":" + port; }
    }
}
