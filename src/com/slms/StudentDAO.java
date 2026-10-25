package com.slms;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class StudentDAO {

    public static void addStudent(int id, String name) {
        try {
            Connection con = DBConnection.getConnection();

            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO students VALUES (?, ?)"
            );
            ps.setInt(1, id);
            ps.setString(2, name);
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}