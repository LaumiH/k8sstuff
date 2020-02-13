import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.DatabaseMetaData;

public class Jdbc {
    private Connection conn = null;
    private Statement state = null;
    private PreparedStatement prepState = null;
    private ResultSet result = null;

    public void readDatabase() throws Exception {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            //conn = DriverManager.getConnection("jdbc:mysql://192.168.39.35:32060/test?" + "user=root&password=password");

            conn = DriverManager.getConnection("jdbc:mysql://mysql/test?" + "user=root&password=password");                            //DNS with ClusterIP service name
            //mysql is the deployment DNS name
            state = conn.createStatement();

            //check if table already exists (due to previous db errors)
            DatabaseMetaData meta = conn.getMetaData();
            result = meta.getTables(null, null, "testdata", null);
            if(result != null) {
                //table already exists
                state.execute("truncate table testdata");
                System.out.println("testdata truncated\n");
            } else {
                state.execute("create table testdata(id int not null auto_increment, name varchar(30) not null, primary key (id))");
                System.out.println("testdata created\n");
            }


            //https://stackoverflow.com/questions/24378270/how-do-you-determine-if-an-insert-or-update-was-successful-using-java-and-mysql

            while(true) {
                for(int i=0; i<50; i++) {
                    prepState = conn.prepareStatement("insert into test.testdata values(default, ?)");
                    prepState.setString(1, "name" + i);
                    prepState.executeUpdate();
                    System.out.println(i);
                    sleep(200);
                }

                result = state.executeQuery("select * from test.testdata");
                writeResultSet(result);

                // wait for 10 sec
                sleep(10000);

                state.execute("truncate table testdata");
                System.out.println("testdata truncated\n");

                state = state = conn.createStatement();
                prepState = null;
                result = null;
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            state.execute("truncate table testdata");
            System.out.println("testdata truncated\n");
            close();
        }
    }

    private void writeResultSet(ResultSet result) throws SQLException{
        while(result.next()) {
            int id = result.getInt("id");
            String name = result.getString("name");
            System.out.println(id + ": " + name);
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
