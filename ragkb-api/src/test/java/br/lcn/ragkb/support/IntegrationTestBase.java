package br.lcn.ragkb.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base para testes de integração do ragkb.
 *
 * <p>Roda contra o PostgreSQL do docker-compose.yml (localhost:5432, banco
 * ragkb_test), não contra Testcontainers. A decisão foi tomada porque o
 * Testcontainers não consegue conectar ao named pipe do Docker Desktop 29.x
 * neste ambiente Windows (procura 'docker_engine', o Docker expõe
 * 'dockerDesktopLinuxEngine').</p>
 *
 * <p>Pré-requisitos para rodar:</p>
 * <ul>
 *   <li>docker compose up -d postgres</li>
 *   <li>Banco ragkb_test criado:
 *       docker exec -it &lt;container&gt; psql -U postgres -c "CREATE DATABASE ragkb_test;"</li>
 * </ul>
 *
 * <p>Custo desta escolha: os testes não são reproduzíveis em outra máquina sem
 * o compose rodando e o banco criado. Se o projeto ganhar CI, migrar para
 * Testcontainers ou para um serviço de banco no pipeline.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class IntegrationTestBase {
}