# Graph Report - .  (2026-04-18)

## Corpus Check
- Corpus is ~28,862 words - fits in a single context window. You may not need a graph.

## Summary
- 459 nodes · 728 edges · 39 communities detected
- Extraction: 75% EXTRACTED · 25% INFERRED · 0% AMBIGUOUS · INFERRED: 183 edges (avg confidence: 0.81)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- [[_COMMUNITY_Decision Models|Decision Models]]
- [[_COMMUNITY_Database Intents|Database Intents]]
- [[_COMMUNITY_Agent Platform Overview|Agent Platform Overview]]
- [[_COMMUNITY_AI Wiring|AI Wiring]]
- [[_COMMUNITY_Task API Controller|Task API Controller]]
- [[_COMMUNITY_Tool Interface|Tool Interface]]
- [[_COMMUNITY_Auth Endpoints|Auth Endpoints]]
- [[_COMMUNITY_Task Memory Store|Task Memory Store]]
- [[_COMMUNITY_User Entity|User Entity]]
- [[_COMMUNITY_Task Response DTO|Task Response DTO]]
- [[_COMMUNITY_Database Tool Runtime|Database Tool Runtime]]
- [[_COMMUNITY_MCP Client Helpers|MCP Client Helpers]]
- [[_COMMUNITY_Agent Properties|Agent Properties]]
- [[_COMMUNITY_JWT Security|JWT Security]]
- [[_COMMUNITY_SQL Builder Flow|SQL Builder Flow]]
- [[_COMMUNITY_Postgres MCP Server|Postgres MCP Server]]
- [[_COMMUNITY_Signup DTO|Signup DTO]]
- [[_COMMUNITY_Authentication Flow|Authentication Flow]]
- [[_COMMUNITY_Task Request DTO|Task Request DTO]]
- [[_COMMUNITY_Login DTO|Login DTO]]
- [[_COMMUNITY_Schema Discovery|Schema Discovery]]
- [[_COMMUNITY_Auth Token DTO|Auth Token DTO]]
- [[_COMMUNITY_Tool Result Model|Tool Result Model]]
- [[_COMMUNITY_Task Request Model|Task Request Model]]
- [[_COMMUNITY_Agent Exception Base|Agent Exception Base]]
- [[_COMMUNITY_Execution Errors|Execution Errors]]
- [[_COMMUNITY_SQL Read Guard|SQL Read Guard]]
- [[_COMMUNITY_Spring Boot Test|Spring Boot Test]]
- [[_COMMUNITY_CORS Wiring|CORS Wiring]]
- [[_COMMUNITY_Configuration Stack|Configuration Stack]]
- [[_COMMUNITY_Improvement Roadmap|Improvement Roadmap]]
- [[_COMMUNITY_Security Filter Chain|Security Filter Chain]]
- [[_COMMUNITY_Test Bootstrap|Test Bootstrap]]
- [[_COMMUNITY_Database Operation Enum|Database Operation Enum]]
- [[_COMMUNITY_Task Status Enum|Task Status Enum]]
- [[_COMMUNITY_Step Type Enum|Step Type Enum]]
- [[_COMMUNITY_CORS Docs|CORS Docs]]
- [[_COMMUNITY_Execute Request Docs|Execute Request Docs]]
- [[_COMMUNITY_Execute Response Docs|Execute Response Docs]]

## God Nodes (most connected - your core abstractions)
1. `ExecuteTaskResponse` - 16 edges
2. `AgentProperties` - 13 edges
3. `AgentExecutor` - 13 edges
4. `AgentTask` - 13 edges
5. `PostgresReadonlyMcpServer` - 12 edges
6. `McpDatabaseClient` - 12 edges
7. `getMessage()` - 12 edges
8. `README` - 12 edges
9. `Completion Report` - 12 edges
10. `DatabaseTool` - 11 edges

## Surprising Connections (you probably didn't know these)
- `Improved In-Memory Store` --semantically_similar_to--> `In-Memory Store`  [INFERRED] [semantically similar]
  src/main/java/com/researchagent/memory/InMemoryStoreImproved.java → ARCHITECTURE.md
- `AiAgentApplication` --conceptually_related_to--> `Autonomous AI Agent Platform`  [INFERRED]
  src/main/java/com/researchagent/AiAgentApplication.java → README.md
- `Insufficient Error Handling and Logging` --rationale_for--> `AgentExecutor`  [EXTRACTED]
  ARCHITECTURE_REVIEW.md → src/main/java/com/researchagent/agent/AgentExecutor.java
- `Hard-Coded Configuration` --rationale_for--> `AgentExecutor`  [EXTRACTED]
  ARCHITECTURE_REVIEW.md → src/main/java/com/researchagent/agent/AgentExecutor.java
