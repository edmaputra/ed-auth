package io.github.edmaputra.edidp.ui.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * JDBC-based implementation of {@link AdminUserService} for IAM operations.
 *
 * @author edmaputra
 * @since 0.0.1
 */
@Service
public class JdbcAdminUserService implements AdminUserService {

  private final JdbcTemplate jdbcTemplate;
  private final PasswordEncoder passwordEncoder;

  public JdbcAdminUserService(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
    this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
    this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "passwordEncoder must not be null");
  }

  @Override
  public List<AdminUserSummary> listUsers() {
    String sql = """
        SELECT u.username, u.enabled, COALESCE(u.tenant_id, 'demo') AS tenant_id,
               up.full_name, up.email, up.department, a.authority
        FROM users u
        LEFT JOIN user_profiles up ON u.username = up.username
        LEFT JOIN authorities a ON u.username = a.username
        ORDER BY u.username ASC
        """;

    Map<String, UserAggregation> map = new LinkedHashMap<>();

    jdbcTemplate.query(sql, rs -> {
      String username = rs.getString("username");
      boolean enabled = rs.getBoolean("enabled");
      String tenantId = rs.getString("tenant_id");
      String rawFullName = rs.getString("full_name");
      String fullName = rawFullName != null ? rawFullName : username;
      String rawEmail = rs.getString("email");
      String email = rawEmail != null ? rawEmail : username + "@example.com";
      String rawDepartment = rs.getString("department");
      String department = rawDepartment != null ? rawDepartment : "general";

      UserAggregation agg = map.computeIfAbsent(username, u -> new UserAggregation(
          username, enabled, tenantId, fullName, email, department
      ));

      String auth = rs.getString("authority");
      if (auth != null && !agg.roles.contains(auth)) {
        agg.roles.add(auth);
      }
    });

    // Populate assigned group names
    String groupSql = """
        SELECT gm.username, g.name AS group_name
        FROM iam_group_members gm
        JOIN iam_groups g ON gm.group_id = g.id
        """;
    jdbcTemplate.query(groupSql, rs -> {
      String username = rs.getString("username");
      String groupName = rs.getString("group_name");
      UserAggregation agg = map.get(username);
      if (agg != null && groupName != null && !agg.groups.contains(groupName)) {
        agg.groups.add(groupName);
      }
    });

    List<AdminUserSummary> result = new ArrayList<>();
    for (UserAggregation agg : map.values()) {
      result.add(new AdminUserSummary(
          agg.username,
          agg.enabled,
          agg.tenantId,
          agg.fullName,
          agg.email,
          List.copyOf(agg.roles),
          List.copyOf(agg.groups),
          agg.department
      ));
    }
    return result;
  }

  @Override
  public java.util.Optional<AdminUserSummary> getUser(String username) {
    if (username == null || username.isBlank()) {
      return java.util.Optional.empty();
    }
    return listUsers().stream()
        .filter(u -> u.username().equalsIgnoreCase(username))
        .findFirst();
  }

  @Override
  public long countUsers() {
    Long count = jdbcTemplate.queryForObject("SELECT count(*) FROM users", Long.class);
    return count != null ? count : 0L;
  }

  @Override
  @Transactional
  public void createUser(CreateUserCommand command) {
    String encodedPassword = passwordEncoder.encode(command.password());

    // 1. Insert into users table
    jdbcTemplate.update(
        "INSERT INTO users (username, password, enabled, tenant_id) VALUES (?, ?, true, ?)",
        command.username(),
        encodedPassword,
        command.tenantId()
    );

    // 2. Insert into authorities
    String authority = command.role().startsWith("ROLE_") ? command.role() : "ROLE_" + command.role();
    jdbcTemplate.update(
        "INSERT INTO authorities (username, authority, tenant_id) VALUES (?, ?, ?)",
        command.username(),
        authority,
        command.tenantId()
    );

    // 3. Insert into user_profiles
    jdbcTemplate.update(
        """
        INSERT INTO user_profiles (username, full_name, email, email_verified, locale, zoneinfo, department, tenant, updated_at)
        VALUES (?, ?, ?, true, 'en-US', 'UTC', ?, ?, ?)
        """,
        command.username(),
        command.fullName(),
        command.email(),
        command.department(),
        command.tenantId(),
        Instant.now().getEpochSecond()
    );
  }

  @Override
  @Transactional
  public void updateUser(UpdateUserCommand command) {
    // 1. Update users table (enabled, tenant_id)
    jdbcTemplate.update(
        "UPDATE users SET enabled = ?, tenant_id = ? WHERE username = ?",
        command.enabled(),
        command.tenantId(),
        command.username()
    );

    // 2. Update user_profiles table
    int updatedProfiles = jdbcTemplate.update(
        """
        UPDATE user_profiles
        SET full_name = ?, email = ?, department = ?, tenant = ?, updated_at = ?
        WHERE username = ?
        """,
        command.fullName(),
        command.email(),
        command.department(),
        command.tenantId(),
        Instant.now().getEpochSecond(),
        command.username()
    );

    if (updatedProfiles == 0) {
      jdbcTemplate.update(
          """
          INSERT INTO user_profiles (username, full_name, email, email_verified, locale, zoneinfo, department, tenant, updated_at)
          VALUES (?, ?, ?, true, 'en-US', 'UTC', ?, ?, ?)
          """,
          command.username(),
          command.fullName(),
          command.email(),
          command.department(),
          command.tenantId(),
          Instant.now().getEpochSecond()
      );
    }

    // 3. Update authorities & iam_user_roles
    jdbcTemplate.update("DELETE FROM authorities WHERE username = ?", command.username());
    jdbcTemplate.update("DELETE FROM iam_user_roles WHERE username = ?", command.username());

    for (String role : command.roles()) {
      String cleanRole = role.trim();
      String authority = cleanRole.startsWith("ROLE_") ? cleanRole : "ROLE_" + cleanRole;
      jdbcTemplate.update(
          "INSERT INTO authorities (username, authority, tenant_id) VALUES (?, ?, ?)",
          command.username(),
          authority,
          command.tenantId()
      );

      jdbcTemplate.update(
          """
          INSERT INTO iam_user_roles (username, role_id, tenant_id)
          SELECT ?, r.id, ? FROM iam_roles r WHERE r.name = ? OR r.id = ?
          """,
          command.username(),
          command.tenantId(),
          authority,
          cleanRole
      );
    }

    // 4. Update group memberships in iam_group_members
    jdbcTemplate.update("DELETE FROM iam_group_members WHERE username = ?", command.username());

    for (String grp : command.groups()) {
      String cleanGrp = grp.trim();
      jdbcTemplate.update(
          """
          INSERT INTO iam_group_members (group_id, username, tenant_id)
          SELECT g.id, ?, ? FROM iam_groups g WHERE g.id = ? OR g.name = ?
          """,
          command.username(),
          command.tenantId(),
          cleanGrp,
          cleanGrp
      );
    }

    // 5. Propagate group roles to authorities if any
    jdbcTemplate.update(
        """
        INSERT INTO authorities (username, authority, tenant_id)
        SELECT DISTINCT ?, r.name, ?
        FROM iam_group_roles gr
        JOIN iam_roles r ON gr.role_id = r.id
        WHERE gr.group_id IN (SELECT g.id FROM iam_groups g WHERE g.id IN (SELECT gm.group_id FROM iam_group_members gm WHERE gm.username = ?))
          AND NOT EXISTS (SELECT 1 FROM authorities a WHERE a.username = ? AND a.authority = r.name)
        """,
        command.username(),
        command.tenantId(),
        command.username(),
        command.username()
    );
  }

  @Override
  @Transactional
  public void setUserEnabled(String username, boolean enabled) {
    jdbcTemplate.update(
        "UPDATE users SET enabled = ? WHERE username = ?",
        enabled,
        username
    );
  }

  private static final class UserAggregation {
    final String username;
    final boolean enabled;
    final String tenantId;
    final String fullName;
    final String email;
    final String department;
    final List<String> roles = new ArrayList<>();
    final List<String> groups = new ArrayList<>();

    UserAggregation(String username, boolean enabled, String tenantId, String fullName, String email, String department) {
      this.username = username;
      this.enabled = enabled;
      this.tenantId = tenantId;
      this.fullName = fullName;
      this.email = email;
      this.department = department;
    }
  }
}
