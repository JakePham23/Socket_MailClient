package all;
import all.ReadJSON;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.StringJoiner;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.json.simple.JSONArray;



public class Tools {
    private static final boolean DEBUG_MODE = false;
    private static final boolean SHOW_SUCCESS_MESSAGE = true;
    private static String host, senderName, username, password;
    private static long port_s, port_r, autoload;
    private static Scanner scanner = new Scanner(System.in);
    static {
        try {
            senderName = (String) getFJSON("sendername");
            username = (String) getFJSON("username");
            password = (String) getFJSON("password");
            host = (String) getFJSON("mailserver");
            port_s = (long) getFJSON("smtp");
            port_r = (long) getFJSON("pop3");
            autoload = (long) getFJSON("autoload");
        } catch (IOException | ParseException e) {
        }
    }
    
    public void startProgram() {
        try {
            // Login();
            //System.out.println(host);
            Scanner scanner = new Scanner(System.in);
            //scanner.nextLine();
            while (true) {
                int optionIndex = 0;
                while ( optionIndex < 1 ||  optionIndex > 3) optionIndex = showOption(); clrscr();
                if (optionIndex == 1) sendMail();
                else if (optionIndex == 2) readMail();
                else { endProgram(scanner); return; }
            }
        } catch (Exception e) {
            System.out.println("Can not start the program!");
        }
    }

