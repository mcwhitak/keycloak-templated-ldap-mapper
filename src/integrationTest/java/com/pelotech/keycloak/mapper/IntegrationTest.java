package com.pelotech.keycloak.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.SynchronizationResultRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

@Testcontainers
public class IntegrationTest {

    private static final Network network = Network.newNetwork();

    @Container
    private static final GenericContainer<?> ldap = new GenericContainer<>("quay.io/389ds/dirsrv:latest")
            .withCopyFileToContainer(MountableFile.forClasspathResource("/people.ldif"), "/etc/openldap/people.ldif")
            .withNetwork(network)
            .withNetworkAliases("ldap")
            .withExposedPorts(3389)
            .withEnv("DS_DM_PASSWORD", "password");

    @Container
    private static final GenericContainer<?> keycloak = new GenericContainer<>("quay.io/keycloak/keycloak:21.1.2")
            .withCopyFileToContainer(
                    MountableFile.forHostPath(System.getenv("JAR_FILE")),
                    "/opt/keycloak/providers/ldap-templated-mapper.jar")
            .withCommand("start-dev")
            .withNetwork(network)
            .withExposedPorts(8080)
            .withEnv("KEYCLOAK_ADMIN", "admin")
            .withEnv("KEYCLOAK_ADMIN_PASSWORD", "admin")
            .waitingFor(Wait.forHttp("/"));

    // Static reference to KC Client we set up for all test cases
    private static Keycloak kcClient;

    @BeforeAll
    public static void setup() throws IOException, InterruptedException {
        // Create root suffix
        ExecResult execResult = ldap.execInContainer(
                "dsconf",
                "-v",
                "localhost",
                "backend",
                "create",
                "--suffix",
                "dc=example,dc=com",
                "--be-name",
                "example",
                "--create-suffix");

        // Add in 'People' ou where users will reside
        execResult = ldap.execInContainer(
                "ldapadd",
                "-w",
                "password",
                "-v",
                "-H",
                "ldap://localhost:3389/",
                "-x",
                "-D",
                "cn=Directory Manager",
                "-f",
                "/etc/openldap/people.ldif");

        String kcUrl = "http://localhost:" + keycloak.getMappedPort(8080);

        kcClient = KeycloakBuilder.builder()
                .serverUrl(kcUrl)
                .realm("master")
                .clientId("admin-cli")
                .grantType(OAuth2Constants.PASSWORD)
                .username("admin")
                .password("admin")
                .build();

        RealmResource realmResource = kcClient.realm("master");
        RealmRepresentation realm = realmResource.toRepresentation();

        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        BiConsumer<String, String> putConfig = (key, value) -> {
            config.put(key, Arrays.asList(value));
        };

        putConfig.accept("enabled", "true");
        putConfig.accept("vendor", "other");
        putConfig.accept("connectionUrl", "ldap://ldap:3389");
        putConfig.accept("bindDn", "cn=Directory Manager");
        putConfig.accept("bindCredential", "password");
        putConfig.accept("startTls", "false");
        putConfig.accept("useTruststoreSpi", "ldapsOnly");
        putConfig.accept("connectionPooling", "false");
        putConfig.accept("authType", "simple");
        putConfig.accept("usersDn", "ou=People,dc=example,dc=com");
        putConfig.accept("usernameLDAPAttribute", "uid");
        putConfig.accept("rdnLDAPAttribute", "uid");
        putConfig.accept("uuidLDAPAttribute", "nsUniqueId");
        putConfig.accept("userObjectClass", "inetOrgPerson,organizationalPerson");
        putConfig.accept("customUserSearchFilter", "");
        putConfig.accept("readTimeout", "");
        putConfig.accept("editMode", "WRITABLE");
        putConfig.accept("searchScope", "");
        putConfig.accept("pagination", "false");
        putConfig.accept("batchSizeForSync", "");
        putConfig.accept("importEnabled", "true");
        putConfig.accept("syncRegistrations", "true");
        putConfig.accept("allowKerberosAuthentication", "false");
        putConfig.accept("useKerberosForPasswordAuthentication", "false");
        putConfig.accept("cachePolicy", "DEFAULT");
        putConfig.accept("usePasswordModifyExtendedOp", "false");
        putConfig.accept("validatePasswordPolicy", "false");
        putConfig.accept("trustEmail", "false");
        putConfig.accept("changedSyncPeriod", "-1");
        putConfig.accept("fullSyncPeriod", "-1");

        ComponentRepresentation ldapComponent = new ComponentRepresentation();
        ldapComponent.setId("ldap-user-federation");
        ldapComponent.setName("ldap");
        ldapComponent.setParentId(realm.getId());
        ldapComponent.setProviderId("ldap");
        ldapComponent.setProviderType("org.keycloak.storage.UserStorageProvider");
        ldapComponent.setConfig(config);

        realmResource.components().add(ldapComponent);

        UserRepresentation user = new UserRepresentation();
        user.setId("ssoTest");
        user.setUsername("ssoTest");
        user.setEmail("sso@test.com");
        user.setFirstName("sso");
        user.setLastName("test");
        user.setEnabled(true);

        realmResource.users().create(user);

        SynchronizationResultRepresentation res =
                realmResource.userStorage().syncUsers("ldap-user-federation", "triggerFullSync");

        assertEquals(1, res.getUpdated());
    }

