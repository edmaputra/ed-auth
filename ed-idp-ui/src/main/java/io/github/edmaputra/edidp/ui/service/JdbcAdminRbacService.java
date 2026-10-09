package io.github.edmaputra.edidp.ui.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * JDBC implementation of {@link AdminRbacService}.
 *
 * @author edmaputra
 * @since 0.0.1
 */
@Service
public class JdbcAdminRbacService implements AdminRbacService {

  private final JdbcTemplate jdbcTemplate;

  public JdbcAdminRbacService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
  }

  @Override
  public List<PermissionSummary> listPermissions() {
    return listPermissions(null, null);
  }

  @Override
  public List<PermissionSummary> listPermissions(String tenantId, String category) {
    StringBuilder sql = new StringBuilder("""
        SELECT id, code, name, description, category, is_system_permission, tenant_id
        FROM iam_permissions
        WHERE 1=1
        """);
    List<Object> params = new ArrayList<>();
    if (tenantId != null && !tenantId.isBlank()) {
      sql.append(" AND (tenant_id = ? OR tenant_id IS NULL)");
      params.add(tenantId);
    }
    if (category != null && !category.isBlank()) {
      sql.append(" AND category = ?");
      params.add(category);
    }
    sql.append(" ORDER BY category ASC, code ASC");

    return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> new PermissionSummary(
        rs.getString("id"),
        rs.getString("code"),
        rs.getString("name"),
        rs.getString("description"),
        rs.getString("category"),
        rs.getBoolean("is_system_permission"),
        rs.getString("tenant_id")
    ), params.toArray());
  }

  @Override
  public Optional<PermissionSummary> getPermissionById(String id) {
    String sql = """
        SELECT id, code, name, description, category, is_system_permission, tenant_id
        FROM iam_permissions
        WHERE id = ?
        """;
    List<PermissionSummary> results = jdbcTemplate.query(sql, (rs, rowNum) -> new PermissionSummary(
        rs.getString("id"),
        rs.getString("code"),
        rs.getString("name"),
        rs.getString("description"),
        rs.getString("category"),
        rs.getBoolean("is_system_permission"),
        rs.getString("tenant_id")
    ), id);
    return results.stream().findFirst();
  }

  @Override
  public Optional<PermissionSummary> getPermissionByCode(String code) {
    String sql = """
        SELECT id, code, name, description, category, is_system_permission, tenant_id
        FROM iam_permissions
        WHERE code = ?
        """;
    List<PermissionSummary> results = jdbcTemplate.query(sql, (rs, rowNum) -> new PermissionSummary(
        rs.getString("id"),
        rs.getString("code"),
        rs.getString("name"),
        rs.getString("description"),
        rs.getString("category"),
        rs.getBoolean("is_system_permission"),
        rs.getString("tenant_id")
    ), code);
    return results.stream().findFirst();
  }

  @Override
  @Transactional
  public PermissionSummary createPermission(CreatePermissionCommand command) {
    String id = "perm-" + UUID.randomUUID().toString().substring(0, 8);
    long now = System.currentTimeMillis();
    jdbcTemplate.update("""
        INSERT INTO iam_permissions (id, code, name, description, category, is_system_permission, tenant_id, created_at, updated_at)
        VALUES (?, ?, ?, ?, ?, false, ?, ?, ?)
        """,
        id,
        command.code().trim(),
        command.name().trim(),
        command.description(),
        command.category().trim().toUpperCase(),
        command.tenantId(),
        now,
        now
    );
    return new PermissionSummary(
        id,
        command.code().trim(),
        command.name().trim(),
        command.description(),
        command.category().trim().toUpperCase(),
        false,
        command.tenantId()
    );
  }

  @Override
  @Transactional
  public PermissionSummary updatePermission(UpdatePermissionCommand command) {
    PermissionSummary existing = getPermissionById(command.id())
        .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + command.id()));
    if (existing.systemPermission()) {
      throw new IllegalStateException("System permissions cannot be modified");
    }
    long now = System.currentTimeMillis();
    jdbcTemplate.update("""
        UPDATE iam_permissions
        SET name = ?, description = ?, category = ?, updated_at = ?
        WHERE id = ? AND is_system_permission = false
        """,
        command.name().trim(),
        command.description(),
        command.category().trim().toUpperCase(),
        now,
        command.id()
    );
    return new PermissionSummary(
        existing.id(),
        existing.code(),
        command.name().trim(),
        command.description(),
        command.category().trim().toUpperCase(),
        false,
        existing.tenantId()
    );
  }

  @Override
  @Transactional
  public void deletePermission(String id) {
    PermissionSummary existing = getPermissionById(id)
        .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + id));
    if (existing.systemPermission()) {
      throw new IllegalStateException("System permissions cannot be deleted");
    }
    jdbcTemplate.update("DELETE FROM iam_permissions WHERE id = ? AND is_system_permission = false", id);
  }

  @Override
  public List<RoleSummary> listRoles() {
    String sql = """
        SELECT r.id, r.name, r.description, r.tenant_id, p.name AS perm_name
        FROM iam_roles r
        LEFT JOIN iam_role_permissions rp ON r.id = rp.role_id
        LEFT JOIN iam_permissions p ON rp.permission_id = p.id
        ORDER BY r.name ASC
        """;

    Map<String, RoleAggregation> map = new LinkedHashMap<>();
    jdbcTemplate.query(sql, rs -> {
      String id = rs.getString("id");
      String name = rs.getString("name");
      String desc = rs.getString("description");
      String tenantId = rs.getString("tenant_id");
      String perm = rs.getString("perm_name");

      RoleAggregation agg = map.computeIfAbsent(id, k -> new RoleAggregation(id, name, desc, tenantId));
      if (perm != null && !agg.permissions.contains(perm)) {
        agg.permissions.add(perm);
      }
    });

    List<RoleSummary> result = new ArrayList<>();
    for (RoleAggregation agg : map.values()) {
      result.add(new RoleSummary(agg.id, agg.name, agg.description, agg.tenantId, List.copyOf(agg.permissions)));
    }
    return result;
  }

  @Override
  public long countRoles() {
    Long count = jdbcTemplate.queryForObject("SELECT count(*) FROM iam_roles", Long.class);
    return count != null ? count : 0L;
  }

  @Override
  @Transactional
  public void createRole(CreateRoleCommand command) {
    String roleId = "role-" + UUID.randomUUID().toString().substring(0, 8);
    String roleName = command.name().trim();
    if (!roleName.startsWith("ROLE_")) {
      roleName = "ROLE_" + roleName.toUpperCase();
    }

    jdbcTemplate.update(
        "INSERT INTO iam_roles (id, name, description, tenant_id) VALUES (?, ?, ?, ?)",
        roleId,
        roleName,
        command.description(),
        command.tenantId()
    );

    if (command.permissionIds() != null && !command.permissionIds().isEmpty()) {
      for (String permId : command.permissionIds()) {
        jdbcTemplate.update(
            "INSERT INTO iam_role_permissions (role_id, permission_id) VALUES (?, ?)",
            roleId,
            permId
        );
      }
    }
  }

  @Override
  @Transactional
  public void deleteRole(String roleId, String tenantId) {
    jdbcTemplate.update("DELETE FROM iam_roles WHERE id = ? AND tenant_id = ?", roleId, tenantId);
  }

  @Override
  public Optional<RoleDetail> getRole(String id) {
    String sql = """
        SELECT r.id, r.name, r.description, r.tenant_id, rp.permission_id
        FROM iam_roles r
        LEFT JOIN iam_role_permissions rp ON r.id = rp.role_id
        WHERE r.id = ?
        """;
    List<String> permIds = new ArrayList<>();
    String[] meta = new String[4];
    boolean[] found = new boolean[1];

    jdbcTemplate.query(sql, rs -> {
      found[0] = true;
      if (meta[0] == null) {
        meta[0] = rs.getString("id");
        meta[1] = rs.getString("name");
        meta[2] = rs.getString("description");
        meta[3] = rs.getString("tenant_id");
      }
      String pId = rs.getString("permission_id");
      if (pId != null && !permIds.contains(pId)) {
        permIds.add(pId);
      }
    }, id);

    if (!found[0]) {
      return Optional.empty();
    }
    return Optional.of(new RoleDetail(meta[0], meta[1], meta[2], meta[3], List.copyOf(permIds)));
  }

  @Override
  @Transactional
  public void updateRole(UpdateRoleCommand command) {
    jdbcTemplate.update(
        "UPDATE iam_roles SET description = ? WHERE id = ?",
        command.description(),
        command.id()
    );
    jdbcTemplate.update("DELETE FROM iam_role_permissions WHERE role_id = ?", command.id());
    if (command.permissionIds() != null && !command.permissionIds().isEmpty()) {
      for (String permId : command.permissionIds()) {
        jdbcTemplate.update(
            "INSERT INTO iam_role_permissions (role_id, permission_id) VALUES (?, ?)",
            command.id(),
            permId
        );
      }
    }
  }

  @Override
  public List<GroupSummary> listGroups() {
    String sql = """
        SELECT g.id, g.name, g.description, g.tenant_id, r.name AS role_name,
               (SELECT count(*) FROM iam_group_members gm WHERE gm.group_id = g.id) AS member_count
        FROM iam_groups g
        LEFT JOIN iam_group_roles gr ON g.id = gr.group_id
        LEFT JOIN iam_roles r ON gr.role_id = r.id
        ORDER BY g.name ASC
        """;

    Map<String, GroupAggregation> map = new LinkedHashMap<>();
    jdbcTemplate.query(sql, rs -> {
      String id = rs.getString("id");
      String name = rs.getString("name");
      String desc = rs.getString("description");
      String tenantId = rs.getString("tenant_id");
      int memberCount = rs.getInt("member_count");
      String role = rs.getString("role_name");

      GroupAggregation agg = map.computeIfAbsent(id, k -> new GroupAggregation(id, name, desc, tenantId, memberCount));
      if (role != null && !agg.roles.contains(role)) {
        agg.roles.add(role);
      }
    });

    List<GroupSummary> result = new ArrayList<>();
    for (GroupAggregation agg : map.values()) {
      result.add(new GroupSummary(agg.id, agg.name, agg.description, agg.tenantId, List.copyOf(agg.roles), agg.memberCount));
    }
    return result;
  }

  @Override
  public long countGroups() {
    Long count = jdbcTemplate.queryForObject("SELECT count(*) FROM iam_groups", Long.class);
    return count != null ? count : 0L;
  }

  @Override
  @Transactional
  public void createGroup(CreateGroupCommand command) {
    String groupId = "group-" + UUID.randomUUID().toString().substring(0, 8);
    jdbcTemplate.update(
        "INSERT INTO iam_groups (id, name, description, tenant_id) VALUES (?, ?, ?, ?)",
        groupId,
        command.name().trim(),
        command.description(),
        command.tenantId()
    );

    if (command.roleIds() != null && !command.roleIds().isEmpty()) {
      for (String roleId : command.roleIds()) {
        jdbcTemplate.update(
            "INSERT INTO iam_group_roles (group_id, role_id) VALUES (?, ?)",
            groupId,
            roleId
        );
      }
    }
  }

  @Override
  @Transactional
  public void deleteGroup(String groupId, String tenantId) {
    jdbcTemplate.update("DELETE FROM iam_groups WHERE id = ? AND tenant_id = ?", groupId, tenantId);
  }

  @Override
  public Optional<GroupDetail> getGroup(String id) {
    String sql = """
        SELECT g.id, g.name, g.description, g.tenant_id, gr.role_id
        FROM iam_groups g
        LEFT JOIN iam_group_roles gr ON g.id = gr.group_id
        WHERE g.id = ?
        """;
    List<String> roleIds = new ArrayList<>();
    String[] meta = new String[4];
    boolean[] found = new boolean[1];

    jdbcTemplate.query(sql, rs -> {
      found[0] = true;
      if (meta[0] == null) {
        meta[0] = rs.getString("id");
        meta[1] = rs.getString("name");
        meta[2] = rs.getString("description");
        meta[3] = rs.getString("tenant_id");
      }
      String rId = rs.getString("role_id");
      if (rId != null && !roleIds.contains(rId)) {
        roleIds.add(rId);
      }
    }, id);

    if (!found[0]) {
      return Optional.empty();
    }
    return Optional.of(new GroupDetail(meta[0], meta[1], meta[2], meta[3], List.copyOf(roleIds)));
  }

  @Override
  @Transactional
  public void updateGroup(UpdateGroupCommand command) {
    jdbcTemplate.update(
        "UPDATE iam_groups SET description = ? WHERE id = ?",
        command.description(),
        command.id()
    );
    jdbcTemplate.update("DELETE FROM iam_group_roles WHERE group_id = ?", command.id());
    if (command.roleIds() != null && !command.roleIds().isEmpty()) {
      for (String roleId : command.roleIds()) {
        jdbcTemplate.update(
            "INSERT INTO iam_group_roles (group_id, role_id) VALUES (?, ?)",
            command.id(),
            roleId
        );
      }
    }
  }

  @Override
  @Transactional
  public void addGroupMember(String groupId, String username, String tenantId) {
    jdbcTemplate.update(
        """
        INSERT INTO iam_group_members (group_id, username, tenant_id)
        SELECT ?, ?, ?
        WHERE NOT EXISTS (SELECT 1 FROM iam_group_members WHERE group_id = ? AND username = ?)
        """,
        groupId, username, tenantId, groupId, username
    );
  }

  @Override
  @Transactional
  public void removeGroupMember(String groupId, String username) {
    jdbcTemplate.update(
        "DELETE FROM iam_group_members WHERE group_id = ? AND username = ?",
        groupId, username
    );
  }

  @Override
  public List<String> getUserGroupNames(String username) {
    String sql = """
        SELECT g.name
        FROM iam_groups g
        JOIN iam_group_members gm ON g.id = gm.group_id
        WHERE gm.username = ?
        ORDER BY g.name ASC
        """;
    return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("name"), username);
  }

  private static final class RoleAggregation {
    final String id;
    final String name;
    final String description;
    final String tenantId;
    final List<String> permissions = new ArrayList<>();

    RoleAggregation(String id, String name, String description, String tenantId) {
      this.id = id;
      this.name = name;
      this.description = description;
      this.tenantId = tenantId;
    }
  }

  private static final class GroupAggregation {
    final String id;
    final String name;
    final String description;
    final String tenantId;
    final int memberCount;
    final List<String> roles = new ArrayList<>();

    GroupAggregation(String id, String name, String description, String tenantId, int memberCount) {
      this.id = id;
      this.name = name;
      this.description = description;
      this.tenantId = tenantId;
      this.memberCount = memberCount;
    }
  }
}
