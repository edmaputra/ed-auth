package io.github.edmaputra.edidp.ui.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

/**
 * Tests for {@link JdbcAdminClientService} and {@link JdbcAdminTenantService}.
 *
 * @author edmaputra
 * @since 0.0.1
 */
class AdminServicesTests {

  private JdbcTemplate jdbcTemplate;
  private JdbcAdminClientService adminClientService;
  private JdbcAdminTenantService adminTenantService;
  private RegisteredClientRepository registeredClientRepository;
  private PasswordEncoder passwordEncoder;
  private TokenSettings tokenSettings;

  @BeforeEach
  void setUp() {
    jdbcTemplate = mock(JdbcTemplate.class);
    registeredClientRepository = mock(RegisteredClientRepository.class);
    passwordEncoder = mock(PasswordEncoder.class);
    tokenSettings = TokenSettings.builder().build();

    adminClientService = new JdbcAdminClientService(
        jdbcTemplate, registeredClientRepository, passwordEncoder, tokenSettings
    );
    adminTenantService = new JdbcAdminTenantService(jdbcTemplate);
  }

  @Test
  void countClients_returnsCountFromJdbc() {
    when(jdbcTemplate.queryForObject(eq("SELECT count(*) FROM oauth2_registered_client"), eq(Long.class)))
        .thenReturn(5L);

    assertThat(adminClientService.countClients()).isEqualTo(5L);
  }

  @Test
  void countTenants_returnsCountFromJdbc() {
    when(jdbcTemplate.queryForObject(
        eq("SELECT COUNT(DISTINCT COALESCE(tenant_id, 'demo')) FROM oauth2_registered_client"),
        eq(Long.class)))
        .thenReturn(2L);

    assertThat(adminTenantService.countTenants()).isEqualTo(2L);
  }

  @Test
  @SuppressWarnings("unchecked")
  void listClients_returnsListFromJdbc() {
    var dummyClient = new AdminClientService.AdminClientSummary(
        "id-1", "client-1", "Test Client", "client_secret_basic",
        "authorization_code", "openid,profile", "http://localhost/cb", "demo"
    );

    when(jdbcTemplate.query(any(String.class), any(RowMapper.class)))
        .thenReturn(List.of(dummyClient));

    List<AdminClientService.AdminClientSummary> results = adminClientService.listClients();
    assertThat(results).hasSize(1);
    assertThat(results.get(0).clientId()).isEqualTo("client-1");
  }

  @Test
  @SuppressWarnings("unchecked")
  void listTenants_returnsListFromJdbc() {
    var dummyTenant = new AdminTenantService.AdminTenantSummary("demo", 3L);

    when(jdbcTemplate.query(any(String.class), any(RowMapper.class)))
        .thenReturn(List.of(dummyTenant));

    List<AdminTenantService.AdminTenantSummary> results = adminTenantService.listTenants();
    assertThat(results).hasSize(1);
    assertThat(results.get(0).tenantId()).isEqualTo("demo");
    assertThat(results.get(0).clientCount()).isEqualTo(3L);
  }

  @Test
  void countUsers_returnsCountFromJdbc() {
    var passwordEncoder = mock(org.springframework.security.crypto.password.PasswordEncoder.class);
    var userService = new JdbcAdminUserService(jdbcTemplate, passwordEncoder);

    when(jdbcTemplate.queryForObject(eq("SELECT count(*) FROM users"), eq(Long.class)))
        .thenReturn(8L);

    assertThat(userService.countUsers()).isEqualTo(8L);
  }

  @Test
  void createUser_executesInserts() {
    var passwordEncoder = mock(org.springframework.security.crypto.password.PasswordEncoder.class);
    when(passwordEncoder.encode("secret")).thenReturn("hashed-secret");
    var userService = new JdbcAdminUserService(jdbcTemplate, passwordEncoder);

    var command = new AdminUserService.CreateUserCommand(
        "alice", "secret", "Alice Smith", "alice@example.com", "finance", "demo", "ROLE_USER"
    );

    userService.createUser(command);

    org.mockito.Mockito.verify(jdbcTemplate).update(
        eq("INSERT INTO users (username, password, enabled, tenant_id) VALUES (?, ?, true, ?)"),
        eq("alice"), eq("hashed-secret"), eq("demo")
    );
  }

  @Test
  void countRoles_returnsCountFromJdbc() {
    var rbacService = new JdbcAdminRbacService(jdbcTemplate);
    when(jdbcTemplate.queryForObject(eq("SELECT count(*) FROM iam_roles"), eq(Long.class)))
        .thenReturn(4L);

    assertThat(rbacService.countRoles()).isEqualTo(4L);
  }

