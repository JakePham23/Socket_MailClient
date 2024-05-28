package all;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.io.BufferedReader;
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
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.json.simple.JSONArray;


public class UI {
    private JFrame frame;
    private JPanel mainPanel;
    private JPanel sendMailPanel;
    private JComboBox<String> folderComboBox;
    private List<String> attachedFiles = new ArrayList<>();
    private static String host, senderName, username, password;
    private static long port_s, port_r, autoload;
    private static Socket socket_s, socket_r;
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
            socket_s = new Socket(host, (int) port_s); 
            socket_r = new Socket(host, (int) port_r); 
        } catch (IOException | ParseException e) {}
    }

    public static void main(String[] args) throws IOException {
        SwingUtilities.invokeLater(() -> {
            UI app = new UI();
            try {
                app.createAndShowGUI();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void createAndShowGUI() throws IOException {
        frame = new JFrame("Email Application");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainPanel = new JPanel();
        sendMailPanel = createSendMailPanel();
        List<Integer> list = new ArrayList<>();
        showFolder("Inbox", list);
        frame.getContentPane().add(mainPanel);
        frame.setSize(1250, 600);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
    
    private JPanel createSendMailPanel() throws UnknownHostException, IOException {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
    
        JPanel toPanel = new JPanel(new BorderLayout());
        JLabel toLabel = new JLabel("To:");
        JTextField toTextField = new JTextField(40);
        toPanel.add(toLabel, BorderLayout.WEST);
        toPanel.add(toTextField, BorderLayout.CENTER);
    
        JPanel ccPanel = new JPanel(new BorderLayout());
        JLabel ccLabel = new JLabel("Cc:");
        JTextField ccTextField = new JTextField(40);
        ccPanel.add(ccLabel, BorderLayout.WEST);
        ccPanel.add(ccTextField, BorderLayout.CENTER);
        ccPanel.setVisible(false);
        final boolean[] ccHide = { true };
    
        JPanel bccPanel = new JPanel(new BorderLayout());
        JLabel bccLabel = new JLabel("Bcc:");
        JTextField bccTextField = new JTextField(40);
        bccPanel.add(bccLabel, BorderLayout.WEST);
        bccPanel.add(bccTextField, BorderLayout.CENTER);
        bccPanel.setVisible(false);
        final boolean[] bccHide = { true };
    
        JButton ccButton = new JButton("Cc");
        JButton bccButton = new JButton("Bcc");
    
        JLabel subjectLabel = new JLabel("Subject:");
        JTextField subjectTextField = new JTextField(40);
    
        JLabel contentLabel = new JLabel("Content:");
        JTextArea contentTextArea = new JTextArea(10, 40);
        JScrollPane contentScrollPane = new JScrollPane(contentTextArea);
    
        JButton attachButton = new JButton("Attach File");
        JLabel attachedFileLabel = new JLabel("Attached File: ");
        JPanel attachedFilePanel = new JPanel(new BorderLayout());
        attachedFilePanel.add(attachedFileLabel, BorderLayout.WEST);
        attachedFilePanel.setVisible(false); attachedFiles.clear();
        JLabel attachedFilesLabel = new JLabel("Attached Files: ");
        gbc.gridy = 6; panel.add(attachedFilesLabel, gbc);
        
        JButton sendButton = new JButton("Send Email");
        JButton backButton = new JButton("Back");
    
        ccLabel.setVisible(false); ccTextField.setVisible(false);
        bccLabel.setVisible(false); bccTextField.setVisible(false);
        
        ccButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ccPanel.setVisible(true); bccPanel.setVisible(false);
            }
        });
    
        bccButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                bccLabel.setVisible(true); bccTextField.setVisible(true);
            }
        });
    
        JToggleButton ccToggle = new JToggleButton("Cc");
        JToggleButton bccToggle = new JToggleButton("Bcc");
    
        ccToggle.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (ccHide[0]) ccLabel.setVisible(true);
                else ccLabel.setVisible(false); ccHide[0] = !ccHide[0];
                ccTextField.setVisible(ccToggle.isSelected());
                panel.revalidate(); panel.repaint();
            }
        });
    
        bccToggle.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (bccHide[0]) bccLabel.setVisible(true);
                else bccLabel.setVisible(false); bccHide[0] = !bccHide[0];
                bccTextField.setVisible(bccToggle.isSelected());
                panel.revalidate(); panel.repaint();
            }
        });
    
        attachButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setMultiSelectionEnabled(true);
                int result = fileChooser.showOpenDialog(frame);
        
                if (result == JFileChooser.APPROVE_OPTION) {
                    java.io.File[] selectedFiles = fileChooser.getSelectedFiles();
                    long totalSize = Arrays.stream(selectedFiles)
                            .mapToLong(file -> file.length())
                            .sum();
                    if (totalSize > 3 * 1024 * 1024) {
                        JOptionPane.showMessageDialog(frame, "Data of file is more than 3MB. Can't send.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    attachedFiles.addAll(Arrays.stream(selectedFiles)
                            .map(file -> file.getAbsolutePath())
                            .collect(Collectors.toList()));
                    if (!attachedFiles.isEmpty()) {
                        StringBuilder filesText = new StringBuilder("Attached Files: ");
                        for (String filePath : attachedFiles) {
                            filesText.append(new File(filePath).getName()).append(", ");
                        }
                        filesText.setLength(filesText.length() - 2);
                        attachedFilesLabel.setText(filesText.toString());
                        attachedFilesLabel.setVisible(true);
                    } else {
                        attachedFilesLabel.setText("Attached Files: ");
                        attachedFilesLabel.setVisible(false);
                    }
                }
            }
        });

        sendButton.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                String to = toTextField.getText();
                String cc = ccTextField.getText();
                String bcc = bccTextField.getText();
                String subject = subjectTextField.getText();
                String content = contentTextArea.getText();
                String[] toArray = to.split(",\\s*");
                String[] ccArray = cc.split(",\\s*");
                String[] bccArray = bcc.split(",\\s*");
                List<String> toList = Arrays.asList(toArray);
                List<String> ccList = Arrays.asList(ccArray);
                List<String> bccList = Arrays.asList(bccArray);
                if (!toList.get(0).equals("")) {
                    for (String toEmail : toList) {
                        if (!isValidEmail(toEmail)) {
                            JOptionPane.showMessageDialog(frame, "Email address at To is not valid, enter again.", "Error", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    }
                }
                if (!ccList.get(0).equals("")) {
                    for (String ccEmail : ccList) {
                        if (!isValidEmail(ccEmail)) {
                            JOptionPane.showMessageDialog(frame, "Email address at Cc is not valid, enter again.", "Error", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    }
                }
                if (!bccList.get(0).equals("")) {
                    for (String bccEmail : bccList) {
                        if (!isValidEmail(bccEmail)) {
                            JOptionPane.showMessageDialog(frame, "Email address at Bcc is not valid, enter again.", "Error", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    }
                }
                if (toList.get(0).equals("") && ccList.get(0).equals("") && bccList.get(0).equals("")) {
                    JOptionPane.showMessageDialog(frame, "Can not send without receiver. Check again", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String typeFolder = null;
                try {typeFolder = ftr(subject, content);} 
                catch (IOException | ParseException e1) {}
                try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket_s.getInputStream()));
                OutputStream outputStream = socket_s.getOutputStream();
                sendCommand(outputStream, "EHLO example.com");
                String response = reader.readLine();
                while (response != null) {
                    if (response.startsWith("250 ")) break;
                    response = reader.readLine();
                }           
                sendCommand(outputStream, "MAIL FROM: <" + username + ">");
                for (String toEmail : toList) {
                    sendCommand(outputStream, "RCPT TO: <" + toEmail + ">");
                }
                for (String ccEmail : ccList) {
                    sendCommand(outputStream, "RCPT TO: <" + ccEmail + ">");
                }
                for (String bccEmail : bccList) {
                    sendCommand(outputStream, "RCPT TO: <" + bccEmail + ">");
                }
                sendCommand(outputStream, "DATA");
                String boundary = "------------foms3E4p9a4ra75te5035dm";
                if (attachedFiles.isEmpty()) sendCommand(outputStream, "Content-Type: text/plain");
                else { 
                sendCommand(outputStream, "Content-Type: multipart/mixed; boundary=\"" + boundary + "\"" ); }
                sendCommand(outputStream, "Date: " + getCurrentDate());
                sendCommand(outputStream, "MIME-Version: 1.0");
                sendCommand(outputStream, "User-Agent: Unknown");
                sendCommand(outputStream, "Content-Language: en-US");
                sendCommand(outputStream, "To: " + String.join(", ", toList));
                if (ccList != null) sendCommand(outputStream, "Cc: " + String.join(", ", ccList));
                if (bccList != null) sendCommand(outputStream, "Bcc: " + String.join(", ", bccList));
                sendCommand(outputStream, "From: <" + senderName + "> " + username);
                sendCommand(outputStream, "Subject: " + subject);
                sendCommand(outputStream, "");
                sendCommand(outputStream, "Folder = " + typeFolder);
                if (!attachedFiles.isEmpty()) sendCommand(outputStream, "This is a multi-part message in MIME format");
                sendCommand(outputStream, "Content-Type: text/plain; charset=\"UTF-8\"; format=flowed");
                sendCommand(outputStream, "Content-Transfer-Encoding: 7bit");
                sendCommand(outputStream, "");
                sendCommand(outputStream, content);
                sendCommand(outputStream, "");
                sendCommand(outputStream, boundary);
                if (!attachedFiles.isEmpty()) {
                    for (int i = 0; i < attachedFiles.size(); i++) {
                        String fileName = getFileName(attachedFiles.get(i));
                        String type = getTypeFile(attachedFiles.get(i));
                        sendCommand(outputStream, "Content-Type: application/" + type + "; name=\"" + fileName +"\"");
                        sendCommand(outputStream, "Content-Dispotion: attachment; filename=\"" + fileName +"\"");
                        sendCommand(outputStream, "Content-Transfer-Encoding: base64");
                        sendCommand(outputStream, "");
                        sendFile(outputStream, attachedFiles.get(i));
                        sendCommand(outputStream, "");
                        sendCommand(outputStream, boundary);
                    }
                }
                sendCommand(outputStream, ".");
                sendCommand(outputStream, "QUIT");
                JOptionPane.showMessageDialog(frame, "Email sent successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                backButton.doClick();
                } catch(IOException e1) {e1.printStackTrace();}
            }
        });
    
        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    List<Integer> tm = new ArrayList<>();
                    showFolder("Inbox", tm);
                } catch (IOException e1) { e1.printStackTrace(); }
            }
        });
    
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(toLabel, gbc);
    
        gbc.gridx = 1;
        panel.add(toTextField, gbc);
    
        gbc.gridx = 2;
        panel.add(ccToggle, gbc);
    
        gbc.gridx = 3;
        panel.add(bccToggle, gbc);
    
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(ccLabel, gbc);
    
        gbc.gridx = 1;
        panel.add(ccTextField, gbc);
    
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(bccLabel, gbc);
    
        gbc.gridx = 1;
        panel.add(bccTextField, gbc);
    
        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(subjectLabel, gbc);
    
        gbc.gridx = 1;
        panel.add(subjectTextField, gbc);
    
        gbc.gridx = 0;
        gbc.gridy = 4;
        panel.add(contentLabel, gbc);
    
        gbc.gridx = 1;
        panel.add(contentScrollPane, gbc);
    
        gbc.gridx = 0;
        gbc.gridy = 5;
        panel.add(new JLabel(), gbc);
    
        gbc.gridy = 5;
        panel.add(attachButton, gbc);
        
        gbc.gridy = 6;
        panel.add(attachedFilePanel, gbc);
        
        gbc.gridy = 7;
        panel.add(sendButton, gbc);        
    
        gbc.gridx = 0;
        gbc.gridy = 8;
        panel.add(backButton, gbc);
    
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(ccPanel, gbc);
    
        gbc.gridx = 1;
        panel.add(bccPanel, gbc);
    
        return panel;
    }

    private void showSendMailPanel() {
        mainPanel.removeAll();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(sendMailPanel, BorderLayout.CENTER);
        frame.revalidate(); frame.repaint();
    }

    private List<Integer> g = new ArrayList<>();
    private boolean newMail = false;
    private void showFolder(String currentFolder, List<Integer> list) throws IOException {
        if (currentFolder.equals("Quit")) {
            System.exit(0);
            socket_s.close();
            socket_s.close();
        } mainPanel.removeAll();
        List<Integer> l = getFolder(socket_r, currentFolder);
        int lastEmailIndex = getLastEmailIndexV1(socket_r); boolean filterMode = true; 
        if (list.equals(l)) filterMode = false;
        if (list.isEmpty()) { 
            list = getFolder(socket_r, currentFolder); filterMode = false;
        }
        if (currentFolder.equals("All") && list.isEmpty()) {
            list.clear();
            for (int i = 1; i <= lastEmailIndex; i++) list.add(i);
        } 
        final List<Integer> listz = list; final boolean filterM = filterMode;
        List<Email> emailList = generateEmails(list);
        
        ScheduledExecutorService scheduler1 = Executors.newScheduledThreadPool(1);
        scheduler1.scheduleAtFixedRate(() -> {
            try { 
                System.out.println(newMail);
                Socket socket_t = new Socket(host, (int) port_r); 
               int ls = getLastEmailIndexV1(socket_t); 
               if (!g.contains(ls)) {
                System.out.println(ls);
                if (ls != -1) g.add(ls);
                if (g.size() < 2) newMail = false;
                if (g.size() >= 2) newMail = true;
                System.out.println(g.size());
               }
               List<Integer> getLas = new ArrayList<>(); getLas.add(g.get(g.size()-1));
               List<Email> emaile = generateEmails(getLas);
            //    if (newMail && !emaile.get(0).getStatus()) {
            //     JButton notificationButton = new JButton("You have a new mail");
            //     notificationButton.addActionListener(new ActionListener() {
            //         @Override
            //         public void actionPerformed(ActionEvent e) {
            //             newMail = false;
            //             try {
            //                 List<Integer> pro = new ArrayList<>();
            //                 showEmailDetailsDialog(emaile.get(0));
            //                 makeStatusMails(g.get(g.size()-1));
            //                 showFolder(currentFolder, pro);
            //             } catch (IOException e1) {
            //                 e1.printStackTrace();
            //             } 
            //         }
            //     });
                
            //     Box horizontalBox = Box.createHorizontalBox();
            //     horizontalBox.add(Box.createHorizontalGlue());
            //     horizontalBox.add(notificationButton);
            //     mainPanel.add(horizontalBox, BorderLayout.SOUTH);
            //    } 
               socket_t.close();
            } catch (IOException e) {
                System.out.println("err");
                e.printStackTrace();
            } 
        }, 0, autoload, TimeUnit.SECONDS);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            try {
                List<Integer> nList = getFolder(socket_r, currentFolder);
                if (nList.size() > listz.size() && !filterM) {
                    List<Integer> nullList = new ArrayList<>();
                    if (currentFolder.equals("Inbox")) 
                    showFolder(currentFolder, nullList);
                    // newMail = true;
                    scheduler.shutdown();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, 0, autoload, TimeUnit.SECONDS);

        JPanel folderContentPanel = new JPanel();
        folderContentPanel.setLayout(new BoxLayout(folderContentPanel, BoxLayout.Y_AXIS));
        Font monospacedFontFLB = new Font("Lucida Console", Font.BOLD, 12);
        Font monospacedFont = new Font("Lucida Console", Font.PLAIN, 12);
        JLabel Label = new JLabel("     Sender       Address                    Subject                                                                   Time\n");
        JLabel space = new JLabel(" ");
        Label.setFont(monospacedFontFLB); folderContentPanel.add(Label); folderContentPanel.add(space);
        
        for (int i = 0; i < emailList.size(); i++) {
            Email email = emailList.get(i);
            email.setIndex(i + 1);
            String formattedEmailInfo = concatenateStrings(email.getSender(), "<" + email.getAddress() + ">", email.getSubject(), email.getTime(), email.getStatus());
            JLabel emailLabel = new JLabel(formattedEmailInfo);
            emailLabel.setFont(monospacedFont);
            emailLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); 
            emailLabel.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent evt) {
                    if (SwingUtilities.isLeftMouseButton(evt)) {
                        try {
                            makeStatusMails(listz.get(email.getIndex()-1));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            showFolder(currentFolder, listz);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        showEmailDetailsDialog(email);
                    }
                }
            });
            folderContentPanel.add(emailLabel);
            folderContentPanel.add(Box.createRigidArea(new Dimension(0, 19)));
        }
        
        String[] folderNames = getFolderNames();
        folderComboBox = new JComboBox<>(folderNames);
        folderComboBox.setSelectedItem(currentFolder);
        folderComboBox.setRenderer(new FolderComboBoxRenderer());
        ((JLabel) folderComboBox.getRenderer()).setHorizontalAlignment(SwingConstants.CENTER);
        folderComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                List<Integer> listd = new ArrayList<>();
                String selectedFolder = (String) folderComboBox.getSelectedItem();
                try {
                    showFolder(selectedFolder, listd);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });

        JLabel folderNameLabel = new JLabel("Folder:");
        folderNameLabel.setFont(new Font("Lucida Console", Font.BOLD, 16));
        
        JTextField filterTextField = new JTextField();
        filterTextField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                
            }
        });

        JButton filterButton = new JButton("Filter");
        Font buttonFont = new Font("Arial", Font.BOLD, 50);
        filterButton.setFont(buttonFont);
        if (filterMode) filterButton.setBackground(new Color(152, 251, 152));
        if (!filterMode) {
            filterButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        showFilterOptions(listz, currentFolder, filterButton);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            });
        }
        else {
            filterButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                   List<Integer> list = new ArrayList<>();
                   try {
                    showFolder(currentFolder, list);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                }
            });
        }

        filterButton.setForeground(Color.BLACK);
        filterButton.setFont(new Font("Arial", Font.BOLD, 14));

        JButton composeButton = new JButton("Compose Email");
        composeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showSendMailPanel();
            }
        });

        composeButton.setForeground(Color.BLACK);
        composeButton.setFont(new Font("Arial", Font.BOLD, 14));
        composeButton.setBorder(BorderFactory.createEmptyBorder(70, 15, 70, 15));

        JPanel folderPanel = new JPanel(new BorderLayout());
        folderPanel.add(folderContentPanel, BorderLayout.CENTER);

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(folderNameLabel, BorderLayout.NORTH);
        leftPanel.add(folderComboBox, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new GridLayout(2, 1));
        buttonPanel.add(filterButton);
        buttonPanel.add(composeButton);
        leftPanel.add(buttonPanel, BorderLayout.SOUTH);

        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(leftPanel, BorderLayout.WEST);
        mainPanel.add(folderPanel, BorderLayout.CENTER);
         
        JButton notificationButton = new JButton("You have a new mail");
        ScheduledExecutorService scheduler2 = Executors.newScheduledThreadPool(1);
        scheduler2.scheduleAtFixedRate(() -> {
            if (newMail) {
                SwingUtilities.invokeLater(() -> {
                    notificationButton.setVisible(true);
                    mainPanel.add(notificationButton, BorderLayout.SOUTH);
                    frame.revalidate();
                    frame.repaint();
                    frame.setVisible(true);
                });
            scheduler2.shutdown();
        } 
        }, 0, autoload, TimeUnit.SECONDS);

        notificationButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    newMail = false;
                    try {
                        int lastEmailIndex = getLastEmailIndexV1(socket_r);
                        List<Integer> last = new ArrayList<>(); last.add(lastEmailIndex);
                        List<Email> lastMail = generateEmails(last);
                        List<Integer> lu = new ArrayList<>();
                        g.clear(); showFolder(currentFolder, lu);
                        showEmailDetailsDialog(lastMail.get(0));
                        makeStatusMails(lastEmailIndex);
                        notificationButton.setVisible(false);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            });
        frame.revalidate();
        frame.repaint();
    }

    

    
    private String selectedFilter;
    private String filterText;
    private void showFilterOptions(List<Integer> list, String currentFolder, Component component) throws IOException {
        JPopupMenu popupMenu = new JPopupMenu();
        
        JMenuItem addressItem = new JMenuItem("Address");
        addressItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectedFilter = "Address"; 
                filterText = showInputDialogWithValidation("Enter Address Filter:");
                List<Integer> listFilter = new ArrayList<>();
                try {
                    listFilter = filterEmails(selectedFilter, filterText, list);
                    if (listFilter.isEmpty()) {
                        JOptionPane.showMessageDialog(mainPanel, "No mails with that filter.");
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                try {
                    showFolder(currentFolder, listFilter);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });
    
        JMenuItem subjectItem = new JMenuItem("Subject");
        subjectItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectedFilter = "Subject"; 
                filterText = showInputDialogWithValidation("Enter Subject Filter:");
                List<Integer> listFilter = new ArrayList<>();
                try {
                    listFilter = filterEmails(selectedFilter, filterText, list);
                    if (listFilter.isEmpty()) {
                        JOptionPane.showMessageDialog(mainPanel, "No mails with that filter.");
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                try {
                    showFolder(currentFolder, listFilter);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });
    
        JMenuItem contentItem = new JMenuItem("Content");
        contentItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectedFilter = "Content"; 
                filterText = showInputDialogWithValidation("Enter Content Filter:");
                List<Integer> listFilter = new ArrayList<>();
                try {
                    listFilter = filterEmails(selectedFilter, filterText, list);
                     if (listFilter.isEmpty()) {
                        JOptionPane.showMessageDialog(mainPanel, "No mails with that filter.");
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                try {
                    showFolder(currentFolder, listFilter);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });
        
        popupMenu.add(addressItem);
        popupMenu.add(subjectItem);
        popupMenu.add(contentItem);
    
        int x = (component.getWidth() - popupMenu.getPreferredSize().width) / 2;
        int y = component.getHeight();
    
        popupMenu.show(component, x, y);
    }

    private List<Integer> filterEmails (String typeFilter, String fillText, List<Integer> list) throws IOException {
        mainPanel.removeAll();
        List<Email> emailList = generateEmails(list);
        List<Integer> filterList = new ArrayList<>(); int i = 0;
        for (Email email : emailList) {
            if (typeFilter.equals("Address")) {
                String add = email.getAddress();
                if (add != null) if (add.contains(fillText)) filterList.add(list.get(i));
            }

            if (typeFilter.equals("Subject")) {
                String sub = email.getSubject();
                if (sub != null) if (sub.contains(fillText)) filterList.add(list.get(i));
            }

            if (typeFilter.equals("Content")) {
                String cont = email.getContent();
                if (cont != null) if (cont.contains(fillText)) filterList.add(list.get(i));
            }
            i++;
        }
        return filterList;
    }
    
    
    private String showInputDialogWithValidation(String message) {
        String input = null; boolean validInput = false;
        while (!validInput) {
            input = JOptionPane.showInputDialog(frame, message);
            if (input == null || input.trim().isEmpty()) {
                int option = JOptionPane.showConfirmDialog(frame, "Please enter filter text. Do you want to retry?", "Error", JOptionPane.YES_NO_OPTION);
                if (option != JOptionPane.YES_OPTION) break;
            } else validInput = true;
        }
        return input;
    }
    
    private String[] getFolderNames() {
        return new String[]{"Inbox", "Project", "Work", "Important", "Spam", "All", "Quit"};
    }

    private List<Email> generateEmails(List<Integer> list) throws IOException {
        List<Email> emails = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
            List<String> getE = getInfoMailv1(list.get(i));
            List<List<String>> ccnbcc = getCcNBcc(list.get(i));
            String sender = getE.get(0);
            String address = getE.get(1);
            List<String> to = ccnbcc.get(0);
            List<String> cc = ccnbcc.get(1);
            List<String> bcc = ccnbcc.get(2);
            String subject = getE.get(2);
            String date = getE.get(3);
            String content = getE.get(4);
            boolean read = getStatusMails(list.get(i));
            List<String> listFile = getListFile(list.get(i));
            List<String> encodeList = getListEncode(list.get(i));
            Email email = new Email(sender, address, to, cc, bcc, subject, date, content, read, listFile, encodeList);
            emails.add(email);
        }
        return emails;
    }

    private static class Email {
        private String sender;
        private String address;
        private List<String> to;
        private List<String> cc;
        private List<String> bcc;
        private String subject;
        private String sentTime;
        private String content;
        private boolean read;
        private int index;
        private List<String> attachments;
        private List<String> encodeList;

        public Email(String sender, String address, List<String> to, List<String> cc, List<String> bcc, String subject, String sentTime, String content, boolean read, List<String> attachments, List<String> encodeList) {
            this.sender = sender;
            this.address = address;
            this.to = to;
            this.cc = cc;
            this.bcc = bcc;
            this.subject = subject;
            this.sentTime = sentTime;
            this.content = content;
            this.read = read;
            this.attachments = attachments;
            this.encodeList = encodeList;
        }

        @Override
        public String toString() {
            StringBuilder text = new StringBuilder("                                                                                                                                                                               ");
            text.insert(15, sender);
            text.insert(45, "<" + address + ">");
            text.insert(120, subject);
            text.insert(190, sentTime);
            return text.toString();
        }

        public String getSender() {
            return sender;
        }
    
        public String getAddress() {
            return address;
        }

        public List<String> getTo() {
            return to;
        }

        public List<String> getCc() {
            return cc;
        }

        public List<String> getBcc() {
            return bcc;
        }

        public String getSubject() {
            return subject;
        }
    
        public String getTime() {
            return sentTime;
        }

        public int getIndex() {
            return index;
        }
    
        public void setIndex(int index) {
            this.index = index;
        }

        public String getContent() {
            return content;
        }

        public boolean getStatus() {
            return read;
        }
    
        public List<String> getAttachments() {
            return attachments;
        }

        public List<String> getEncodeLists() {
            return encodeList;
        }
    }

    class FolderComboBoxRenderer extends JLabel implements ListCellRenderer<String> {
        public FolderComboBoxRenderer() {
            setOpaque(true);
        }
    
        @Override
        public Component getListCellRendererComponent(JList<? extends String> list,
                                                      String value, int index, boolean isSelected, boolean cellHasFocus) {
            setText(value);
    
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
    
            if (index == list.getSelectedIndex()) setIcon(UIManager.getIcon("ComboBox.arrowIcon"));
            else setIcon(null);
            return this;
        }
    }

    public class EmailDetailsDialog extends JDialog {

        public EmailDetailsDialog(JFrame parent, Email email) {
            super(parent, "Detail Email", true);

            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setBorder(BorderFactory.createEmptyBorder(27, 10, 10, 10)); 

            Font normalFont = new Font(Font.DIALOG, Font.PLAIN, 12);
            Font boldFont = new Font(Font.DIALOG, Font.BOLD, 12); 
            JLabel fromLabel = new JLabel("From: " + email.getSender() + "  <" + email.getAddress() + ">                                                                                At: " + email.getTime());
            fromLabel.setFont(normalFont);

            List<String> toList = email.getTo();
            String to = String.join(", ", toList);
            JLabel toLabel = new JLabel("To: " + to);
            toLabel.setFont(normalFont);
            
            List<String> ccList = email.getCc();
            String cc = String.join(", ", ccList);
            JLabel ccLabel = new JLabel("Cc: " + cc);
            ccLabel.setFont(normalFont);

            List<String> bccList = email.getBcc();
            String bcc = getValueIfStringExists(bccList, username);
            JLabel bccLabel = new JLabel("Bcc: " + bcc);
            bccLabel.setFont(normalFont);

            JLabel subjectLabel = new JLabel("Subject: " + email.getSubject());
            subjectLabel.setFont(normalFont);

            JLabel contentLabel = new JLabel("Content: ");
            contentLabel.setFont(boldFont);

            JLabel attachFileLabel = new JLabel("Attach File: ");
            attachFileLabel.setFont(boldFont);
            
            List<String> listEncodes = email.getEncodeLists();

            panel.add(fromLabel);
            panel.add(Box.createVerticalStrut(5)); 
            if (!to.isEmpty()) {
            panel.add(toLabel);
            panel.add(Box.createVerticalStrut(17));
            }
            if (!cc.equals("")) {
            panel.add(ccLabel);
            panel.add(Box.createVerticalStrut(17));
            }
            if (bcc != null) {
            panel.add(bccLabel);
            panel.add(Box.createVerticalStrut(17));
            }

            panel.add(subjectLabel);
            panel.add(Box.createVerticalStrut(30));
            panel.add(contentLabel);
            panel.add(Box.createVerticalStrut(5));

            JTextArea contentTextArea = new JTextArea(email.getContent());
            contentTextArea.setEditable(false); 
            List<String> attachmentNames = getAttachmentNames(email);
            JList<String> attachmentsList = new JList<>(attachmentNames.toArray(new String[0]));
            attachmentsList.setFont(normalFont);
            if (!attachmentNames.isEmpty()) {
                attachmentsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                attachmentsList.addListSelectionListener(new ListSelectionListener() {
                    @Override
                    public void valueChanged(ListSelectionEvent e) {
                        if (!e.getValueIsAdjusting()) {
                            int selectedIndex = attachmentsList.getSelectedIndex();
                            if (selectedIndex != -1) {
                                String selectedAttachment = attachmentsList.getSelectedValue();
                                int option = showCustomDialog(selectedAttachment);
                                if (option == 0) {
                                    String filePath = attachmentNames.get(selectedIndex);
                                    String encodedData = listEncodes.get(selectedIndex);
                                    try {
                                        byte[] decodedData = Base64.getDecoder().decode(encodedData.getBytes(StandardCharsets.UTF_8));
                                        String type = getFileExtension(filePath);
                                        File tempFile = File.createTempFile("tempFile", "." + type);
                                        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                                            fos.write(decodedData);
                                        }
                                        String os = System.getProperty("os.name").toLowerCase();
                                        ProcessBuilder processBuilder;
                                        if (os.contains("win")) {
                                            processBuilder = new ProcessBuilder("cmd", "/c", "start", tempFile.getAbsolutePath());
                                        } else if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
                                            processBuilder = new ProcessBuilder("xdg-open", tempFile.getAbsolutePath());
                                        } else {
                                            System.err.println("Unsupported platform.");
                                            return;
                                        }
                                        processBuilder.start();
                                    } catch (IOException e1) {
                                        e1.printStackTrace();
                                    }
                                } else if (option == 1) {
                                    JFileChooser fileChooser = new JFileChooser();
                                    fileChooser.setDialogTitle("Choose folder to save file");
                                    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                                    int result = fileChooser.showDialog(EmailDetailsDialog.this, "Choose");
                                    if (result == JFileChooser.APPROVE_OPTION) {
                                        File selectedDirectory = fileChooser.getSelectedFile();
                                        String path = selectedDirectory.getAbsolutePath() + '/' + attachmentNames.get(selectedIndex);
                                        downFile(listEncodes.get(selectedIndex), path);
                                        String message = "You saved file " + selectedAttachment + " to path: " + path;
                                        JOptionPane.showMessageDialog(EmailDetailsDialog.this, message);
                                    }
                                }
                            }
                        }
                    }
                });
                attachmentsList.setMaximumSize(new Dimension(Integer.MAX_VALUE - 30, Integer.MAX_VALUE));
            }

            panel.add(contentTextArea);
            panel.add(Box.createVerticalStrut(20));
            if (!attachmentNames.isEmpty()) {
            panel.add(attachFileLabel);
            panel.add(Box.createVerticalStrut(5));
            panel.add(attachmentsList);
            panel.add(Box.createVerticalStrut(5));
            }
            getContentPane().add(panel);
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

            int width = 780;
            int height = 400;
            setSize(width, height);
            setLocationRelativeTo(parent);
        }

        private int showCustomDialog(String selectedAttachment) {
            Object[] options = {"Preview", "Download"};
            return JOptionPane.showOptionDialog(
                    EmailDetailsDialog.this,
                    "Choose an action for file: " + selectedAttachment,
                    "File Options",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]);
        }
    }

    private List<String> getAttachmentNames(Email email) {
        List<String> listFile = email.getAttachments();
        return listFile;
    }

    private void showEmailDetailsDialog(Email email) {
        EmailDetailsDialog detailsDialog = new EmailDetailsDialog(frame, email);
        detailsDialog.setVisible(true);
    }

    public static String concatenateStrings(String str1, String str2, String str3, String str4, boolean read) {
        StringBuilder result = new StringBuilder("                                                                                                                                                        ");
        if (!read) result.insert(0, "***");
        result.insert(5, str1);
        result.insert(18, str2);
        result.insert(46, str3);
        result.insert(120, str4);
        return result.toString();
    }

     private static String getFileExtension(String filePath) {
        Path path = FileSystems.getDefault().getPath(filePath);
        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
    }

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    private static List<String> getInfoMailv1(int i) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket_r.getInputStream()));
        OutputStream outputStream = socket_r.getOutputStream();
        sendCommand(outputStream, "USER " + username);
        sendCommand(outputStream, "PASS " + password);
        sendCommand(outputStream, "STAT");
        sendCommand(outputStream, "RETR " + i);
        String subject = null, date = null, line, to = null;
        StringJoiner emailContent = new StringJoiner("\n");
        List<String> nAST = new ArrayList<>();
        while ((line = reader.readLine()) != null) {
            //System.out.println(line);
            if (line.contains("Date")) date = DateExtraction(line);
            if (line.contains("From")) nAST = NameAddressExtraction(line);
            if (line.contains("Subject")) subject = SubjectExtraction(line);
            emailContent.add(line);
            if (line.equals(".")) break;
            if (line.contains("To:")) to = extractRecipient(line);
        }
        nAST.add(subject); nAST.add(date);
        String emailBody = emailContent.toString();
        String content = getContentMail(emailBody, "Content-Transfer-Encoding: 7bit", "------------foms3E4p9a4ra75te5035dm");
        if (content == null) content = "";
        nAST.add(content); nAST.add(to);
        return nAST;
    }

    private static void sendCommand(OutputStream outputStream, String command) throws IOException {
        outputStream.write((command + "\r\n").getBytes());
        outputStream.flush();
    }

    private static Object getFJSON(String getWhat) throws IOException, ParseException  {
        String jsonFilePath_t = "src/all/config.json";
        String jsonFilePath = new File(jsonFilePath_t).getAbsolutePath();
        try (InputStream inputStream = new FileInputStream(jsonFilePath)) {
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
        }
        return -1;
    }

    private static String DateExtraction (String inputString) {
        String regex = "^Date: (.+?)(?:\\s\\+\\d{4})?$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(inputString);
        if (matcher.find()) {
            String dateTime = matcher.group(1).trim();
            return dateTime;
        } 
        return null;
    }

    private static ArrayList<String> NameAddressExtraction(String inputString) {
        ArrayList<String> nAA = new ArrayList<>();
        String regex = "^From: <(.+?)> (.+)$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(inputString);
        if (matcher.find()) {
            String name = matcher.group(1).trim();
            String address = matcher.group(2).trim();
            nAA.add(name); nAA.add(address);
            return nAA;
        } 
        return null;
    }

    private static String SubjectExtraction(String inputString) {
        String sj;
        String regex = "^Subject: (.+)$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(inputString);
        if (matcher.find()) {
            sj = matcher.group(1).trim();
            return sj;
        } 
        return null;
    }

    public static String extractFolderValue(String inputString) {
        String regex = "^Folder = (.+)$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(inputString);
        if (matcher.find()) {
            return matcher.group(1).trim();
        } return null;
    }

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

    private static int getLastEmailIndexV1(Socket socket_r) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket_r.getInputStream()));
        OutputStream outputStream = socket_r.getOutputStream();
        sendCommand(outputStream, "USER " + username);
        sendCommand(outputStream, "PASS " + password);
        sendCommand(outputStream, "STAT");
        String emailListResponse = getEmailList(outputStream, reader);
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


    private static List<Integer> getFolder(Socket socket_r, String type) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket_r.getInputStream()));
        OutputStream outputStream = socket_r.getOutputStream();
        sendCommand(outputStream, "USER " + username);
        sendCommand(outputStream, "PASS " + password);
        sendCommand(outputStream, "STAT"); 
        String emailListResponse = getEmailList(outputStream, reader); 
        List<Integer> list = new ArrayList<>();
        int lastEmailIndex = getLastEmailIndex(emailListResponse); String type_v = null;  
        for (int i = 1; i <= lastEmailIndex; i++) {
            sendCommand(outputStream, "RETR " + i);
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equals(".")) break;
                if (line.contains("Folder")) {
                    type_v = extractFolderValue(line);
                    if (type_v.equals(type)) list.add(i);
                }
            } 
        }
        Collections.sort(list, Collections.reverseOrder());
        return list;
    }  

    private static String getEmailList(OutputStream outputStream, BufferedReader reader) throws IOException {
        sendCommand(outputStream, "LIST");
        StringJoiner emailList = new StringJoiner("\n"); String line;
        while (!(line = reader.readLine()).equals(".")) {
            emailList.add(line);
        }
        return emailList.toString();
    }

    private static List<String> getListFile(int readIndexMail) throws IOException{
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket_r.getInputStream()));
        OutputStream outputStream = socket_r.getOutputStream();  
        sendCommand(outputStream, "USER " + username);
        sendCommand(outputStream, "PASS " + password);
        sendCommand(outputStream, "STAT");    
        sendCommand(outputStream, "RETR " + readIndexMail);
        StringJoiner emailContent = new StringJoiner("\n"); String line; String fileName;
        List<String> listFile = new ArrayList<>();
        boolean startReading = false;
        while ((line = reader.readLine()) != null) {
            if (line.equals(".")) break;
            if (startReading) emailContent.add(line);   
            if (line.contains("filename=")) {
                fileName = getListFileName(line);
                listFile.add(fileName);
            }
        }
        return listFile;
    }

    private static List<String> getListEncode(int readIndexMail) throws IOException{
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket_r.getInputStream()));
        OutputStream outputStream = socket_r.getOutputStream();  
        sendCommand(outputStream, "USER " + username);
        sendCommand(outputStream, "PASS " + password);
        sendCommand(outputStream, "STAT");
        sendCommand(outputStream, "RETR " + readIndexMail);
        List<String> emailContent = new ArrayList<>(); String line;
        while ((line = reader.readLine()) != null) {
            if (line.equals(".")) break;
            emailContent.add(line);
        }
        String emailBody = String.join("\n", emailContent);
        List<String> encodedList = getListEncoded(emailBody, "Content-Transfer-Encoding: base64", "------------foms3E4p9a4ra75te5035dm");
        return encodedList;
    }

    private static String getListFileName(String text) {
        String regex = "filename=\"([^\"]+)\"";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        String filename = null;
        while (matcher.find()) {
            filename = matcher.group(1);
        }
        return filename;
    }

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

    private static String getContentMail(String text, String startLine, String endLine) {
        String[] lines = text.split("\n");
        int reading = 0; boolean startReading = false;
        StringJoiner emailContent = new StringJoiner("\n");
        for (String line : lines) {
            if (line.contains(startLine)) {
                startReading = !startReading;
            }
            if (line.contains(endLine) && startReading) break;
            if (reading > 1) emailContent.add(line);
            if (startReading) reading++; 
        }
        String emailBody = emailContent.toString();
        return emailBody;
    }

    private static String getFileName(String inputString) {
        int lastDotIndex = inputString.lastIndexOf('\\');
        if (lastDotIndex != -1 && lastDotIndex < inputString.length() - 1) {
            return inputString.substring(lastDotIndex + 1);
        } else {
            return inputString;
        }
    }

    public static void downFile(String encodedData, String pathFolder) {
        byte[] decodedData = Base64.getDecoder().decode(encodedData);
        try (FileOutputStream fos = new FileOutputStream(pathFolder)) {
            fos.write(decodedData);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean isValidEmail(String email) {
        return email.matches("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
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

    private static String getCurrentDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");
        dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        return dateFormat.format(new Date());
    }

    private static String getTypeFile(String filePath) {
        int lastDotIndex = filePath.lastIndexOf('.');
        if (lastDotIndex != -1 && lastDotIndex < filePath.length() - 1) {
            return filePath.substring(lastDotIndex + 1);
        } else {
            return filePath;
        }
    }

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

    private static String getEncode(OutputStream outputStream, String filePath) throws IOException {
        byte[] fileContent = readFileContent(filePath);
        String encodedContent = Base64.getEncoder().encodeToString(fileContent);
        return encodedContent;
    }

    private static byte[] readFileContent(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        return Files.readAllBytes(path);
    }

    private static List<List<String>> getCcNBcc(int readIndexMail) throws IOException{
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket_r.getInputStream()));
        OutputStream outputStream = socket_r.getOutputStream();  
        sendCommand(outputStream, "USER " + username);
        sendCommand(outputStream, "PASS " + password);
        sendCommand(outputStream, "STAT");
        sendCommand(outputStream, "RETR " + readIndexMail);
        String line; List<List<String>>arrayOfLists = new ArrayList<>(); List<String> listTo = new ArrayList<>();
        List<String> listCc = new ArrayList<>(); List<String> listBcc = new ArrayList<>();
        while ((line = reader.readLine()) != null) {
            if (line.equals(".")) break;
            if (line.contains("To:")) {
                String emailsPart = line.replace("To: ", "");
                String[] stringArray = emailsPart.split(",\\s*");
                listTo = Arrays.asList(stringArray);
            }
            if (line.contains("Cc:")) {
                String emailsPart = line.replace("Cc: ", "");
                String[] stringArray = emailsPart.split(",\\s*");
                listCc = Arrays.asList(stringArray);
                
            }
              if (line.contains("Bcc:")) {
                String emailsPart = line.replace("Bcc: ", "");
                String[] stringArray = emailsPart.split(",\\s*");
                listBcc = Arrays.asList(stringArray);
            }
        }
        arrayOfLists.add(listTo); arrayOfLists.add(listCc); arrayOfLists.add(listBcc); 
        return arrayOfLists;
    }

    private static String extractRecipient(String inputString) {
        if (inputString.startsWith("To:")) {

            return inputString.substring("To:".length()).trim();
        } else {
            return null;
        }
    }

    private static String getValueIfStringExists(List<String> stringList, String targetString) {
        if (stringList.contains(targetString)) {
            return targetString;
        }
        return null;
    }
    
    private static boolean getStatusMails(int numberMail) {
        ArrayList<Integer> list = new ArrayList<>();
        String statusMails = "src/all/StatusMails.txt";
        String absolutePath = new File(statusMails).getAbsolutePath();
        list = getListTypeMails(absolutePath);
        if (list.contains(numberMail)) return true;
        return false;
    }

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

}
