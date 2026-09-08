package dps.productionline;

import java.util.Scanner;

public class ExitOperation extends Thread {
    private final int lineId;
    private final Runnable onExitRequest;

    public ExitOperation(int lineId, Runnable onExitRequest) {
        this.lineId = lineId;
        this.onExitRequest = onExitRequest;
        this.setDaemon(true);
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.equalsIgnoreCase("exit")) {
                System.out.println("Exit of line " + lineId + " has been requested");
                onExitRequest.run();
                break;
            }
        }
        scanner.close();
    }

}