    private static int showOption() {
        clrscr();
        System.out.println("Please choose your option: ");
        System.out.println("1. To send mail");
        System.out.println("2. To read mail");
        System.out.println("3. To exit! ");
        System.out.print("You choose: ");
        String optionIndex_t = scanner.nextLine();
        try {
            int optionIndex = Integer.parseInt(optionIndex_t);
            return optionIndex;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // All func support to send mail
    // Func to send command to test mail server
    private static void sendCommand(OutputStream outputStream, String command) throws IOException {
        if (DEBUG_MODE) {
            System.out.println("Client: " + command);
        }
        outputStream.write((command + "\r\n").getBytes());
        outputStream.flush();
    }

    // Func to get real time
    private static String getCurrentDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");
        dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        return dateFormat.format(new Date());
    }

    // Func to check form of mail
 

    // Func to read content of sended file
    private static byte[] readFileContent(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        return Files.readAllBytes(path);
    }

    // Func to send encode of sended file to test mail server
    private static void sendFile(OutputStream outputStream, String filePath) throws IOException {
        String encode = getEncode(outputStream, filePath);
        int start = 0, chunk = 250;
        while(true) {
            if (chunk > encode.length()) {
                chunk = encode.length();
                String sendEncode = encode.substring(start, chunk);
                sendCommand(outputStream, sendEncode);
                break;
            }
            String sendEncode = encode.substring(start, chunk);
            sendCommand(outputStream, sendEncode);
            start+=250; chunk+=250;
        }
    }

    private static boolean isValidEmail(String email) {
        return email.matches("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    }

    // Func to get list of mails
    private static String getEmailList(OutputStream outputStream, BufferedReader reader) throws IOException {
        sendCommand(outputStream, "LIST");
        StringJoiner emailList = new StringJoiner("\n"); String line;
        while (!(line = reader.readLine()).equals(".")) {
            emailList.add(line);
        }
        return emailList.toString();
    }

    // Func to get last index in list mail
    private static int getLastEmailIndex(String emailListResponse) {
        String[] lines = emailListResponse.split("\n");
        if (lines.length > 1) {
            String lastLine = lines[lines.length - 2].trim();
            String[] parts = lastLine.split("\\s+");
            if (parts.length > 0 && parts[0].matches("\\d+")) {
                return Integer.parseInt(parts[0]) + 1;
            }
        }
        return -1;
    }

    // Func to get header content of a mail
    private static String extractHeaderContent(String emailHeader, String headerName) {
        Pattern pattern = Pattern.compile(headerName + ":\\s*(.*?)$", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(emailHeader);
        if (matcher.find()) return matcher.group(1);
        return null;
    }

    // Func to get subject of a mail
    private static String extractSubject(String emailHeader) {
        Pattern pattern = Pattern.compile("Subject:\\s*(.*?)$", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(emailHeader);
        if (matcher.find()) return matcher.group(1);
        return null;
    }

    // Func to clear screen
    public static void clrscr() {
        try {
            if (System.getProperty("os.name").contains("Windows"))
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            else
                Runtime.getRuntime().exec("clear");
        } catch (IOException | InterruptedException ex) {}
    }
    
    // Func to show subject of a mail
    private static void showPerSubject(Socket socket_r, int i) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket_r.getInputStream()));
        OutputStream outputStream = socket_r.getOutputStream();
        ArrayList<String> addressList = new ArrayList<>();
        String fromAddress = "", subject = null;
        sendCommand(outputStream, "RETR " + i);
        StringJoiner emailContent = new StringJoiner("\n");
        String line;
        boolean startReading = false;
        while ((line = reader.readLine()) != null) {
            if (line.equals(".")) break;
            if (startReading) emailContent.add(line);
            else if (line.isEmpty()) startReading = true;
            if (!startReading) {
                String fromAddress_temp = extractHeaderContent(line, "From");
                subject = extractSubject(line);
                if (fromAddress_temp != null) fromAddress += fromAddress_temp;
            }
        }
        ArrayList<String> nAA = new ArrayList<>();
        nAA = StringSplitExample(fromAddress);
        String name = nAA.get(0), ad = nAA.get(1);
        System.out.println(name + "-" + ad);
        if (!getStatusMails(i)) System.out.print("<Unread> ");
        addressList.add(fromAddress);
        System.out.print("From: " + fromAddress + "/ ");
        if (subject != null) System.out.println("Subject: " + subject);
    }


    private static void showProjectPerFolder(Socket socket_r, ArrayList<Integer> Folder) throws IOException {
        for (int i = 0; i < Folder.size(); i++) {
            System.out.print(i+1 + ": ");
            showPerSubject(socket_r, Folder.get(i));
        }
    }

    // Func to show content of a mail
    private static void showContentMail(Socket socket_r, int readIndexMail) throws IOException{
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket_r.getInputStream()));
        OutputStream outputStream = socket_r.getOutputStream();
        String fromAddress = "", toAddress = "", ccAddress = "", bccAddress = "", subject = null, filePath = null;      
        sendCommand(outputStream, "RETR " + readIndexMail);
        StringJoiner emailContent = new StringJoiner("\n");
        String line;
        boolean startReading = false;
        while ((line = reader.readLine()) != null) {
            if (line.equals(".")) break;
            if (startReading) emailContent.add(line);
            else if (line.isEmpty()) startReading = true;
            if (!startReading) {
                String fromAddress_temp = extractHeaderContent(line, "From");
                String toAddress_temp = extractHeaderContent(line, "To");
                String ccAddress_temp = extractHeaderContent(line, "Cc");
                String bccAddress_temp = extractHeaderContent(line, "Bcc");
                subject = extractSubject(line);
                if (fromAddress_temp != null) fromAddress += fromAddress_temp;
                if (toAddress_temp != null) toAddress += toAddress_temp + "/ ";
                if (ccAddress_temp != null && !ccAddress_temp.equals(bccAddress_temp)) ccAddress += ccAddress_temp + "/ ";
                if (bccAddress_temp != null) bccAddress += bccAddress_temp + "/ ";
            }
        }
        System.out.println("From Address: " + fromAddress);
        System.out.println("To Address: " + toAddress);
        System.out.println("Cc Address: " + ccAddress);
        if (bccAddress.contains(username)) System.out.println("Bcc: " + username);
        if (subject != null) System.out.println("Subject: " + subject);
        String emailBody = emailContent.toString();
        ArrayList<String> listFile = getListFileName(emailBody);
        //System.out.println(emailBody);
        ArrayList<String> encodedList = getListEncoded(emailBody, "Content-Transfer-Encoding: base64", "------------foms3E4p9a4ra75te5035dm");
        
        //System.out.print(encodedList.size());
        String contentMail = getContentMail(emailBody, "Content-Transfer-Encoding: 7bit", "------------foms3E4p9a4ra75te5035dm");
        System.out.println("Email Content:\n" + contentMail);
                
        ArrayList<String> fileName = new ArrayList<>();
        for (int i = 0; i < listFile.size(); i++) {
            String t = getFileName(listFile.get(i));
            fileName.add(t);
        }
        if (!listFile.isEmpty()) {
            System.out.println("Attached File: ");
            for (int i = 1; i <= fileName.size(); i++) {
                System.out.println(i + ": " + fileName.get(i-1));
            }
            boolean checkk = false; String k = "";
            while(!checkk) {
                System.out.print("Enter number of files (saperate by commas, example: 1, 3, ...) to download or \"0\" to skip: ");
                k = scanner.nextLine();
                checkk = checkSumOfFile(k, encodedList.size());
                if (!checkk) System.out.println("Invalid data, please enter again!\n");
            }
            if (!k.equals("0")) {
                System.out.print("Enter Folder path to save: ");
                String pathFolder = scanner.nextLine();
                ArrayList<Integer> nus = extractNumbers(k);
                for (int i = 0; i < nus.size(); i++) {
                int index = nus.get(i)-1;
                String encoded_with_space = encodedList.get(index);
                String encoded = encoded_with_space.replaceAll("\\s+", "");
                downFile(encoded, pathFolder, fileName. get(index));
                }
            }
        }
        System.out.println("\n\nPress Enter to continue read mails!");
    }
    
