import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.DatabaseMetaData;

import java.util.logging.FileHandler;
import java.util.logging.Logger;

public class Jdbc {

    public Jdbc() throws IOException {
        boolean append = true;
        this.handler = new FileHandler("jdbc.log", append);
        this.logger = Logger.getLogger(Jdbc.class.getName());
        this.logger.addHandler(handler);
    }

    private Connection conn = null;
    private Statement state = null;
    private PreparedStatement prepState = null;
    private ResultSet result = null;

    private Logger logger;
    private FileHandler handler;


    public void readDatabase() throws Exception {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            //for running jdbc locally to access minikube mysql pod
            //conn = DriverManager.getConnection("jdbc:mysql://192.168.39.35:32060/test?" + "user=root&password=password");

            //for running jdbc as a pod inside kubernetes alongside mysql pod
            //mysql is the deployment DNS name (through a ClusterIP=None service)
            //I know, password as password is not how you're supposed to do it ^^
            conn = DriverManager.getConnection("jdbc:mysql://mysql/test?" + "user=root&password=password");
            state = conn.createStatement();

            //check if table already exists (scenario 2)
            DatabaseMetaData meta = conn.getMetaData();
            result = meta.getTables(null, null, "testdata", null);
            if(result.next()) {
                //table already exists
                state.execute("truncate table testdata");
                System.out.println("testdata truncated\n");
                logger.info("testdata truncated because table was already filled\n");
            } else {
                //table does not exist yet
                state.execute("create table testdata(id int not null auto_increment, number int not null, primary key (id))");
                System.out.println("testdata created\n");
                logger.info("testdata created\n");
            }

            //create 50 entries with random digits
            for(int i=0; i<50; i++) {
                int random = (int)(Math.random() * 50 + 1);
                prepState = conn.prepareStatement("insert into test.testdata values(default, ?)");
                prepState.setInt(1, random);
                prepState.executeUpdate();
                System.out.println("number " + random + " inserted");
                sleep(200);
            }

            System.out.println("inserted 50 random numbers into testdata\n");
            logger.info("inserted 50 random numbers into testdata\n");

            // constantly perform db operations
            while(true) {
                
                // clear buffers
                state = state = conn.createStatement();
                prepState = null;
                result = null;

                // print whole testtable to log
                //result = state.executeQuery("select * from test.testdata");
                //writeResultSet(result);

                //random id
                int id = (int)(Math.random() * 50 + 1);
                //select entry with random id
                prepState = conn.prepareStatement("select number from test.testdata where id=?");
                prepState.setInt(1, id);
                result = prepState.executeQuery();

                //substitute number of id with new random digit
                int after = (int)(Math.random() * 50 + 1);
                prepState = conn.prepareStatement("update test.testdata set number=? where id=?");
                prepState.setInt(1, after);
                prepState.setInt(2, id);
                prepState.executeUpdate();

                logger.info("id " + id + " updated: -> " + after);

                // wait for 10 sec
                sleep(10000);
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            state.execute("truncate table testdata");
            System.out.println("testdata truncated\n");
            logger.info("testdata truncated\n");
            close();
        }
    }

    //writes table entries to stdout and log
    private void writeResultSet(ResultSet result) throws SQLException{
        while(result.next()) {
            int id = result.getInt("id");
            int number = result.getInt("number");
            System.out.println(id + ": " + number);
            logger.info(id + ": " + number + "\n");
            sleep(2000);
        }
    }

    private void close() {
        try {
            if(result!=null) {
                result.close();
            }
            if(state!=null) {
                state.close();
            }
            if(conn!=null) {
                conn.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }
}