- `Synchronous Tool Execution` --rationale_for--> `AgentExecutor`  [EXTRACTED]
  ARCHITECTURE_REVIEW.md → src/main/java/com/researchagent/agent/AgentExecutor.java

## Hyperedges (group relationships)
- **Agentic Execution Pipeline** — agent_controller, agent_service, agent_executor, task_agent, in_memory_store [EXTRACTED 0.95]
- **Documentation Suite** — readme_document, architecture_document, quick_reference_document [INFERRED 0.90]
- **Postgres Read-Only MCP Stack** — postgres_readonly_mcp_server, postgres_read_only_service, read_only_sql_guard [EXTRACTED 0.95]
- **Auth API Contract** — auth_controller, login_request, signup_request, auth_response [INFERRED 0.92]
- **Agent Error Model** — agent_exception, agent_error_code, task_execution_exception, tool_execution_exception [EXTRACTED 1.00]
- **Agent Configuration Stack** — agent_config, agent_properties, ai_config, cors_config [INFERRED 0.84]
- **Agent Tool Implementations** — agenttool, databasetool, emailtool, loggingtool [EXTRACTED 1.00]
- **Database Intent Validation Flow** — databaseintent, databaseintentvalidator, databaseschemaregistry, filtercondition [INFERRED 0.87]
- **SQL query construction flow** — sqlbuilder_build, sqlbuilder_buildCountQuery, sqlbuilder_buildListQuery, sqlbuilder_buildWhereClause, sqlbuilder_toSqlCondition, sqlbuilder_escapeSql [EXTRACTED 0.92]

## Communities

### Community 0 - "Decision Models"
Cohesion: 0.06
Nodes (7): AgentDecision, AgentExecutor, AgentStep, AgentTask, AiAgentApplication, TaskAgent, ToolExecutionException

### Community 1 - "Database Intents"
Cohesion: 0.07
Nodes (7): DatabaseIntent, DatabaseIntentValidator, DatabaseResultFormatter, DatabaseSchemaRegistry, FilterCondition, ReadOnlySqlGuardTest, SqlBuilder

### Community 2 - "Agent Platform Overview"
Cohesion: 0.13
Nodes (32): Agent API Endpoints, AgentController, AgentExecutor, AgentService, Agentic Loop, AiAgentApplication, AI Chatbot Landing Page, Architecture (+24 more)

### Community 3 - "AI Wiring"
Cohesion: 0.13
Nodes (5): AgentConfig, AiConfig, JsonSupport, PostgresReadonlyMcpServer, SecurityConfig

### Community 4 - "Task API Controller"
Cohesion: 0.1
Nodes (17): AgentController, Agent Decision, Agent Service, AgentService, Agent Step, Agent Step Type, Agent Task, Agent Task Status (+9 more)

### Community 5 - "Tool Interface"
Cohesion: 0.1
Nodes (8): AgentTool, Database Tool, Email Tool, EmailTool, Logging Tool, LoggingTool, MCP Database Client, Tool Result

### Community 6 - "Auth Endpoints"
Cohesion: 0.16
Nodes (3): getMessage(), AuthController, PostgresReadOnlyService

### Community 7 - "Task Memory Store"
Cohesion: 0.13
Nodes (4): InMemoryStore, CachedTask, InMemoryStoreImproved, TaskExecutionException

### Community 8 - "User Entity"
Cohesion: 0.18
Nodes (3): AppUser, AppUserRepository, AuthService

### Community 9 - "Task Response DTO"
Cohesion: 0.17
Nodes (1): ExecuteTaskResponse

### Community 10 - "Database Tool Runtime"
Cohesion: 0.23
Nodes (5): fromEnvironment(), optionalEnv(), parseInt(), parseSchemas(), DatabaseTool

### Community 11 - "MCP Client Helpers"
Cohesion: 0.26
Nodes (1): McpDatabaseClient

### Community 12 - "Agent Properties"
Cohesion: 0.14
Nodes (1): AgentProperties

### Community 13 - "JWT Security"
Cohesion: 0.25
Nodes (2): JwtAuthenticationFilter, JwtService

### Community 14 - "SQL Builder Flow"
Cohesion: 0.33
Nodes (10): DatabaseIntent, DatabaseOperation, FilterCondition, SqlBuilder, build(DatabaseIntent), buildCountQuery(DatabaseIntent, String), buildListQuery(DatabaseIntent, String), buildWhereClause(List<FilterCondition>) (+2 more)

### Community 15 - "Postgres MCP Server"
Cohesion: 0.42
Nodes (9): DatabaseConfig, JsonSupport, PostgreSQL Read-Only MCP Server README, PostgresReadOnlyService, PostgresReadonlyMcpServer, Read-Only PostgreSQL MCP Server, ReadOnlySqlGuard, ReadOnlySqlGuardTest (+1 more)

