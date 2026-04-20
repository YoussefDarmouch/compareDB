import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.util.Scanner;
import Package.CompareDbConsole;
import Package.CompareDbGui;


public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String again;

        do {
            int choice;

            System.out.println("====================================");
            System.out.println("     DATABASE COMPARISON TOOL       ");
            System.out.println("====================================");
            System.out.println("        Choose your mode:           ");
            System.out.println("------------------------------------");
            System.out.println("1️⃣  GUI Mode (Graphical Interface)");
            System.out.println("2️⃣  Console Mode (Text Interface)");
            System.out.println("------------------------------------");
            System.out.print("Enter your choice → ");
            choice = sc.nextInt();
            sc.nextLine(); // consume newline

            if (choice == 1) {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {}
                SwingUtilities.invokeLater(() -> new CompareDbGui().setVisible(true));
            } else if (choice == 2) {
                CompareDbConsole.run();
            }

            // ask user if he wants to continue
            System.out.print("Do you want to run again? (y/n) → ");
            again = sc.nextLine();

        } while (again.equalsIgnoreCase("y"));

        System.out.println("Program closed.");
        sc.close();

    }}