  @Test
  void countGroups_returnsCountFromJdbc() {
    var rbacService = new JdbcAdminRbacService(jdbcTemplate);
    when(jdbcTemplate.queryForObject(eq("SELECT count(*) FROM iam_groups"), eq(Long.class)))
        .thenReturn(2L);

    assertThat(rbacService.countGroups()).isEqualTo(2L);
  }

  @Test
  void createRole_executesInserts() {
    var rbacService = new JdbcAdminRbacService(jdbcTemplate);
    var command = new AdminRbacService.CreateRoleCommand(
        "AUDITOR", "Auditor role", "demo", List.of("perm-clients-read")
    );

    rbacService.createRole(command);

    org.mockito.Mockito.verify(jdbcTemplate).update(
        eq("INSERT INTO iam_roles (id, name, description, tenant_id) VALUES (?, ?, ?, ?)"),
        any(String.class), eq("ROLE_AUDITOR"), eq("Auditor role"), eq("demo")
    );
  }

  @Test
  void updateUser_executesUpdatesAndRoleGroupAssignments() {
    var passwordEncoder = mock(org.springframework.security.crypto.password.PasswordEncoder.class);
    var userService = new JdbcAdminUserService(jdbcTemplate, passwordEncoder);

    var command = new AdminUserService.UpdateUserCommand(
        "alice", "Alice Updated", "alice.updated@example.com", "finance", "demo", true,
        List.of("ROLE_ADMIN"), List.of("Engineering")
    );

    userService.updateUser(command);

    // Verify user table update
    org.mockito.Mockito.verify(jdbcTemplate).update(
        eq("UPDATE users SET enabled = ?, tenant_id = ? WHERE username = ?"),
        eq(true), eq("demo"), eq("alice")
    );

    // Verify authority delete and insert
    org.mockito.Mockito.verify(jdbcTemplate).update(
        eq("DELETE FROM authorities WHERE username = ?"),
        eq("alice")
    );
    org.mockito.Mockito.verify(jdbcTemplate).update(
        eq("INSERT INTO authorities (username, authority, tenant_id) VALUES (?, ?, ?)"),
        eq("alice"), eq("ROLE_ADMIN"), eq("demo")
    );

    // Verify group members delete and insert
    org.mockito.Mockito.verify(jdbcTemplate).update(
        eq("DELETE FROM iam_group_members WHERE username = ?"),
        eq("alice")
    );
  }

  @Test
  void createPermission_executesInsertAndReturnsSummary() {
    var rbacService = new JdbcAdminRbacService(jdbcTemplate);
    var command = new AdminRbacService.CreatePermissionCommand(
        "reports:export", "Export Reports", "Allow export", "REPORTS", "demo"
    );

    var result = rbacService.createPermission(command);

    assertThat(result.code()).isEqualTo("reports:export");
    assertThat(result.name()).isEqualTo("Export Reports");
    assertThat(result.category()).isEqualTo("REPORTS");
    assertThat(result.systemPermission()).isFalse();
    assertThat(result.tenantId()).isEqualTo("demo");

    org.mockito.Mockito.verify(jdbcTemplate).update(
        eq("""
        INSERT INTO iam_permissions (id, code, name, description, category, is_system_permission, tenant_id, created_at, updated_at)
        VALUES (?, ?, ?, ?, ?, false, ?, ?, ?)
        """),
        any(String.class), eq("reports:export"), eq("Export Reports"), eq("Allow export"),
        eq("REPORTS"), eq("demo"), any(Long.class), any(Long.class)
    );
  }

  @Test
  @SuppressWarnings("unchecked")
  void updatePermission_whenCustom_updatesAndReturnsSummary() {
    var rbacService = new JdbcAdminRbacService(jdbcTemplate);
    var existing = new AdminRbacService.PermissionSummary(
        "perm-custom", "reports:export", "Export Reports", "Old desc", "REPORTS", false, "demo"
    );
    when(jdbcTemplate.query(any(String.class), any(RowMapper.class), eq("perm-custom")))
        .thenReturn(List.of(existing));

    var command = new AdminRbacService.UpdatePermissionCommand(
        "perm-custom", "Export Compliance Reports", "New desc", "REPORTS"
    );

    var updated = rbacService.updatePermission(command);

    assertThat(updated.name()).isEqualTo("Export Compliance Reports");
    org.mockito.Mockito.verify(jdbcTemplate).update(
        eq("""
        UPDATE iam_permissions
        SET name = ?, description = ?, category = ?, updated_at = ?
        WHERE id = ? AND is_system_permission = false
        """),
        eq("Export Compliance Reports"), eq("New desc"), eq("REPORTS"), any(Long.class), eq("perm-custom")
    );
  }

