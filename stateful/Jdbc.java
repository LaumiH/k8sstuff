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


    public void databaseOperations() throws Exception {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            String db_url = System.getenv("db_url");    // dynamically modify the databas url for execution
            //jdbc:mysql://mysql/?
            //for running jdbc as a pod inside kubernetes alongside mysql pod
            //mysql is the deployment DNS name (through a ClusterIP=None service)

            //I know, password as password is not how you're supposed to do it ^^
            conn = DriverManager.getConnection(db_url + "user=root&password=password");
            state = conn.createStatement();

            String existence_check = System.getenv("check");
            if(existence_check.equals("yes")) {
                //check if database already exists (szenario 2)
                state.executeUpdate("create database if not exists test");
                state.executeUpdate("use test");

                //check if table testdata already exists
                result = conn.getMetaData().getTables(null, null, "testdata", null);
                if(result.next()) {
                    //table already exists
                    state.execute("truncate table testdata");
                    logger.info("table 'testdata' truncated because table was already filled\n");
                } else {
                    //table does not exist yet
                    state.execute("create table testdata(id int not null auto_increment, number int not null, primary key (id))");
                    logger.info("table 'testdata' created\n");
                }
            } else {
                //create database and table
                state.executeUpdate("create database test");
                state.executeUpdate("use test");
                state.execute("create table testdata(id int not null auto_increment, number int not null, primary key (id))");
                logger.info("database 'test' and table 'testdata' created\n");
            }

            //create 50 entries with random digits
            for(int i=0; i<50; i++) {
                int random = (int)(Math.random() * 50 + 1);
                prepState = conn.prepareStatement("insert into test.testdata values(default, ?)");
                prepState.setInt(1, random);
                prepState.executeUpdate();
                System.out.println("number " + random + " inserted");
                sleep(100);
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
                logger.info("Executing query 'select number from test.testdata where id=" + id + "' ...");
                System.out.println("Executing query 'select number from test.testdata where id=" + id + "' ...");
                prepState = conn.prepareStatement("select number from test.testdata where id=?");
                prepState.setInt(1, id);

                result = prepState.executeQuery();
                logger.info("Done.");
                result.next();
                int before = result.getInt("number");

                //substitute number of id with new random digit
                int after = (int)(Math.random() * 50 + 1);
                prepState = conn.prepareStatement("update test.testdata set number=? where id=?");
                prepState.setInt(1, after);
                prepState.setInt(2, id);
                logger.info("Executing query 'update test.testdata set number=" + after + " where id=" + id + "' ...");
                prepState.executeUpdate();

                logger.info("id " + id + " updated: " + before + " -> " + after + "\n\n");

                // wait for 5 sec
                sleep(5000);
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
