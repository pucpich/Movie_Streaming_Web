package com.mivestreaming.website;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * Entity đại diện cho người dùng trong hệ thống (MongoDB).
 * Hỗ trợ 2 vai trò: USER (mặc định) và ADMIN.
 */
@Document(collection = "users")
public class User {

    // ----------------------------- Enum Role -----------------------------

    /**
     * Phân quyền hệ thống:
     *   ROLE_USER  – người dùng thông thường
     *   ROLE_ADMIN – quản trị viên, có quyền CRUD phim
     */
    public enum Role {
        ROLE_USER,
        ROLE_ADMIN
    }

    // ----------------------------- Fields --------------------------------

    @Id
    private String id;

    @Field("full_name")
    private String fullName;

    @Field("email")
    private String email;

    @Field("password")
    private String password;

    /** Vai trò của user, mặc định là ROLE_USER */
    @Field("role")
    private Role role = Role.ROLE_USER;

    // ----------------------------- Getters / Setters ---------------------

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    // Tiện ích kiểm tra nhanh
    public boolean isAdmin() {
        return Role.ROLE_ADMIN.equals(this.role);
    }
}
