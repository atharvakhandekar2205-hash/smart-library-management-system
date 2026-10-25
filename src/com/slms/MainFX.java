package com.slms;

import javafx.application.Application;
import javafx.collections.*;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.animation.*;
import javafx.util.Duration;
import javafx.scene.chart.*;
import javafx.scene.Node;

import java.sql.*;

public class MainFX extends Application {

    BorderPane root = new BorderPane();
    VBox content = new VBox(20);

    ObservableList<Book> bookList = FXCollections.observableArrayList();
    ObservableList<Student> studentList = FXCollections.observableArrayList();

    @Override
    public void start(Stage stage) {

        // 🔹 NAVBAR
        HBox navbar = new HBox(30);
        navbar.setPadding(new Insets(15));
        navbar.setStyle("-fx-background-color: white;");

        Label title = new Label("Library Management System");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Button dash = topButton("Dashboard");
        Button books = topButton("Books");
        Button students = topButton("Members");
        Button issue = topButton("Issue Book");

        navbar.getChildren().addAll(title, dash, books, students, issue);

        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #f5f7fb;");

        root.setTop(navbar);
        root.setCenter(content);

        dash.setOnAction(e -> showDashboard());
        books.setOnAction(e -> showBooks());
        students.setOnAction(e -> showStudents());
        issue.setOnAction(e -> showIssue());

        showDashboard();

        stage.setScene(new Scene(root, 1200, 750));
        stage.setTitle("SLMS");
        stage.show();
    }

    // ================= NAV BUTTON =================
    Button topButton(String text) {
        Button btn = new Button(text);

        btn.setStyle("-fx-background-color: transparent; -fx-font-size: 14px;");

        btn.setOnMouseEntered(e ->
                btn.setStyle("-fx-background-color: #eef2ff; -fx-background-radius: 8;")
        );

        btn.setOnMouseExited(e ->
                btn.setStyle("-fx-background-color: transparent;")
        );

        return btn;
    }

