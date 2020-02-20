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
            //for running locally to access minikube mysql pod
            //conn = DriverManager.getConnection("jdbc:mysql://192.168.39.35:32060/test?" + "user=root&password=password");

            //for running as a pod inside kubernetes
            conn = DriverManager.getConnection("jdbc:mysql://mysql/test?" + "user=root&password=password");
            //mysql is the deployment DNS name (through a ClusterIP=None service)
            state = conn.createStatement();

            //check if table already exists
            DatabaseMetaData meta = conn.getMetaData();
            result = meta.getTables(null, null, "testdata", null);
            if(result.next()) {
                //table already exists
                state.execute("truncate table testdata");
                System.out.println("testdata truncated\n");
                logger.info("testdata truncated because table was already filled\n");
            } else {
                state.execute("create table testdata(id int not null auto_increment, number int not null, primary key (id))");
                System.out.println("testdata created\n");
                logger.info("testdata created\n");
            }

            //https://stackoverflow.com/questions/24378270/how-do-you-determine-if-an-insert-or-update-was-successful-using-java-and-mysql

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

            while(true) {
                // constantly perform db operations

                // clear buffers
                state = state = conn.createStatement();
                prepState = null;
                result = null;

                // print whole testtable to log
                //result = state.executeQuery("select * from test.testdata");
                //writeResultSet(result);

                int id = (int)(Math.random() * 50 + 1);

                prepState = conn.prepareStatement("select number from test.testdata where id=?");
                prepState.setInt(1, id);
                result = prepState.executeQuery();
                //int before = result.getInt(1);

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
