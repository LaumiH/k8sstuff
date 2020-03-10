import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.DatabaseMetaData;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Random;
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
            //db_url=jdbc:mysql://192.168.39.35:32060/?
            //for running jdbc as a pod inside kubernetes alongside mysql pod
            //mysql is the deployment DNS name (through a ClusterIP=None service)

            //I know, password as password is not how you're supposed to do it ^^
            conn = DriverManager.getConnection(db_url + "user=root&password=password");
            state = conn.createStatement();

            String existence_check = System.getenv("check");
            if(existence_check.equals("yes")) {
                //check if database already exists (szenario 2)
                state.executeUpdate("CREATE DATABASE IF NOT EXISTS students");
                state.executeUpdate("USE students");

                //check if table testdata already exists
                result = conn.getMetaData().getTables(null, null, "grades", null);
                if(result.next()) {
                    //table already exists
                    state.execute("TRUNCATE TABLE grades");
                    logger.info("table 'grades' truncated because table was already filled\n");
                } else {
                    //table does not exist yet
                    state.execute("CREATE TABLE grades(student_id int not null auto_increment, grade decimal(2,1) not null, PRIMARY KEY (student_id))");
                    logger.info("table 'grades' created\n");
                }
            } else {
                //create database and table
                state.executeUpdate("CREATE DATABASE students");
                state.executeUpdate("USE students");
                state.execute("CREATE TABLE grades(student_id int not null auto_increment, grade decimal(2,1) not null, PRIMARY KEY (student_id))");
                logger.info("database 'students' and table 'grades' created\n");
            }

            state.execute("ALTER TABLE grades AUTO_INCREMENT=757180");

            BigDecimal grades[] = {new BigDecimal("1.0"), new BigDecimal("1.3"), new BigDecimal("1.7"), new BigDecimal("2.0"),
                    new BigDecimal("2.3"), new BigDecimal("2.7"), new BigDecimal("3.0"), new BigDecimal("3.3"), new BigDecimal("3.7"), new BigDecimal("4.0"), new BigDecimal("5.0")};
            Random generator = new Random();
            int randomIndex;

            // constantly perform db operations
            while(true) {

                // clear buffers
                state = state = conn.createStatement();
                prepState = null;
                result = null;

                //create 50 entries with random grades
                for(int i=0; i<50; i++) {
                    randomIndex = generator.nextInt(grades.length);
                    prepState = conn.prepareStatement("INSERT INTO students.grades VALUES(default, ?)");
                    prepState.setObject(1, grades[randomIndex]);
                    prepState.executeUpdate();
                }

                logger.info("inserted 50 grades into students.grades\n");
                result = state.executeQuery("SELECT * FROM students.grades");
                writeResultSet(result);

                for(int i=0; i<5; i++) {
                    sleep(7000);
                    result = state.executeQuery("SELECT * FROM students.grades ORDER BY RAND() LIMIT 1");
                    result.next();
                    int student_id = result.getInt("student_id");
                    randomIndex = generator.nextInt(grades.length);
                    prepState = conn.prepareStatement("UPDATE students.grades SET grade=? WHERE student_id=?");
                    prepState.setObject(1, grades[randomIndex]);
                    prepState.setInt(2, student_id);
                    //logger.info("Executing query 'UPDATE students.grades SET grade=" + grades[randomIndex] + " WHERE student_id=" + student_id + "' ...");
                    logger.info("Student " + student_id + ": grade corrected from " + result.getDouble("grade") + " to " + grades[randomIndex]);
                    prepState.executeUpdate();
                }
                sleep(7000);

                state.execute("TRUNCATE TABLE grades");
                logger.info("table 'grades' truncated at end of loop.\n");


                /*
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
                 */
                // wait for 10 sec
                sleep(10000);
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            state.execute("TRUNCATE TABLE grades");
            System.out.println("grades truncated\n");
            logger.info("grades truncated\n");
            close();
        }
    }

    //writes table entries to stdout and log
    private void writeResultSet(ResultSet result) throws SQLException{
        while(result.next()) {
            int student_id = result.getInt("student_id");
            double grade = result.getDouble("grade");
            //logger.info(student_id + ": " + grade + "\n");
            System.out.println(student_id + ": " + grade + "\n");
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
