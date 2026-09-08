package dps.adminclient;

import dps.adminclient.rest.AdminServerApi;
import dps.common.ProductionLineInfo;
import dps.common.OperationalState;

import java.util.Scanner;
import java.util.List;
import java.util.Optional;

public class AdminClientApp {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Write: AdminClientApp <adminServerAddress> <adminServerPort>");
            return;
        }

        String serverAddress = args[0];
        int serverPort = Integer.parseInt(args[1]);

        AdminServerApi api = new AdminServerApi(serverAddress, serverPort);
        Scanner scanner = new Scanner(System.in);

        boolean running = true;

        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    handleGetAllLines(api);
                    break;
                case "2":
                    handleGetState(api, scanner);
                    break;
                case "3":
                    hangleGetAverage(api, scanner);
                    break;
                case "4":
                    running = false;
                    System.out.println("Goodbye");
                    break;
                default:
                    System.out.println("Wrong choice, retry: ");
                    break;
            }
        }
        scanner.close();
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("=== SmartFab admin client ===");
        System.out.println("1. List all production lines");
        System.out.println("2. Get operational state of a line");
        System.out.println("3. Get average vibration of a line between two timestamps");
        System.out.println("4. Exit");
        System.out.println("Choose: ");
    }

    private static void handleGetAllLines(AdminServerApi api) {
        List<ProductionLineInfo> lines = api.getAllLines();
        if (lines.isEmpty()) {
            System.out.println("No production lines are registered");
            return;
        }
        for (ProductionLineInfo line : lines) {
            System.out.println(line);
        }
    }

    private static void handleGetState(AdminServerApi api, Scanner scanner) {
        System.out.print("Enter line ID: ");
        int id = readInt(scanner);

        Optional<OperationalState> state = api.getState(id);
        if (state.isEmpty()) {
            System.out.println("Line " + id + " not found");
        } else {
            System.out.println("Line " + id + " state: " + state.get());
        }
    }

    private static void hangleGetAverage(AdminServerApi api, Scanner scanner) {
        System.out.print("Enter line ID: ");
        int id = readInt(scanner);

        System.out.print("Enter timestamp t1: ");
        long from = readLong(scanner);

        System.out.print("Enter timestamp t2: ");
        long to = readLong(scanner);

        if (from > to) {
            System.out.println("Wrong interval: t1 must be less than or equal to t2");
            return;
        }

        Optional<Double> avg = api.getAverage(id, from, to);
        if (avg.isEmpty()) {
            System.out.println("Line " + id + " does not contain any data in the interval " + from + " - " + to);
        } else {
            System.out.println(
                    "Average vibration from line " + id + " in the interval " + from + " - " + to + " is: "
                            + avg.get());
        }
    }

    private static int readInt(Scanner scanner) {
        while (true) {
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.print("Wrong or invalid argument (insert valid number), retry: ");
            }
        }
    }

    private static long readLong(Scanner scanner) {
        while (true) {
            try {
                return Long.parseLong(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.print("Wrong or invalid argument (insert valid number), retry: ");
            }
        }
    }
}
