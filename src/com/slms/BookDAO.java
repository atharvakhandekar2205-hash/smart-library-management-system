package com.slms;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BookDAO {

    // ================= ADD BOOK =================
    public static void addBook(int id, String name, String author) {
        try {
            Connection con = DBConnection.getConnection();

            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO books (id, name, author, available) VALUES (?, ?, ?, true)"
            );
            ps.setInt(1, id);
            ps.setString(2, name);
            ps.setString(3, author);
            ps.executeUpdate();

            con.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= GET BOOK LIST =================
    public static List<Book> getBooksList() {

        List<Book> list = new ArrayList<>();

        try {
            Connection con = DBConnection.getConnection();
            ResultSet rs = con.createStatement().executeQuery("SELECT * FROM books");

            while (rs.next()) {
                list.add(new Book(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("author")
                ));
            }

            con.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // ================= ISSUE BOOK =================
    public static String issueBook(int bookId, int studentId) {
        try {
            Connection con = DBConnection.getConnection();

            // Check availability
            PreparedStatement check = con.prepareStatement(
                    "SELECT * FROM books WHERE id=? AND available=true"
            );
            check.setInt(1, bookId);
            ResultSet rs = check.executeQuery();

            if (rs.next()) {

                // Set book unavailable
                PreparedStatement ps1 = con.prepareStatement(
                        "UPDATE books SET available=false WHERE id=?"
                );
                ps1.setInt(1, bookId);
                ps1.executeUpdate();

                // Insert issue record with due date (7 days)
                PreparedStatement ps2 = con.prepareStatement(
                        "INSERT INTO issue (book_id, student_id, issue_date, due_date, status) VALUES (?, ?, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 7 DAY), 'Issued')"
                );
                ps2.setInt(1, bookId);
                ps2.setInt(2, studentId);
                ps2.executeUpdate();

                con.close();
                return "Book Issued Successfully! Due in 7 days.";

            } else {
                con.close();
                return "Book not available!";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "Error!";
        }
    }

    // ================= RETURN BOOK =================
    public static String returnBook(int bookId) {
        try {
            Connection con = DBConnection.getConnection();

            PreparedStatement ps = con.prepareStatement(
                    "SELECT id, due_date FROM issue WHERE book_id=? AND status='Issued'"
            );
            ps.setInt(1, bookId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {

                int issueId = rs.getInt("id");
                Date dueDate = rs.getDate("due_date");
                Date today = new Date(System.currentTimeMillis());

                long diff = today.getTime() - dueDate.getTime();
                long daysLate = diff / (1000 * 60 * 60 * 24);

                double fine = 0;
                if (daysLate > 0) {
                    fine = daysLate * 5;  // ₹5 per day
                }

                // Update issue table
                PreparedStatement ps2 = con.prepareStatement(
                        "UPDATE issue SET return_date=?, fine=?, status='Returned' WHERE id=?"
                );
                ps2.setDate(1, today);
                ps2.setDouble(2, fine);
                ps2.setInt(3, issueId);
                ps2.executeUpdate();

                // Make book available again
                PreparedStatement ps3 = con.prepareStatement(
                        "UPDATE books SET available=true WHERE id=?"
                );
                ps3.setInt(1, bookId);
                ps3.executeUpdate();

                con.close();

                return "Book Returned Successfully! Fine = ₹" + fine;

            } else {
                con.close();
                return "No active issue record found!";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "Error!";
        }
    }

    // ================= CHECK FINE =================
    public static String checkFine(int bookId) {
        try {
            Connection con = DBConnection.getConnection();

            PreparedStatement ps = con.prepareStatement(
                    "SELECT fine, status FROM issue WHERE book_id=? ORDER BY id DESC LIMIT 1"
            );
            ps.setInt(1, bookId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                double fine = rs.getDouble("fine");
                String status = rs.getString("status");

                con.close();
                return "Status: " + status + " | Fine: ₹" + fine;
            } else {
                con.close();
                return "No record found!";
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "Error!";
    }
}