### Community 16 - "Signup DTO"
Cohesion: 0.25
Nodes (1): SignupRequest

### Community 17 - "Authentication Flow"
Cohesion: 0.39
Nodes (8): App User, App User Repository, Auth Controller, Auth Response, Auth Service, Jwt Service, Login Request, Signup Request

### Community 18 - "Task Request DTO"
Cohesion: 0.29
Nodes (1): ExecuteTaskRequest

### Community 19 - "Login DTO"
Cohesion: 0.33
Nodes (1): LoginRequest

### Community 20 - "Schema Discovery"
Cohesion: 0.4
Nodes (1): SchemaDiscoveryService

### Community 21 - "Auth Token DTO"
Cohesion: 0.4
Nodes (1): AuthResponse

### Community 22 - "Tool Result Model"
Cohesion: 0.4
Nodes (1): ToolResult

### Community 23 - "Task Request Model"
Cohesion: 0.5
Nodes (1): TaskRequest

### Community 24 - "Agent Exception Base"
Cohesion: 0.5
Nodes (1): AgentException

### Community 25 - "Execution Errors"
Cohesion: 0.83
Nodes (4): Agent Error Code, Agent Exception, Task Execution Exception, Tool Execution Exception

### Community 26 - "SQL Read Guard"
Cohesion: 0.67
Nodes (1): ReadOnlySqlGuard

### Community 27 - "Spring Boot Test"
Cohesion: 0.67
Nodes (1): AiAgentApplicationTests

### Community 28 - "CORS Wiring"
Cohesion: 0.67
Nodes (1): CorsConfig

### Community 29 - "Configuration Stack"
Cohesion: 1.0
Nodes (3): Agent Config, Agent Properties, AI Config

### Community 30 - "Improvement Roadmap"
Cohesion: 1.0
Nodes (2): Code Improvement Guide, Code Improvement Roadmap

### Community 31 - "Security Filter Chain"
Cohesion: 1.0
Nodes (2): JWT Authentication Filter, Security Config

### Community 32 - "Test Bootstrap"
Cohesion: 1.0
Nodes (1): AiAgentApplicationTests

### Community 33 - "Database Operation Enum"
Cohesion: 1.0
Nodes (0): 

### Community 34 - "Task Status Enum"
Cohesion: 1.0
Nodes (0): 

### Community 35 - "Step Type Enum"
Cohesion: 1.0
Nodes (0): 

### Community 36 - "CORS Docs"
Cohesion: 1.0
Nodes (1): CORS Config

### Community 37 - "Execute Request Docs"
Cohesion: 1.0
Nodes (1): Execute Task Request

### Community 38 - "Execute Response Docs"
Cohesion: 1.0
Nodes (1): Execute Task Response

## Knowledge Gaps
- **22 isolated node(s):** `Code Improvement Guide`, `Documentation Summary`, `AiAgentApplication`, `JsonSupport`, `ReadOnlySqlGuardTest` (+17 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **Thin community `Improvement Roadmap`** (2 nodes): `Code Improvement Guide`, `Code Improvement Roadmap`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Security Filter Chain`** (2 nodes): `JWT Authentication Filter`, `Security Config`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Test Bootstrap`** (2 nodes): `AiAgentApplicationTests`, `contextLoads()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Database Operation Enum`** (1 nodes): `DatabaseOperation.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Task Status Enum`** (1 nodes): `AgentTaskStatus.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Step Type Enum`** (1 nodes): `AgentStepType.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `CORS Docs`** (1 nodes): `CORS Config`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Execute Request Docs`** (1 nodes): `Execute Task Request`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Execute Response Docs`** (1 nodes): `Execute Task Response`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `AgentExecutor` connect `Decision Models` to `Task API Controller`?**
  _High betweenness centrality (0.074) - this node is a cross-community bridge._
- **Why does `getMessage()` connect `Auth Endpoints` to `MCP Client Helpers`, `Decision Models`, `Database Tool Runtime`, `AI Wiring`?**
  _High betweenness centrality (0.061) - this node is a cross-community bridge._
- **Why does `Tool Result` connect `Tool Interface` to `Task API Controller`?**
  _High betweenness centrality (0.057) - this node is a cross-community bridge._
- **What connects `Code Improvement Guide`, `Documentation Summary`, `AiAgentApplication` to the rest of the system?**
  _22 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Decision Models` be split into smaller, more focused modules?**
  _Cohesion score 0.06 - nodes in this community are weakly interconnected._
- **Should `Database Intents` be split into smaller, more focused modules?**
  _Cohesion score 0.07 - nodes in this community are weakly interconnected._
- **Should `Agent Platform Overview` be split into smaller, more focused modules?**
  _Cohesion score 0.13 - nodes in this community are weakly interconnected._