    // ================= ANIMATION =================
    void animateContent() {
        FadeTransition ft = new FadeTransition(Duration.millis(300), content);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    // ================= DASHBOARD =================
    void showDashboard() {
        content.getChildren().clear();

        GridPane cards = new GridPane();
        cards.setHgap(20);

        cards.add(card("Total Books", String.valueOf(getCount("books"))), 0, 0);
        cards.add(card("Total Students", String.valueOf(getCount("students"))), 1, 0);

        LineChart<String, Number> lineChart = createLineChart();
        BarChart<String, Number> barChart = createBarChart();

        HBox charts = new HBox(20,
                wrapChart(lineChart, "Book Status"),
                wrapChart(barChart, "Books by Author")
        );

        content.getChildren().addAll(cards, charts);
        animateContent();
    }

    // ================= LINE CHART (DB) =================
    private LineChart<String, Number> createLineChart() {
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis();

        LineChart<String, Number> chart = new LineChart<>(x, y);
        XYChart.Series<String, Number> s = new XYChart.Series<>();

        try {
            Connection con = DBConnection.getConnection();

            ResultSet rs = con.createStatement().executeQuery(
                    "SELECT CASE WHEN available=true THEN 'Available' ELSE 'Issued' END AS status, COUNT(*) as total FROM books GROUP BY available"
            );

            while (rs.next()) {
                s.getData().add(new XYChart.Data<>(
                        rs.getString("status"),
                        rs.getInt("total")
                ));
            }

            con.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        chart.getData().add(s);
        return chart;
    }

    // ================= BAR CHART (DB) =================
    private BarChart<String, Number> createBarChart() {
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis();

        BarChart<String, Number> chart = new BarChart<>(x, y);
        XYChart.Series<String, Number> s = new XYChart.Series<>();

        try {
            Connection con = DBConnection.getConnection();

            ResultSet rs = con.createStatement().executeQuery(
                    "SELECT author, COUNT(*) as total FROM books GROUP BY author"
            );

            while (rs.next()) {
                s.getData().add(new XYChart.Data<>(
                        rs.getString("author"),
                        rs.getInt("total")
                ));
            }

            con.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        chart.getData().add(s);
        return chart;
    }

    // ================= CHART BOX =================
    private VBox wrapChart(Node chart, String title) {
        VBox box = new VBox(10);
        box.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 12;");

        Label t = new Label(title);
        t.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        box.getChildren().addAll(t, chart);
        return box;
    }

    // ================= BOOKS =================
    void showBooks() {
        content.getChildren().clear();

        TableView<Book> table = new TableView<>();

        TableColumn<Book, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<Book, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Book, String> authorCol = new TableColumn<>("Author");
        authorCol.setCellValueFactory(new PropertyValueFactory<>("author"));

        TableColumn<Book, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        table.getColumns().addAll(idCol, nameCol, authorCol, statusCol);

        loadBooks();
        table.setItems(bookList);

        TextField id = new TextField();
        id.setPromptText("Book ID");

        TextField name = new TextField();
        name.setPromptText("Book Name");

        TextField author = new TextField();
        author.setPromptText("Author");

        Button add = new Button("Add Book");

        add.setOnAction(e -> {
            try {
                Connection con = DBConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO books (id, name, author, available) VALUES (?, ?, ?, true)"
                );
                ps.setInt(1, Integer.parseInt(id.getText()));
                ps.setString(2, name.getText());
                ps.setString(3, author.getText());
                ps.executeUpdate();
                con.close();

                id.clear(); name.clear(); author.clear();
                loadBooks();

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        content.getChildren().addAll(table, id, name, author, add);
        animateContent();
    }

    void loadBooks() {
        try {
            bookList.clear();
            Connection con = DBConnection.getConnection();
            ResultSet rs = con.createStatement().executeQuery("SELECT * FROM books");

            while (rs.next()) {
                String status = rs.getBoolean("available") ? "Available" : "Issued";

                bookList.add(new Book(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("author"),
                        status
                ));
            }

            con.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= STUDENTS =================
    void showStudents() {
        content.getChildren().clear();

        TableView<Student> table = new TableView<>();

        TableColumn<Student, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<Student, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        table.getColumns().addAll(idCol, nameCol);

        loadStudents();
        table.setItems(studentList);

        TextField id = new TextField();
        id.setPromptText("Student ID");

        TextField name = new TextField();
        name.setPromptText("Student Name");

        Button add = new Button("Add Student");

        add.setOnAction(e -> {
            try {
                Connection con = DBConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO students VALUES (?, ?)"
                );
                ps.setInt(1, Integer.parseInt(id.getText()));
                ps.setString(2, name.getText());
                ps.executeUpdate();
                con.close();

                id.clear(); name.clear();
                loadStudents();

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        content.getChildren().addAll(table, id, name, add);
        animateContent();
    }

    void loadStudents() {
        try {
            studentList.clear();
            Connection con = DBConnection.getConnection();
            ResultSet rs = con.createStatement().executeQuery("SELECT * FROM students");

            while (rs.next()) {
                studentList.add(new Student(
                        rs.getInt("id"),
                        rs.getString("name")
                ));
            }

            con.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= ISSUE =================
    void showIssue() {
        content.getChildren().clear();

        TextField bookId = new TextField();
        bookId.setPromptText("Book ID");

        TextField studentId = new TextField();
        studentId.setPromptText("Student ID");

        Button issueBtn = new Button("Issue");

        Label result = new Label();

        issueBtn.setOnAction(e -> {
            String msg = BookDAO.issueBook(
                    Integer.parseInt(bookId.getText()),
                    Integer.parseInt(studentId.getText())
            );
            result.setText(msg);
            loadBooks();
        });

        content.getChildren().addAll(bookId, studentId, issueBtn, result);
    }

    // ================= COUNT =================
    int getCount(String table) {
        try {
            Connection con = DBConnection.getConnection();
            ResultSet rs = con.createStatement().executeQuery("SELECT COUNT(*) FROM " + table);
            rs.next();
            int count = rs.getInt(1);
            con.close();
            return count;
        } catch (Exception e) {
            return 0;
        }
    }

    // ================= CARD =================
    VBox card(String title, String value) {
        Label t = new Label(title);
        Label v = new Label(value);
        v.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        VBox box = new VBox(10, t, v);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1),10,0,0,2);");

        box.setOnMouseEntered(e -> scale(box, 1.05));
        box.setOnMouseExited(e -> scale(box, 1));

        return box;
    }

    void scale(VBox box, double val) {
        ScaleTransition st = new ScaleTransition(Duration.millis(150), box);
        st.setToX(val);
        st.setToY(val);
        st.play();
    }

    // ================= MODELS =================
    public static class Book {
        private int id;
        private String name;
        private String author;
        private String status;

        public Book(int id, String name, String author, String status) {
            this.id = id;
            this.name = name;
            this.author = author;
            this.status = status;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public String getAuthor() { return author; }
        public String getStatus() { return status; }
    }

    public static class Student {
        private int id;
        private String name;

        public Student(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() { return id; }
        public String getName() { return name; }
    }

    public static void main(String[] args) {
        launch();
    }
}