  @Test
  @SuppressWarnings("unchecked")
  void updatePermission_whenSystemPermission_throwsIllegalStateException() {
    var rbacService = new JdbcAdminRbacService(jdbcTemplate);
    var systemPerm = new AdminRbacService.PermissionSummary(
        "perm-clients-read", "clients:read", "View Clients", "Desc", "CLIENTS", true, null
    );
    when(jdbcTemplate.query(any(String.class), any(RowMapper.class), eq("perm-clients-read")))
        .thenReturn(List.of(systemPerm));

    var command = new AdminRbacService.UpdatePermissionCommand(
        "perm-clients-read", "Hacked View", "Hacked", "CLIENTS"
    );

    org.junit.jupiter.api.Assertions.assertThrows(
        IllegalStateException.class,
        () -> rbacService.updatePermission(command)
    );
  }

  @Test
  @SuppressWarnings("unchecked")
  void deletePermission_whenCustom_executesDelete() {
    var rbacService = new JdbcAdminRbacService(jdbcTemplate);
    var existing = new AdminRbacService.PermissionSummary(
        "perm-custom", "reports:export", "Export Reports", "Desc", "REPORTS", false, "demo"
    );
    when(jdbcTemplate.query(any(String.class), any(RowMapper.class), eq("perm-custom")))
        .thenReturn(List.of(existing));

    rbacService.deletePermission("perm-custom");

    org.mockito.Mockito.verify(jdbcTemplate).update(
        eq("DELETE FROM iam_permissions WHERE id = ? AND is_system_permission = false"),
        eq("perm-custom")
    );
  }

  @Test
  @SuppressWarnings("unchecked")
  void deletePermission_whenSystemPermission_throwsIllegalStateException() {
    var rbacService = new JdbcAdminRbacService(jdbcTemplate);
    var systemPerm = new AdminRbacService.PermissionSummary(
        "perm-clients-read", "clients:read", "View Clients", "Desc", "CLIENTS", true, null
    );
    when(jdbcTemplate.query(any(String.class), any(RowMapper.class), eq("perm-clients-read")))
        .thenReturn(List.of(systemPerm));

    org.junit.jupiter.api.Assertions.assertThrows(
        IllegalStateException.class,
        () -> rbacService.deletePermission("perm-clients-read")
    );
  }

  @Test
  void updateRole_executesUpdatesAndRolePermissions() {
    var rbacService = new JdbcAdminRbacService(jdbcTemplate);
    var command = new AdminRbacService.UpdateRoleCommand(
        "role-admin", "Administrator with updated scope", List.of("perm-clients-read", "perm-users-read")
    );

    rbacService.updateRole(command);

    org.mockito.Mockito.verify(jdbcTemplate).update(
        eq("UPDATE iam_roles SET description = ? WHERE id = ?"),
        eq("Administrator with updated scope"), eq("role-admin")
    );
    org.mockito.Mockito.verify(jdbcTemplate).update(
        eq("DELETE FROM iam_role_permissions WHERE role_id = ?"),
        eq("role-admin")
    );
    org.mockito.Mockito.verify(jdbcTemplate).update(
        eq("INSERT INTO iam_role_permissions (role_id, permission_id) VALUES (?, ?)"),
        eq("role-admin"), eq("perm-clients-read")
    );
    org.mockito.Mockito.verify(jdbcTemplate).update(
        eq("INSERT INTO iam_role_permissions (role_id, permission_id) VALUES (?, ?)"),
        eq("role-admin"), eq("perm-users-read")
    );
  }

  @Test
  void updateGroup_executesUpdatesAndGroupRoles() {
    var rbacService = new JdbcAdminRbacService(jdbcTemplate);
    var command = new AdminRbacService.UpdateGroupCommand(
        "group-eng", "Engineering team with devops role", List.of("role-user", "role-admin")
    );

    rbacService.updateGroup(command);

    org.mockito.Mockito.verify(jdbcTemplate).update(
        eq("UPDATE iam_groups SET description = ? WHERE id = ?"),
        eq("Engineering team with devops role"), eq("group-eng")
    );
    org.mockito.Mockito.verify(jdbcTemplate).update(
        eq("DELETE FROM iam_group_roles WHERE group_id = ?"),
        eq("group-eng")
    );
    org.mockito.Mockito.verify(jdbcTemplate).update(
        eq("INSERT INTO iam_group_roles (group_id, role_id) VALUES (?, ?)"),
        eq("group-eng"), eq("role-user")
    );
    org.mockito.Mockito.verify(jdbcTemplate).update(
        eq("INSERT INTO iam_group_roles (group_id, role_id) VALUES (?, ?)"),
        eq("group-eng"), eq("role-admin")
    );
  }
}