    // Func to get a list address of a list mail
    private static ArrayList<String> getListAddress(Socket socket_r, ArrayList<String> contentList, ArrayList<String> subjectList) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket_r.getInputStream()));
        OutputStream outputStream = socket_r.getOutputStream();
        String emailListResponse = getEmailList(outputStream, reader);
        ArrayList<String> addressList = new ArrayList<>();
        int lastEmailIndex = getLastEmailIndex(emailListResponse);
        String subject = "";
        for (int i = 1; i<= lastEmailIndex; i++ ) {
            String fromAddress = "";
            sendCommand(outputStream, "RETR " + i);
            StringJoiner emailContent = new StringJoiner("\n");
            String line;
            boolean startReading = false;
            while ((line = reader.readLine()) != null) {
                if (line.equals(".")) break;
                if (startReading) emailContent.add(line);
                else if (line.isEmpty()) startReading = true;
                if (!startReading) {
                    String fromAddress_temp = extractHeaderContent(line, "From");
                    if (fromAddress_temp != null) fromAddress += fromAddress_temp;
                    subject = extractSubject(line);
                }
            }
            String emailBody = emailContent.toString();
            contentList.add(emailBody);
            addressList.add(fromAddress);
            subjectList.add(subject);
        }
        return addressList;
    }



    // Func to send a mail
    public void sendMail() throws IOException, ParseException{
        System.out.println("From: <" + senderName + "> " + username);
        List<String> recipientEmails = new ArrayList<>();
        List<String> ccEmails = new ArrayList<>();
        List<String> bccEmails = new ArrayList<>();

        boolean isValidEmail = false;
        String emailList = "";
        while (!isValidEmail) {
            System.out.print("To: ");
            emailList = scanner.nextLine();
            if (!emailList.isEmpty()) {
                int checkValid = 0;
                String[] emailArray = emailList.split(",");
                for (String email : emailArray) {
                    String trimmedEmail = email.trim();
                    if (!isValidEmail(trimmedEmail)) {
                        checkValid++;
                    }
                }
                if (checkValid != 0) {
                    System.out.println("Invalid email format, please enter again!");
                    continue;
                }
                for (String email : emailArray) {
                    String trimmedEmail = email.trim();
                    recipientEmails.add(trimmedEmail);
                }
            }
            isValidEmail = true;
        }

        isValidEmail = false;
        String ccList = "";
        while (!isValidEmail) {
            System.out.print("Cc: ");
            ccList = scanner.nextLine();
            if (!ccList.isEmpty()) {
                int checkValid = 0;
                String[] ccArray = ccList.split(",");
                for (String email : ccArray) {
                    String trimmedEmail = email.trim();
                    if (!isValidEmail(trimmedEmail)) {
                        checkValid++;
                    }
                }
                if (checkValid != 0) {
                    System.out.println("Invalid email format, please enter again!");
                    continue;
                }
                for (String ccEmail : ccArray) {
                    String trimmedCcEmail = ccEmail.trim();
                        ccEmails.add(trimmedCcEmail);
                }
            }
            isValidEmail = true;
        }

        isValidEmail = false;
        String bccList = "";
        while (!isValidEmail) {
            System.out.print("Bcc: ");
            bccList = scanner.nextLine();
            if (!bccList.isEmpty()) {
                int checkValid = 0;
                String[] bccArray = bccList.split(",");
                 for (String email : bccArray) {
                    String trimmedEmail = email.trim();
                    if (!isValidEmail(trimmedEmail)) {
                        checkValid++;
                    }
                }
                if (checkValid != 0) {
                    System.out.println("Invalid email format, please enter again!");
                    continue;
                }
                for (String bccEmail : bccArray) {
                    String trimmedBccEmail = bccEmail.trim();
                        bccEmails.add(trimmedBccEmail);
                }
            }
            isValidEmail = true;
        }

        if (emailList == "" && ccList == "" && bccList == "") {
            System.out.println("Can not send mail without receiver, please try again!");
            scanner.nextLine();
            clrscr();
            sendMail();
        }

        System.out.print("Subject: ");
        String subject = scanner.nextLine();
        System.out.print("Content: ");
        String body = scanner.nextLine();
        System.out.print("Enter \"F\" to send Attached File or any key to skip: ");
        String sF = scanner.nextLine();
        boolean checkSendFile = false;
        int numberOfFile = 0;
        ArrayList<String> listFilePath = new ArrayList<>();
        if (sF.equals("f")) {
            checkSendFile = true;
        }
        if (checkSendFile) {
            while (numberOfFile <= 0) {
                System.out.print("Enter number of file to send: ");
                String get = scanner.nextLine();
                try {
                    numberOfFile = Integer.parseInt(get);
                    if (numberOfFile <= 0) { numberOfFile = 0; 
                        System.out.println("Invalid data, please enter again!"); }
                } catch (NumberFormatException e) {
                    numberOfFile = 0;
                    System.out.println("Invalid data, please enter again!");
                }
            }
            for (int i = 1; i <= numberOfFile; i++) {
                if (numberOfFile == 1) 
                System.out.print("Enter file path: ");
                else 
                System.out.print("Enter file " + i + " path: ");
                String filePath = scanner.nextLine();
                File file = new File(filePath);
                if (!file.exists() || !file.isFile()) System.out.println("This file is not exist! Can't send this file!");
                else { 
                    if (checkDataFile(filePath)) {
                        listFilePath.add(filePath);
                    }
                    else {
                        System.out.println("This file's capacity exceeds 3MB, can't send!");
                        scanner.nextLine();
                    }
                }
            }
        }
        String typeFolder = ftr(subject, body);
        try {
            Socket socket_s = new Socket(host, (int) port_s);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket_s.getInputStream()));
            OutputStream outputStream = socket_s.getOutputStream();
            String welcomeMessage = reader.readLine();
            if (DEBUG_MODE) {
                System.out.println("Server: " + welcomeMessage);
            }
            sendCommand(outputStream, "EHLO example.com");
            String response = reader.readLine();
            while (response != null) {
                if (DEBUG_MODE) {
                    System.out.println("Server: " + response);
                }
                if (response.startsWith("250 ")) {
                    break;
                }
                response = reader.readLine();
            }           
            sendCommand(outputStream, "MAIL FROM: <" + username + ">");
            for (String recipientEmail : recipientEmails) {
                sendCommand(outputStream, "RCPT TO: <" + recipientEmail + ">");
            }
            for (String ccEmail : ccEmails) {
                sendCommand(outputStream, "RCPT TO: <" + ccEmail + ">");
            }
            for (String bccEmail : bccEmails) {
                sendCommand(outputStream, "RCPT TO: <" + bccEmail + ">");
            }
            sendCommand(outputStream, "DATA");
            String boundary = "------------foms3E4p9a4ra75te5035dm";
            if (!checkSendFile) sendCommand(outputStream, "Content-Type: text/plain");
            else { 
            sendCommand(outputStream, "Content-Type: multipart/mixed; boundary=\"" + boundary + "\"" ); }
            sendCommand(outputStream, "Date: " + getCurrentDate());
            sendCommand(outputStream, "MIME-Version: 1.0");
            sendCommand(outputStream, "User-Agent: Unknown");
            sendCommand(outputStream, "Content-Language: en-US");
            sendCommand(outputStream, "To: " + String.join(", ", recipientEmails));
            if (ccList != null) sendCommand(outputStream, "Cc: " + String.join(", ", ccEmails));
            if (bccList != null) sendCommand(outputStream, "Bcc: " + String.join(", ", bccEmails));
            sendCommand(outputStream, "From: <" + senderName + "> " + username);
            sendCommand(outputStream, "Subject: " + subject);
            sendCommand(outputStream, "");
            sendCommand(outputStream, "Folder = " + typeFolder);
            if (checkSendFile) sendCommand(outputStream, "This is a multi-part message in MIME format");
            sendCommand(outputStream, "Content-Type: text/plain; charset=\"UTF-8\"; format=flowed");
            sendCommand(outputStream, "Content-Transfer-Encoding: 7bit");
            sendCommand(outputStream, "");
            sendCommand(outputStream, body);
            sendCommand(outputStream, "");
            sendCommand(outputStream, boundary);

            if (checkSendFile) {
                for (int i = 0; i < listFilePath.size(); i++) {
                    String fileName = getFileName(listFilePath.get(i));
                    String type = getTypeFile(listFilePath.get(i));
                    sendCommand(outputStream, "Content-Type: application/" + type + "; name=\"" + fileName +"\"");
                    sendCommand(outputStream, "Content-Dispotion: attachment; filename=\"" + fileName +"\"");
                    sendCommand(outputStream, "Content-Transfer-Encoding: base64");
                    sendCommand(outputStream, "");
                    sendFile(outputStream, listFilePath.get(i));
                    sendCommand(outputStream, "");
                    sendCommand(outputStream, boundary);
                }
            }

            sendCommand(outputStream, ".");
            sendCommand(outputStream, "QUIT");
            if (SHOW_SUCCESS_MESSAGE) {
                System.out.println("\n\nEmails sent successfully!\n\n");
                scanner.nextLine();
            }
            socket_s.close();
            }  catch (IOException e) {
                    e.printStackTrace();
            }
    }
        
    //Func to read a mail
    private static void readMail() throws IOException{
        clrscr();
        int typeMail = 0; String nameTypeFolder = null;
        while (typeMail <= 0 || typeMail > 5) {
            System.out.println("List of folders in your Mailbox: ");
            System.out.println("1. Inbox ");
            System.out.println("2. Project ");
            System.out.println("3. Important ");
            System.out.println("4. Work");
            System.out.println("5. Spam");
            System.out.print("Enter folder mails: ");
            String getType = scanner.nextLine();
            try {
                typeMail = Integer.parseInt(getType);
                if (typeMail <= 0) typeMail = 0;
            } catch (NumberFormatException e) {
                typeMail = 0;
            }
            if (typeMail == 1)  nameTypeFolder = "Inbox";
            else if (typeMail == 2)  nameTypeFolder = "Project"; 
            else if (typeMail == 3)  nameTypeFolder = "Important";
            else if (typeMail == 4)  nameTypeFolder = "Work";
            else if (typeMail == 5)  nameTypeFolder = "Spam";
            clrscr();
        }
        readMail_cont(nameTypeFolder);
    }

    private static void readMail_cont(String typeFolder) throws UnknownHostException, IOException {
        Socket socket_r = new Socket(host, (int) port_r);
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket_r.getInputStream()));
        OutputStream outputStream = socket_r.getOutputStream();
        sendCommand(outputStream, "USER " + username);
        sendCommand(outputStream, "PASS " + password);
        sendCommand(outputStream, "STAT");
        String emailListResponse = getEmailList(outputStream, reader);
        int lastEmailIndex = getLastEmailIndex(emailListResponse);
        boolean check = true;
        ArrayList<Integer> listz = getFolder(socket_r, typeFolder);
        if (listz.size() == 0) { clrscr(); System.out.println("No mails founded in this folder!"); System.out.print("Press any key to continue read mail!");
        scanner.nextLine();return; } clrscr();
        check = true;
        System.out.print("Folder "); System.out.print(typeFolder); System.out.println(":");
        //loadMailAuto(socket_r, lastEmailIndex, typeFolder, check);
        showProjectPerFolder(socket_r, listz);
        System.out.println("Enter \"F\" to filter mail!");
        int readIndexMail = 0;
        boolean checkFilter = false;
        while (readIndexMail <= 0) {
            System.out.print("\nEnter the index number mail you wanna read: ");
            String readIndexMail_t = scanner.nextLine();
            try {
                readIndexMail = Integer.parseInt(readIndexMail_t);
                if (readIndexMail <= 0 || readIndexMail > listz.size()) readIndexMail = 0;
            } catch (NumberFormatException e) {
                if (readIndexMail_t.equals("f"))  { checkFilter = true; break; }
                readIndexMail = 0;
            }
            if (readIndexMail == 0) System.out.println("Invalid data, please enter again!");
        }
        check = false;
        ArrayList<String> listContent = new ArrayList<>();
        ArrayList<String> listSubject = new ArrayList<>();
        ArrayList<String> addressList = getListAddress(socket_r, listContent, listSubject);
        boolean continuE = false;
        if (checkFilter) {
            System.out.println("Choose type of filter: ");
            System.out.println("1. Filled by address");
            System.out.println("2. Filled by subject");
            System.out.println("3. Filled by content");
            System.out.print("Your option filter: ");
            int indexOptionFilter = scanner.nextInt();
            scanner.nextLine();

            if (indexOptionFilter == 1) {
                listz.clear();
                System.out.print("Enter address: ");
                String filterAddress = scanner.nextLine();
                int clrscr_do = 0;
                for (int i = 0; i < addressList.size(); i++) {
                    String str = addressList.get(i);
                    String[] words = str.split("\\s+");
                    String lastFrom = words[words.length - 1];
                    boolean areContains = lastFrom.contains(filterAddress);
                    if (areContains) {
                        ++clrscr_do; if (clrscr_do == 1) {
                            clrscr();
                            if (isValidEmail(filterAddress))
                            System.out.println("Mails sended by " + filterAddress + ": ");
                            else System.out.println("Mails with related address results: ");
                        }
                        listz.add(i+1);
                    }
                } 
                Collections.sort(listz, Collections.reverseOrder());
                showProjectPerFolder(socket_r, listz);
                continuE = true;
            }

            else if (indexOptionFilter == 3) {
                listz.clear();
                System.out.print("Enter content: ");
                String filterContent = scanner.nextLine();
                int clrscr_do = 0;
                for (int i = 0; i < listContent.size(); i++) {
                    String str = listContent.get(i);
                    boolean areContains = str.contains(filterContent);
                    if (areContains) {
                        ++clrscr_do; if (clrscr_do == 1) {
                            clrscr();
                            System.out.println("Mails with related content \"" + filterContent + "\" results: ");
                        }
                        listz.add(i+1);
                    }
                } 
                Collections.sort(listz, Collections.reverseOrder());
                showProjectPerFolder(socket_r, listz);
                continuE = true;
            }

            else if (indexOptionFilter == 2) {
                listz.clear();
                System.out.print("Enter subject: ");
                String filterSubject = scanner.nextLine();
                int clrscr_do = 0;
                System.out.println(listSubject.size());
                for (int i = 0; i < listSubject.size(); i++) {
                    String str = listSubject.get(i);
                    if (str != null) {
                    boolean areContains = str.contains(filterSubject);
                        if (areContains) {
                            ++clrscr_do; if (clrscr_do == 1) {
                                clrscr();
                                System.out.println("Mails with related subject \"" + filterSubject + "\" results: ");
                            }
                            listz.add(i+1);
                        }
                    }
                } 
                Collections.sort(listz, Collections.reverseOrder());
                showProjectPerFolder(socket_r, listz);
                continuE = true;
            }
        }

        if (continuE) {
            System.out.print("\nEnter the index number mail you wanna read: ");
            readIndexMail = scanner.nextInt();
            clrscr(); 
            showContentMail(socket_r, listz.get(readIndexMail-1)); 
            makeStatusMails(listz.get(readIndexMail-1));
            scanner.nextLine();
            scanner.nextLine();
            return;
        }
        clrscr();
        showContentMail(socket_r, listz.get(readIndexMail-1));  
        makeStatusMails(listz.get(readIndexMail-1));
        scanner.nextLine();
        return;
    }

    // Func to end program
    public void endProgram(Scanner scanner) throws IOException {
        scanner.close();
        System.out.println("THE PROGRAM IS END!");
    }

    // Func to show option to user

    
    // Func to get a list type of list mail
    private static ArrayList<Integer> getListTypeMails(String typeMail) {
        ArrayList<Integer> numbersList = new ArrayList<>();
        try {
            File file = new File(typeMail);
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextInt()) {
                int number = scanner.nextInt();
                numbersList.add(number);
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return numbersList;
    }

    // Func to make a status of a mail
    private static void makeStatusMails(int numberMail) throws IOException {
        ArrayList<Integer> list = new ArrayList<>();
        String statusMails = "src/all/StatusMails.txt";
        String absolutePath = new File(statusMails).getAbsolutePath();
        list = getListTypeMails(absolutePath);
        if (!list.contains(numberMail)) {
            list.add(numberMail);
            Collections.sort(list);
            FileWriter writer = new FileWriter(absolutePath, false);
            for (int i = 0; i<list.size(); i++) {
                String r = Integer.toString(list.get(i));
                writer.write(r); writer.write(" ");        
            }
            writer.close();
        }  
    }

    // Func to get status of a mail
    private static boolean getStatusMails(int numberMail) {
        ArrayList<Integer> list = new ArrayList<>();
        String statusMails = "src/all/StatusMails.txt";
        String absolutePath = new File(statusMails).getAbsolutePath();
        list = getListTypeMails(absolutePath);
        if (list.contains(numberMail)) return true;
        return false;
    }

    // Func to get content of a mail
    private static String getContentMail(String text, String startLine, String endLine) {
        String[] lines = text.split("\n");
        boolean reading = false;
        StringBuilder resultBuilder = new StringBuilder();
        for (String line : lines) {
            if (line.contains(startLine)) {
                reading = true;
                continue;
            }
            if (line.contains(endLine)) {
                break;
            }
            if (reading) {
                resultBuilder.append(line).append("\n");
            }
        }
        return resultBuilder.toString();
    }

    // Func to download a file from a mail
    public static void downFile(String encodedData, String pathFolder, String fileName) {
        byte[] decodedData = Base64.getDecoder().decode(encodedData);
        String outputPath = pathFolder + '/' + fileName;
        System.out.println(outputPath);
        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            fos.write(decodedData);
            System.out.println("File " + fileName + " saved in " + outputPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Func to get type a dowmload file
    private static String getTypeFile(String filePath) {
        int lastDotIndex = filePath.lastIndexOf('.');
        if (lastDotIndex != -1 && lastDotIndex < filePath.length() - 1) {
            return filePath.substring(lastDotIndex + 1);
        } else {
            return filePath;
        }
    }

    // Func to get encode of a file
    private static String getEncode(OutputStream outputStream, String filePath) throws IOException {
        byte[] fileContent = readFileContent(filePath);
        String encodedContent = Base64.getEncoder().encodeToString(fileContent);
        return encodedContent;
    }

    // Func to get list encode
    private static ArrayList<String> getListEncoded(String multilineText, String startLine, String endLine) {
        Scanner scanner = new Scanner(multilineText);
        ArrayList<String> linesBetweenAB = new ArrayList<>();
        boolean reading = false;
        String goof = "";
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.equals(startLine)) {
                goof = "";
                reading = true;
                continue;
            }
            if (reading) {
                if (line.equals(endLine)) {
                    linesBetweenAB.add(goof);
                    reading = false;
                } else {
                    goof += line;
                }
            }
        }
        return linesBetweenAB;
    }

    // Func to extract a list of number from an array
    private static ArrayList<Integer> extractNumbers(String numbersString) {
        String regex = "\\s*,\\s*";
        Pattern pattern = Pattern.compile(regex);
        String[] numberStrings = pattern.split(numbersString);
        ArrayList<Integer> numbersList = new ArrayList<>();
        for (String numberStr : numberStrings) {
            try {
                int number = Integer.parseInt(numberStr);
                numbersList.add(number);
            } catch (NumberFormatException e) {
                System.out.println("Invalid number format: " + numberStr);
            }
        }
        return numbersList;
    }

    // Func to get list file name
    private static ArrayList<String> getListFileName(String text) {
        String regex = "filename=\"([^\"]+)\"";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        ArrayList<String> filenames = new ArrayList<>();
        while (matcher.find()) {
            String filename = matcher.group(1);
            filenames.add(filename);
        }
        return filenames;
    }

    // Func to get file name
    private static String getFileName(String inputString) {
        int lastDotIndex = inputString.lastIndexOf('\\');
        if (lastDotIndex != -1 && lastDotIndex < inputString.length() - 1) {
            // Sử dụng phương thức substring để lấy phần sau dấu "." cuối cùng
            return inputString.substring(lastDotIndex + 1);
        } else {
            return inputString;
        }
    }

    private static boolean checkDataFile(String filePath) {
        File file = new File(filePath);
        long MB = 0;
        if (file.exists()) {
            long fileSizeInBytes = file.length()/1000;
            MB = fileSizeInBytes/1024;
        } 
        if (MB > 3) return false;
        return true;
    }

    public ArrayList<String> Login() throws IOException, ParseException {
        System.out.println("-----LOGIN-----");
        System.out.println("Suggest Account:: username: \"minhtr@example.com\" - password: \"minhit\"\n");
        System.out.print("Username: ");
        String userName = scanner.nextLine();
        System.out.print("Password: ");
        String passWord = scanner.nextLine();
        while (!userName.equals(username) || (!passWord.equals(password))) {
            System.out.println("Username or Password is incorrect, please try again!");
            scanner.nextLine();
            clrscr();
            System.out.println("-----LOGIN-----");
            System.out.print("Username: ");
            userName = scanner.nextLine();
            System.out.print("Password: ");
            passWord = scanner.nextLine();
        }
        ArrayList<String> account = new ArrayList<>();
        account.add(username);
        account.add(password);
        return account;
    }



    private static void loadMailAuto(Socket socket_r, int lastEmailIndex_current, String typeFolder, boolean check) throws IOException {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        
        scheduler.scheduleAtFixedRate(() -> {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket_r.getInputStream()));
                OutputStream outputStream = socket_r.getOutputStream();
                sendCommand(outputStream, "USER " + username);
                sendCommand(outputStream, "PASS " + password);
                sendCommand(outputStream, "STAT");
                String emailListResponse = getEmailList(outputStream, reader);
                int lastEmailIndex = getLastEmailIndex(emailListResponse);
                
                // Kiểm tra điều kiện
                if (lastEmailIndex == lastEmailIndex_current + 1) {
                    sendCommand(outputStream, "RETR " + lastEmailIndex);
                    StringJoiner emailContent = new StringJoiner("\n");
                    String line;
                    boolean startReading = false;
                    while ((line = reader.readLine()) != null) {
                        if (line.equals(".")) break;
                        if (startReading) emailContent.add(line);
                        else if (line.isEmpty()) startReading = true;
                    }
                    String emailBody = emailContent.toString();
                    String folder = getNumberFolder(emailBody);
                    if (folder == typeFolder && check) {
                    clrscr();
                    readMail_cont(typeFolder);
                    scheduler.shutdown();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, 0, autoload, TimeUnit.SECONDS);
    }

    private static String ftr(String Subject, String Body) throws IOException, ParseException {
        String jsonFilePath = "all/config.json";
        InputStream inputStream = ReadJSON.class.getClassLoader().getResourceAsStream(jsonFilePath);
        String jsonContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(jsonContent);
        JSONObject jsonObject = (JSONObject) obj;
        JSONObject filter = (JSONObject) jsonObject.get("filter");
        JSONArray projectList = (JSONArray) filter.get("project");
        JSONArray importantList = (JSONArray) filter.get("important");
        JSONArray workList = (JSONArray) filter.get("work");
        JSONArray spamList = (JSONArray) filter.get("spam");
        for (int i = 0; i < projectList.size(); i++) {
            String checkKey = (String) projectList.get(i);
            if (Subject.contains(checkKey) || Body.contains(checkKey)) return "Project";
        }
        for (int i = 0; i < importantList.size(); i++) {
            String checkKey = (String) importantList.get(i);
            if (Subject.contains(checkKey) || Body.contains(checkKey)) return "Important";
        }
        for (int i = 0; i < workList.size(); i++) {
            String checkKey = (String) workList.get(i);
            if (Subject.contains(checkKey) || Body.contains(checkKey)) return "Work";
        }
          for (int i = 0; i < spamList.size(); i++) {
            String checkKey = (String) spamList.get(i);
            if (Subject.contains(checkKey) || Body.contains(checkKey)) return "Spam";
        }
        return "Inbox";
    }    

    private static Object getFJSON(String getWhat) throws IOException, ParseException  {
        String jsonFilePath_t = "src/all/config.json";
        String jsonFilePath = new File(jsonFilePath_t).getAbsolutePath();
        InputStream inputStream = new FileInputStream(jsonFilePath);
        String jsonContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(jsonContent);
        JSONObject jsonObject = (JSONObject) obj;
        JSONObject general = (JSONObject) jsonObject.get("general");
        String username = (String) general.get("username");
        String password = (String) general.get("password");
        String senderName = (String) general.get("sendername");
        String mailServer = (String) general.get("mailserver");
        long smtpPort = (long) general.get("SMTP");
        long pop3Port = (long) general.get("POP3");
        long autoload = (long) general.get("Autoload");
        JSONObject filter = (JSONObject) jsonObject.get("filter");
        JSONArray projectList = (JSONArray) filter.get("project");
        JSONArray importantList = (JSONArray) filter.get("important");
        JSONArray workList = (JSONArray) filter.get("work");
        JSONArray spamList = (JSONArray) filter.get("spam");
        if (getWhat.equals("username")) return username;
        else if (getWhat.equals("password")) return password;
        else if (getWhat.equals("mailserver")) return mailServer;
        else if (getWhat.equals("smtp")) return smtpPort;
        else if (getWhat.equals("pop3")) return pop3Port;
        else if (getWhat.equals("autoload")) return autoload;
        else if (getWhat.equals("project")) return projectList;
        else if (getWhat.equals("important")) return importantList;
        else if (getWhat.equals("work")) return workList;
        else if (getWhat.equals("spam")) return spamList;
        else if (getWhat.equals("sendername")) return senderName;
        return -1;
    }

    private static ArrayList<Integer> getFolder(Socket socket_r, String type) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket_r.getInputStream()));
        OutputStream outputStream = socket_r.getOutputStream();
        sendCommand(outputStream, "USER " + username);
        sendCommand(outputStream, "PASS " + password);
        sendCommand(outputStream, "STAT");
        String emailListResponse = getEmailList(outputStream, reader);
        ArrayList<Integer> list = new ArrayList<>();
        int lastEmailIndex = getLastEmailIndex(emailListResponse); String type_v = null;  
        System.out.println(lastEmailIndex + "t");
        for (int i = 1; i <= lastEmailIndex; i++) {
            sendCommand(outputStream, "RETR " + i);
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equals(".")) break;
                if (line.contains("Folder")) {
                    type_v = getNumberFolder(line);
                    if (type_v.equals(type)) list.add(i);
                }
            } 
        }
        System.out.println(list.size());
        Collections.sort(list, Collections.reverseOrder());
        return list;
    }  

    public static String getNumberFolder(String inputString) {
        String regex = "^Folder = (.+)$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(inputString);
        if (matcher.find()) {
            return matcher.group(1).trim();
        } return null;
    }

    private static boolean containsAlphabets(String input) {
        return input.matches(".*[a-zA-Z].*");
    }

    private static boolean checkSumOfFile(String input, int maxValue) {
        if (containsAlphabets(input)) return false;
        String[] numbers = input.split(",");
        for (String number : numbers) {
            try {
                int numericValue = Integer.parseInt(number.trim());
                if (numericValue > maxValue) {
                    return false;
                }
            } catch (NumberFormatException e) {
            }
        }
        return true; 
    }

    /////////////////////////////////

    public static ArrayList<String> StringSplitExample(String inputString) {
        String regex = "<(.*?)>";
        String name = null, address = null;
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(inputString);
        if (matcher.find()) {
            name = matcher.group(1).trim();
            address = inputString.replace(matcher.group(0), "").trim();
        }   
        ArrayList<String> nAA = new ArrayList<>();
        nAA.add(name); nAA.add(address);
        return nAA;
    }


}




