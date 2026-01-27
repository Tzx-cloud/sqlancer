package sqlancer.postgres.gen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Function;

import sqlancer.BaseConfigurationGenerator;
import sqlancer.MainOptions;
import sqlancer.Randomly;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.postgres.PostgresGlobalState;

public final class PostgresSetGenerator extends BaseConfigurationGenerator {

    @Override
    public ConfigurationAction[] getAllActions() {
        return Action.values();
    }

    private PostgresSetGenerator(Randomly r, MainOptions options) {
        super(r, options);
    }

    @Override
    protected String getDatabaseType() {
        return "postgres";
    }

    @Override
    protected String getActionName(Object action) {
        return ((Action)action).getName();
    }


    @Override
    public SQLQueryAdapter generateDefaultConfigForParameter(ConfigurationAction action) {
        StringBuilder sb = new StringBuilder();
        sb.append("SET ");
        sb.append(action.getName());
        sb.append(" = DEFAULT");

        return new SQLQueryAdapter(sb.toString());
    }

    @Override
    public SQLQueryAdapter generateConfigForParameter(ConfigurationAction action) {
        StringBuilder sb = new StringBuilder();
        sb.append("SET ");
        
        sb.append(action.getName());
        sb.append(" = ");
        sb.append(action.generateValue(r));

        return new SQLQueryAdapter(sb.toString());
    }