    @Test
    void enabledMapper() throws IOException, InterruptedException {
        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        BiConsumer<String, String> putConfig = (key, value) -> {
            config.put(key, Arrays.asList(value));
        };

        putConfig.accept("user.model.attribute", "enabled");
        putConfig.accept("ldap.attribute", "nsaccountlock");
        putConfig.accept("read.only", "false");
        putConfig.accept("always.read.value.from.ldap", "false");
        putConfig.accept("is.mandatory.in.ldap", "false");
        putConfig.accept("is.binary.attribute", "false");
        putConfig.accept("attribute.default.value", "");

        // If lock=true, set enabled=false
        // If enabled=true, set lock=false
        putConfig.accept("from.ldap.template", "<#if ldapValue ==\"true\">false<#else>true</#if>");
        putConfig.accept("from.sso.template", "<#if ssoValue == \"true\">false<#else>true</#if>");

        ComponentRepresentation enabledMapper = new ComponentRepresentation();
        enabledMapper.setId("enabled-templated-mapper");
        enabledMapper.setName("enabledMapper");
        enabledMapper.setParentId("ldap-user-federation");
        enabledMapper.setProviderType("org.keycloak.storage.ldap.mappers.LDAPStorageMapper");
        enabledMapper.setProviderId("ldap-templated-mapper");
        enabledMapper.setConfig(config);

        RealmResource realmResource = kcClient.realm("master");
        realmResource.components().add(enabledMapper);

        // Lock account on LDAP side and watch it flow through to SSO
        ExecResult execResult = ldap.execInContainer(
                "dsidm",
                "localhost",
                "-b",
                "dc=example,dc=com",
                "account",
                "lock",
                "uid=ssoTest,ou=People,dc=example,dc=com");

        SynchronizationResultRepresentation res =
                realmResource.userStorage().syncUsers("ldap-user-federation", "triggerFullSync");

        UserRepresentation lockedUser = realmResource.users().search("ssoTest").get(0);
        assertFalse(lockedUser.isEnabled());

        // Enable account on SSO side and watch it flow through to unlock on LDAP side
        lockedUser.setEnabled(true);

        realmResource.users().get(lockedUser.getId()).update(lockedUser);

        res = realmResource.userStorage().syncUsers("ldap-user-federation", "triggerFullSync");

        execResult = ldap.execInContainer(
                "dsidm",
                "localhost",
                "-b",
                "dc=example,dc=com",
                "account",
                "unlock",
                "uid=ssoTest,ou=People,dc=example,dc=com");
        assertEquals("Error: Account is already active\n", execResult.getStderr());
    }

    @Test
    public void callAdminAPI() {
        // Sanity check to ensure response can be deserialized
        kcClient.serverInfo().getInfo();
    }

    private static String getResourceAsString(String filename) {
        try {
            Path path = Paths.get(
                    IntegrationTest.class.getClassLoader().getResource(filename).toURI());
            return new String(Files.readAllBytes(path));
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static String replaceVariable(String source, String name, String value) {
        return source.replaceAll("\\$\\{" + name + "\\}", value);
    }
}
