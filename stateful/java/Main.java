import java.io.IOException;

public class Main {
    public static void main(String[] args) throws InterruptedException {

        String loop = System.getenv("loop");

        if(loop.equals("yes")) {
            while (true) {
                try {
                    Jdbc test = new Jdbc();
                    test.databaseOperations();
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    System.out.println("Retry in 1 sec");
                    Thread.sleep(2000);
                }
            }
        } else {
            try {
                Jdbc test = new Jdbc();
                test.databaseOperations();
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }
}