    private enum Action implements ConfigurationAction {
        ALLOW_IN_PLACE_TABLESPACES("allow_in_place_tablespaces", (r) -> Randomly.fromOptions("on", "off")), //
        //APPLICATION_NAME("application_name", (r) -> r.getString()), //
        ARRAY_NULLS("array_nulls", (r) -> Randomly.fromOptions("on", "off")), //
        BACKEND_FLUSH_AFTER("backend_flush_after", (r) -> r.getLongWithBoundaryBias(0, 256)), //
        BACKSLASH_QUOTE("backslash_quote", (r) -> Randomly.fromOptions("on", "off", "safe_encoding")), //
        //BACKTRACE_FUNCTIONS("backtrace_functions", (r) -> r.getString()), //
        BLOCK_SIZE("block_size", (r) -> r.getLongWithBoundaryBias(8192, 8192)), //
        BYTEA_OUTPUT("bytea_output", (r) -> Randomly.fromOptions("hex", "escape")), //
        CHECK_FUNCTION_BODIES("check_function_bodies", (r) -> Randomly.fromOptions("on", "off")), //
        CLIENT_CONNECTION_CHECK_INTERVAL("client_connection_check_interval", (r) -> r.getLongWithBoundaryBias(0, 2147483647)), //
        //CLIENT_ENCODING("client_encoding", (r) -> r.getString()), //
        CLIENT_MIN_MESSAGES("client_min_messages", (r) -> Randomly.fromOptions("debug5", "debug4", "debug3", "debug2", "debug1", "log", "notice", "warning", "error")), //
        COMMIT_DELAY("commit_delay", (r) -> r.getLongWithBoundaryBias(0, 100000)), //
        COMMIT_SIBLINGS("commit_siblings", (r) -> r.getLongWithBoundaryBias(0, 1000)), //
        COMPUTE_QUERY_ID("compute_query_id", (r) -> Randomly.fromOptions("on", "off", "auto")), //
        CONSTRAINT_EXCLUSION("constraint_exclusion", (r) -> Randomly.fromOptions("on", "off", "partition")), //
        CPU_INDEX_TUPLE_COST("cpu_index_tuple_cost", (r) -> r.getDoubleWithBoundaryBias(0,1.0)), //
        CPU_OPERATOR_COST("cpu_operator_cost", (r) -> r.getDoubleWithBoundaryBias(0,1.0)), //
        PU_TUPLE_COST("cpu_tuple_cost", (r) -> r.getDoubleWithBoundaryBias(0,1.0)), //
        //CREATEROLE_SELF_GRANT("createrole_self_grant", (r) -> r.getString()), //
        CURSOR_TUPLE_FRACTION("cursor_tuple_fraction", (r) -> r.getDoubleWithBoundaryBias(0,1.0)), //
        DATA_CHECKSUMS("data_checksums", (r) -> Randomly.fromOptions("on", "off")), //
        //DATESTYLE("datestyle", (r) -> r.getString()), //
        DEADLOCK_TIMEOUT("deadlock_timeout", (r) -> r.getLongWithBoundaryBias(1, 2147483647)), //
        DEBUG_ASSERTIONS("debug_assertions", (r) -> Randomly.fromOptions("on", "off")), //
        DEBUG_DISCARD_CACHES("debug_discard_caches", (r) -> r.getLongWithBoundaryBias(0, 1000)), //
        DEBUG_LOGICAL_REPLICATION_STREAMING("debug_logical_replication_streaming", (r) -> Randomly.fromOptions("buffered", "immediate")), //
        DEBUG_PARALLEL_QUERY("debug_parallel_query", (r) -> Randomly.fromOptions("on", "off", "always")), //
        DEBUG_PRETTY_PRINT("debug_pretty_print", (r) -> Randomly.fromOptions("on", "off")), //
        DEBUG_PRINT_PARSE("debug_print_parse", (r) -> Randomly.fromOptions("on", "off")), //
        DEBUG_PRINT_PLAN("debug_print_plan", (r) -> Randomly.fromOptions("on", "off")), //
        DEBUG_PRINT_REWRITTEN("debug_print_rewritten", (r) -> Randomly.fromOptions("on", "off")), //
        DEFAULT_STATISTICS_TARGET("default_statistics_target", (r) -> r.getLongWithBoundaryBias(1, 10000)), //
        //DEFAULT_TABLESPACE("default_tablespace", (r) -> r.getString()), //
        DEFAULT_TABLE_ACCESS_METHOD("default_table_access_method", (r) -> Randomly.fromOptions("heap")), //
        //DEFAULT_TEXT_SEARCH_CONFIG("default_text_search_config", (r) -> r.getString()), //
        DEFAULT_TOAST_COMPRESSION("default_toast_compression", (r) -> Randomly.fromOptions("pglz", "lz4")), //
        DEFAULT_TRANSACTION_DEFERRABLE("default_transaction_deferrable", (r) -> Randomly.fromOptions("on", "off")), //
        DEFAULT_TRANSACTION_ISOLATION("default_transaction_isolation", (r) -> Randomly.fromOptions("read uncommitted", "read committed", "repeatable read", "serializable")), //
        DEFAULT_TRANSACTION_READ_ONLY("default_transaction_read_only", (r) -> Randomly.fromOptions("on", "off")), //
        //DYNAMIC_LIBRARY_PATH("dynamic_library_path", (r) -> r.getString()), //
        EFFECTIVE_CACHE_SIZE("effective_cache_size", (r) -> r.getLongWithBoundaryBias(1, 2147483647)), //
        EFFECTIVE_IO_CONCURRENCY("effective_io_concurrency", (r) -> r.getLongWithBoundaryBias(0, 1000)), //
        ENABLE_ASYNC_APPEND("enable_async_append", (r) -> Randomly.fromOptions("on", "off")), //
        ENABLE_BITMAPSCAN("enable_bitmapscan", (r) -> Randomly.fromOptions("on", "off")), //
        ENABLE_GATHERMERGE("enable_gathermerge", (r) -> Randomly.fromOptions("on", "off")), //
        ENABLE_HASHAGG("enable_hashagg", (r) -> Randomly.fromOptions("on", "off")), //
        ENABLE_HASHJOIN("enable_hashjoin", (r) -> Randomly.fromOptions("on", "off")), //
        ENABLE_INCREMENTAL_SORT("enable_incremental_sort", (r) -> Randomly.fromOptions("on", "off")), //
        ENABLE_INDEXONLYSCAN("enable_indexonlyscan", (r) -> Randomly.fromOptions("on", "off")), //
        ENABLE_INDEXSCAN("enable_indexscan", (r) -> Randomly.fromOptions("on", "off")), //
        ENABLE_MATERIAL("enable_material", (r) -> Randomly.fromOptions("on", "off")), //
        ENABLE_MEMOIZE("enable_memoize", (r) -> Randomly.fromOptions("on", "off")), //
        ENABLE_MERGEJOIN("enable_mergejoin", (r) -> Randomly.fromOptions("on", "off")), //
        ENABLE_NESTLOOP("enable_nestloop", (r) -> Randomly.fromOptions("on", "off")), //
        ENABLE_PARALLEL_APPEND("enable_parallel_append", (r) -> Randomly.fromOptions("on", "off")), //
        ENABLE_PARALLEL_HASH("enable_parallel_hash", (r) -> Randomly.fromOptions("on", "off")), //
        ENABLE_PARTITIONWISE_AGGREGATE("enable_partitionwise_aggregate", (r) -> Randomly.fromOptions("on", "off")), //
        ENABLE_PARTITIONWISE_JOIN("enable_partitionwise_join", (r) -> Randomly.fromOptions("on", "off")), //
        ENABLE_PARTITION_PRUNING("enable_partition_pruning", (r) -> Randomly.fromOptions("on", "off")), //
        ENABLE_PRESORTED_AGGREGATE("enable_presorted_aggregate", (r) -> Randomly.fromOptions("on", "off")), //
        ENABLE_SEQSCAN("enable_seqscan", (r) -> Randomly.fromOptions("on", "off")), //
        ENABLE_SORT("enable_sort", (r) -> Randomly.fromOptions("on", "off")), //
        ENABLE_TIDSCAN("enable_tidscan", (r) -> Randomly.fromOptions("on", "off")), //
        ESCAPE_STRING_WARNING("escape_string_warning", (r) -> Randomly.fromOptions("on", "off")), //
        EVENT_TRIGGERS("event_triggers", (r) -> Randomly.fromOptions("on", "off")), //
        EXIT_ON_ERROR("exit_on_error", (r) -> Randomly.fromOptions("on", "off")), //
        EXTRA_FLOAT_DIGITS("extra_float_digits", (r) -> r.getLongWithBoundaryBias(-15, 3)), //
        FROM_COLLAPSE_LIMIT("from_collapse_limit", (r) -> r.getLongWithBoundaryBias(1, 50)), //
        GEQO("geqo", (r) -> Randomly.fromOptions("on", "off")), //
        GEQO_EFFORT("geqo_effort", (r) -> r.getLongWithBoundaryBias(1, 10)), //
        GEQO_GENERATIONS("geqo_generations", (r) -> r.getLongWithBoundaryBias(1, 2147483647)), //
        GEQO_POOL_SIZE("geqo_pool_size", (r) -> r.getLongWithBoundaryBias(0, 2147483647)), //
        GEQO_SEED("geqo_seed", (r) -> r.getDoubleWithBoundaryBias(0,1.0)), //
        GEQO_SELECTION_BIAS("geqo_selection_bias", (r) -> r.getDoubleWithBoundaryBias(0,1.0)), //
        GEQO_THRESHOLD("geqo_threshold", (r) -> r.getLongWithBoundaryBias(2, 50)), //
        GIN_FUZZY_SEARCH_LIMIT("gin_fuzzy_search_limit", (r) -> r.getLongWithBoundaryBias(0, 2147483647)), //
        GIN_PENDING_LIST_LIMIT("gin_pending_list_limit", (r) -> r.getLongWithBoundaryBias(64, 2147483647)), //
        HASH_MEM_MULTIPLIER("hash_mem_multiplier", (r) -> r.getDoubleWithBoundaryBias(0,1.0)), //
        ICU_VALIDATION_LEVEL("icu_validation_level", (r) -> Randomly.fromOptions("disabled", "check", "error")), //
        IDLE_IN_TRANSACTION_SESSION_TIMEOUT("idle_in_transaction_session_timeout", (r) -> r.getLongWithBoundaryBias(0, 2147483647)), //
        IDLE_SESSION_TIMEOUT("idle_session_timeout", (r) -> r.getLongWithBoundaryBias(0, 2147483647)), //
        IGNORE_CHECKSUM_FAILURE("ignore_checksum_failure", (r) -> Randomly.fromOptions("on", "off")), //
        IGNORE_SYSTEM_INDEXES("ignore_system_indexes", (r) -> Randomly.fromOptions("on", "off")), //
        INTEGER_DATETIMES("integer_datetimes", (r) -> Randomly.fromOptions("on", "off")), //
        INTERVALSTYLE("intervalstyle", (r) -> Randomly.fromOptions("postgres", "postgres_verbose", "sql_standard", "iso_8601")), //
        IN_HOT_STANDBY("in_hot_standby", (r) -> Randomly.fromOptions("on", "off")), //
        JIT("jit", (r) -> Randomly.fromOptions("on", "off")), //
        JIT_ABOVE_COST("jit_above_cost", (r) -> r.getDoubleWithBoundaryBias(-1.0, 1000000.0)), //
        JIT_DUMP_BITCODE("jit_dump_bitcode", (r) -> Randomly.fromOptions("on", "off")), //
        JIT_EXPRESSIONS("jit_expressions", (r) -> Randomly.fromOptions("on", "off")), //
        JIT_INLINE_ABOVE_COST("jit_inline_above_cost", (r) -> r.getDoubleWithBoundaryBias(-1.0, 1000000.0)), //
        JIT_OPTIMIZE_ABOVE_COST("jit_optimize_above_cost", (r) -> r.getDoubleWithBoundaryBias(-1.0, 1000000.0)), //
        JIT_TUPLE_DEFORMING("jit_tuple_deforming", (r) -> Randomly.fromOptions("on", "off")), //
        JOIN_COLLAPSE_LIMIT("join_collapse_limit", (r) -> r.getLongWithBoundaryBias(1, 50)), //
//        LC_MESSAGES("lc_messages", (r) -> r.getString()), //
//        LC_MONETARY("lc_monetary", (r) -> r.getString()), //
//        LC_NUMERIC("lc_numeric", (r) -> r.getString()), //
//        LC_TIME("lc_time", (r) -> r.getString()), //
       // LOCAL_PRELOAD_LIBRARIES("local_preload_libraries", (r) -> r.getString(), Scope.BACKEND), //
        LOCK_TIMEOUT("lock_timeout", (r) -> r.getLongWithBoundaryBias(0, 2147483647)), //
        LOGICAL_DECODING_WORK_MEM("logical_decoding_work_mem", (r) -> r.getLongWithBoundaryBias(64, Long.MAX_VALUE)), //
        LOG_CONNECTIONS("log_connections", (r) -> Randomly.fromOptions("on", "off")), //
        LOG_DISCONNECTIONS("log_disconnections", (r) -> Randomly.fromOptions("on", "off")), //
        LOG_DURATION("log_duration", (r) -> Randomly.fromOptions("on", "off")), //
        LOG_ERROR_VERBOSITY("log_error_verbosity", (r) -> Randomly.fromOptions("terse", "default", "verbose")), //
        LOG_EXECUTOR_STATS("log_executor_stats", (r) -> Randomly.fromOptions("on", "off")), //
        LOG_LOCK_WAITS("log_lock_waits", (r) -> Randomly.fromOptions("on", "off")), //
        LOG_MIN_DURATION_SAMPLE("log_min_duration_sample", (r) -> r.getLongWithBoundaryBias(-1, 2147483647)), //
        LOG_MIN_DURATION_STATEMENT("log_min_duration_statement", (r) -> r.getLongWithBoundaryBias(-1, 2147483647)), //
        LOG_MIN_ERROR_STATEMENT("log_min_error_statement", (r) -> Randomly.fromOptions("debug5", "debug4", "debug3", "debug2", "debug1", "info", "notice", "warning", "error", "log", "fatal", "panic")), //
        LOG_MIN_MESSAGES("log_min_messages", (r) -> Randomly.fromOptions("debug5", "debug4", "debug3", "debug2", "debug1", "info", "notice", "warning", "error", "log", "fatal", "panic")), //
        LOG_PARAMETER_MAX_LENGTH("log_parameter_max_length", (r) -> r.getLongWithBoundaryBias(-1, 2147483647)), //
        LOG_PARAMETER_MAX_LENGTH_ON_ERROR("log_parameter_max_length_on_error", (r) -> r.getLongWithBoundaryBias(-1, 2147483647)), //
        LOG_PARSER_STATS("log_parser_stats", (r) -> Randomly.fromOptions("on", "off")), //
        LOG_PLANNER_STATS("log_planner_stats", (r) -> Randomly.fromOptions("on", "off")), //
        LOG_REPLICATION_COMMANDS("log_replication_commands", (r) -> Randomly.fromOptions("on", "off")), //
        LOG_STATEMENT("log_statement", (r) -> Randomly.fromOptions("none", "ddl", "mod", "all")), //
        LOG_STATEMENT_SAMPLE_RATE("log_statement_sample_rate", (r) ->r.getDoubleWithBoundaryBias(0,1.0)), //
        LOG_STATEMENT_STATS("log_statement_stats", (r) -> Randomly.fromOptions("on", "off")), //
        LOG_TEMP_FILES("log_temp_files", (r) -> r.getLongWithBoundaryBias(-1, 2147483647)), //
        LOG_TRANSACTION_SAMPLE_RATE("log_transaction_sample_rate", (r) ->r.getDoubleWithBoundaryBias(0,1.0)), //
        LO_COMPAT_PRIVILEGES("lo_compat_privileges", (r) -> Randomly.fromOptions("on", "off")), //
        MAINTENANCE_IO_CONCURRENCY("maintenance_io_concurrency", (r) -> r.getLongWithBoundaryBias(0, 1000)), //
        MAINTENANCE_WORK_MEM("maintenance_work_mem", (r) -> r.getLongWithBoundaryBias(1024, Long.MAX_VALUE)), //
        MAX_FUNCTION_ARGS("max_function_args", (r) -> r.getLongWithBoundaryBias(100, 100)), //
        MAX_IDENTIFIER_LENGTH("max_identifier_length", (r) -> r.getLongWithBoundaryBias(63, 63)), // Read-only
        MAX_INDEX_KEYS("max_index_keys", (r) -> r.getLongWithBoundaryBias(32, 32)), // Read-only
        MAX_PARALLEL_MAINTENANCE_WORKERS("max_parallel_maintenance_workers", (r) -> r.getLongWithBoundaryBias(0, 1024)), //
        MAX_PARALLEL_WORKERS("max_parallel_workers", (r) -> r.getLongWithBoundaryBias(0, 1024)), //
        MAX_PARALLEL_WORKERS_PER_GATHER("max_parallel_workers_per_gather", (r) -> r.getLongWithBoundaryBias(0, 1024)), //
        MAX_STACK_DEPTH("max_stack_depth", (r) -> r.getLongWithBoundaryBias(100, 2097151)), //
        MIN_PARALLEL_INDEX_SCAN_SIZE("min_parallel_index_scan_size", (r) -> r.getLongWithBoundaryBias(0, 2147483647)), //
        MIN_PARALLEL_TABLE_SCAN_SIZE("min_parallel_table_scan_size", (r) -> r.getLongWithBoundaryBias(0, 2147483647)), //
        PARALLEL_LEADER_PARTITION("parallel_leader_participation", (r) -> Randomly.fromOptions("on", "off")), //
        PARALLEL_SETUP_COST("parallel_setup_cost", (r) -> r.getLongWithBoundaryBias(0, 1000000)), //
        PARALLEL_TUPLE_COST("parallel_tuple_cost", (r) ->r.getDoubleWithBoundaryBias(0,1.0)), //
        PASSWORD_ENCRYPTION("password_encryption", (r) -> Randomly.fromOptions("md5", "scram-sha-256")), //
        PLAN_CACHE_MODE("plan_cache_mode", (r) -> Randomly.fromOptions("auto", "force_generic_plan", "force_custom_plan")), //
        QUOTE_ALL_IDENTIFIERS("quote_all_identifiers", (r) -> Randomly.fromOptions("on", "off")), //
        RANDOM_PAGE_COST("random_page_cost", (r) -> r.getDoubleWithBoundaryBias(0,100.0)), //
        RECURSIVE_WORKTABLE_FACTOR("recursive_worktable_factor", (r) -> r.getDoubleWithBoundaryBias(1.0,1000.0)), //
        //RESTRICT_NONSYSTEM_RELATION_KIND("restrict_nonsystem_relation_kind", (r) -> r.getString()), //
        ROW_SECURITY("row_security", (r) -> Randomly.fromOptions("on", "off", "force")), //
        SCRAM_ITERATIONS("scram_iterations", (r) -> r.getLongWithBoundaryBias(4096, 2147483647)), //
       //  SEARCH_PATH("search_path", (r) -> r.getString()), //
        SEGMENT_SIZE("segment_size", (r) -> r.getLongWithBoundaryBias(1, 1024)), //
        SEQ_PAGE_COST("seq_page_cost", (r) ->r.getDoubleWithBoundaryBias(0,100.0)), //
//        SERVER_ENCODING("server_encoding", (r) -> r.getString()), //
//        SERVER_VERSION("server_version", (r) -> r.getString()), //
        SERVER_VERSION_NUM("server_version_num", (r) -> r.getLongWithBoundaryBias(100000, 200000)), //
        // SESSION_PRELOAD_LIBRARIES("session_preload_libraries", (r) -> r.getString()), //
        SESSION_REPLICATION_ROLE("session_replication_role", (r) -> Randomly.fromOptions("origin", "replica", "local")), //
        SHARED_MEMORY_SIZE("shared_memory_size", (r) -> r.getLongWithBoundaryBias(128, Long.MAX_VALUE)), //
        SHARED_MEMORY_SIZE_IN_HUGE_PAGES("shared_memory_size_in_huge_pages", (r) -> r.getLongWithBoundaryBias(0, Long.MAX_VALUE)), //
        //SSL_LIBRARY("ssl_library", (r) -> r.getString()), //
        STANDARD_CONFORMING_STRINGS("standard_conforming_strings", (r) -> Randomly.fromOptions("on", "off")), //
        STATEMENT_TIMEOUT("statement_timeout", (r) -> r.getLongWithBoundaryBias(0, 2147483647)), //
        STATS_FETCH_CONSISTENCY("stats_fetch_consistency", (r) -> Randomly.fromOptions("none", "cache", "snapshot")), //
        SYNCHRONIZE_SEQSCANS("synchronize_seqscans", (r) -> Randomly.fromOptions("on", "off")), //
        SYNCHRONOUS_COMMIT("synchronous_commit", (r) -> Randomly.fromOptions("on", "off", "local", "remote_write", "remote_apply")), //
        TCP_KEEPALIVES_COUNT("tcp_keepalives_count", (r) -> r.getLongWithBoundaryBias(0, 2147483647)), //
        TCP_KEEPALIVES_IDLE("tcp_keepalives_idle", (r) -> r.getLongWithBoundaryBias(0, 2147483647)), //
        TCP_KEEPALIVES_INTERVAL("tcp_keepalives_interval", (r) -> r.getLongWithBoundaryBias(0, 2147483647)), //
        TCP_USER_TIMEOUT("tcp_user_timeout", (r) -> r.getLongWithBoundaryBias(0, 2147483647)), //
        TEMP_BUFFERS("temp_buffers", (r) -> r.getLongWithBoundaryBias(100, 100000)), //
        TEMP_FILE_LIMIT("temp_file_limit", (r) -> r.getLongWithBoundaryBias(-1, 2147483647)), //
//        TEMP_TABLESPACES("temp_tablespaces", (r) -> r.getString()), //
//        TIMEZONE("timezone", (r) -> r.getString()), //
//        TIMEZONE_ABBREVIATIONS("timezone_abbreviations", (r) -> r.getString()), //
        TRACE_NOTIFY("trace_notify", (r) -> Randomly.fromOptions("on", "off")), //
        TRACE_SORT("trace_sort", (r) -> Randomly.fromOptions("on", "off")), //
        TRACK_ACTIVITIES("track_activities", (r) -> Randomly.fromOptions("on", "off")), //
        TRACK_COUNTS("track_counts", (r) -> Randomly.fromOptions("on", "off")), //
        TRACK_FUNCTIONS("track_functions", (r) -> Randomly.fromOptions("none", "pl", "all")), //
        TRACK_IO_TIMING("track_io_timing", (r) -> Randomly.fromOptions("on", "off")), //
        TRACK_WAL_IO_TIMING("track_wal_io_timing", (r) -> Randomly.fromOptions("on", "off")), //
        TRANSACTION_DEFERRABLE("transaction_deferrable", (r) -> Randomly.fromOptions("on", "off")), //
        TRANSACTION_ISOLATION("transaction_isolation", (r) -> Randomly.fromOptions("read uncommitted", "read committed", "repeatable read", "serializable")), //
        TRANSACTION_READ_ONLY("transaction_read_only", (r) -> Randomly.fromOptions("on", "off")), //
        TRANSFORM_NULL_EQUALS("transform_null_equals", (r) -> Randomly.fromOptions("on", "off")), //
        UPDATE_PROCESS_TITLE("update_process_title", (r) -> Randomly.fromOptions("on", "off")), //
        VACUUM_BUFFER_USAGE_LIMIT("vacuum_buffer_usage_limit", (r) -> r.getLongWithBoundaryBias(0, 1048576)), //
        VACUUM_COST_DELAY("vacuum_cost_delay", (r) -> r.getDoubleWithBoundaryBias(0,100.0)), //
        VACUUM_COST_LIMIT("vacuum_cost_limit", (r) -> r.getLongWithBoundaryBias(1, 10000)), //
        VACUUM_COST_PAGE_DIRTY("vacuum_cost_page_dirty", (r) -> r.getLongWithBoundaryBias(0, 10000)), //
        VACUUM_COST_PAGE_HIT("vacuum_cost_page_hit", (r) -> r.getLongWithBoundaryBias(0, 10000)), //
        VACUUM_COST_PAGE_MISS("vacuum_cost_page_miss", (r) -> r.getLongWithBoundaryBias(0, 10000)), //
        VACUUM_FREEZE_MIN_AGE("vacuum_freeze_min_age", (r) -> r.getLongWithBoundaryBias(0, 1000000000)), //
        VACUUM_FREEZE_TABLE_AGE("vacuum_freeze_table_age", (r) -> r.getLongWithBoundaryBias(0, 2000000000)), //
        VACUUM_MULTIXACT_FREEZE_MIN_AGE("vacuum_multixact_freeze_min_age", (r) -> r.getLongWithBoundaryBias(0, 1000000000)), //
        VACUUM_MULTIXACT_FREEZE_TABLE_AGE("vacuum_multixact_freeze_table_age", (r) -> r.getLongWithBoundaryBias(0, 2000000000)), //
        VACUUM_TRUNCATE("vacuum_truncate", (r) -> Randomly.fromOptions("on", "off")), //
        WAL_BLOCK_SIZE("wal_block_size", (r) -> r.getLongWithBoundaryBias(8192, 8192)), //
        WAL_COMPRESSION("wal_compression", (r) -> Randomly.fromOptions("on", "off", "pglz", "lz4", "zstd")), //
        //WAL_CONSISTENCY_CHECKING("wal_consistency_checking", (r) -> r.getString()), //
        WAL_SKIP_THRESHOLD("wal_skip_threshold", (r) -> r.getLongWithBoundaryBias(0, 2147483647)), //
        WORK_MEM("work_mem", (r) -> r.getLongWithBoundaryBias(64, Long.MAX_VALUE)), //
        XMLBINARY("xmlbinary", (r) -> Randomly.fromOptions("base64", "hex")), //
        XMLOPTION("xmloption", (r) -> Randomly.fromOptions("content", "document")), //
        ZERO_DAMAGED_PAGES("zero_damaged_pages", (r) -> Randomly.fromOptions("on", "off")); //


        private final GenericAction delegate;

        Action(String name, Function<Randomly, Object> prod) {
            this.delegate = new GenericAction(name, prod, Scope.GLOBAL);
        }
        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public Object generateValue(Randomly r) {
            return delegate.generateValue(r);
        }

        @Override
        public Scope[] getScopes() {
            return delegate.getScopes();
        }

        @Override
        public boolean canBeUsedInScope(Scope scope) {
            return delegate.canBeUsedInScope(scope);
        }
    }

    public static SQLQueryAdapter create(PostgresGlobalState globalState) {
        StringBuilder sb = new StringBuilder();
        ArrayList<Action> options = new ArrayList<>(Arrays.asList(Action.values()));
        //options.remove(Action.DEFAULT_WITH_OIDS);
        Action option = Randomly.fromList(options);
        sb.append("SET ");
        if (Randomly.getBoolean()) {
            sb.append(Randomly.fromOptions("SESSION", "LOCAL"));
            sb.append(" ");
        }
        sb.append(option.getName());
        sb.append("=");
        if (Randomly.getBoolean()) {
            sb.append("DEFAULT");
        } else {
            sb.append(option.generateValue(globalState.getRandomly()));
        }
        return new SQLQueryAdapter(sb.toString());
    }

}
