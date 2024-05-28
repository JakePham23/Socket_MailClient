package all;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ReadJSON {
    static String jsonFilePath = "src/all/config.json";  // Đường dẫn từ thư mục resources

    public static void main(String[] args) {
        try {
            String absolutePath = new File(jsonFilePath).getAbsolutePath();
            InputStream inputStream = new FileInputStream(absolutePath);

            // Đọc nội dung của file JSON
            String jsonContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            // Sử dụng JSONParser để chuyển đổi chuỗi JSON thành đối tượng JSON
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(jsonContent);
            JSONObject jsonObject = (JSONObject) obj;

            // Đọc thông tin từ phần "general"
            JSONObject general = (JSONObject) jsonObject.get("general");
            String username = (String) general.get("username");
            String password = (String) general.get("password");
            String mailServer = (String) general.get("mailserver");
            long smtpPort = (long) general.get("SMTP");
            long pop3Port = (long) general.get("POP3");
            long autoload = (long) general.get("Autoload");

            // Đọc thông tin từ phần "filter"
            JSONObject filter = (JSONObject) jsonObject.get("filter");
            JSONArray projectList = (JSONArray) filter.get("project");
            JSONArray importantList = (JSONArray) filter.get("important");
            JSONArray workList = (JSONArray) filter.get("work");
            JSONArray spamList = (JSONArray) filter.get("spam");

            // In ra thông tin đọc được
            System.out.println("Username: " + username);
            System.out.println("Password: " + password);
            System.out.println("Mail Server: " + mailServer);
            System.out.println("SMTP Port: " + smtpPort);
            System.out.println("POP3 Port: " + pop3Port);
            System.out.println("Autoload: " + autoload);

            System.out.println("Project List: " + projectList);
            System.out.println("Important List: " + importantList.get(0));
            System.out.println("Work List: " + workList);
            System.out.println("Spam List: " + spamList);

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }
}

    // private static void SignUp() {
    //     Scanner scanner = new Scanner(System.in);
    //     System.out.print("Username: ");
    //     String username = scanner.nextLine();
    //     System.out.print("Password: ");
    //     String password = scanner.nextLine();
    // }

    // private static String readJsonFile(String filePath) throws IOException {
    //     StringBuilder content = new StringBuilder();
    //     try (FileReader reader = new FileReader(filePath)) {
    //         int character;
    //         while ((character = reader.read()) != -1) {
    //             content.append((char) character);
    //         }
    //     }
    //     return content.toString();
    // }
//}

// import java.util.Timer;
// import java.util.TimerTask;

// //import all.ReadJSON.ReloadTask;

// public class ReadJSON {

//     public static void main(String[] args) {
//         // Tạo một đối tượng Timer
//         ArrayList<Integer> i = new ArrayList<>();
//         i = getListTypeMails("C:\\Users\\min\\source\\repos\\SocketPJ\\InboxMails.txt");
//         System.out.println(i.size());
//     }

//     // Lớp TimerTask để định nhiệm vụ cần thực hiện
//         private static ArrayList<Integer> getListTypeMails(String typeMail) {
//         ArrayList<Integer> numbersList = new ArrayList<>();
//         try {
//             File file = new File(typeMail);
//             Scanner scanner = new Scanner(file);
//             while (scanner.hasNextInt()) {
//                 int number = scanner.nextInt();
//                 numbersList.add(number);
//             }
//             scanner.close();
//         } catch (FileNotFoundException e) {
//             e.printStackTrace();
//         }
//         return numbersList;
//     }
// }






